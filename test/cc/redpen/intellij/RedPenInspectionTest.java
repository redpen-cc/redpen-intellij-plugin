package cc.redpen.intellij;

import cc.redpen.RedPen;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.LineOffset;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.section.WordFrequencyValidator;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedPenInspectionTest {
  RedPenInspection inspection = new RedPenInspection();
  ErrorGenerator errorGenerator = new ErrorGenerator();
  RedPen redPen;

  @Before
  public void setUp() throws Exception {
    inspection.redPenProvider = mock(RedPenProvider.class, RETURNS_DEEP_STUBS);
    redPen = inspection.redPenProvider.getRedPen();
  }

  @Test
  public void toGlobalOffset_noOffset() throws Exception {
    assertEquals(0, inspection.toGlobalOffset(Optional.empty(), new String[] {""}));
  }

  @Test
  public void toGlobalOffset_singleLine() throws Exception {
    assertEquals(3, inspection.toGlobalOffset(Optional.of(new LineOffset(1, 3)), new String[] {"Hello"}));
  }

  @Test
  public void toGlobalOffset_multiLine() throws Exception {
    assertEquals(8, inspection.toGlobalOffset(Optional.of(new LineOffset(2, 3)), new String[] {"Hello", "World"}));
  }

  @Test
  public void toRange() throws Exception {
    TextRange textRange = inspection.toRange(errorGenerator.at(5, 5), new String[] {"Hello"});
    assertEquals(new TextRange(5, 5), textRange);
  }

  @Test
  public void checkFile_convertsRedPenErrorsIntoIDEAProblemDescriptors() throws Exception {
    Document doc = redPen.parse(DocumentParser.PLAIN, "Hello");
    when(redPen.validate(doc)).thenReturn(asList(errorGenerator.at(0, 3), errorGenerator.at(3, 5)));

    ProblemDescriptor[] problems = inspection.checkFile(mockPsiFile("Hello"), mock(InspectionManager.class), true);
    assertNotNull(problems);
    assertEquals(2, problems.length);
  }

  private PsiFile mockPsiFile(String text) {
    PsiFile psiFile = mock(PsiFile.class);
    when(psiFile.getText()).thenReturn(text);
    when(psiFile.getChildren()).thenReturn(new PsiElement[]{mock(PsiElement.class)});
    return psiFile;
  }

  static class ErrorGenerator extends WordFrequencyValidator {
    public ValidationError at(int start, int end) {
      List<ValidationError> errors = new ArrayList<>();
      setErrorList(errors);
      addErrorWithPosition("Hello", new Sentence("Hello", 1), start, end);
      return errors.get(0);
    }
  }

}