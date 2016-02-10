package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;

import java.util.List;

public abstract class BaseTest {
  protected Configuration redPenConfigWithValidators(List<ValidatorConfiguration> validatorConfigs) {
    Configuration.ConfigurationBuilder builder = new Configuration.ConfigurationBuilder();
    validatorConfigs.forEach(builder::addValidatorConfig);
    return builder.build();
  }

  protected Configuration redPenConfigWithSymbols(List<Symbol> symbols) {
    Configuration.ConfigurationBuilder builder = new Configuration.ConfigurationBuilder();
    symbols.forEach(builder::addSymbol);
    return builder.build();
  }
}
