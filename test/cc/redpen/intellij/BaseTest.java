package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  protected Configuration cloneableConfig(String key) {
    Configuration config = config(key);
    Configuration configClone = config(key);
    when(config.clone()).thenReturn(configClone);
    return config;
  }

  protected Configuration config(String key) {
    Configuration config = mock(Configuration.class);
    when(config.getKey()).thenReturn(key);
    return config;
  }
}
