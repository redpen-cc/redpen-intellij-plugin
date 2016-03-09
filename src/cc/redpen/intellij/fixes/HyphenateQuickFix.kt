package cc.redpen.intellij.fixes

open class HyphenateQuickFix(text: String) : BaseQuickFix(text) {
    override fun fixedText() = text.replace(' ', '-')
}
