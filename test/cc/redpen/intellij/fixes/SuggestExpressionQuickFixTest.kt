package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestExpressionQuickFixTest : BaseQuickFixTest(SuggestExpressionQuickFix("info", "Found invalid word \"info\". Use the synonym \"information\" instead.")) {
    @Test
    fun name() {
        assertEquals("Change to information", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("More info here")
        whenever(problem.textRangeInElement).thenReturn(TextRange(5, 9))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(5, 9, "information")
    }
}