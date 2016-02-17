package cc.redpen.intellij

import cc.redpen.config.Configuration
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType.*
import cc.redpen.config.ValidatorConfiguration
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.File
import java.util.*
import java.util.Arrays.asList
import java.util.Collections.emptyMap
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.JFileChooser.CANCEL_OPTION
import javax.swing.table.DefaultTableModel

class SettingsPaneTest : BaseTest() {
    internal var provider = RedPenProvider(LinkedHashMap(mapOf("en" to cloneableConfig("en"), "ja" to cloneableConfig("ja"))))
    internal var settingsPane = spy(SettingsPane(provider))

    @Before
    fun setUp() {
        settingsPane.validators = mock(RETURNS_DEEP_STUBS)
        settingsPane.symbols = mock(RETURNS_DEEP_STUBS)
    }

    @Test
    fun allConfigsAreClonedOnCreation() {
        assertSame(settingsPane.provider.getInitialConfig("en")!!.clone(), settingsPane.getConfig("en"))
        assertSame(settingsPane.provider.getInitialConfig("ja")!!.clone(), settingsPane.getConfig("ja"))
    }

    @Test
    fun autodetectCheckboxIsInitializedToFalse() {
        provider.autodetect = false
        settingsPane.initLanguages()
        assertFalse(settingsPane.autodetectLanguage.isSelected)
    }

    @Test
    fun autodetectCheckboxIsInitializedToTrue() {
        provider.autodetect = true
        settingsPane.initLanguages()
        assertTrue(settingsPane.autodetectLanguage.isSelected)
    }

    @Test
    fun languagesAndVariantsArePrepopulated() {
        provider.activeConfig = provider.getConfig("ja")!!

        settingsPane.initLanguages()

        assertEquals(2, settingsPane.language.itemCount.toLong())
        assertEquals("en", settingsPane.language.getItemAt(0))
        assertEquals("ja", settingsPane.language.getItemAt(1))
        assertEquals("ja", settingsPane.language.selectedItem)
    }

    @Test
    fun getPaneInitsEverything() {
        settingsPane = mock()

        doCallRealMethod().whenever(settingsPane).pane
        doCallRealMethod().whenever(settingsPane).initTabs()

        settingsPane.pane

        verify(settingsPane).initLanguages()
        verify(settingsPane).initValidators()
        verify(settingsPane).initSymbols()
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
        assertSame(provider.getConfig("ja")!!.clone(), settingsPane.config)
        verify(settingsPane, times(2)).initTabs()
    }

    @Test
    fun validatorsAreListedInSettings() {
        val allValidators = asList(
                validatorConfig("ModifiedAttributes", mapOf("attr1" to "val1", "attr2" to "val2")),
                validatorConfig("InitialAttributes", mapOf("attr1" to "val1", "attr2" to "val2")),
                validatorConfig("NoAttributes", emptyMap()))

        whenever(provider.getInitialConfig("en")!!.validatorConfigs).thenReturn(allValidators)
        doReturn(configWithValidators(listOf(validatorConfig("ModifiedAttributes", mapOf("foo" to "bar"))))).whenever(settingsPane).config

        val model = mock<DefaultTableModel>()
        doReturn(model).whenever(settingsPane).createValidatorsModel()

        settingsPane.initValidators()

        verify(model).addRow(arrayOf(true, "ModifiedAttributes", "foo=bar"))
        verify(model).addRow(arrayOf(false, "InitialAttributes", "attr2=val2, attr1=val1"))
        verify(model).addRow(arrayOf(false, "NoAttributes", ""))
    }

