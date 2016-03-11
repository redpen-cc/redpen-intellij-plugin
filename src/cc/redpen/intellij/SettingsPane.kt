package cc.redpen.intellij

import cc.redpen.RedPenException
import cc.redpen.config.*
import com.intellij.configurationStore.save
import com.intellij.openapi.ui.Messages
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.*
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.event.PopupMenuEvent
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

open class SettingsPane(internal var provider: RedPenProvider) {
    open val configs: MutableMap<String, Configuration> = LinkedHashMap()
    internal var root = JPanel()
    internal var validators = JTable(createValidatorsModel())
    internal var symbols = JTable(createSymbolsModel())
    internal var language = JComboBox<String>()
    internal var exportButton = JButton("Export...")
    internal var importButton = JButton("Import...")
    internal var resetButton = JButton("Reset to defaults")
    internal var fileChooser = JFileChooser()
    internal var configurationExporter = ConfigurationExporter()
    internal var configurationLoader = ConfigurationLoader()

    init {
        cloneConfigs()
        fileChooser.fileFilter = FileNameExtensionFilter("RedPen Configuration", "xml")

        root.layout = GridLayoutManager(2, 7)
        root.add(JLabel("Language"), GridConstraints(0, 0, 1, 1, ANCHOR_WEST, FILL_NONE, SIZEPOLICY_FIXED, SIZEPOLICY_FIXED, null, null, null))
        root.add(language, GridConstraints(0, 1, 1, 1, ANCHOR_WEST, FILL_HORIZONTAL, SIZEPOLICY_FIXED, SIZEPOLICY_FIXED, null, null, null))
        root.add(Spacer(), GridConstraints(0, 3, 1, 1, ANCHOR_CENTER, FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_CAN_SHRINK, null, null, null))
        root.add(exportButton, GridConstraints(0, 5, 1, 1, ANCHOR_CENTER, FILL_HORIZONTAL, SIZEPOLICY_CAN_SHRINK or SIZEPOLICY_CAN_GROW, SIZEPOLICY_FIXED, null, null, null))
        root.add(importButton, GridConstraints(0, 4, 1, 1, ANCHOR_CENTER, FILL_HORIZONTAL, SIZEPOLICY_CAN_SHRINK or SIZEPOLICY_CAN_GROW, SIZEPOLICY_FIXED, null, null, null))
        root.add(resetButton, GridConstraints(0, 6, 1, 1, ANCHOR_CENTER, FILL_HORIZONTAL, SIZEPOLICY_CAN_SHRINK or SIZEPOLICY_CAN_GROW, SIZEPOLICY_FIXED, null, null, null))

        val tabbedPane = JTabbedPane()
        root.add(tabbedPane, GridConstraints(1, 0, 1, 7, ANCHOR_CENTER, FILL_BOTH, SIZEPOLICY_CAN_SHRINK or SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK or SIZEPOLICY_CAN_GROW, null, null, null))

        tabbedPane.addTab("Validators", JScrollPane(validators))
        validators.rowHeight = (validators.font.size * 1.5).toInt()
        validators.getDefaultEditor(String::class.java).addCellEditorListener(object: CellEditorListener {
            override fun editingCanceled(e: ChangeEvent?) {}
            override fun editingStopped(e: ChangeEvent?) = showValidatorPropertyErrorIfNeeded(e)
        })

        tabbedPane.addTab("Symbols", JScrollPane(symbols))
        symbols.rowHeight = (validators.font.size * 1.5).toInt()
    }

    internal fun showValidatorPropertyErrorIfNeeded(e: ChangeEvent?) {
        val text = (e?.source as CellEditor).cellEditorValue.toString()
        if (!isCorrectValidatorPropertiesFormat(text)) showValidatorPropertyError(text)
    }

    open internal fun showValidatorPropertyError(s: String) {
        Messages.showMessageDialog("Validator property must be in key=value format: " + s, "Invalid validator property format", Messages.getErrorIcon())
    }

    internal fun isCorrectValidatorPropertiesFormat(text: String): Boolean {
        return parseProperties(text) != null
    }

    open internal fun cloneConfigs() {
        provider.configs.forEach { e -> configs[e.key] = e.value.clone() }
    }

    val pane: JPanel
        get() {
            initLanguages()
            initTabs()
            initButtons()
            return root
        }

    open internal fun initButtons() {
        exportButton.addActionListener { a -> exportConfig() }
        importButton.addActionListener { a -> importConfig() }
        resetButton.addActionListener { a -> resetToDefaults() }
    }

    internal fun importConfig() {
        try {
            if (fileChooser.showOpenDialog(root) != APPROVE_OPTION) return
            config = configurationLoader.load(fileChooser.selectedFile)
            initTabs()
        } catch (e: RedPenException) {
            Messages.showMessageDialog("Cannot load: " + e.message, "RedPen", Messages.getErrorIcon())
        }
    }

    internal fun exportConfig() {
        try {
            if (fileChooser.showSaveDialog(root) != APPROVE_OPTION) return
            save()
            configurationExporter.export(config, FileOutputStream(fileChooser.selectedFile))
        } catch (e: IOException) {
            Messages.showMessageDialog("Cannot write to file: " + e.message, "RedPen", Messages.getErrorIcon())
        }

    }

