package cc.redpen.intellij;

import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;
import org.junit.Before;
import org.junit.Test;

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
  }

  @Test
  public void applyValidators() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(new ValidatorConfiguration("1"), new ValidatorConfiguration("2"));
    when(manager.redPenProvider.getConfig()).thenReturn(redPenConfigWithValidators(allValidators));

    List<ValidatorConfiguration> activeValidators = new ArrayList<>(allValidators.subList(0, 1));
    when(manager.settingsPane.getActiveValidators()).thenReturn(activeValidators);

    manager.apply();

    assertEquals(activeValidators, manager.redPenProvider.getConfig().getValidatorConfigs());
    verify(manager).restartInspections();
  }

  @Test
  public void applySymbols() throws Exception {
    Symbol symbol = new Symbol(BACKSLASH, '\\');
    when(manager.settingsPane.getSymbols()).thenReturn(singletonList(symbol));

    manager.apply();

    verify(manager.redPenProvider.getConfig().getSymbolTable()).overrideSymbol(symbol);
    verify(manager).restartInspections();
  }
}