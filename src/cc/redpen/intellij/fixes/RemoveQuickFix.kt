package cc.redpen.intellij.fixes

import com.intellij.codeInspection.ProblemDescriptor

open class RemoveQuickFix(text: String) : BaseQuickFix(text) {
   override fun getName() = "Remove " + text

    override fun fixedText() = ""

    override fun getStart(problem: ProblemDescriptor): Int {
        val text = containingDocument(problem.psiElement).text
        var startOffset = super.getStart(problem)
        while (startOffset > 0 && text[startOffset - 1] == ' ') startOffset--
        return startOffset
    }
}
