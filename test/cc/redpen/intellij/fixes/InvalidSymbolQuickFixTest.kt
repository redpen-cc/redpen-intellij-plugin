package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class InvalidSymbolQuickFixTest : BaseQuickFixTest(InvalidSymbolQuickFix(Configuration.builder().build(), "！")) {
    @Test
    fun name() {
        assertEquals("Change to !", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("OK！")
        whenever(problem.textRangeInElement).thenReturn(TextRange(2, 3))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(2, 3, "!")
    }
}