package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

abstract class BaseQuickFix(val text: String) : LocalQuickFix {

    override fun getFamilyName() = "RedPen"

    open fun containingDocument(psiElement: PsiElement) = PsiDocumentManager.getInstance(psiElement.project).getDocument(psiElement.containingFile)!!

    override fun getName() = "Change to " + fixedText()

    abstract protected fun fixedText(): String
    open protected fun getEnd(problem: ProblemDescriptor) = problem.textRangeInElement.endOffset
    open protected fun getStart(problem: ProblemDescriptor) = problem.textRangeInElement.startOffset

    override fun applyFix(project: Project, problem: ProblemDescriptor) {
        val document = containingDocument(problem.psiElement)
        writeAction(project) {
            document.replaceString(getStart(problem), getEnd(problem), fixedText())
        }
    }

    open internal fun writeAction(project: Project, runnable: () -> Unit) {
        object : WriteCommandAction<Any>(project) {
            override fun run(result: Result<Any>) = runnable.invoke()
        }.execute()
    }

    override fun equals(other: Any?) = other?.javaClass == javaClass && text == (other as RemoveQuickFix).text
    override fun hashCode() = text.hashCode()
    override fun toString() = javaClass.simpleName + "[" + text + "]"

    companion object {
        fun forValidator(name: String, config: Configuration, text: String): BaseQuickFix {
            return when (name) {
                "Hyphenation" -> HyphenateQuickFix(text)
                "InvalidSymbol" -> InvalidSymbolQuickFix(config, text)
                else -> RemoveQuickFix(text)
            }
        }
    }
}
