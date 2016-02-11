package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.parser.DocumentParser;
import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiFile;

import java.util.LinkedHashMap;
import java.util.Map;

public class RedPenProvider {
  private static RedPenProvider instance;
  private Map<String, Configuration> initialConfigs = new LinkedHashMap<>();
  private Map<String, Configuration> configs = new LinkedHashMap<>();
  private String configKey = "en";

  Map<String, DocumentParser> parsers = ImmutableMap.of(
    "PLAIN_TEXT", DocumentParser.PLAIN,
    "Markdown", DocumentParser.MARKDOWN,
    "AsciiDoc", DocumentParser.ASCIIDOC
  );

  private RedPenProvider() {
    loadConfig("redpen-conf.xml", configs);
    loadConfig("redpen-conf-ja.xml", configs);
    loadConfig("redpen-conf-ja-hankaku.xml", configs);
    loadConfig("redpen-conf-ja-zenkaku2.xml", configs);

    loadConfig("redpen-conf.xml", initialConfigs);
    loadConfig("redpen-conf-ja.xml", initialConfigs);
    loadConfig("redpen-conf-ja-hankaku.xml", initialConfigs);
    loadConfig("redpen-conf-ja-zenkaku2.xml", initialConfigs);
  }

  private void loadConfig(String fileName, Map<String, Configuration> target) {
    try {
      Configuration configuration = new ConfigurationLoader().loadFromResource("/" + fileName);
      target.put(configuration.getKey(), configuration);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  public static RedPenProvider getInstance() {
    if (instance == null) instance = new RedPenProvider();
    return instance;
  }

  public RedPen getRedPen() {
    try {
      return new RedPen(configs.get(configKey));
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  public DocumentParser getParser(PsiFile file) {
    return parsers.get(file.getFileType().getName());
  }

  public Configuration getInitialConfig(String key) {
    return initialConfigs.get(key);
  }

  public Map<String, Configuration> getConfigs() {
    return configs;
  }

  public Configuration getConfig(String key) {
    return configs.get(key);
  }

  public Configuration getActiveConfig() {
    return configs.get(configKey);
  }

  public void setActiveConfig(Configuration config) {
    configKey = config.getKey();
  }
}
