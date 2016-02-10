package cc.redpen.intellij;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class RedPenProviderTest {
  private RedPenProvider provider = RedPenProvider.getInstance();

  @Test
  public void findConfigFiles() throws Exception {
    List<String> confFiles = provider.findConfFiles();
    assertTrue(confFiles.size() >= 4);
    assertTrue(confFiles.contains("redpen-conf.xml"));
  }

  @Test
  public void allConfigFilesAreLoaded() throws Exception {
    assertTrue(provider.configs.keySet().contains("en"));
    assertTrue(provider.configs.keySet().contains("ja"));
    assertTrue(provider.configs.keySet().contains("ja.hankaku"));
    assertTrue(provider.configs.keySet().contains("ja.zenkaku2"));
  }
}