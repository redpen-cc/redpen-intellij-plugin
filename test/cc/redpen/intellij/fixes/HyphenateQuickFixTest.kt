package cc.redpen.intellij.fixes

import cc.redpen.intellij.BaseTest
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class HyphenateQuickFixTest : BaseTest() {
    val problem = mock<ProblemDescriptor>(RETURNS_DEEP_STUBS)
    val document = mock<Document>()
    val psiElement = problem.psiElement
    val quickFix = spy(HyphenateQuickFix("can do"))

    @Before
    fun setUp() {
        doReturn(document).whenever(quickFix).containingDocument(psiElement)
        doNothing().whenever(quickFix).writeAction(any(), any())
    }

    @Test
    fun name() {
        assertEquals("Change to can-do", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("mega can do it")
        whenever(problem.textRangeInElement).thenReturn(TextRange(5, 11))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(5, 11, "can-do")
    }
}