package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

open class InvalidSymbolQuickFix(val config: Configuration, text: String) : BaseQuickFix(text) {

    override fun applyFix(project: Project, problem: ProblemDescriptor) {
        val document = containingDocument(problem.psiElement)
        writeAction(project) {
            document.replaceString(problem.textRangeInElement.startOffset, problem.textRangeInElement.endOffset, fixedText())
        }
    }

    override fun fixedText(): String {
        val symbolTable = config.symbolTable
        val name = symbolTable.names.find { text[0] in symbolTable.getSymbol(it).invalidChars }
        return symbolTable.getSymbol(name).value.toString()
    }
}
