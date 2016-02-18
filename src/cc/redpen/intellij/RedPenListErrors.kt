package cc.redpen.intellij

import cc.redpen.RedPen
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
    internal var provider = RedPenProvider.getInstance()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)
        val file = event.getData(LangDataKeys.PSI_FILE)
        val title = "RedPen " + RedPen.VERSION
        if (file == null) {
            showMessageDialog(project, "No file currently active", title, Messages.getInformationIcon())
            return
        }

        try {
            val text = file.text
            val redPen = provider.getRedPenFor(text)
            val redPenDoc = redPen.parse(provider.getParser(file), text)
            val errors = redPen.validate(redPenDoc)

            showMessageDialog(project, errors.map({ e -> getLineNumber(e) + ":" + getOffset(e.startPosition) + "-" + getOffset(e.endPosition) + " " + e.message })
                    .joinToString("\n"), file.name, Messages.getInformationIcon())
        } catch (e: Exception) {
            showMessageDialog(project, e.toString(), title, Messages.getInformationIcon())
        }
    }

    private fun getLineNumber(e: ValidationError): String {
        return if (e.startPosition.isPresent) e.startPosition.get().lineNum.toString() else "?"
    }

    private fun getOffset(lineOffset: Optional<LineOffset>): String {
        return if (lineOffset.isPresent) lineOffset.get().offset.toString() else "?"
    }
}