    open internal fun initLanguages() {
        provider.configs.keys.forEach { k -> language.addItem(k) }
        language.selectedItem = provider.activeConfig.key
        language.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                ensureTableEditorsStopped()
                applyChanges()
            }
        })
        language.addActionListener { a -> initTabs() }
    }

    open internal fun initTabs() {
        initValidators()
        initSymbols()
    }

    open internal fun initSymbols() {
        symbols.model = createSymbolsModel()
        symbols.columnModel.getColumn(0).minWidth = 250
        symbols.setDefaultEditor(Char::class.javaObjectType, SingleCharEditor())

        val symbolTable = config.symbolTable
        for (key in symbolTable.names) {
            val symbol = symbolTable.getSymbol(key)
            (symbols.model as DefaultTableModel).addRow(arrayOf(symbol.type.toString(), symbol.value, String(symbol.invalidChars), symbol.isNeedBeforeSpace, symbol.isNeedAfterSpace))
        }

        symbols.doLayout()
    }

    open internal fun initValidators() {
        validators.model = createValidatorsModel()
        validators.columnModel.getColumn(0).maxWidth = 20

        val validatorConfigs = config.validatorConfigs.groupByName()
        for (config in combinedValidatorConfigs()) {
            (validators.model as DefaultTableModel).addRow(arrayOf(validatorConfigs.containsKey(config.key), config.key, config.value.asString()))
        }

        validators.doLayout()
    }

    private fun ValidatorConfiguration.asString() = properties.entries.joinToString("; ")

    fun List<ValidatorConfiguration>.groupByName(): Map<String, ValidatorConfiguration> {
        return associateBy({ it.configurationName }, { it })
    }

    fun combinedValidatorConfigs() = provider.initialConfigs[config.key]!!.validatorConfigs.groupByName() + config.validatorConfigs.groupByName()

    open fun getEditedValidators(): List<ValidatorConfiguration> {
        val result = ArrayList<ValidatorConfiguration>()
        val model = validators.model
        val allConfigs = combinedValidatorConfigs()

        val editorText = ((validators.cellEditor as? DefaultCellEditor)?.component as? JTextField)?.text
        val editingRow = validators.editingRow
        fun editableValueAt(i: Int) = if (editingRow == i && editorText != null) editorText else model.getValueAt(i, 2) as String

        for (i in 0..model.rowCount-1) {
            if (model.getValueAt(i, 0) as Boolean) {
                val validator = allConfigs[model.getValueAt(i, 1)]!!.clone()
                validator.properties.clear()
                parseProperties(editableValueAt(i))?.forEach { validator.addProperty(it.key, it.value) }
                result.add(validator)
            }
        }
        return result
    }

    fun parseProperties(text: String): Map<String, String>? {
        return text.split(";\\s*".toRegex()).filter { it.isNotEmpty() }.map { it.split("=".toRegex(), 2) }.associate {
            if (it.size < 2 || it[0].isEmpty()) return null
            it[0].trim() to it[1]
        }
    }

    open fun getEditedSymbols(): List<Symbol> {
        val model = symbols.model

        val editorText = ((symbols.cellEditor as? DefaultCellEditor)?.component as? JTextField)?.text
        val editingRow = symbols.editingRow
        val editingColumn = symbols.editingColumn
        fun editableValueAt(i: Int, j: Int) = if (editorText != null && editingRow == i && editingColumn == j) editorText
                else model.getValueAt(i, j).toString()

        return (0..model.rowCount-1).map { i ->
            Symbol(SymbolType.valueOf(model.getValueAt(i, 0) as String), editableValueAt(i, 1)[0], editableValueAt(i, 2),
                   model.getValueAt(i, 3) as Boolean, model.getValueAt(i, 4) as Boolean)
        }
    }

    open internal fun applyValidatorsChanges() {
        val validators = config.validatorConfigs
        val editedValidators = getEditedValidators()
        validators.clear()
        validators.addAll(editedValidators)
    }

    open internal fun applySymbolsChanges() {
        val symbolTable = config.symbolTable
        getEditedSymbols().forEach { symbolTable.overrideSymbol(it) }
    }

    open internal fun ensureTableEditorsStopped() {
        if (validators.isEditing) validators.cellEditor.stopCellEditing()
        if (symbols.isEditing) symbols.cellEditor.stopCellEditing()
    }

    open internal fun applyChanges() {
        applyValidatorsChanges()
        applySymbolsChanges()
    }

    open fun save() {
        ensureTableEditorsStopped()
        applyChanges()
        provider.configs.putAll(configs)
        cloneConfigs()
    }

    open fun resetChanges() {
        cloneConfigs()
        initTabs()
    }

    fun resetToDefaults() {
        provider.initialConfigs.forEach { e -> configs[e.key] = e.value.clone() }
        initTabs()
    }

    internal fun createValidatorsModel(): DefaultTableModel {
        val model: DefaultTableModel = object : DefaultTableModel() {
            override fun getColumnClass(i: Int): Class<*> {
                return if (i == 0) Boolean::class.javaObjectType else String::class.java
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column != 1
            }
        }
        model.addColumn("")
        model.addColumn("Name")
        model.addColumn("Properties")
        return model
    }

    internal fun createSymbolsModel(): DefaultTableModel {
        val model: DefaultTableModel = object : DefaultTableModel() {
            override fun getColumnClass(i: Int): Class<*> {
                return if (i == 1) Char::class.javaObjectType else if (i == 3 || i == 4) Boolean::class.javaObjectType else String::class.java
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column != 0
            }
        }
        model.addColumn("Symbols")
        model.addColumn("Value")
        model.addColumn("Invalid chars")
        model.addColumn("Space before")
        model.addColumn("Space after")
        return model
    }

    open var config: Configuration
        get() = configs[language.selectedItem as String]!!
        set(config) {
            configs[config.key] = config
            language.selectedItem = config.key
            if (config.key != language.selectedItem) {
                provider += config
                language.addItem(config.key)
                language.selectedItem = config.key
            }
        }
}
