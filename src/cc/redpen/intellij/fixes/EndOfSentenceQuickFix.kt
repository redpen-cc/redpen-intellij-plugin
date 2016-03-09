package cc.redpen.intellij.fixes

import com.intellij.codeInspection.ProblemDescriptor

open class EndOfSentenceQuickFix(text: String) : BaseQuickFix(text) {
    override fun getName() = "Swap symbols"

    override fun getEnd(problem: ProblemDescriptor): Int {
        val end = super.getEnd(problem)
        text += containingDocument(problem.psiElement).text[end]
        return end + 1
    }

    override fun fixedText() = text.last().toString() + text.first()
}
