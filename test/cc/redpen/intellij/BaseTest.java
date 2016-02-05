package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;

import java.util.List;

public abstract class BaseTest {
  protected Configuration redPenConfig(List<ValidatorConfiguration> validatorConfigs) {
    Configuration.ConfigurationBuilder builder = new Configuration.ConfigurationBuilder();
    validatorConfigs.forEach(builder::addValidatorConfig);
    return builder.build();
  }
}
