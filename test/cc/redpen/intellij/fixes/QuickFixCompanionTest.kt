package cc.redpen.intellij.fixes

import cc.redpen.intellij.BaseTest
import cc.redpen.model.Sentence
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickFixCompanionTest() : BaseTest() {
    @Test
    fun sentenceLevelErrorHaveNoQuickFixes() {
        val error = ErrorGenerator.sentence(Sentence("Too long sentence", 1))
        val quickFixes = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(0, 0))
        assertEquals(0, quickFixes.size)
    }

    @Test
    fun removeQuickFixByDefault() {
        val error = ErrorGenerator.at(5, 9)
        val quickFixes = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        val quickFix = quickFixes.get(0)
        assertTrue(quickFix is RemoveQuickFix)
        assertEquals("text", quickFix?.text)
    }

    @Test
    fun validatorSpecificQuickFix() {
        val error = spy(ErrorGenerator.at(5, 9))
        doReturn("Hyphenation").whenever(error).validatorName
        val quickFixes = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        val quickFix = quickFixes.get(0)
        assertTrue(quickFix is HyphenateQuickFix)
    }

    @Test
    fun supportedSentenceLevelQuickFix() {
        val error = spy(ErrorGenerator.sentence(Sentence("Too long sentence", 1)))
        doReturn("ParagraphStartWith").whenever(error).validatorName
        val quickFixes = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        val quickFix = quickFixes.get(0)
        assertTrue(quickFix is ParagraphStartWithQuickFix)
    }
}
