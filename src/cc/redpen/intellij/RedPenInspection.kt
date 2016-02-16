package cc.redpen.intellij

import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

open class RedPenInspection : LocalInspectionTool() {
    internal var provider = RedPenProvider.instance

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

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val parser = provider.getParser(file) ?: return null

        val text = file.text
        val redPen = provider.getRedPenFor(text)
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
}
