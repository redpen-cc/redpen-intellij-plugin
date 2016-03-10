package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.model.Sentence
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.showMessageDialog
import java.util.*

open class RedPenListErrors : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)!!
        val provider = RedPenProvider.forProject(project)
        val file = event.getData(LangDataKeys.PSI_FILE)
        val title = "RedPen " + RedPen.VERSION
        if (file == null) {
            showMessage(project, title, "No file currently active")
            return
        }

        try {
            val redPen = provider.getRedPenFor(file)
            val text = file.text
            val redPenDoc = redPen.parse(provider.getParser(file), text)
            val errors = redPen.validate(redPenDoc)

            val message = errors.map { e ->
                getLineNumber(e) to getLineNumber(e).toString() + ":" + getOffset(e.startPosition, e.sentence) + "-" +
                        getOffset(e.endPosition, e.sentence) + " " + e.message + " (" + e.validatorName + ")"
            }.sortedBy { it.component1() }.map { it.component2() }.joinToString("\n")

            showMessage(project, file.name, message)
        } catch (e: Exception) {
            showMessage(project, title, e.toString())
        }
    }

    open internal fun showMessage(project: Project, title: String, text: String) {
        showMessageDialog(project, text, title, Messages.getInformationIcon())
    }

    private fun getLineNumber(e: ValidationError): Int {
        return e.startPosition.orElse(e.sentence.getOffset(0).orElse(null))?.lineNum ?: 0
    }

    private fun getOffset(lineOffset: Optional<LineOffset>, sentence: Sentence): Int {
        return lineOffset.orElse(sentence.getOffset(0).orElse(null))?.offset ?: 0
    }
}
