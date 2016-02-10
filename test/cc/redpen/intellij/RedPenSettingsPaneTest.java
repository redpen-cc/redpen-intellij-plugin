package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class RedPenSettingsPaneTest extends BaseTest {
  RedPenSettingsPane settingsPane = new RedPenSettingsPane();

  @Before
  public void setUp() throws Exception {
    settingsPane.redPenProvider = mock(RedPenProvider.class);
    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void validatorsAreListedInSettings() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(
      validatorConfig("first", ImmutableMap.of("attr1", "val1", "attr2", "val2")),
      validatorConfig("second one", emptyMap()));

    when(settingsPane.redPenProvider.getInitialConfig()).thenReturn(redPenConfig(allValidators));
    when(settingsPane.redPenProvider.getConfig()).thenReturn(redPenConfig(singletonList(validatorConfig("second one", emptyMap()))));

    DefaultTableModel model = mock(DefaultTableModel.class);
    settingsPane = spy(settingsPane);
    doReturn(model).when(settingsPane).createModel();

    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);
    assertNotNull(settingsPane.getPane());

    verify(model).addRow(new Object[] {false, "first", "attr2=val2, attr1=val1"});
    verify(model).addRow(new Object[] {true, "second one", ""});
  }

  @Test
  public void getActiveValidators_returnsOnlySelectedValidators() throws Exception {
    Configuration config = redPenConfig(asList(
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
    Configuration config = redPenConfig(singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100", "height", "300", "depth", "1"))));
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
    Configuration config = redPenConfig(singletonList(validatorConfig("Hello", ImmutableMap.of("width", "100"))));
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

  private ValidatorConfiguration validatorConfig(String name, Map<String, String> attributes) {
    ValidatorConfiguration config = new ValidatorConfiguration(name);
    attributes.entrySet().stream().forEach(entry -> config.addAttribute(entry.getKey(), entry.getValue()));
    return config;
  }
}