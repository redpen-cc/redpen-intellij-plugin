package cc.redpen.intellij;

import cc.redpen.ConfigurationLoader;
import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.model.Document;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.LineOffset;
import cc.redpen.validator.ValidationError;
import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPlainTextFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class RedPenInspection extends LocalInspectionTool {
  RedPenProvider redPenProvider = new RedPenProvider();

  Map<String, DocumentParser> parsers = ImmutableMap.of(
    "PLAIN_TEXT", DocumentParser.PLAIN,
    "Markdown", DocumentParser.MARKDOWN
  );

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
    String fileType = file.getFileType().getName();
    if (!parsers.containsKey(fileType)) return null;

    try {
      RedPen redPen = redPenProvider.getRedPen();
      String text = file.getText();
      Document redPenDoc = redPen.parse(parsers.get(fileType), text);
      List<ValidationError> errors = redPen.validate(redPenDoc);

      PsiElement theElement = file.getChildren()[0];
      String[] lines = text.split("(?<=\n)");

      List<ProblemDescriptor> problems = errors.stream().map(e ->
        manager.createProblemDescriptor(theElement, toRange(e, lines),
          e.getMessage(), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly)
      ).collect(toList());

      return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
    catch (RedPenException e) {
      throw new RuntimeException(e);
    }
  }

  TextRange toRange(ValidationError e, String[] lines) {
    LineOffset start = e.getStartPosition().orElse(e.getSentence().getOffset(0).orElse(null));
    LineOffset end = e.getEndPosition().orElse(addOne(e.getSentence().getOffset(0).orElse(null)));
    return new TextRange(toGlobalOffset(start, lines), toGlobalOffset(end, lines));
  }

  private LineOffset addOne(LineOffset lineOffset) {
    return new LineOffset(lineOffset.lineNum, lineOffset.offset+1);
  }

  int toGlobalOffset(@Nullable LineOffset lineOffset, String[] lines) {
    if (lineOffset == null) return 0;
    int result = 0;
    for (int i = 1; i < lineOffset.lineNum; i++) {
      result += lines[i-1].length();
    }
    return result + lineOffset.offset;
  }
}
