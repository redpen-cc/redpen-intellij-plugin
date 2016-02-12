package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.mockito.Mockito.*;

public class RedPenSettingsManagerTest extends BaseTest {
  RedPenSettingsManager manager = spy(new RedPenSettingsManager());

  @Before
  public void setUp() throws Exception {
    doNothing().when(manager).restartInspections();
    manager.redPenProvider = mock(RedPenProvider.class, RETURNS_DEEP_STUBS);
    manager.settingsPane = mock(RedPenSettingsPane.class);
    manager.settingsPane.autodetectLanguage = mock(JCheckBox.class);
  }

  @Test
  public void applyLanguage() throws Exception {
    manager.settingsPane.config = mock(Configuration.class);

    manager.apply();

    verify(manager.redPenProvider).setActiveConfig(manager.settingsPane.config);
  }

  @Test
  public void applyAutodetect() throws Exception {
    when(manager.settingsPane.autodetectLanguage.isSelected()).thenReturn(false);

    manager.apply();

    verify(manager.redPenProvider).setAutodetect(false);
  }

  @Test
  public void applyValidatorsAndSymbols() throws Exception {
    Configuration config = mock(Configuration.class);
    when(manager.redPenProvider.getActiveConfig()).thenReturn(config);
    doCallRealMethod().when(manager.settingsPane).save(config);

    manager.apply();

    verify(manager.settingsPane).applyValidatorsChanges(config);
    verify(manager.settingsPane).applySymbolsChanges(config);
    verify(manager).restartInspections();
  }
}