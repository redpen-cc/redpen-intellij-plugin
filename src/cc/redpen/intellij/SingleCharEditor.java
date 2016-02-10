package cc.redpen.intellij;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

class SingleCharEditor extends DefaultCellEditor {
  public SingleCharEditor() {
    super(new JTextField(new SingleCharDocument(), null, 1));
  }

  @Override public boolean stopCellEditing() {
    return ((JTextField)getComponent()).getText().length() == 1 && super.stopCellEditing();
  }

  static class SingleCharDocument extends PlainDocument {
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
      if (str != null && str.length() + getLength() == 1) super.insertString(offset, str, a);
    }
  }
}
