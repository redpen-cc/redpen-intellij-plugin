package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;

import static cc.redpen.config.SymbolType.AMPERSAND;
import static cc.redpen.config.SymbolType.ASTERISK;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class RedPenSettingsPaneTest extends BaseTest {
  RedPenSettingsPane settingsPane = new RedPenSettingsPane();

  @Before
  public void setUp() throws Exception {
    settingsPane.redPenProvider = mock(RedPenProvider.class);
    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);
    settingsPane.symbols = mock(JTable.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void languagesAndVariantsArePrepopulated() throws Exception {
    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(new Configuration.ConfigurationBuilder().setLanguage("en").build());

    settingsPane.addLanguages();

    assertEquals(1, settingsPane.language.getItemCount());
    assertEquals("en", settingsPane.language.getItemAt(0));
  }

  @Test
  public void validatorsAreListedInSettings() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(
      validatorConfig("first", ImmutableMap.of("attr1", "val1", "attr2", "val2")),
      validatorConfig("second one", emptyMap()));

    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(redPenConfigWithValidators(allValidators));
    when(settingsPane.redPenProvider.getConfig()).thenReturn(redPenConfigWithValidators(singletonList(validatorConfig("second one", emptyMap()))));

    DefaultTableModel model = mock(DefaultTableModel.class);
    settingsPane = spy(settingsPane);
    doReturn(model).when(settingsPane).createValidatorsModel();

    assertNotNull(settingsPane.getPane());

    verify(model).addRow(new Object[] {false, "first", "attr2=val2, attr1=val1"});
    verify(model).addRow(new Object[] {true, "second one", ""});
  }

  @Test
  public void getActiveValidators_returnsOnlySelectedValidators() throws Exception {
    Configuration config = redPenConfigWithValidators(asList(
      new ValidatorConfiguration("first"),
      new ValidatorConfiguration("second one")));

    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(config);

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
    Configuration config = redPenConfigWithValidators(singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100", "height", "300", "depth", "1"))));
    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(config);

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(1);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(true);
    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn(" width=200 ,   height=300 ");

    List<ValidatorConfiguration> activeValidators = settingsPane.getActiveValidators();
    assertEquals(1, activeValidators.size());
    assertEquals(ImmutableMap.of("width", "200", "height", "300"), activeValidators.get(0).getAttributes());
  }

  @Test
  public void getActiveValidators_reportsInvalidAttributes() throws Exception {
    Configuration config = redPenConfigWithValidators(singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100"))));
    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(config);
    settingsPane = spy(settingsPane);
    doNothing().when(settingsPane).showPropertyError(any(ValidatorConfiguration.class), anyString());

    when(settingsPane.validators.getModel().getRowCount()).thenReturn(1);
    when(settingsPane.validators.getModel().getValueAt(0, 0)).thenReturn(true);

    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn("width");
    settingsPane.getActiveValidators();
    verify(settingsPane).showPropertyError(config.getValidatorConfigs().get(0), "width");

    when(settingsPane.validators.getModel().getValueAt(0, 2)).thenReturn("=");
    settingsPane.getActiveValidators();
    verify(settingsPane).showPropertyError(config.getValidatorConfigs().get(0), "=");
  }

  @Test
  public void getActiveValidators_appliesActiveCellEditorChanges() throws Exception {
    when(settingsPane.validators.isEditing()).thenReturn(true);
    settingsPane.getActiveValidators();
    verify(settingsPane.validators.getCellEditor()).stopCellEditing();
  }

  @Test
  public void symbolsAreListedInSettings() throws Exception {
    Configuration config = redPenConfigWithSymbols(asList(new Symbol(AMPERSAND, '&', "$%", true, false), new Symbol(ASTERISK, '*', "", false, true)));

    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(redPenConfigWithValidators(emptyList()));
    when(settingsPane.redPenProvider.getConfig()).thenReturn(config);

    DefaultTableModel model = mock(DefaultTableModel.class);
    settingsPane = spy(settingsPane);
    doReturn(model).when(settingsPane).createSymbolsModel();

    assertNotNull(settingsPane.getPane());

    verify(model).addRow(new Object[] {AMPERSAND.toString(), '&', "$%", true, false});
    verify(model).addRow(new Object[] {ASTERISK.toString(), '*', "", false, true});
  }

  @Test
  public void getSymbols() throws Exception {
    when(settingsPane.symbols.getModel().getRowCount()).thenReturn(2);

    when(settingsPane.symbols.getModel().getValueAt(0, 0)).thenReturn("AMPERSAND");
    when(settingsPane.symbols.getModel().getValueAt(0, 1)).thenReturn('&');
    when(settingsPane.symbols.getModel().getValueAt(0, 2)).thenReturn("$%");
    when(settingsPane.symbols.getModel().getValueAt(0, 3)).thenReturn(true);
    when(settingsPane.symbols.getModel().getValueAt(0, 4)).thenReturn(false);

    when(settingsPane.symbols.getModel().getValueAt(1, 0)).thenReturn("ASTERISK");
    when(settingsPane.symbols.getModel().getValueAt(1, 1)).thenReturn("*");
    when(settingsPane.symbols.getModel().getValueAt(1, 2)).thenReturn("");
    when(settingsPane.symbols.getModel().getValueAt(1, 3)).thenReturn(false);
    when(settingsPane.symbols.getModel().getValueAt(1, 4)).thenReturn(true);

    List<Symbol> symbols = settingsPane.getSymbols();
    assertEquals(asList(new Symbol(AMPERSAND, '&', "$%", true, false), new Symbol(ASTERISK, '*', "", false, true)), symbols);
  }

  private ValidatorConfiguration validatorConfig(String name, Map<String, String> attributes) {
    ValidatorConfiguration config = new ValidatorConfiguration(name);
    attributes.entrySet().stream().forEach(entry -> config.addAttribute(entry.getKey(), entry.getValue()));
    return config;
  }
}