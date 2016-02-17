package cc.redpen.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class StatusWidget extends EditorBasedWidget implements CustomStatusBarWidget {
  RedPenProvider provider = RedPenProvider.getInstance();
  private final TextPanel component;

  protected StatusWidget(@NotNull Project project) {
    super(project);
    component = new TextPanel.ExtraSize();
  }

  public void update(String configKey) {
    component.setText("RedPen: " + configKey);
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    VirtualFile newFile = event.getNewFile();
    Project project = getProject();
    if (ApplicationManager.getApplication().isUnitTestMode() || project == null || newFile == null) return;

    PsiFile file = PsiManager.getInstance(project).findFile(newFile);

    if (file == null || provider.getParser(file) == null) return;
    update(provider.getConfigKeyFor(file.getText()));
  }

  @Override
  public JComponent getComponent() {
    return component;
  }

  @NotNull @Override public String ID() {
    return "RedPen";
  }

  @Nullable @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType platformType) {
    return null;
  }
}
