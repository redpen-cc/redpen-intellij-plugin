package cc.redpen.intellij.fixes

open class StartWithCapitalLetterQuickFix(text: String) : BaseQuickFix(text) {
    override fun fixedText() = text.toUpperCase()
}
