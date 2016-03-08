package cc.redpen.intellij

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class RedPenQuickFixTest : BaseTest() {
    val problem = mock<ProblemDescriptor>(RETURNS_DEEP_STUBS)
    val document = mock<Document>()
    val psiElement = problem.psiElement
    val quickFix = spy(RedPenQuickFix("DoubledWord"))

    @Before
    fun setUp() {
        doReturn(document).whenever(quickFix).containingDocument(psiElement)
        doNothing().whenever(quickFix).writeAction(any(), any())
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("foo  foo bar")
        whenever(problem.textRangeInElement).thenReturn(TextRange(5, 8))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(3, 8, "")
    }
}