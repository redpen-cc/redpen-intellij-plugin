package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

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
  public void applyConfigSwitch() throws Exception {
    manager.settingsPane.setConfig(mock(Configuration.class));
    manager.apply();
    verify(manager.provider).setActiveConfig(manager.settingsPane.getConfig());
  }

  @Test
  public void applyAutodetectIfNeeded() throws Exception {
    when(manager.settingsPane.autodetectLanguage.isSelected()).thenReturn(false);
    manager.apply();
    verify(manager.provider).setAutodetect(false);
  }

  @Test
  public void doNotApplyAutodetectIfNotNeeded() throws Exception {
    when(manager.settingsPane.autodetectLanguage.isSelected()).thenReturn(true);
    manager.apply();
    verify(manager.provider).setAutodetect(true);
  }

  @Test
  public void applyValidatorsAndSymbols() throws Exception {
    manager.apply();
    verify(manager.settingsPane).save();
    verify(manager).restartInspections();
  }

  @Test
  public void reset() throws Exception {
    manager.reset();
    verify(manager.settingsPane).resetChanges();
  }
}