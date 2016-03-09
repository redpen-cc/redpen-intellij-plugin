package cc.redpen.intellij.fixes

import com.intellij.codeInspection.ProblemDescriptor

open class SpaceBeginningOfSentenceQuickFix(text: String) : BaseQuickFix(text) {
    override fun getName() = "Add space"

    override fun fixedText() = " "

    override fun getEnd(problem: ProblemDescriptor) = getStart(problem)
}
