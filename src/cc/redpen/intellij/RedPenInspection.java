package cc.redpen.intellij;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.model.Document;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.LineOffset;
import cc.redpen.validator.ValidationError;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class RedPenInspection extends LocalInspectionTool {
  RedPen redPen;

  public RedPenInspection() {
    try {
      redPen = new RedPen("/redpen-conf.xml");
    }
    catch (RedPenException e) {
      throw new RuntimeException("Cannot read RedPen conf file", e);
    }
  }

  @NotNull @Override public String getDisplayName() {
    return "RedPen Validation";
  }

  @NotNull @Override public String getGroupDisplayName() {
    return GroupNames.STYLE_GROUP_NAME;
  }

  @NotNull @Override public String getShortName() {
    return "RedPen";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nullable @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    try {
      String text = file.getText();
      redPen = new RedPen("/redpen-conf.xml");
      Document redPenDoc = redPen.parse(DocumentParser.PLAIN, text);
      List<ValidationError> errors = redPen.validate(redPenDoc);

      PsiElement theElement = file.getChildren()[0];

      List<ProblemDescriptor> problems = errors.stream().map(e ->
        manager.createProblemDescriptor(theElement, toRange(e, text),
          e.getMessage(), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly)
      ).collect(toList());

      return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  TextRange toRange(ValidationError e, String text) {
    return new TextRange(toGlobalOffset(e.getStartPosition(), text), toGlobalOffset(e.getEndPosition(), text));
  }

  int toGlobalOffset(Optional<LineOffset> lineOffset, String text) {
    if (!lineOffset.isPresent()) return 0;
    String[] lines = text.split("\r?\n");
    int result = 0;
    for (int i = 1; i < lineOffset.get().lineNum; i++) {
      result += lines[i-1].length();
    }
    return result + lineOffset.get().offset;
  }
}