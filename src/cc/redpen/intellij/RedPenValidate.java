package cc.redpen.intellij;

import cc.redpen.RedPen;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

public class RedPenValidate extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        if (file == null)
            Messages.showMessageDialog(project, "No file currently open", "RedPen " + RedPen.VERSION, Messages.getInformationIcon());
        else
            Messages.showMessageDialog(project, file.getText(), file.getName(), Messages.getInformationIcon());
    }
}
