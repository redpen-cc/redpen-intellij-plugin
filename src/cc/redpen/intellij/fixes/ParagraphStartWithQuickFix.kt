package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.util.TextRange

open class ParagraphStartWithQuickFix(config: Configuration, var fullText: String, val range: TextRange) :
        BaseQuickFix(fullText.substring(range.startOffset, range.endOffset)) {
    val prefix = config.validatorConfigs.find { it.configurationName == "ParagraphStartWith" }?.getProperty("start_from") ?: ""

    override fun getName() = "Add paragraph prefix " + prefix

    override fun fixedText() = prefix

    override fun getEnd(problem: ProblemDescriptor) = skipWhitespace(fullText, range.startOffset)

    protected fun skipWhitespace(line: String, start: Int): Int {
        return (start..line.length - 1).find { !line[it].isWhitespace() } ?: line.length
    }
}
