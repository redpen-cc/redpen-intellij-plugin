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
import com.intellij.psi.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
  public void nonPlainTextFilesAreIgnored() throws Exception {
    assertNull(inspection.checkFile(mockFileOfType("JAVA"), mock(InspectionManager.class), true));
    assertNull(inspection.checkFile(mockFileOfType("XML"), mock(InspectionManager.class), true));
  }

  @Test
  public void toGlobalOffset_noOffset() throws Exception {
    assertEquals(0, inspection.toGlobalOffset(null, new String[] {""}));
  }

  @Test
  public void toGlobalOffset_singleLine() throws Exception {
    assertEquals(3, inspection.toGlobalOffset(new LineOffset(1, 3), new String[] {"Hello"}));
  }

  @Test
  public void toGlobalOffset_multiLine() throws Exception {
    assertEquals(8, inspection.toGlobalOffset(new LineOffset(2, 3), new String[] {"Hello", "World"}));
  }

  @Test
  public void toRange() throws Exception {
    TextRange textRange = inspection.toRange(errorGenerator.at(5, 5), new String[] {"Hello"});
    assertEquals(new TextRange(5, 5), textRange);
  }

  @Test
  public void toRange_sentenceLevelError() throws Exception {
    Sentence sentence = new Sentence("Hello.", singletonList(new LineOffset(1, 25)), emptyList());
    TextRange textRange = inspection.toRange(errorGenerator.sentence(sentence), new String[] {sentence.getContent()});
    assertEquals(new TextRange(25, 26), textRange);
  }

  @Test
  public void checkFile_convertsRedPenErrorsIntoIDEAProblemDescriptors() throws Exception {
    Document doc = redPen.parse(DocumentParser.PLAIN, "Hello");
    when(redPen.validate(doc)).thenReturn(asList(errorGenerator.at(0, 3), errorGenerator.at(3, 5)));

    ProblemDescriptor[] problems = inspection.checkFile(mockTextFile("Hello"), mock(InspectionManager.class), true);
    assertNotNull(problems);
    assertEquals(2, problems.length);
  }

  @Test
  public void checkFile_splitsTextIntoLinesPreservingAllCharacters() throws Exception {
    inspection = spy(inspection);
    Document doc = redPen.parse(DocumentParser.PLAIN, "Hello\nworld");
    ValidationError error = errorGenerator.at(1, 2);
    when(redPen.validate(doc)).thenReturn(singletonList(error));

    inspection.checkFile(mockTextFile("Hello\nworld"), mock(InspectionManager.class), true);

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(inspection).toRange(eq(error), captor.capture());

    assertArrayEquals(new String[] {"Hello\n", "world"}, captor.getValue());
  }

  private PsiFile mockTextFile(String text) {
    PsiFile psiFile = mockFileOfType("PLAIN_TEXT");
    when(psiFile.getText()).thenReturn(text);
    when(psiFile.getChildren()).thenReturn(new PsiElement[]{mock(PsiElement.class)});
    return psiFile;
  }

  private PsiFile mockFileOfType(String typeName) {
    PsiFile file = mock(PsiFile.class, RETURNS_DEEP_STUBS);
    when(file.getFileType().getName()).thenReturn(typeName);
    return file;
  }

  static class ErrorGenerator extends WordFrequencyValidator {
    public ValidationError at(int start, int end) {
      List<ValidationError> errors = new ArrayList<>();
      setErrorList(errors);
      addErrorWithPosition("Hello", new Sentence("Hello", 1), start, end);
      return errors.get(0);
    }

    public ValidationError sentence(Sentence sentence) {
      List<ValidationError> errors = new ArrayList<>();
      setErrorList(errors);
      addError("Hello", sentence);
      return errors.get(0);
    }
  }
}