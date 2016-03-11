package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import cc.redpen.config.ValidatorConfiguration
import com.intellij.openapi.util.TextRange
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.*
import org.junit.Test

class ParagraphStartWithQuickFixTest : BaseQuickFixTest(ParagraphStartWithQuickFix(Configuration.builder()
        .addValidatorConfig(ValidatorConfiguration("ParagraphStartWith").addProperty("start_from", "    ")).build(),
        "mega can do it", TextRange(0, 1))) {

    @Test
    fun name() {
        assertEquals("Add paragraph prefix     ", quickFix.name)
    }

    @Test
    fun applyFixNoPrefix() {
        (quickFix as ParagraphStartWithQuickFix).let {
            whenever(document.text).thenReturn(it.fullText)
            whenever(problem.textRangeInElement).thenReturn(it.range)
        }
        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(0, 0, "    ")
    }

    @Test
    fun applyFixWrongPrefix() {
        (quickFix as ParagraphStartWithQuickFix).let {
            it.fullText = "  mega can do it";
            whenever(document.text).thenReturn(it.fullText)
            whenever(problem.textRangeInElement).thenReturn(it.range)
        }
        quickFix.applyFix(project, problem)

        verify(quickFix).writeAction(eq(project), capture { it.invoke() })
        verify(document).replaceString(0, 2, "    ")
    }
}