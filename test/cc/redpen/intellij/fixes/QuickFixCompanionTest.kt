package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import cc.redpen.intellij.BaseTest
import cc.redpen.model.Sentence
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class QuickFixCompanionTest() : BaseTest() {
    @Test
    fun sentenceLevelErrorHaveNoQuickFixes() {
        val error = ErrorGenerator.sentence(Sentence("Too long sentence", 1))
        assertNull(BaseQuickFix.forValidator(error, mock(), "full text", TextRange(0, 0)))
    }

    @Test
    fun removeQuickFixByDefault() {
        val error = ErrorGenerator.at(5, 9)
        val quickFix = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        assertTrue(quickFix is RemoveQuickFix)
        assertEquals("text", quickFix?.text)
    }

    @Test
    fun validatorSpecificQuickFix() {
        val error = spy(ErrorGenerator.at(5, 9))
        doReturn("Hyphenation").whenever(error).validatorName
        val quickFix = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        assertTrue(quickFix is HyphenateQuickFix)
    }

    @Test
    fun supportedSentenceLevelQuickFix() {
        val error = spy(ErrorGenerator.sentence(Sentence("Too long sentence", 1)))
        doReturn("ParagraphStartWith").whenever(error).validatorName
        val quickFix = BaseQuickFix.forValidator(error, mock(), "full text", TextRange(5, 9))
        assertTrue(quickFix is ParagraphStartWithQuickFix)
    }
}