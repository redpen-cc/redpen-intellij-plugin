package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;

public class RedPenProvider {
  Configuration config;

  public RedPenProvider() {
    try {
      config = new ConfigurationLoader().loadFromResource("/redpen-conf.xml");
    }
    catch (RedPenException e) {
      throw new RuntimeException("Cannot read RedPen conf file", e);
    }
  }

  RedPen getRedPen() {
    try {
      return new RedPen(config);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  };
}
