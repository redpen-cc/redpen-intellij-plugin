package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class EndOfSentenceQuickFixTest : BaseQuickFixTest(EndOfSentenceQuickFix("\"")) {
    @Test
    fun name() {
        assertEquals("Swap symbols", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("Hello \"world\".")
        whenever(problem.textRangeInElement).thenReturn(TextRange(12, 13))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(12, 14, ".\"")
    }
}