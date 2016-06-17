package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import cc.redpen.validator.ValidationError
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

abstract class BaseQuickFix(var text: String) : LocalQuickFix {

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
        fun forValidator(error: ValidationError, config: Configuration, fullText: String, range: TextRange): Array<BaseQuickFix> {
            val text = fullText.substring(range.startOffset, range.endOffset)
            val mutableList : MutableList<BaseQuickFix> = arrayListOf()
            when (error.validatorName) {
                "Hyphenation" -> mutableList.add(HyphenateQuickFix(text))
                "InvalidSymbol" -> mutableList.add(InvalidSymbolQuickFix(config, fullText, range))
                "SymbolWithSpace" -> mutableList.add(SymbolWithSpaceQuickFix(config, text))
                "StartWithCapitalLetter" -> mutableList.add(StartWithCapitalLetterQuickFix(text))
                "NumberFormat" -> mutableList.add(NumberFormatQuickFix(config, text))
                "SpaceBeginningOfSentence" -> mutableList.add(SpaceBeginningOfSentenceQuickFix(text))
                "EndOfSentence" -> mutableList.add(EndOfSentenceQuickFix(text))
                "SuggestExpression" -> mutableList.add(SuggestExpressionQuickFix(text, error.message))
                "ParagraphStartWith" -> mutableList.add(ParagraphStartWithQuickFix(config, fullText, range))
                else -> if (!isSentenceLevelError(error)) mutableList.add(RemoveQuickFix(text))
            }
            return mutableList.toTypedArray()
        }

        private fun isSentenceLevelError(error: ValidationError) = !error.startPosition.isPresent
    }
}
