package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiFile
import com.intellij.util.xmlb.SerializationFilter

open class RedPenInspection : LocalInspectionTool() {
    internal var provider = RedPenProvider.instance
    internal var statusWidget: StatusWidget? = null

    override fun getDisplayName(): String {
        return "RedPen Validation"
    }

    override fun getGroupDisplayName(): String {
        return GroupNames.STYLE_GROUP_NAME
    }

    override fun getShortName(): String {
        return "RedPen"
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    open fun createStatusWidget(file: PsiFile): StatusWidget {
        val widget = StatusWidget(file.project);
        addWidgetToStatusBar(file.project, widget);
        return widget;
    }

    open fun addWidgetToStatusBar(project: Project, widget: StatusWidget) {
        WindowManager.getInstance().getStatusBar(project).addWidget(widget, "before Encoding")
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val parser = provider.getParser(file) ?: return null

        val text = file.text
        val redPen = provider.getRedPenFor(text)

        updateStatus(file, redPen)

        val redPenDoc = redPen.parse(parser, text)
        val errors = redPen.validate(redPenDoc)

        val theElement = file.children[0]
        val lines = text.split("(?<=\n)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val problems = errors.map({ e ->
            manager.createProblemDescriptor(theElement, toRange(e, lines),
                    e.getMessage(), ProblemHighlightType.GENERIC_ERROR, isOnTheFly)
        })

        return problems.toTypedArray()
    }

    open fun updateStatus(file: PsiFile, redPen: RedPen) {
        if (statusWidget == null) statusWidget = createStatusWidget(file)
        statusWidget!!.update(redPen.configuration.key)
    }

    internal open fun toRange(e: ValidationError, lines: Array<String>): TextRange {
        val start = e.startPosition.orElse(e.sentence.getOffset(0).orElse(null))
        val end = e.endPosition.orElse(addOne(e.sentence.getOffset(0).orElse(null)))
        return TextRange(toGlobalOffset(start, lines), toGlobalOffset(end, lines))
    }

    private fun addOne(lineOffset: LineOffset): LineOffset {
        return LineOffset(lineOffset.lineNum, lineOffset.offset + 1)
    }

    internal fun toGlobalOffset(lineOffset: LineOffset?, lines: Array<String>): Int {
        if (lineOffset == null) return 0
        var result = 0
        for (i in 1..lineOffset.lineNum - 1) {
            result += lines[i - 1].length
        }
        return result + lineOffset.offset
    }

    public override fun getSerializationFilter(): SerializationFilter {
        return SerializationFilter { a, o -> false }
    }
}
