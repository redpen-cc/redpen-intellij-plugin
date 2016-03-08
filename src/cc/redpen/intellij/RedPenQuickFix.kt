package cc.redpen.intellij

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

open class RedPenQuickFix(val validatorName: String) : LocalQuickFix {

    override fun applyFix(project: Project, problem: ProblemDescriptor) {
        val document = containingDocument(problem.psiElement)
        val text = document!!.text
        var startOffset = problem.textRangeInElement.startOffset
        while (startOffset > 0 && text[startOffset - 1] == ' ') startOffset--
        writeAction(project) {
            document.replaceString(startOffset, problem.textRangeInElement.endOffset, "")
        }
    }

    override fun getFamilyName() = "RedPen"

    override fun getName() = "Fix " + validatorName

    open fun containingDocument(psiElement: PsiElement) = PsiDocumentManager.getInstance(psiElement.project).getDocument(psiElement.containingFile)

    open fun writeAction(project: Project, runnable: () -> Unit) {
        object : WriteCommandAction<Any>(project) {
            override fun run(result: Result<Any>) = runnable.invoke()
        }.execute()
    }

    override fun equals(other: Any?) = other?.javaClass == javaClass && validatorName == (other as RedPenQuickFix).validatorName
    override fun hashCode() = validatorName.hashCode()
}
