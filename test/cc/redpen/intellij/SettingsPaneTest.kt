package cc.redpen.intellij

import cc.redpen.config.Configuration
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType.*
import cc.redpen.config.ValidatorConfiguration
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.File
import java.util.*
import java.util.Arrays.asList
import java.util.Collections.emptyMap
import javax.swing.CellEditor
import javax.swing.DefaultCellEditor
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.JFileChooser.CANCEL_OPTION
import javax.swing.JTextField
import javax.swing.event.ChangeEvent
import javax.swing.table.DefaultTableModel

class SettingsPaneTest : BaseTest() {
    var settingsPane: SettingsPane

    init {
        provider = RedPenProvider(project, LinkedHashMap(mapOf("en" to cloneableConfig("en"), "ja" to cloneableConfig("ja"))))
        settingsPane = spy(SettingsPane(provider))
        settingsPane.validators = mock(RETURNS_DEEP_STUBS)
        settingsPane.symbols = mock(RETURNS_DEEP_STUBS)
    }

    @Test
    fun allConfigsAreClonedOnCreation() {
        assertSame(settingsPane.provider.initialConfigs["en"]!!.clone(), settingsPane.configs["en"])
        assertSame(settingsPane.provider.initialConfigs["ja"]!!.clone(), settingsPane.configs["ja"])
    }

    @Test
    fun languagesAndVariantsArePrepopulated() {
        provider.activeConfig = provider.configs["ja"]!!

        settingsPane.initLanguages()

        assertEquals(2, settingsPane.language.itemCount.toLong())
        assertEquals("en", settingsPane.language.getItemAt(0))
        assertEquals("ja", settingsPane.language.getItemAt(1))
        assertEquals("ja", settingsPane.language.selectedItem)
    }

    @Test
    fun getPaneInitsEverything() {
        doNothing().whenever(settingsPane).initLanguages()
        doNothing().whenever(settingsPane).initValidators()
        doNothing().whenever(settingsPane).initSymbols()
        doNothing().whenever(settingsPane).initButtons()

        settingsPane.pane

        verify(settingsPane).initLanguages()
        verify(settingsPane).initValidators()
        verify(settingsPane).initSymbols()
        verify(settingsPane).initButtons()
    }

    @Test
    fun changingOfLanguageAppliesOldChangesAndInitsNewValidatorsAndSymbols() {
        doNothing().whenever(settingsPane).initTabs()
        doNothing().whenever(settingsPane).applyChanges()

        settingsPane.pane
        assertSame(provider.activeConfig.clone(), settingsPane.config)
        verify(settingsPane).initTabs()

        settingsPane.language.firePopupMenuWillBecomeVisible()
        verify(settingsPane).applyChanges()

        settingsPane.language.selectedItem = "ja"
        assertSame(provider.configs["ja"]!!.clone(), settingsPane.config)
        verify(settingsPane, times(2)).initTabs()
    }

    @Test
    fun validatorsAreListedInSettings() {
        val allValidators = asList(
                validatorConfig("ModifiedAttributes", mapOf("attr1" to "val1", "attr2" to "val2")),
                validatorConfig("InitialAttributes", mapOf("attr1" to "val1", "attr2" to "val2", "space" to " ")),
                validatorConfig("NoAttributes", emptyMap()))

        whenever(provider.initialConfigs["en"]!!.validatorConfigs).thenReturn(allValidators)
        doReturn(configWithValidators(listOf(
                validatorConfig("ModifiedAttributes", mapOf("foo" to "bar")),
                validatorConfig("NewValidator", mapOf("key" to "value"))))).whenever(settingsPane).config

        val model = mock<DefaultTableModel>()
        whenever(settingsPane.validators.model).thenReturn(model)

        settingsPane.initValidators()

        verify(model).addRow(arrayOf(true, "ModifiedAttributes", "foo=bar"))
        verify(model).addRow(arrayOf(false, "InitialAttributes", "attr2=val2; attr1=val1; space= "))
        verify(model).addRow(arrayOf(false, "NoAttributes", ""))
        verify(model).addRow(arrayOf(true, "NewValidator", "key=value"))
    }

