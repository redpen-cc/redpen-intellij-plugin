package cc.redpen.intellij

import cc.redpen.intellij.fixes.RemoveQuickFix
import cc.redpen.model.Document
import cc.redpen.model.Sentence
import cc.redpen.parser.DocumentParser
import cc.redpen.parser.LineOffset
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.LightVirtualFile
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Arrays.asList
import java.util.Collections.emptyList

class RedPenInspectionTest : BaseTest() {
    internal var inspection = spy(RedPenInspection())

    @Test
    fun notSupportedFilesAreIgnored() {
        assertNull(inspection.checkFile(mockFileOfType("JAVA", "java", ""), mock(), true))
        assertNull(inspection.checkFile(mockFileOfType("XML", "xml", ""), mock(), true))
    }

    @Test
    fun plainTextIsSupported() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockTextFile("Hello"), mock(), true)
        verify(redPen).parse(DocumentParser.PLAIN, "Hello")
    }

    @Test
    fun markdownIsSupported() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockFileOfType("Markdown", "md", "Hello"), mock(), true)
        verify(redPen).parse(DocumentParser.MARKDOWN, "Hello")
    }

    @Test
    fun asciiDocIsSupported() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockFileOfType("AsciiDoc", "asciidoc", "Hello"), mock(), true)
        verify(redPen).parse(DocumentParser.ASCIIDOC, "Hello")
    }

    @Test
    fun ReVIEWIsSupported() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockFileOfType("ReVIEW", "re", "Hello"), mock(), true)
        verify(redPen).parse(DocumentParser.REVIEW, "Hello")
    }

    @Test
    fun LaTeXIsSupported() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockFileOfType("LaTeX", "tex", "Hello"), mock(), true)
        verify(redPen).parse(DocumentParser.LATEX, "Hello")
    }

    @Test
    fun canParseEmptyDocument() {
        whenever(redPen.validate(any<Document>())).thenReturn(emptyList())
        inspection.checkFile(mockTextFile(""), mock(), true)
        verify(redPen).parse(DocumentParser.PLAIN, "")
    }

    @Test
    fun toGlobalOffset_noOffset() {
        assertEquals(0, inspection.toGlobalOffset(null, listOf("")))
    }

    @Test
    fun toGlobalOffset_singleLine() {
        assertEquals(3, inspection.toGlobalOffset(LineOffset(1, 3), listOf("Hello")))
    }

    @Test
    fun toGlobalOffset_multiLine() {
        assertEquals(8, inspection.toGlobalOffset(LineOffset(2, 3), listOf("Hello", "World")))
    }

    @Test
    fun toRange() {
        val textRange = inspection.toRange(ErrorGenerator.at(5, 5), listOf("Hello"))
        assertEquals(TextRange(5, 5), textRange)
    }

    @Test
    fun toRange_sentenceLevelError() {
        val sentence = Sentence("Hello.", listOf(LineOffset(1, 25)), emptyList())
        val textRange = inspection.toRange(ErrorGenerator.sentence(sentence), listOf(sentence.content))
        assertEquals(TextRange(25, 26), textRange)
    }

    @Test
    fun checkFile_convertsRedPenErrorsIntoIDEAProblemDescriptors() {
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello")
        whenever(redPen.validate(doc)).thenReturn(asList(ErrorGenerator.at(0, 3), ErrorGenerator.at(3, 5)))
        val manager = mock<InspectionManager>()
        whenever(manager.createProblemDescriptor(any(), any<TextRange>(), any<String>(), any(), any<Boolean>(), any<LocalQuickFix>())).thenReturn(mock())

        val file = mockTextFile("Hello")
        val problems = inspection.checkFile(file, manager, true)
        assertNotNull(problems)
        assertEquals(2, problems?.size)

        verify(manager).createProblemDescriptor(file.children[0], TextRange(0, 3), "Hello (ErrorGenerator)", GENERIC_ERROR_OR_WARNING, true, RemoveQuickFix("Hel"))
        verify(manager).createProblemDescriptor(file.children[0], TextRange(3, 5), "Hello (ErrorGenerator)", GENERIC_ERROR_OR_WARNING, true, RemoveQuickFix("lo"))
        verifyNoMoreInteractions(manager);
    }

    @Test
    fun checkFile_skipErrorsThatFailToConvertToProblems() {
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello")
        whenever(redPen.validate(doc)).thenReturn(asList(ErrorGenerator.at(0, 3)))
        val manager = mock<InspectionManager>()
        whenever(manager.createProblemDescriptor(any(), any<TextRange>(), any<String>(), any(), any<Boolean>(), any<LocalQuickFix>())).thenThrow(RuntimeException())

        val file = mockTextFile("Hello")
        assertEquals(0, inspection.checkFile(file, manager, true)?.size)
    }

    @Test
    fun checkFile_splitsTextIntoLinesPreservingAllCharacters() {
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello\nworld")
        val error = ErrorGenerator.at(1, 2)
        whenever(redPen.validate(doc)).thenReturn(listOf(error))

        inspection.checkFile(mockTextFile("Hello\nworld"), mock(), true)

        verify(inspection).toRange(eq(error), capture {
            assertEquals(listOf("Hello\n", "world"), it)
        })
    }

    @Test
    fun checkFile_updatesStatusWidget() {
        doCallRealMethod().whenever(inspection).updateStatus(any(), any())
        val file = mockTextFile("Hello")
        val config = config("ja")
        whenever(redPen.configuration).thenReturn(config)
        inspection.checkFile(file, mock(), true)
        verify(statusWidget).update("ja")
    }

    @Test
    fun checkFile_ignoresEditFieldsInDialogs() {
        val file = mockTextFile("Hello")
        val notReallyAFile = LightVirtualFile()
        whenever(file.virtualFile).thenReturn(notReallyAFile)

        assertNull(inspection.checkFile(file, mock(), false))
    }

    @Test
    fun doNotSerializeSettings() {
        assertFalse(inspection.serializationFilter.accepts(mock(), mock()));
    }
}
