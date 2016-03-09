package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class StartWithCapitalLetterQuickFixTest : BaseQuickFixTest(StartWithCapitalLetterQuickFix("h")) {
    @Test
    fun name() {
        assertEquals("Change to H", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("hello")
        whenever(problem.textRangeInElement).thenReturn(TextRange(0, 1))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(0, 1, "H")
    }
}