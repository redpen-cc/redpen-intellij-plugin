package cc.redpen.intellij.fixes

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

open class HyphenateQuickFix(text: String) : BaseQuickFix(text) {

    override fun applyFix(project: Project, problem: ProblemDescriptor) {
        val document = containingDocument(problem.psiElement)
        writeAction(project) {
            document.replaceString(problem.textRangeInElement.startOffset, problem.textRangeInElement.endOffset, fixedText())
        }
    }

    override fun getName() = "Change to " + fixedText()

    private fun fixedText() = text.replace(' ', '-')
}
