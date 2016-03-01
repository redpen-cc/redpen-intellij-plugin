package cc.redpen.intellij

import cc.redpen.RedPenException
import cc.redpen.config.*
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

        tabbedPane.addTab("Symbols", JScrollPane(symbols))
        symbols.rowHeight = (validators.font.size * 1.5).toInt()
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

        for (initialValidator in provider.initialConfigs[config.key]!!.validatorConfigs) {
            val validator = config.validatorConfigs.find { v -> v.configurationName == initialValidator.configurationName }
            (validators.model as DefaultTableModel).addRow(arrayOf<Any>(validator != null, initialValidator.configurationName, attributes(validator ?: initialValidator)))
        }

        validators.doLayout()
    }

    open fun getEditedValidators(): List<ValidatorConfiguration> {
        val result = ArrayList<ValidatorConfiguration>()
        val model = validators.model
        for (i in 0..model.rowCount-1) {
            if (model.getValueAt(i, 0) as Boolean) {
                val validator = provider.initialConfigs[config.key]!!.validatorConfigs[i].clone()
                validator.attributes.clear()
                val attributes = model.getValueAt(i, 2) as String
                attributes.split(";\\s*".toRegex()).filter { it.isNotEmpty() }.forEach { s ->
                    val attr = s.split("=".toRegex(), 2)
                    if (attr.size < 2 || attr[0].isEmpty())
                        showPropertyError(validator.configurationName, s)
                    else
                        validator.addAttribute(attr[0].trim(), attr[1])
                }
                result.add(validator)
            }
        }
        return result
    }

    open internal fun showPropertyError(validatorName: String, s: String) {
        Messages.showMessageDialog("Validator property must be in key=value format: " + s, validatorName, Messages.getErrorIcon())
    }

    open fun getEditedSymbols(): List<Symbol> {
        val model = symbols.model
        return (0..model.rowCount-1).map { i ->
            Symbol(SymbolType.valueOf(model.getValueAt(i, 0) as String), model.getValueAt(i, 1).toString()[0], model.getValueAt(i, 2) as String,
                   model.getValueAt(i, 3) as Boolean, model.getValueAt(i, 4) as Boolean)
        }
    }

    private fun attributes(validatorConfig: ValidatorConfiguration): String {
        return validatorConfig.attributes.entries.joinToString("; ")
    }

    open internal fun applyValidatorsChanges() {
        val validators = config.validatorConfigs
        validators.clear()
        validators.addAll(getEditedValidators())
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
