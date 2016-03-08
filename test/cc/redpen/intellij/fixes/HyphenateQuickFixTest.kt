package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class HyphenateQuickFixTest : BaseQuickFixTest(HyphenateQuickFix("can do")) {
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