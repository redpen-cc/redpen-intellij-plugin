package cc.redpen.intellij

import cc.redpen.intellij.fixes.RemoveQuickFix
import cc.redpen.parser.DocumentParser
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.*
import org.junit.Test

import org.junit.Assert.*
import java.util.*

class RedPenListErrorsTest : BaseTest() {
    internal var errorGenerator = RedPenInspectionTest.ErrorGenerator()
    internal val redPenListErrors = spy(RedPenListErrors())

    @Test
    fun actionPerformed() {
        val file = mockTextFile("Hello\nworld!")
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello\nworld!")
        whenever(file.name).thenReturn("foo.txt")
        whenever(redPen.validate(doc)).thenReturn(Arrays.asList(errorGenerator.at(0, 3), errorGenerator.at(3, 5)))

        val event = mock<AnActionEvent>()
        whenever(event.getData(PlatformDataKeys.PROJECT)).thenReturn(project)
        whenever(event.getData(LangDataKeys.PSI_FILE)).thenReturn(file)

        doNothing().whenever(redPenListErrors).showMessage(any(), any(), any())

        redPenListErrors.actionPerformed(event)

        verify(redPenListErrors).showMessage(project, "foo.txt", "1:0-3 Hello (ErrorGenerator)\n1:3-5 Hello (ErrorGenerator)")
    }
}