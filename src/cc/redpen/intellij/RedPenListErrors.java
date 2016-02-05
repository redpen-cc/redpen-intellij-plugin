package cc.redpen.intellij;

import cc.redpen.RedPen;
import cc.redpen.model.Document;
import cc.redpen.parser.LineOffset;
import cc.redpen.validator.ValidationError;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

public class RedPenListErrors extends AnAction {
    RedPenProvider redPenProvider = RedPenProvider.getInstance();

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        String title = "RedPen " + RedPen.VERSION;
        if (file == null) {
            Messages.showMessageDialog(project, "No file currently active", title, Messages.getInformationIcon());
            return;
        }

        try {
            RedPen redPen = redPenProvider.getRedPen();
            Document redPenDoc = redPen.parse(redPenProvider.getParser(file), file.getText());
            List<ValidationError> errors = redPen.validate(redPenDoc);

            Messages.showMessageDialog(project, errors.stream().map(e ->
              getLineNumber(e) + ":" + getOffset(e.getStartPosition()) + "-" + getOffset(e.getEndPosition()) + " " + e.getMessage())
              .collect(joining("\n")), file.getName(), Messages.getInformationIcon());
        }
        catch (Exception e) {
            Messages.showMessageDialog(project, e.toString(), title, Messages.getInformationIcon());
        }
    }

    private Serializable getLineNumber(ValidationError e) {
        return e.getStartPosition().isPresent() ? e.getStartPosition().get().lineNum : "?";
    }

    private String getOffset(Optional<LineOffset> lineOffset) {
        return lineOffset.isPresent() ? String.valueOf(lineOffset.get().offset) : "?";
    }
}
