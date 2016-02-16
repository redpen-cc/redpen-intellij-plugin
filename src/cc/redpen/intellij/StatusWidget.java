package cc.redpen.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class StatusWidget extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {
  RedPenProvider provider = RedPenProvider.getInstance();
  private final TextPanel component;

  protected StatusWidget(@NotNull Project project) {
    super(project);
    component = new TextPanel.ExtraSize();
    update();
  }

  private void update() {
    component.setText("RedPen: " + provider.getActiveConfig().getKey());
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    provider.getRedPenFor(FileDocumentManager.getInstance().getDocument(event.getNewFile()).getText());
    update();
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

  @Override public StatusBarWidget copy() {
    return new StatusWidget(getProject());
  }
}
