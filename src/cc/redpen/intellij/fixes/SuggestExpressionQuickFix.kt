package cc.redpen.intellij.fixes

open class SuggestExpressionQuickFix(text: String, val errorMessage: String) : BaseQuickFix(text) {
    override fun fixedText() = errorMessage.replace(".*\"(.+?)\".*".toRegex(), "$1")
}
