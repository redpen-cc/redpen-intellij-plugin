package cc.redpen.intellij.fixes

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

open class RemoveQuickFix(text: String) : BaseQuickFix(text) {

    override fun applyFix(project: Project, problem: ProblemDescriptor) {
        val document = containingDocument(problem.psiElement)
        val text = document.text
        var startOffset = problem.textRangeInElement.startOffset
        while (startOffset > 0 && text[startOffset - 1] == ' ') startOffset--
        writeAction(project) {
            document.replaceString(startOffset, problem.textRangeInElement.endOffset, fixedText())
        }
    }

    override fun getName() = "Remove " + text

    override fun fixedText() = ""
}
