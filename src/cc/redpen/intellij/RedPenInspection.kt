package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.intellij.fixes.BaseQuickFix
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.xmlb.SerializationFilter

open class RedPenInspection : LocalInspectionTool() {
    override fun getShortName() = "RedPen"
    override fun getDisplayName() = "RedPen Validation"
    override fun getGroupDisplayName() = GroupNames.STYLE_GROUP_NAME
    override fun isEnabledByDefault() = true
    override fun getDefaultLevel() = HighlightDisplayLevel.ERROR
    override fun getStaticDescription() =
        "Validates text with RedPen, a proofreading tool.\nConfigure specific validators in Settings -> Editor -> RedPen."

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.virtualFile is LightVirtualFile) return null
        if (file.children.isEmpty()) return null

        val provider = RedPenProvider.forProject(file.project)
        val parser = provider.getParser(file) ?: return null

        val redPen = provider.getRedPenFor(file)

        updateStatus(file, redPen)

        val text = file.text
        val redPenDoc = redPen.parse(parser, text)
        val errors = redPen.validate(redPenDoc)

        val element = file.children[0]
        val lines = text.split("(?<=\n)".toRegex())

        return errors.map { e ->
            try {
                val range = toRange(e, lines)
                manager.createProblemDescriptor(element, range,
                        e.message + " (" + e.validatorName + ")", GENERIC_ERROR_OR_WARNING, isOnTheFly,
                        BaseQuickFix.forValidator(e, redPen.configuration, text, range))
            } catch (ex: Exception) {
                Logger.getInstance(javaClass.name).warn(e.message + ": " + ex.toString());
                null
            }
        }.filterNotNull().toTypedArray()
    }

    open fun updateStatus(file: PsiFile, redPen: RedPen) {
        StatusWidget.forProject(file.project).update(redPen.configuration.key)
    }

    internal open fun toRange(e: ValidationError, lines: List<String>): TextRange {
        val start = e.startPosition.orElse(e.sentence.getOffset(0).orElse(null))
        val end = e.endPosition.orElse(addOne(e.sentence.getOffset(0).orElse(null)))
        return TextRange(toGlobalOffset(start, lines), toGlobalOffset(end, lines))
    }

    private fun addOne(lineOffset: LineOffset): LineOffset {
        return LineOffset(lineOffset.lineNum, lineOffset.offset + 1)
    }

    internal fun toGlobalOffset(lineOffset: LineOffset?, lines: List<String>): Int {
        if (lineOffset == null) return 0
        var result = 0
        for (i in 1..lineOffset.lineNum - 1) {
            result += lines[i - 1].length
        }
        return result + lineOffset.offset
    }

    public override fun getSerializationFilter() = SerializationFilter { a, o -> false }
}
