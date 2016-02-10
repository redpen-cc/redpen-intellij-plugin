package cc.redpen.intellij;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RedPenProviderTest {
  private RedPenProvider provider = RedPenProvider.getInstance();

  @Test
  public void allConfigFilesAreLoaded() throws Exception {
    assertTrue(provider.configs.keySet().contains("en"));
    assertTrue(provider.configs.keySet().contains("ja"));
    assertTrue(provider.configs.keySet().contains("ja.hankaku"));
    assertTrue(provider.configs.keySet().contains("ja.zenkaku2"));
  }
}