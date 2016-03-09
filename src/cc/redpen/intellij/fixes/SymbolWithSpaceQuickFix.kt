package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration

open class SymbolWithSpaceQuickFix(val config: Configuration, text: String) : BaseQuickFix(text) {
    override fun getName() = "Add space"

    override fun fixedText(): String {
        val symbol = config.symbolTable.getSymbolByValue(text[0])
        return when {
            symbol.isNeedBeforeSpace -> " " + text
            symbol.isNeedAfterSpace -> text + " "
            else -> text
        }
    }
}
