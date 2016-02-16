package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.parser.DocumentParser;
import cc.redpen.util.LanguageDetector;
import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiFile;

import java.util.LinkedHashMap;
import java.util.Map;

public class RedPenProvider {
  private static RedPenProvider instance;
  private Map<String, Configuration> initialConfigs = new LinkedHashMap<>();
  private Map<String, Configuration> configs = new LinkedHashMap<>();
  private String configKey = "en";
  private boolean autodetect = true;

  Map<String, DocumentParser> parsers = ImmutableMap.of(
    "PLAIN_TEXT", DocumentParser.PLAIN,
    "Markdown", DocumentParser.MARKDOWN,
    "AsciiDoc", DocumentParser.ASCIIDOC
  );

  public static RedPenProvider getInstance() {
    if (instance == null) instance = new RedPenProvider();
    return instance;
  }

  private RedPenProvider() {
    loadConfig("redpen-conf.xml");
    loadConfig("redpen-conf-ja.xml");
    loadConfig("redpen-conf-ja-hankaku.xml");
    loadConfig("redpen-conf-ja-zenkaku2.xml");
    reset();
  }

  public void reset() {
    initialConfigs.forEach((k,v) -> configs.put(k, v.clone()));
  }

  /** For tests */
  RedPenProvider(Map<String, Configuration> configs) {
    this.configs = configs;
    this.initialConfigs = new LinkedHashMap<>(configs);
  }

  private void loadConfig(String fileName) {
    try {
      Configuration configuration = new ConfigurationLoader().loadFromResource("/" + fileName);
      initialConfigs.put(configuration.getKey(), configuration);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  public void addConfig(Configuration config) {
    initialConfigs.put(config.getKey(), config.clone());
    configs.put(config.getKey(), config.clone());
  }

  public RedPen getRedPen() {
    try {
      return new RedPen(configs.get(configKey));
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  public RedPen getRedPenFor(String text) {
    if (autodetect) configKey = new LanguageDetector().detectLanguage(text);
    return getRedPen();
  }

  public DocumentParser getParser(PsiFile file) {
    return parsers.get(file.getFileType().getName());
  }

  public Map<String, Configuration> getInitialConfigs() {
    return initialConfigs;
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

  public void setAutodetect(boolean autodetect) {
    this.autodetect = autodetect;
  }

  public boolean isAutodetect() {
    return autodetect;
  }
}
