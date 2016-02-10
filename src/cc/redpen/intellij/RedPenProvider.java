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
  Map<String, Configuration> configs = new LinkedHashMap<>();
  Configuration config;

  Map<String, DocumentParser> parsers = ImmutableMap.of(
    "PLAIN_TEXT", DocumentParser.PLAIN,
    "Markdown", DocumentParser.MARKDOWN,
    "AsciiDoc", DocumentParser.ASCIIDOC
  );

  private RedPenProvider() {
    try {
      config = new ConfigurationLoader().loadFromResource("/redpen-conf.xml");
      loadConfig("redpen-conf.xml");
      loadConfig("redpen-conf-ja.xml");
      loadConfig("redpen-conf-ja-hankaku.xml");
      loadConfig("redpen-conf-ja-zenkaku2.xml");
    }
    catch (RedPenException e) {
      throw new RuntimeException("Cannot read RedPen conf file", e);
    }
  }

  private void loadConfig(String fileName) {
    try {
      Configuration configuration = new ConfigurationLoader().loadFromResource("/" + fileName);
      configs.put(configuration.getKey(), configuration);
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
      return new RedPen(config);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  public DocumentParser getParser(PsiFile file) {
    return parsers.get(file.getFileType().getName());
  }

  public Configuration getInitialConfig() {
    return configs.get("en");
  }

  public Map<String, Configuration> getAvailableConfigs() {
    return configs;
  }

  public Configuration getConfig() {
    return config;
  }
}
