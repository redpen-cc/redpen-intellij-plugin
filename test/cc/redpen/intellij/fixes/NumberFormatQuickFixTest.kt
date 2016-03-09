package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import cc.redpen.config.ValidatorConfiguration
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatQuickFixTest : BaseQuickFixTest(NumberFormatQuickFix(createConfig("en"), "7000000,50")) {
    companion object {
        fun createConfig(lang: String) = Configuration.builder(lang).addValidatorConfig(ValidatorConfiguration("NumberFormat")).build()
    }

    @Test
    fun name() {
        assertEquals("Change to 7,000,000.50", quickFix.name)
    }

    @Test
    fun applyFixForUS() {
        whenever(document.text).thenReturn("Amount: $7000000.50")
        whenever(problem.textRangeInElement).thenReturn(TextRange(9, 19))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(9, 19, "7,000,000.50")
    }

    @Test
    fun applyFixForUK() {
        (quickFix as NumberFormatQuickFix).config.validatorConfigs[0].properties["decimal_delimiter_is_comma"] = "true"

        whenever(document.text).thenReturn("Amount: £7000000.50")
        whenever(problem.textRangeInElement).thenReturn(TextRange(9, 19))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(9, 19, "7.000.000,50")
    }

    @Test
    fun applyFixForJapaneseZenkaku() {
        (quickFix as NumberFormatQuickFix).config = createConfig("ja")

        whenever(document.text).thenReturn("7000000.50元")
        whenever(problem.textRangeInElement).thenReturn(TextRange(0, 10))

        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(0, 10, "7．000．000・50")
    }
}

