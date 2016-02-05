package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class RedPenSettingsManager implements SearchableConfigurable {
  RedPenProvider redPenProvider = RedPenProvider.getInstance();
  RedPenSettingsPane settingsPane;

  @NotNull @Override public String getId() {
    return getHelpTopic();
  }

  @Nullable @Override public Runnable enableSearch(String s) {
    return null;
  }

  @Nls @Override public String getDisplayName() {
    return "RedPen";
  }

  @Nullable @Override public String getHelpTopic() {
    return "reference.settings.ide.settings.redpen";
  }

  @Nullable @Override public JComponent createComponent() {
    Configuration config = redPenProvider.getConfig();
    settingsPane = new RedPenSettingsPane(config);
    return settingsPane.getPane();
  }

  @Override public boolean isModified() {
    return true;
  }

  @Override public void apply() throws ConfigurationException {
    List<ValidatorConfiguration> validators = redPenProvider.getConfig().getValidatorConfigs();
    List<ValidatorConfiguration> remainingValidators = settingsPane.getActiveValidators();
    validators.clear();
    validators.addAll(remainingValidators);
  }

  @Override public void reset() {
  }

  @Override public void disposeUIResources() {
  }
}
