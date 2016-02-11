package cc.redpen.intellij;

import cc.redpen.RedPen;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RedPenProviderTest {
  private RedPenProvider provider = RedPenProvider.getInstance();

  @Test
  public void allConfigFilesAreLoaded() throws Exception {
    assertEquals("en", provider.getConfig("en").getKey());
    assertEquals("ja", provider.getConfig("ja").getKey());
    assertEquals("ja.hankaku", provider.getConfig("ja.hankaku").getKey());
    assertEquals("ja.zenkaku2", provider.getConfig("ja.zenkaku2").getKey());
  }

  @Test
  public void getRedPenFor_autodetectsLanguage() throws Exception {
    RedPen redPen = provider.getRedPenFor("Hello");
    assertEquals("en", redPen.getConfiguration().getKey());

    redPen = provider.getRedPenFor("こんにちは");
    assertEquals("ja", redPen.getConfiguration().getKey());
  }

  @Test
  public void languageAutodetectionCanBeDisabled() throws Exception {
    provider.setAutodetect(false);

    RedPen redPen = provider.getRedPenFor("こんにちは");
    assertEquals("en", redPen.getConfiguration().getKey());
  }
}