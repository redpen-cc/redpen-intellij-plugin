package cc.redpen.intellij;

import org.junit.Test;

import javax.swing.*;
import javax.swing.text.Document;

import static org.junit.Assert.*;

public class SingleCharEditorTest {
  private SingleCharEditor editor = new SingleCharEditor();

  @Test
  public void editingCannotBeStoppedIfEmpty() throws Exception {
    assertFalse(editor.stopCellEditing());
  }

  @Test
  public void editingCannotBeStoppedIfMoreChars() throws Exception {
    ((JTextField)editor.getComponent()).setText("ab");
    assertFalse(editor.stopCellEditing());
  }

  @Test
  public void editingCanBeStoppedIfSingleChar() throws Exception {
    ((JTextField)editor.getComponent()).setText("a");
    assertTrue(editor.stopCellEditing());
  }

  @Test
  public void cannotInputMoreThanOneChar() throws Exception {
    Document document = ((JTextField)editor.getComponent()).getDocument();
    document.insertString(0, "a", null);
    assertEquals("a", document.getText(0, document.getLength()));

    document.insertString(1, "b", null);
    assertEquals("a", document.getText(0, document.getLength()));
  }
}