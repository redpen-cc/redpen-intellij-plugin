package cc.redpen.intellij

import cc.redpen.RedPenException
import cc.redpen.config.*
import com.intellij.openapi.ui.Messages
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import java.awt.Dimension
import java.awt.Insets
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.stream.Collectors.toList
import java.util.stream.IntStream.range
import java.util.stream.Stream
import javax.swing.*
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.event.PopupMenuEvent
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

open class SettingsPane(internal var provider: RedPenProvider) {
    open val configs: MutableMap<String, Configuration> = LinkedHashMap()
    internal var root = JPanel()
    private val tabbedPane = JTabbedPane()
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

        root.layout = GridLayoutManager(2, 7, Insets(0, 0, 0, 0), -1, -1)
        root.add(tabbedPane, GridConstraints(1, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, Dimension(200, 200), null, 0, false))
        val panel1 = JPanel()
        panel1.layout = GridLayoutManager(1, 1, Insets(0, 0, 0, 0), -1, -1)
        tabbedPane.addTab("Validators", panel1)
        val scrollPane1 = JScrollPane()
        panel1.add(scrollPane1, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
        validators.autoCreateRowSorter = true
        validators.showHorizontalLines = true
        validators.showVerticalLines = true
        scrollPane1.setViewportView(validators)
        val panel2 = JPanel()
        panel2.layout = GridLayoutManager(1, 1, Insets(0, 0, 0, 0), -1, -1)
        tabbedPane.addTab("Symbols", panel2)
        val scrollPane2 = JScrollPane()
        panel2.add(scrollPane2, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
        scrollPane2.setViewportView(symbols)
        root.add(language, GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        val label1 = JLabel("Language")
        root.add(label1, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        exportButton.isEnabled = true
        root.add(exportButton, GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        val spacer1 = Spacer()
        root.add(spacer1, GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false))
        root.add(importButton, GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        root.add(resetButton, GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    }

    open internal fun cloneConfigs() {
        provider.getConfigs().forEach { e -> configs.put(e.key, e.value.clone()) }
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
        provider.getConfigs().keys.forEach { k -> language.addItem(k) }
        language.selectedItem = provider.activeConfig.key
        language.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
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
        symbols.rowHeight = (validators.font.size * 1.5).toInt()
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
        validators.rowHeight = (validators.font.size * 1.5).toInt()

        validators.columnModel.getColumn(0).maxWidth = 20

        for (initialValidator in provider.getInitialConfig(config.key)!!.validatorConfigs) {
            val validator = config.validatorConfigs.find { v -> v == initialValidator }
            (validators.model as DefaultTableModel).addRow(arrayOf<Any>(validator != null, initialValidator.configurationName, attributes(validator ?: initialValidator)))
        }

        validators.doLayout()
    }

    open val activeValidators: List<ValidatorConfiguration>
        get() {
            val result = ArrayList<ValidatorConfiguration>()
            if (validators.isEditing) validators.cellEditor.stopCellEditing()
            val model = validators.model
            for (i in 0..(model.rowCount - 1)) {
                if (model.getValueAt(i, 0) as Boolean) {
                    val validator = provider.getInitialConfig(config.key)!!.validatorConfigs[i].clone()
                    validator.attributes.clear()
                    val attributes = model.getValueAt(i, 2) as String
                    Stream.of(*attributes.trim { it <= ' ' }.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).filter { s -> !s.isEmpty() }.forEach { s ->
                        val attr = s.split("=".toRegex(), 2).toTypedArray()
                        if (attr.size < 2 || attr[0].isEmpty())
                            showPropertyError(validator, s)
                        else
                            validator.addAttribute(attr[0], attr[1])
                    }
                    result.add(validator)
                }
            }
            return result
        }

    open internal fun showPropertyError(validator: ValidatorConfiguration, s: String) {
        Messages.showMessageDialog("Validator property must be in key=value format: " + s, validator.configurationName, Messages.getErrorIcon())
    }

    open fun getSymbols(): List<Symbol> {
        if (symbols.isEditing) symbols.cellEditor.stopCellEditing()

        val model = symbols.model
        return range(0, model.rowCount).mapToObj { i ->
            Symbol(
                    SymbolType.valueOf(model.getValueAt(i, 0) as String), model.getValueAt(i, 1).toString()[0], model.getValueAt(i, 2) as String,
                    model.getValueAt(i, 3) as Boolean, model.getValueAt(i, 4) as Boolean)
        }.collect(toList<Symbol>())
    }

    private fun attributes(validatorConfig: ValidatorConfiguration): String {
        val result = validatorConfig.attributes.toString()
        return result.substring(1, result.length - 1)
    }

    open internal fun applyValidatorsChanges() {
        val validators = config.validatorConfigs
        val remainingValidators = activeValidators
        validators.clear()
        validators.addAll(remainingValidators)
    }

    open internal fun applySymbolsChanges() {
        val symbolTable = config.symbolTable
        getSymbols().forEach { symbolTable.overrideSymbol(it) }
    }

    open internal fun applyChanges() {
        applyValidatorsChanges()
        applySymbolsChanges()
    }

    open fun save() {
        applyChanges()
        provider.getConfigs().putAll(configs)
        cloneConfigs()
    }

    open fun resetChanges() {
        cloneConfigs()
        initTabs()
    }

    fun resetToDefaults() {
        provider.getInitialConfigs().forEach { e -> configs.put(e.key, e.value.clone()) }
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
        model.addColumn("Properties (comma-separated)")
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
        get() = getConfig(language.selectedItem as String)
        set(config) {
            configs.put(config.key, config)
            language.selectedItem = config.key
            if (config.key != language.selectedItem) {
                provider.addConfig(config)
                language.addItem(config.key)
                language.selectedItem = config.key
            }
        }

    fun getConfig(key: String): Configuration {
        return configs[key]!!
    }
}
