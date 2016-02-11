package cc.redpen.intellij;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RedPenProviderTest {
  private RedPenProvider provider = RedPenProvider.getInstance();

  @Test
  public void allConfigFilesAreLoaded() throws Exception {
    assertTrue(provider.getAvailableConfigs().keySet().contains("en"));
    assertTrue(provider.getAvailableConfigs().keySet().contains("ja"));
    assertTrue(provider.getAvailableConfigs().keySet().contains("ja.hankaku"));
    assertTrue(provider.getAvailableConfigs().keySet().contains("ja.zenkaku2"));
  }
}