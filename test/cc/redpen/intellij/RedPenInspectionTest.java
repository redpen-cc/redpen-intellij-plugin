package cc.redpen.intellij;

import cc.redpen.model.Sentence;
import cc.redpen.parser.LineOffset;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;
import cc.redpen.validator.section.WordFrequencyValidator;
import com.intellij.openapi.util.TextRange;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class RedPenInspectionTest {
  RedPenInspection inspection = new RedPenInspection();
  ErrorGenerator errorGenerator = new ErrorGenerator();

  @Test
  public void toGlobalOffset_noOffset() throws Exception {
    assertEquals(0, inspection.toGlobalOffset(Optional.empty(), ""));
  }

  @Test
  public void toGlobalOffset_singleLine() throws Exception {
    assertEquals(3, inspection.toGlobalOffset(Optional.of(new LineOffset(1, 3)), "Hello"));
  }

  @Test
  public void toGlobalOffset_multiLine() throws Exception {
    assertEquals(8, inspection.toGlobalOffset(Optional.of(new LineOffset(2, 3)), "Hello\nWorld"));
  }

  @Test
  public void toRange() throws Exception {
    TextRange textRange = inspection.toRange(errorGenerator.at(5, 5), "Hello");
    assertEquals(new TextRange(5, 5), textRange);
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