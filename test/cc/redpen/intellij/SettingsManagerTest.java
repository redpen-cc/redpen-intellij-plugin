package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SettingsManagerTest extends BaseTest {
  SettingsManager manager = spy(new SettingsManager());

  @Before
  public void setUp() throws Exception {
    doNothing().when(manager).restartInspections();
    manager.provider = mock(RedPenProvider.class, RETURNS_DEEP_STUBS);
    manager.settingsPane = mock(SettingsPane.class);
    manager.settingsPane.autodetectLanguage = mock(JCheckBox.class);
  }

  @Test
  public void applyLanguage() throws Exception {
    manager.settingsPane.config = mock(Configuration.class);

    manager.apply();

    verify(manager.provider).setActiveConfig(manager.settingsPane.config);
  }

  @Test
  public void applyAutodetect() throws Exception {
    when(manager.settingsPane.autodetectLanguage.isSelected()).thenReturn(false);

    manager.apply();

    verify(manager.provider).setAutodetect(false);
  }

  @Test
  public void applyValidatorsAndSymbols() throws Exception {
    Configuration config = manager.settingsPane.config = cloneableConfig("en");
    doCallRealMethod().when(manager.settingsPane).apply();

    manager.apply();

    assertEquals(config.clone(), manager.settingsPane.config);
    verify(manager.settingsPane).applyValidatorsChanges();
    verify(manager.settingsPane).applySymbolsChanges();
    verify(manager).restartInspections();
  }

  @Test
  public void reset() throws Exception {
    manager.reset();

    verify(manager.settingsPane).initTabs();
  }
}