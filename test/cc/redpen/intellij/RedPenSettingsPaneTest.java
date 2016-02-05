package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

public class RedPenSettingsPaneTest {

  @Test
  public void validatorsAreListedInSettings() throws Exception {
    Configuration config = redPenConfig(asList(
      validatorConfig("first", ImmutableMap.of("attr1", "val1", "attr2", "val2")),
      validatorConfig("second one", emptyMap())));

    DefaultTableModel model = mock(DefaultTableModel.class);
    RedPenSettingsPane settingsPane = spy(new RedPenSettingsPane(config));
    doReturn(model).when(settingsPane).model();
    settingsPane.validators = mock(JTable.class, RETURNS_DEEP_STUBS);

    settingsPane.getPane();

    verify(model).addRow(new Object[] {true, "first", "attr2=val2, attr1=val1"});
    verify(model).addRow(new Object[] {true, "second one", ""});
  }

  private Configuration redPenConfig(List<ValidatorConfiguration> validatorConfigs) {
    Configuration.ConfigurationBuilder builder = new Configuration.ConfigurationBuilder();
    validatorConfigs.forEach(builder::addValidatorConfig);
    return builder.build();
  }

  private ValidatorConfiguration validatorConfig(String name, Map<String, String> attributes) {
    ValidatorConfiguration config = new ValidatorConfiguration(name);
    attributes.entrySet().stream().forEach(entry -> config.addAttribute(entry.getKey(), entry.getValue()));
    return config;
  }
}