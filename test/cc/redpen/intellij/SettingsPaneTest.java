package cc.redpen.intellij;

import cc.redpen.config.*;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cc.redpen.config.SymbolType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.CANCEL_OPTION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SettingsPaneTest extends BaseTest {
  RedPenProvider provider = new RedPenProvider(new LinkedHashMap<>(ImmutableMap.of("en", cloneableConfig("en"), "ja", cloneableConfig("ja"))));
  SettingsPane settingsPane = spy(new SettingsPane(provider));

  @Before
  public void setUp() throws Exception {
    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);
    settingsPane.symbols = mock(JTable.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void activeConfigIsClonedOnCreation() throws Exception {
    settingsPane.initLanguages();
    assertSame(provider.getActiveConfig().clone(), settingsPane.getConfig());
  }

  @Test
  public void autodetectCheckboxIsInitializedToFalse() throws Exception {
    provider.setAutodetect(false);
    settingsPane.initLanguages();
    assertFalse(settingsPane.autodetectLanguage.isSelected());
  }

  @Test
  public void autodetectCheckboxIsInitializedToTrue() throws Exception {
    provider.setAutodetect(true);
    settingsPane.initLanguages();
    assertTrue(settingsPane.autodetectLanguage.isSelected());
  }

  @Test
  public void languagesAndVariantsArePrepopulated() throws Exception {
    provider.setActiveConfig(provider.getConfig("ja"));

    settingsPane.initLanguages();

    assertEquals(2, settingsPane.language.getItemCount());
    assertEquals("en", settingsPane.language.getItemAt(0));
    assertEquals("ja", settingsPane.language.getItemAt(1));
    assertEquals("ja", settingsPane.language.getSelectedItem());
  }

  @Test
  public void getPaneInitsEverything() throws Exception {
    settingsPane = mock(SettingsPane.class);

    doCallRealMethod().when(settingsPane).getPane();
    doCallRealMethod().when(settingsPane).initTabs();

    settingsPane.getPane();

    verify(settingsPane).initLanguages();
    verify(settingsPane).initValidators();
    verify(settingsPane).initSymbols();
  }

  @Test
  public void changingOfLanguageAppliesOldChangesAndInitsNewValidatorsAndSymbols() throws Exception {
    doNothing().when(settingsPane).initTabs();
    doNothing().when(settingsPane).applyLocalChanges();

    settingsPane.getPane();
    assertSame(provider.getActiveConfig().clone(), settingsPane.getConfig());
    verify(settingsPane).initTabs();

    settingsPane.language.firePopupMenuWillBecomeVisible();
    verify(settingsPane).applyLocalChanges();

    settingsPane.language.setSelectedItem("ja");
    assertSame(provider.getConfig("ja").clone(), settingsPane.getConfig());
    verify(settingsPane, times(2)).initTabs();
  }

  @Test
  public void validatorsAreListedInSettings() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(
      validatorConfig("first", ImmutableMap.of("attr1", "val1", "attr2", "val2")),
      validatorConfig("second one", emptyMap()));

    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(allValidators);
    doReturn(configWithValidators(singletonList(validatorConfig("second one", emptyMap())))).when(settingsPane).getConfig();

    DefaultTableModel model = mock(DefaultTableModel.class);
    doReturn(model).when(settingsPane).createValidatorsModel();

    settingsPane.initValidators();

    verify(model).addRow(new Object[] {false, "first", "attr2=val2, attr1=val1"});
    verify(model).addRow(new Object[] {true, "second one", ""});
  }

  @Test
  public void getActiveValidators_returnsOnlySelectedValidators() throws Exception {
    settingsPane.initLanguages();
    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(asList(
      new ValidatorConfiguration("first"),
      new ValidatorConfiguration("second one")));

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(2);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(false);
    when(settingsPane.validators.getModel().getValueAt(1, 0)).thenReturn(true);
    when(settingsPane.validators.getModel().getValueAt(1, 2)).thenReturn("");

    List<ValidatorConfiguration> activeValidators = settingsPane.getActiveValidators();
    assertEquals(1, activeValidators.size());
    assertEquals("second one", activeValidators.get(0).getConfigurationName());
  }

  @Test
  public void getActiveValidators_modifiesAttributes() throws Exception {
    settingsPane.initLanguages();
    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(
      singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100", "height", "300", "depth", "1"))));

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(1);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(true);
    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn(" width=200 ,   height=300 ");

    List<ValidatorConfiguration> activeValidators = settingsPane.getActiveValidators();
    assertEquals(1, activeValidators.size());
    assertEquals(ImmutableMap.of("width", "200", "height", "300"), activeValidators.get(0).getAttributes());
    assertNotSame(provider.getInitialConfig("en").getValidatorConfigs().get(0), activeValidators.get(0));
  }

  @Test
  public void getActiveValidators_reportsInvalidAttributes() throws Exception {
    settingsPane.initLanguages();
    ValidatorConfiguration validator = validatorConfig("Hello", ImmutableMap.of("width", "100"));
    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(singletonList(validator));

    doNothing().when(settingsPane).showPropertyError(any(ValidatorConfiguration.class), anyString());

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(1);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(true);

    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn("width");
    settingsPane.getActiveValidators();
    verify(settingsPane).showPropertyError(validator, "width");

    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn("=");
    settingsPane.getActiveValidators();
    verify(settingsPane).showPropertyError(validator, "=");
  }

  @Test
  public void getActiveValidators_appliesActiveCellEditorChanges() throws Exception {
    when(settingsPane.validators.isEditing()).thenReturn(true);
    settingsPane.getActiveValidators();
    verify(settingsPane.validators.getCellEditor()).stopCellEditing();
  }

  @Test
  public void getSymbols_appliesActiveCellEditorChanges() throws Exception {
    when(settingsPane.symbols.isEditing()).thenReturn(true);
    settingsPane.getSymbols();
    verify(settingsPane.symbols.getCellEditor()).stopCellEditing();
  }

  @Test
  public void symbolsAreListedInSettings() throws Exception {
    settingsPane.setConfig(configWithSymbols(asList(new Symbol(AMPERSAND, '&', "$%", true, false), new Symbol(ASTERISK, '*', "", false, true))));

    DefaultTableModel model = mock(DefaultTableModel.class);
    doReturn(model).when(settingsPane).createSymbolsModel();

    settingsPane.initSymbols();

    verify(model).addRow(new Object[] {AMPERSAND.toString(), '&', "$%", true, false});
    verify(model).addRow(new Object[] {ASTERISK.toString(), '*', "", false, true});
  }

  @Test
  public void getSymbols() throws Exception {
    TableModel model = settingsPane.symbols.getModel();
    when(model.getRowCount()).thenReturn(2);

    when(model.getValueAt(0, 0)).thenReturn("AMPERSAND");
    when(model.getValueAt(0, 1)).thenReturn('&');
    when(model.getValueAt(0, 2)).thenReturn("$%");
    when(model.getValueAt(0, 3)).thenReturn(true);
    when(model.getValueAt(0, 4)).thenReturn(false);

    when(model.getValueAt(1, 0)).thenReturn("ASTERISK");
    when(model.getValueAt(1, 1)).thenReturn("*");
    when(model.getValueAt(1, 2)).thenReturn("");
    when(model.getValueAt(1, 3)).thenReturn(false);
    when(model.getValueAt(1, 4)).thenReturn(true);

    List<Symbol> symbols = settingsPane.getSymbols();
    assertEquals(asList(new Symbol(AMPERSAND, '&', "$%", true, false), new Symbol(ASTERISK, '*', "", false, true)), symbols);
  }

  @Test
  public void fileChooserUsesXmlFileFilter() throws Exception {
    assertEquals("RedPen Configuration", settingsPane.fileChooser.getFileFilter().getDescription());
    File file = mock(File.class);

    when(file.getName()).thenReturn("blah.xml");
    assertTrue(settingsPane.fileChooser.getFileFilter().accept(file));

    when(file.getName()).thenReturn("blah.txt");
    assertFalse(settingsPane.fileChooser.getFileFilter().accept(file));
  }

  @Test
  public void canCancelExportingConfiguration() throws Exception {
    prepareImportExport();
    settingsPane.initButtons();
    when(settingsPane.fileChooser.showSaveDialog(any(Component.class))).thenReturn(CANCEL_OPTION);

    settingsPane.exportButton.doClick();

    verify(settingsPane, never()).apply();
    verify(settingsPane.fileChooser).showSaveDialog(settingsPane.root);
    verifyNoMoreInteractions(settingsPane.fileChooser);
  }

  @Test
  public void canExportConfiguration() throws Exception {
    prepareImportExport();
    File file = File.createTempFile("redpen-conf", ".xml");
    file.deleteOnExit();

    when(settingsPane.fileChooser.showSaveDialog(any(Component.class))).thenReturn(APPROVE_OPTION);
    when(settingsPane.fileChooser.getSelectedFile()).thenReturn(file);

    settingsPane.exportConfig();

    verify(settingsPane).apply();
    verify(settingsPane.fileChooser).showSaveDialog(settingsPane.root);
    verify(settingsPane.fileChooser).getSelectedFile();
    verify(settingsPane.configurationExporter).export(eq(settingsPane.getConfig()), any(FileOutputStream.class));
  }

  @Test
  public void canCancelImportingConfiguration() throws Exception {
    prepareImportExport();
    settingsPane.initButtons();
    when(settingsPane.fileChooser.showOpenDialog(any(Component.class))).thenReturn(CANCEL_OPTION);

    settingsPane.importButton.doClick();

    verify(settingsPane.fileChooser).showOpenDialog(settingsPane.root);
    verifyNoMoreInteractions(settingsPane.fileChooser);
  }

  @Test
  public void canImportConfiguration() throws Exception {
    prepareImportExport();
    File file = mock(File.class);
    Configuration config = config("ja.hankaku");

    when(settingsPane.fileChooser.showOpenDialog(any(Component.class))).thenReturn(APPROVE_OPTION);
    when(settingsPane.fileChooser.getSelectedFile()).thenReturn(file);
    when(settingsPane.configurationLoader.load(file)).thenReturn(config);
    when(settingsPane.language.getSelectedItem()).thenReturn("ja.hankaku");

    settingsPane.importConfig();

    verify(settingsPane.fileChooser).showOpenDialog(settingsPane.root);
    verify(settingsPane.fileChooser).getSelectedFile();
    verify(settingsPane.configurationLoader).load(file);
    assertSame(settingsPane.getConfig(), config);
    verify(settingsPane).initTabs();
    verify(settingsPane.language).setSelectedItem("ja.hankaku");
  }

  @Test
  public void canImportConfigurationAddingNewLanguage() throws Exception {
    prepareImportExport();
    Configuration config = config("za");
    Configuration clone1 = config("za");
    Configuration clone2 = config("za");

    when(settingsPane.fileChooser.showOpenDialog(any(Component.class))).thenReturn(APPROVE_OPTION);
    when(settingsPane.configurationLoader.load(any(File.class))).thenReturn(config);
    when(config.clone()).thenReturn(clone1, clone2);

    settingsPane.importConfig();

    assertSame(clone1, settingsPane.provider.getInitialConfig("za"));
    assertSame(clone2, settingsPane.provider.getConfig("za"));
    verify(settingsPane.language).addItem("za");
    verify(settingsPane.language, times(2)).setSelectedItem("za");
  }

  @SuppressWarnings("unchecked")
  private void prepareImportExport() {
    settingsPane.fileChooser = mock(JFileChooser.class);
    settingsPane.configurationLoader = mock(ConfigurationLoader.class);
    settingsPane.configurationExporter = mock(ConfigurationExporter.class);
    settingsPane.language = mock(JComboBox.class);
    doNothing().when(settingsPane).initTabs();
    doNothing().when(settingsPane).apply();
  }

  @Test
  public void applyValidators() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(new ValidatorConfiguration("1"), new ValidatorConfiguration("2"));
    settingsPane.setConfig(configWithValidators(allValidators));

    List<ValidatorConfiguration> activeValidators = new ArrayList<>(allValidators.subList(0, 1));
    doReturn(activeValidators).when(settingsPane).getActiveValidators();

    settingsPane.applyValidatorsChanges();

    assertEquals(activeValidators, settingsPane.getConfig().getValidatorConfigs());
  }

  @Test
  public void applySymbols() throws Exception {
    Symbol symbol = new Symbol(BACKSLASH, '\\');
    doReturn(singletonList(symbol)).when(settingsPane).getSymbols();
    doReturn(mock(Configuration.class, RETURNS_DEEP_STUBS)).when(settingsPane).getConfig();

    settingsPane.applySymbolsChanges();

    verify(settingsPane.getConfig().getSymbolTable()).overrideSymbol(symbol);
  }

  @Test
  public void applyClonesLocalConfigs() throws Exception {
    doNothing().when(settingsPane).applyLocalChanges();
    settingsPane.apply();
    verify(settingsPane).applyLocalChanges();
    verify(settingsPane).cloneConfigs();
  }

  @Test
  public void resetToDefaults() throws Exception {
    settingsPane.initButtons();
    doNothing().when(settingsPane).initTabs();
    settingsPane.provider = mock(RedPenProvider.class);

    settingsPane.resetButton.doClick();

    verify(settingsPane.provider).reset();
    verify(settingsPane).cloneConfigs();
    verify(settingsPane).initTabs();
  }

  @Test
  public void resetChanges() throws Exception {
    doNothing().when(settingsPane).cloneConfigs();
    doNothing().when(settingsPane).initTabs();

    settingsPane.resetChanges();

    verify(settingsPane).cloneConfigs();
    verify(settingsPane).initTabs();
  }

  private ValidatorConfiguration validatorConfig(String name, Map<String, String> attributes) {
    ValidatorConfiguration config = new ValidatorConfiguration(name);
    attributes.entrySet().stream().forEach(entry -> config.addAttribute(entry.getKey(), entry.getValue()));
    return config;
  }
}