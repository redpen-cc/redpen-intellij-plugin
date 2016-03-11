package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.openapi.util.TextRange

open class InvalidSymbolQuickFix(config: Configuration, var fullText: String, var range: TextRange) : BaseQuickFix(fullText[range.startOffset].toString()) {
    val symbolTable = config.symbolTable

    override fun fixedText(): String {
        val c = text[0]
        val after = if (range.startOffset < fullText.length - 1) fullText[range.startOffset + 1] else ' '
        val before = if (range.startOffset > 0) fullText[range.startOffset - 1] else ' '
        val symbols = symbolTable.names.map { symbolTable.getSymbol(it) }.filter { c in it.invalidChars }
        if (symbols.isEmpty())
            return text
        else
            return (symbols.find {
                it.isNeedAfterSpace && !after.isLetterOrDigit() || it.isNeedBeforeSpace && !before.isLetterOrDigit()
            } ?: symbols[0]).value.toString()
    }
}
