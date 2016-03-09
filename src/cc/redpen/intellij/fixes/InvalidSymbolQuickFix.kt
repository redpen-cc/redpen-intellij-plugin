package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration

open class InvalidSymbolQuickFix(val config: Configuration, text: String) : BaseQuickFix(text) {
    override fun fixedText(): String {
        val symbolTable = config.symbolTable
        val name = symbolTable.names.find { text[0] in symbolTable.getSymbol(it).invalidChars }
        return symbolTable.getSymbol(name).value.toString()
    }
}
