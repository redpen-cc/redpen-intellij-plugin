package cc.redpen.intellij;

import cc.redpen.config.ValidatorConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RedPenSettingsManagerTest extends BaseTest {
  RedPenSettingsManager manager = new RedPenSettingsManager();

  @Test
  public void applyEnabledValidators() throws Exception {
    List<ValidatorConfiguration> allValidators = asList(new ValidatorConfiguration("1"), new ValidatorConfiguration("2"));
    manager.redPenProvider = mock(RedPenProvider.class, RETURNS_DEEP_STUBS);
    when(manager.redPenProvider.getConfig()).thenReturn(redPenConfig(allValidators));

    List<ValidatorConfiguration> activeValidators = new ArrayList<>(allValidators.subList(0, 1));
    manager.settingsPane = mock(RedPenSettingsPane.class);
    when(manager.settingsPane.getActiveValidators()).thenReturn(activeValidators);

    manager.apply();

    assertEquals(activeValidators, manager.redPenProvider.getConfig().getValidatorConfigs());
  }
}