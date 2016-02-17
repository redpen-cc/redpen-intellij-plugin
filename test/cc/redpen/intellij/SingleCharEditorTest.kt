package cc.redpen.intellij

import org.junit.Assert.*
import org.junit.Test
import javax.swing.JTextField

class SingleCharEditorTest {
    private val editor = SingleCharEditor()

    @Test
    fun editingCannotBeStoppedIfEmpty() {
        assertFalse(editor.stopCellEditing())
    }

    @Test
    fun editingCannotBeStoppedIfMoreChars() {
        (editor.component as JTextField).text = "ab"
        assertFalse(editor.stopCellEditing())
    }

    @Test
    fun editingCanBeStoppedIfSingleChar() {
        (editor.component as JTextField).text = "a"
        assertTrue(editor.stopCellEditing())
    }

    @Test
    fun cannotInputMoreThanOneChar() {
        val document = (editor.component as JTextField).document
        document.insertString(0, "a", null)
        assertEquals("a", document.getText(0, document.length))

        document.insertString(1, "b", null)
        assertEquals("a", document.getText(0, document.length))
    }
}