    @Test
    fun getEditedValidators_returnsOnlySelectedValidators() {
        settingsPane.config = cloneableConfig("en")

        settingsPane.initLanguages()
        whenever(provider.initialConfigs["en"]!!.validatorConfigs).thenReturn(asList(
                ValidatorConfiguration("first"),
                ValidatorConfiguration("second one")))

        whenever(settingsPane.validators.model.rowCount).thenReturn(2)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(false)
        whenever(settingsPane.validators.model.getValueAt(0, 1)).thenReturn("first")
        whenever(settingsPane.validators.model.getValueAt(1, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(1, 1)).thenReturn("second one")
        whenever(settingsPane.validators.model.getValueAt(1, 2)).thenReturn("")

        val activeValidators = settingsPane.getEditedValidators()
        assertEquals(1, activeValidators.size.toLong())
        assertEquals("second one", activeValidators[0].configurationName)
    }

    @Test
    fun getEditedValidators_modifiesAttributes() {
        settingsPane.config = cloneableConfig("en")
        settingsPane.initLanguages()
        whenever(provider.initialConfigs["en"]!!.validatorConfigs).thenReturn(
                listOf(validatorConfig("Hello", mapOf("width" to "100", "height" to "300", "depth" to "1"))))

        whenever(settingsPane.validators.model.rowCount).thenReturn(1)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(0, 1)).thenReturn("Hello")
        whenever(settingsPane.validators.model.getValueAt(0, 2)).thenReturn(" width=200;   height=300; space= ")

        val activeValidators = settingsPane.getEditedValidators()
        assertEquals(1, activeValidators.size.toLong())
        assertEquals(mapOf("width" to "200", "height" to "300", "space" to " "), activeValidators[0].properties)
        assertNotSame(provider.initialConfigs["en"]!!.validatorConfigs[0], activeValidators[0])
    }

    @Test
    fun getEditedValidators_doesNotApplyActiveCellEditorChanges() {
        settingsPane.config = cloneableConfig("en")
        whenever(settingsPane.validators.isEditing).thenReturn(true)
        settingsPane.getEditedValidators()
        verify(settingsPane.validators.cellEditor, never()).stopCellEditing()
    }

    @Test
    fun getEditedValidators_usesCurrentlyOpenCellEditor() {
        settingsPane.config = cloneableConfig("en")
        settingsPane.initLanguages()
        whenever(provider.initialConfigs["en"]!!.validatorConfigs).thenReturn(
                listOf(validatorConfig("Hello", emptyMap())))

        whenever(settingsPane.validators.model.rowCount).thenReturn(1)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(0, 1)).thenReturn("Hello")

        val cellEditor = mock<DefaultCellEditor>()
        whenever(settingsPane.validators.cellEditor).thenReturn(cellEditor)
        val cellEditorField = mock<JTextField>()
        whenever(cellEditor.component).thenReturn(cellEditorField)
        whenever(cellEditorField.text).thenReturn("foo=bar")
        whenever(settingsPane.validators.editingRow).thenReturn(0)

        val activeValidators = settingsPane.getEditedValidators()

