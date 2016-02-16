package cc.redpen.intellij;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;

public class RedPenInspectionProvider implements InspectionToolProvider {
  public RedPenInspectionProvider() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      if (project.isInitialized() && project.isOpen() && !project.isDefault()) {
        WindowManager.getInstance().getStatusBar(project).addWidget(new StatusWidget(project), "before Encoding");
      }
    }
  }

  public Class[] getInspectionClasses() {
    return new Class[] { RedPenInspection.class};
  }
}
