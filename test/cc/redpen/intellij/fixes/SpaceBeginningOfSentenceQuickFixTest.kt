package cc.redpen.intellij.fixes

import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceBeginningOfSentenceQuickFixTest : BaseQuickFixTest(SpaceBeginningOfSentenceQuickFix("")) {
    @Test
    fun name() {
        assertEquals("Add space", quickFix.name)
    }

    @Test
    fun applyFix() {
        whenever(document.text).thenReturn("First sentence.Second sentence.")
        whenever(problem.textRangeInElement).thenReturn(TextRange(16, 17))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(16, 16, " ")
    }
}