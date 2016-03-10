package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.model.Sentence
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.showMessageDialog
import java.util.*

class RedPenListErrors : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)!!
        val provider = RedPenProvider.forProject(project)
        val file = event.getData(LangDataKeys.PSI_FILE)
        val title = "RedPen " + RedPen.VERSION
        if (file == null) {
            showMessageDialog(project, "No file currently active", title, Messages.getInformationIcon())
            return
        }

        try {
            val redPen = provider.getRedPenFor(file)
            val text = file.text
            val redPenDoc = redPen.parse(provider.getParser(file), text)
            val errors = redPen.validate(redPenDoc)

            showMessageDialog(project, errors.map { e ->
                getLineNumber(e) + ":" + getOffset(e.startPosition, e.sentence) + "-" + getOffset(e.endPosition, e.sentence) + " " + e.message
            }.joinToString("\n"), file.name, Messages.getInformationIcon())
        } catch (e: Exception) {
            showMessageDialog(project, e.toString(), title, Messages.getInformationIcon())
        }
    }

    private fun getLineNumber(e: ValidationError): String {
        return e.startPosition.orElse(e.sentence.getOffset(0).orElse(null))?.lineNum?.toString() ?: "?"
    }

    private fun getOffset(lineOffset: Optional<LineOffset>, sentence: Sentence): String {
        return lineOffset.orElse(sentence.getOffset(0).orElse(null))?.offset?.toString() ?: "?"
    }
}
