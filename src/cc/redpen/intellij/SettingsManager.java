package cc.redpen.intellij;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsManager implements SearchableConfigurable {
  RedPenProvider provider = RedPenProvider.Companion.getInstance();
  SettingsPane settingsPane = new SettingsPane(provider);

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
    return settingsPane.getPane();
  }

  @Override public boolean isModified() {
    return true;
  }

  @Override public void apply() throws ConfigurationException {
    provider.setActiveConfig(settingsPane.getConfig());
    provider.setAutodetect(settingsPane.autodetectLanguage.isSelected());
    settingsPane.save();
    restartInspections();
  }

  @Override public void reset() {
    settingsPane.resetChanges();
  }

  @Override public void disposeUIResources() {
  }

  public void restartInspections() {
    ApplicationManager.getApplication().invokeLater(() -> {
      Project[] projects = ProjectManager.getInstance().getOpenProjects();
      for (Project project : projects) {
        if (project.isInitialized() && project.isOpen() && !project.isDefault()) {
          DaemonCodeAnalyzer.getInstance(project).restart();
        }
      }
    });
  }
}
