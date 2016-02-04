package cc.redpen.intellij;

import cc.redpen.RedPen;
import cc.redpen.model.Document;
import cc.redpen.parser.DocumentParser;
import cc.redpen.validator.ValidationError;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class RedPenValidate extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        if (file == null) {
            Messages.showMessageDialog(project, "No file currently open", "RedPen " + RedPen.VERSION, Messages.getInformationIcon());
            return;
        }

        try {
            RedPen redPen = new RedPen("/redpen-conf.xml");
            Document redPenDoc = redPen.parse(DocumentParser.PLAIN, file.getText());
            List<ValidationError> errors = redPen.validate(redPenDoc);

            Messages.showMessageDialog(project, errors.stream().map(ValidationError::getMessage).collect(joining("\n")), file.getName(), Messages.getInformationIcon());
        }
        catch (Exception e) {
            Messages.showMessageDialog(project, e.toString(), "RedPen " + RedPen.VERSION, Messages.getInformationIcon());
        }
    }
}
