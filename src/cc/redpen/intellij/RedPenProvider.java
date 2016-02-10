package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.parser.DocumentParser;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.intellij.psi.PsiFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

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
      findConfFiles().stream().forEach(this::loadConfig);
      config = new ConfigurationLoader().loadFromResource("/redpen-conf.xml");
    }
    catch (RedPenException e) {
      throw new RuntimeException("Cannot read RedPen conf file", e);
    }
  }

  List<String> findConfFiles() {
    try {
      return ClassPath.from(getClass().getClassLoader()).getResources().stream()
        .map(ClassPath.ResourceInfo::getResourceName)
        .filter(f -> f.startsWith("redpen-conf")).collect(toList());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
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

  public Configuration getConfig() {
    return config;
  }
}
