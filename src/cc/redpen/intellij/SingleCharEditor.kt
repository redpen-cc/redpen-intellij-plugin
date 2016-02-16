package cc.redpen.intellij

import java.awt.Color
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.border.LineBorder
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

internal class SingleCharEditor : DefaultCellEditor(JTextField(SingleCharEditor.SingleCharDocument(), null, 1)) {
    init {
        (component as JComponent).border = LineBorder(Color.black)
    }

    override fun stopCellEditing(): Boolean {
        return (component as JTextField).text.length == 1 && super.stopCellEditing()
    }

    internal class SingleCharDocument : PlainDocument() {
        override fun insertString(offset: Int, str: String?, a: AttributeSet?) {
            if (str != null && str.length + length == 1) super.insertString(offset, str, a)
        }
    }
}