        assertEquals(mapOf("foo" to "bar"), activeValidators[0].properties)
    }

    @Test
    fun getEditedSymbols_doesNotApplyActiveCellEditorChanges() {
        whenever(settingsPane.symbols.isEditing).thenReturn(true)
        settingsPane.getEditedSymbols()
        verify(settingsPane.symbols.cellEditor, never()).stopCellEditing()
    }

    @Test
    fun symbolsAreListedInSettings() {
        settingsPane.config = configWithSymbols(asList(Symbol(AMPERSAND, '&', "$%", true, false), Symbol(ASTERISK, '*', "", false, true)))

        val model = mock<DefaultTableModel>()
        whenever(settingsPane.symbols.model).thenReturn(model)

        settingsPane.initSymbols()

        verify(model).addRow(arrayOf(AMPERSAND.toString(), '&', "$%", true, false))
        verify(model).addRow(arrayOf(ASTERISK.toString(), '*', "", false, true))
    }

    @Test
    fun getEditedSymbols() {
        val model = settingsPane.symbols.model
        whenever(model.rowCount).thenReturn(2)

        whenever(model.getValueAt(0, 0)).thenReturn("AMPERSAND")
        whenever(model.getValueAt(0, 1)).thenReturn('&')
        whenever(model.getValueAt(0, 2)).thenReturn("$%")
        whenever(model.getValueAt(0, 3)).thenReturn(true)
        whenever(model.getValueAt(0, 4)).thenReturn(false)

        whenever(model.getValueAt(1, 0)).thenReturn("ASTERISK")
        whenever(model.getValueAt(1, 1)).thenReturn("*")
        whenever(model.getValueAt(1, 2)).thenReturn("")
        whenever(model.getValueAt(1, 3)).thenReturn(false)
        whenever(model.getValueAt(1, 4)).thenReturn(true)

        val symbols = settingsPane.getEditedSymbols()
        assertEquals(asList(Symbol(AMPERSAND, '&', "$%", true, false), Symbol(ASTERISK, '*', "", false, true)), symbols)
    }


    @Test
    fun getEditedSymbols_usesCurrentlyOpenCellEditor() {
        val model = settingsPane.symbols.model
        whenever(model.rowCount).thenReturn(1)

        whenever(model.getValueAt(0, 0)).thenReturn("AMPERSAND")
        whenever(model.getValueAt(0, 1)).thenReturn('&')
        whenever(model.getValueAt(0, 2)).thenReturn("%*")
        whenever(model.getValueAt(0, 3)).thenReturn(true)
        whenever(model.getValueAt(0, 4)).thenReturn(false)

        val cellEditor = mock<DefaultCellEditor>()
        whenever(settingsPane.symbols.cellEditor).thenReturn(cellEditor)
        val cellEditorField = mock<JTextField>()
        whenever(cellEditor.component).thenReturn(cellEditorField)
        whenever(cellEditorField.text).thenReturn("$")
        whenever(settingsPane.symbols.editingRow).thenReturn(0)
        whenever(settingsPane.symbols.editingColumn).thenReturn(1)

        assertEquals('$', settingsPane.getEditedSymbols()[0].value)

        whenever(cellEditorField.text).thenReturn("abc")
        whenever(settingsPane.symbols.editingColumn).thenReturn(2)

        assertArrayEquals(charArrayOf('a', 'b', 'c'), settingsPane.getEditedSymbols()[0].invalidChars)
    }

    @Test
    fun fileChooserUsesXmlFileFilter() {
        assertEquals("RedPen Configuration", settingsPane.fileChooser.fileFilter.description)
        val file = mock<File>()

        whenever(file.name).thenReturn("blah.xml")
        assertTrue(settingsPane.fileChooser.fileFilter.accept(file))

        whenever(file.name).thenReturn("blah.txt")
        assertFalse(settingsPane.fileChooser.fileFilter.accept(file))
    }

    @Test
    fun canCancelExportingConfiguration() {
        prepareImportExport()
        settingsPane.initButtons()
        whenever(settingsPane.fileChooser.showSaveDialog(any())).thenReturn(CANCEL_OPTION)

        settingsPane.exportButton.doClick()

        verify(settingsPane, never()).save()
        verify(settingsPane.fileChooser).showSaveDialog(settingsPane.root)
        verifyNoMoreInteractions(settingsPane.fileChooser)
    }

    @Test
    fun canExportConfiguration() {
        prepareImportExport()
        val file = File.createTempFile("redpen-conf", ".xml")
        file.deleteOnExit()

        doReturn(config("en")).whenever(settingsPane).config
        whenever(settingsPane.fileChooser.showSaveDialog(any())).thenReturn(APPROVE_OPTION)
        whenever(settingsPane.fileChooser.selectedFile).thenReturn(file)

        settingsPane.exportConfig()

        verify(settingsPane).save()
        verify(settingsPane.fileChooser).showSaveDialog(settingsPane.root)
        verify(settingsPane.fileChooser).selectedFile
        verify(settingsPane.configurationExporter).export(eq(settingsPane.config), any())
    }

    @Test
    fun canCancelImportingConfiguration() {
        prepareImportExport()
        settingsPane.initButtons()
        whenever(settingsPane.fileChooser.showOpenDialog(any())).thenReturn(CANCEL_OPTION)

        settingsPane.importButton.doClick()

        verify(settingsPane.fileChooser).showOpenDialog(settingsPane.root)
        verifyNoMoreInteractions(settingsPane.fileChooser)
    }

    @Test
    fun canImportConfiguration() {
        prepareImportExport()
        val file = mock<File>()
        val config = config("ja.hankaku")

        whenever(settingsPane.fileChooser.showOpenDialog(any())).thenReturn(APPROVE_OPTION)
        whenever(settingsPane.fileChooser.selectedFile).thenReturn(file)
        whenever(settingsPane.configurationLoader.load(file)).thenReturn(config)
        whenever(settingsPane.language.selectedItem).thenReturn("ja.hankaku")

        settingsPane.importConfig()

        verify(settingsPane.fileChooser).showOpenDialog(settingsPane.root)
        verify(settingsPane.fileChooser).selectedFile
        verify(settingsPane.configurationLoader).load(file)
        assertSame(settingsPane.config, config)
        verify(settingsPane).initTabs()
        verify(settingsPane.language).selectedItem = "ja.hankaku"
    }

    @Test
    fun canImportConfigurationAddingNewLanguage() {
        prepareImportExport()
        val config = config("za")
        val clone1 = config("za")
        val clone2 = config("za")

        whenever(settingsPane.fileChooser.showOpenDialog(any())).thenReturn(APPROVE_OPTION)
        whenever(settingsPane.configurationLoader.load(any<File>())).thenReturn(config)
        whenever(config.clone()).thenReturn(clone1, clone2)

        settingsPane.importConfig()

        assertSame(clone1, settingsPane.provider.initialConfigs["za"])
        assertSame(clone2, settingsPane.provider.configs["za"])
        verify(settingsPane.language).addItem("za")
        verify(settingsPane.language, times(2)).selectedItem = "za"
    }

    private fun prepareImportExport() {
        settingsPane.fileChooser = mock()
        settingsPane.configurationLoader = mock()
        settingsPane.configurationExporter = mock()
        settingsPane.language = mock()
        doNothing().whenever(settingsPane).initTabs()
        doNothing().whenever(settingsPane).save()
    }

    @Test
    fun applyValidatorChanges() {
        val allValidators = asList(ValidatorConfiguration("1"), ValidatorConfiguration("2"))
        settingsPane.config = configWithValidators(allValidators)

        val activeValidators = ArrayList(allValidators.subList(0, 1))
        doReturn(activeValidators).whenever(settingsPane).getEditedValidators()

        settingsPane.applyValidatorsChanges()

        assertEquals(activeValidators, settingsPane.config.validatorConfigs)
    }

    @Test
    fun applyValidatorChanges_addsNewValidatorsIfNeeded() {
        whenever(provider.initialConfigs["en"]!!.validatorConfigs).thenReturn(asList(
                ValidatorConfiguration("1"),
                ValidatorConfiguration("2")))
        val validators = asList(
                ValidatorConfiguration("2"),
                ValidatorConfiguration("active new"),
                ValidatorConfiguration("inactive new"))

        doReturn(configWithValidators(validators)).whenever(settingsPane).config

        whenever(settingsPane.validators.model.rowCount).thenReturn(2)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(0, 1)).thenReturn("2")
        whenever(settingsPane.validators.model.getValueAt(0, 2)).thenReturn("")

        whenever(settingsPane.validators.model.getValueAt(1, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(1, 1)).thenReturn("active new")
        whenever(settingsPane.validators.model.getValueAt(1, 2)).thenReturn("")

        whenever(settingsPane.validators.model.getValueAt(2, 0)).thenReturn(false)
        whenever(settingsPane.validators.model.getValueAt(2, 1)).thenReturn("inactive new")
        whenever(settingsPane.validators.model.getValueAt(2, 2)).thenReturn("")

        settingsPane.applyValidatorsChanges()

        assertEquals(validators.subList(0, 2), settingsPane.config.validatorConfigs)
    }

    @Test
    fun applySymbolsChanges() {
        val symbol = Symbol(BACKSLASH, '\\')
        doReturn(listOf(symbol)).whenever(settingsPane).getEditedSymbols()
        doReturn(mock<Configuration>(RETURNS_DEEP_STUBS)).whenever(settingsPane).config

        settingsPane.applySymbolsChanges()

        verify(settingsPane.config.symbolTable).overrideSymbol(symbol)
    }

    @Test
    fun saveClonesLocalConfigs() {
        doNothing().whenever(settingsPane).applyChanges()
        doNothing().whenever(settingsPane).cloneConfigs()
        settingsPane.save()
        verify(settingsPane).applyChanges()
        verify(settingsPane).cloneConfigs()
    }

    @Test
    fun resetToDefaults() {
        settingsPane.initButtons()
        doNothing().whenever(settingsPane).initTabs()

        settingsPane.resetButton.doClick()

        assertEquals(settingsPane.provider.initialConfigs["en"]!!.clone(), settingsPane.configs["en"])
        assertEquals(settingsPane.provider.initialConfigs["ja"]!!.clone(), settingsPane.configs["ja"])
        verify(settingsPane).initTabs()
    }

    @Test
    fun resetChanges() {
        doNothing().whenever(settingsPane).cloneConfigs()
        doNothing().whenever(settingsPane).initTabs()

        settingsPane.resetChanges()

        verify(settingsPane).cloneConfigs()
        verify(settingsPane).initTabs()
    }

    @Test
    fun applyChanges() {
        doNothing().whenever(settingsPane).applySymbolsChanges()
        doNothing().whenever(settingsPane).applyValidatorsChanges()

        settingsPane.applyChanges()

        verify(settingsPane).applySymbolsChanges()
        verify(settingsPane).applyValidatorsChanges()
    }

    @Test
    fun isCorrectPropertiesFormat() {
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat(""))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo="))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar"))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;"))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;foo2="))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;foo2=bar2"))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;foo2=bar2;"))
        assertTrue(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;foo2=bar2;foo3=bar3"))
        assertFalse(settingsPane.isCorrectValidatorPropertiesFormat("foo"))
        assertFalse(settingsPane.isCorrectValidatorPropertiesFormat("=bar"))
        assertFalse(settingsPane.isCorrectValidatorPropertiesFormat("=bar;"))
        assertFalse(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;="))
        assertFalse(settingsPane.isCorrectValidatorPropertiesFormat("foo=bar;foo2"))
    }

    @Test
    fun reportInvalidValidatorAttributesFormatWhenEditorIsStopped() {
        doNothing().whenever(settingsPane).showValidatorPropertyError(any())
        val event = mock<ChangeEvent>()
        val source = mock<CellEditor>()
        whenever(event.source).thenReturn(source)

        whenever(source.cellEditorValue).thenReturn("width")
        settingsPane.showValidatorPropertyErrorIfNeeded(event)
        verify(settingsPane).showValidatorPropertyError("width")

        whenever(source.cellEditorValue).thenReturn("=")
        settingsPane.showValidatorPropertyErrorIfNeeded(event)
        verify(settingsPane).showValidatorPropertyError("=")

        whenever(source.cellEditorValue).thenReturn("width=120")
        settingsPane.showValidatorPropertyErrorIfNeeded(event)
        verifyNoMoreInteractions(settingsPane)
    }

    private fun validatorConfig(name: String, attributes: Map<String, String>): ValidatorConfiguration {
        val config = ValidatorConfiguration(name)
        attributes.forEach{ e -> config.addProperty(e.key, e.value) }
        return config
    }
}