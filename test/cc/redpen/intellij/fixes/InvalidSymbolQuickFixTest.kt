package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType
import cc.redpen.config.SymbolType.*
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.stubbing.OngoingStubbing

class InvalidSymbolQuickFixTest : BaseQuickFixTest(InvalidSymbolQuickFix(Configuration.builder("en").build(), "OK！", TextRange(2, 3))) {
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

    private val leftQuote = Symbol(LEFT_DOUBLE_QUOTATION_MARK, '«', "\"", true, false)
    private val rightQuote = Symbol(RIGHT_DOUBLE_QUOTATION_MARK, '»', "\"", false, true)

    @Test
    fun lookAtSpaceAfterForCorrectSuggestion() {
        mockText("test\" ", TextRange(4, 5), leftQuote, rightQuote)

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(4, 5, "»")
    }

    @Test
    fun lookAtEndOfWordAfterForCorrectSuggestion() {
        mockText("test\"!", TextRange(4, 5), leftQuote, rightQuote)

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(4, 5, "»")
    }

    @Test
    fun lookAtEndOfLineForCorrectSuggestion() {
        mockText("test\"", TextRange(4, 5), leftQuote, rightQuote)

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(4, 5, "»")
    }

    @Test
    fun lookAtSpaceBeforeForCorrectSuggestion() {
        mockText(" \"test", TextRange(1, 2), rightQuote, leftQuote)
        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(1, 2, "«")
    }

    @Test
    fun lookAtBeginningOfLineForCorrectSuggestion() {
        mockText("\"test", TextRange(0, 1), rightQuote, leftQuote)

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(0, 1, "«")
    }

    private fun mockText(fullText: String, range: TextRange, vararg symbols: Symbol): OngoingStubbing<TextRange>? {
        (quickFix as InvalidSymbolQuickFix).let {
            symbols.forEach { s -> it.symbolTable.overrideSymbol(s) }
            it.text = "\"";
            it.fullText = fullText
            it.range = range
            whenever(document.text).thenReturn(it.fullText)
            return whenever(problem.textRangeInElement).thenReturn(it.range)
        }
    }
}