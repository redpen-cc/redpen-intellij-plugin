package cc.redpen.intellij;

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
}