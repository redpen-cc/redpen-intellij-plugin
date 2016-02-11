package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Map;

import static cc.redpen.config.SymbolType.AMPERSAND;
import static cc.redpen.config.SymbolType.ASTERISK;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RedPenSettingsPaneTest extends BaseTest {
  RedPenProvider provider = new RedPenProvider(ImmutableMap.of("en", config("en"), "ja", config("ja")));
  RedPenSettingsPane settingsPane = new RedPenSettingsPane(provider);

  @Before
  public void setUp() throws Exception {
    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);
    settingsPane.symbols = mock(JTable.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void activeConfigIsRetrievedOnCreation() throws Exception {
    assertSame(provider.getActiveConfig(), settingsPane.config);
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
    settingsPane.config = provider.getConfig("ja");

    settingsPane.initLanguages();

    assertEquals(2, settingsPane.language.getItemCount());
    assertEquals("en", settingsPane.language.getItemAt(0));
    assertEquals("ja", settingsPane.language.getItemAt(1));
    assertEquals("ja", settingsPane.language.getSelectedItem());
  }

  @Test
  public void getPaneInitsEverything() throws Exception {
    settingsPane = mock(RedPenSettingsPane.class);

    doCallRealMethod().when(settingsPane).getPane();
    doCallRealMethod().when(settingsPane).initTabs();

    settingsPane.getPane();

    verify(settingsPane).initLanguages();
    verify(settingsPane).initValidators();
    verify(settingsPane).initSymbols();
  }

  @Test
  public void changingOfLanguageRebuildsValidatorsAndSymbols() throws Exception {
    settingsPane = spy(settingsPane);
    doNothing().when(settingsPane).initTabs();

    settingsPane.getPane();
    assertSame(provider.getActiveConfig(), settingsPane.config);
    verify(settingsPane).initTabs();

    settingsPane.language.setSelectedItem("ja");
    assertSame(provider.getConfigs().get("ja"), settingsPane.config);
    verify(settingsPane, times(2)).initTabs();
  }

  @Test
  public void validatorsAreListedInSettings() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(
      validatorConfig("first", ImmutableMap.of("attr1", "val1", "attr2", "val2")),
      validatorConfig("second one", emptyMap()));

    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(allValidators);
    settingsPane.config = redPenConfigWithValidators(singletonList(validatorConfig("second one", emptyMap())));

    DefaultTableModel model = mock(DefaultTableModel.class);
    settingsPane = spy(settingsPane);
    doReturn(model).when(settingsPane).createValidatorsModel();

    settingsPane.initValidators();

    verify(model).addRow(new Object[] {false, "first", "attr2=val2, attr1=val1"});
    verify(model).addRow(new Object[] {true, "second one", ""});
  }

  @Test
  public void getActiveValidators_returnsOnlySelectedValidators() throws Exception {
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
    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(
      singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100", "height", "300", "depth", "1"))));

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(1);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(true);
    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn(" width=200 ,   height=300 ");

    List<ValidatorConfiguration> activeValidators = settingsPane.getActiveValidators();
    assertEquals(1, activeValidators.size());
    assertEquals(ImmutableMap.of("width", "200", "height", "300"), activeValidators.get(0).getAttributes());
  }

  @Test
  public void getActiveValidators_reportsInvalidAttributes() throws Exception {
    ValidatorConfiguration validator = validatorConfig("Hello", ImmutableMap.of("width", "100"));
    when(provider.getInitialConfig("en").getValidatorConfigs()).thenReturn(singletonList(validator));

    settingsPane = spy(settingsPane);
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
  public void symbolsAreListedInSettings() throws Exception {
    settingsPane.config = redPenConfigWithSymbols(asList(new Symbol(AMPERSAND, '&', "$%", true, false), new Symbol(ASTERISK, '*', "", false, true)));

    DefaultTableModel model = mock(DefaultTableModel.class);
    settingsPane = spy(settingsPane);
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

  private ValidatorConfiguration validatorConfig(String name, Map<String, String> attributes) {
    ValidatorConfiguration config = new ValidatorConfiguration(name);
    attributes.entrySet().stream().forEach(entry -> config.addAttribute(entry.getKey(), entry.getValue()));
    return config;
  }

  private Configuration config(String key) {
    Configuration config = mock(Configuration.class);
    when(config.getKey()).thenReturn(key);
    return config;
  }
}