    @Test
    fun getActiveValidators_returnsOnlySelectedValidators() {
        settingsPane.initLanguages()
        whenever(provider.getInitialConfig("en")!!.validatorConfigs).thenReturn(asList(
                ValidatorConfiguration("first"),
                ValidatorConfiguration("second one")))

        whenever(settingsPane.validators.model.rowCount).thenReturn(2)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(false)
        whenever(settingsPane.validators.model.getValueAt(1, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(1, 2)).thenReturn("")

        val activeValidators = settingsPane.activeValidators
        assertEquals(1, activeValidators.size.toLong())
        assertEquals("second one", activeValidators[0].configurationName)
    }

    @Test
    fun getActiveValidators_modifiesAttributes() {
        settingsPane.initLanguages()
        whenever(provider.getInitialConfig("en")!!.validatorConfigs).thenReturn(
                listOf(validatorConfig("Hello", mapOf("width" to "100", "height" to "300", "depth" to "1"))))

        whenever(settingsPane.validators.model.rowCount).thenReturn(1)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(true)
        whenever(settingsPane.validators.model.getValueAt(0, 2)).thenReturn(" width=200 ,   height=300 ")

        val activeValidators = settingsPane.activeValidators
        assertEquals(1, activeValidators.size.toLong())
        assertEquals(mapOf("width" to "200", "height" to "300"), activeValidators[0].attributes)
        assertNotSame(provider.getInitialConfig("en")!!.validatorConfigs[0], activeValidators[0])
    }

    @Test
    fun getActiveValidators_reportsInvalidAttributes() {
        settingsPane.initLanguages()
        val validator = validatorConfig("Hello", mapOf("width" to "100"))
        whenever(provider.getInitialConfig("en")!!.validatorConfigs).thenReturn(listOf(validator))

        doNothing().whenever(settingsPane).showPropertyError(any(), any())

        whenever(settingsPane.validators.model.rowCount).thenReturn(1)
        whenever(settingsPane.validators.model.getValueAt(0, 0)).thenReturn(true)

        whenever(settingsPane.validators.model.getValueAt(0, 2)).thenReturn("width")
        settingsPane.activeValidators
        verify(settingsPane).showPropertyError(validator, "width")

        whenever(settingsPane.validators.model.getValueAt(0, 2)).thenReturn("=")
        settingsPane.activeValidators
        verify(settingsPane).showPropertyError(validator, "=")
    }

    @Test
    fun getActiveValidators_appliesActiveCellEditorChanges() {
        whenever(settingsPane.validators.isEditing).thenReturn(true)
        settingsPane.activeValidators
        verify(settingsPane.validators.cellEditor).stopCellEditing()
    }

    @Test
    fun getSymbols_appliesActiveCellEditorChanges() {
        whenever(settingsPane.symbols.isEditing).thenReturn(true)
        settingsPane.getSymbols()
        verify(settingsPane.symbols.cellEditor).stopCellEditing()
    }

    @Test
    fun symbolsAreListedInSettings() {
        settingsPane.config = configWithSymbols(asList(Symbol(AMPERSAND, '&', "$%", true, false), Symbol(ASTERISK, '*', "", false, true)))

        val model = mock<DefaultTableModel>()
        doReturn(model).whenever(settingsPane).createSymbolsModel()

        settingsPane.initSymbols()

        verify(model).addRow(arrayOf(AMPERSAND.toString(), '&', "$%", true, false))
        verify(model).addRow(arrayOf(ASTERISK.toString(), '*', "", false, true))
    }

    @Test
    fun getSymbols() {
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

        val symbols = settingsPane.getSymbols()
        assertEquals(asList(Symbol(AMPERSAND, '&', "$%", true, false), Symbol(ASTERISK, '*', "", false, true)), symbols)
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

        assertSame(clone1, settingsPane.provider.getInitialConfig("za"))
        assertSame(clone2, settingsPane.provider.getConfig("za"))
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
    fun applyValidators() {
        val allValidators = asList(ValidatorConfiguration("1"), ValidatorConfiguration("2"))
        settingsPane.config = configWithValidators(allValidators)

        val activeValidators = ArrayList(allValidators.subList(0, 1))
        doReturn(activeValidators).whenever(settingsPane).activeValidators

        settingsPane.applyValidatorsChanges()

        assertEquals(activeValidators, settingsPane.config.validatorConfigs)
    }

    @Test
    fun applySymbols() {
        val symbol = Symbol(BACKSLASH, '\\')
        doReturn(listOf(symbol)).whenever(settingsPane).getSymbols()
        doReturn(mock<Configuration>(RETURNS_DEEP_STUBS)).whenever(settingsPane).config

        settingsPane.applySymbolsChanges()

        verify(settingsPane.config.symbolTable).overrideSymbol(symbol)
    }

    @Test
    fun applyClonesLocalConfigs() {
        doNothing().whenever(settingsPane).applyChanges()
        settingsPane.save()
        verify(settingsPane).applyChanges()
        verify(settingsPane).cloneConfigs()
    }

    @Test
    fun resetToDefaults() {
        settingsPane.initButtons()
        doNothing().whenever(settingsPane).initTabs()

        settingsPane.resetButton.doClick()

        assertEquals(settingsPane.provider.getInitialConfig("en")!!.clone(), settingsPane.getConfig("en"))
        assertEquals(settingsPane.provider.getInitialConfig("ja")!!.clone(), settingsPane.getConfig("ja"))
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

    private fun validatorConfig(name: String, attributes: Map<String, String>): ValidatorConfiguration {
        val config = ValidatorConfiguration(name)
        attributes.forEach({ e -> config.addAttribute(e.key, e.value) })
        return config
    }
}