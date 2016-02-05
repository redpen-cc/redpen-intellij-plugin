package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RedPenSettingsManager implements SearchableConfigurable {
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
    Configuration config = RedPenProvider.getInstance().getConfig();
    return new RedPenSettingsPane(config).getPane();
  }

  @Override public boolean isModified() {
    return false;
  }

  @Override public void apply() throws ConfigurationException {

  }

  @Override public void reset() {

  }

  @Override public void disposeUIResources() {

  }
}
