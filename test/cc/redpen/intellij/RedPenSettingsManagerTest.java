package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static cc.redpen.config.SymbolType.BACKSLASH;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
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
  public void applyValidators() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(new ValidatorConfiguration("1"), new ValidatorConfiguration("2"));
    when(manager.redPenProvider.getActiveConfig()).thenReturn(redPenConfigWithValidators(allValidators));

    List<ValidatorConfiguration> activeValidators = new ArrayList<>(allValidators.subList(0, 1));
    when(manager.settingsPane.getActiveValidators()).thenReturn(activeValidators);

    manager.apply();

    assertEquals(activeValidators, manager.redPenProvider.getActiveConfig().getValidatorConfigs());
    verify(manager).restartInspections();
  }

  @Test
  public void applySymbols() throws Exception {
    Symbol symbol = new Symbol(BACKSLASH, '\\');
    when(manager.settingsPane.getSymbols()).thenReturn(singletonList(symbol));

    manager.apply();

    verify(manager.redPenProvider.getActiveConfig().getSymbolTable()).overrideSymbol(symbol);
    verify(manager).restartInspections();
  }
}