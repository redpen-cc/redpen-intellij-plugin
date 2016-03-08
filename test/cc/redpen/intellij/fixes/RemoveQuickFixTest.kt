package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoveQuickFixTest : BaseQuickFixTest(RemoveQuickFix("very")) {
    @Test
    fun name() {
        assertEquals("Remove very", quickFix.name)
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