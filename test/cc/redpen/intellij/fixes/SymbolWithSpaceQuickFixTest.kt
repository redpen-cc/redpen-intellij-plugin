package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolWithSpaceQuickFixTest : BaseQuickFixTest(SymbolWithSpaceQuickFix(Configuration.builder().build(), "")) {
    @Test
    fun name() {
        assertEquals("Add space", quickFix.name)
    }

    @Test
    fun applyFixForSpaceBefore() {
        quickFix.text = "("
        whenever(document.text).thenReturn("Hello(World)")
        whenever(problem.textRangeInElement).thenReturn(TextRange(5, 6))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(5, 6, " (")
    }

    @Test
    fun applyFixForSpaceAfter() {
        quickFix.text = ")"
        whenever(document.text).thenReturn("(Hello)World")
        whenever(problem.textRangeInElement).thenReturn(TextRange(6, 7))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(6, 7, ") ")
    }
}