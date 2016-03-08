package cc.redpen.intellij

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.*
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class RedPenQuickFixTest : BaseTest() {

    @Test
    fun applyFix() {
        val problem = mock<ProblemDescriptor>(RETURNS_DEEP_STUBS)
        val document = mock<Document>()
        val quickFix = spy(RedPenQuickFix("DoubledWord"))
        val psiElement = problem.psiElement

        doReturn(document).whenever(quickFix).containingDocument(psiElement)
        whenever(document.text).thenReturn("foo  foo bar")
        whenever(problem.textRangeInElement).thenReturn(TextRange(5, 8))

        quickFix.applyFix(project, problem)

        verify(document).replaceString(3, 8, "")
    }
}