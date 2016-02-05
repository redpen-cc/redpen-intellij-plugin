package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.parser.DocumentParser;
import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiFile;

import java.util.Map;

public class RedPenProvider {
  private static RedPenProvider instance;
  Configuration config;

  Map<String, DocumentParser> parsers = ImmutableMap.of(
    "PLAIN_TEXT", DocumentParser.PLAIN,
    "Markdown", DocumentParser.MARKDOWN,
    "AsciiDoc", DocumentParser.ASCIIDOC
  );

  private RedPenProvider() {
    try {
      config = new ConfigurationLoader().loadFromResource("/redpen-conf.xml");
    }
    catch (RedPenException e) {
      throw new RuntimeException("Cannot read RedPen conf file", e);
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

  public Configuration getConfig() {
    return config;
  }
}
