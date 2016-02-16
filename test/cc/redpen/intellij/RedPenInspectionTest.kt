package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.model.Document
import cc.redpen.model.Sentence
import cc.redpen.parser.DocumentParser
import cc.redpen.parser.LineOffset
import cc.redpen.validator.ValidationError
import cc.redpen.validator.section.WordFrequencyValidator
import com.intellij.codeInspection.InspectionManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.doReturn
import java.util.*
import java.util.Arrays.asList
import java.util.Collections.emptyList

class RedPenInspectionTest {
    internal var inspection = RedPenInspection()
    internal var errorGenerator = ErrorGenerator()
    internal var redPen: RedPen = mock()

    @Before
    fun setUp() {
        inspection.provider = spy(inspection.provider)
        doReturn(redPen).`when`(inspection.provider).getRedPen()
    }

    @Test
    fun notSupportedFilesAreIgnored() {
        assertNull(inspection.checkFile(mockFileOfType("JAVA", ""), mock<InspectionManager>(), true))
        assertNull(inspection.checkFile(mockFileOfType("XML", ""), mock<InspectionManager>(), true))
    }

    @Test
    fun plainTextIsSupported() {
        whenever<List<ValidationError>>(redPen.validate(Matchers.any(Document::class.java))).thenReturn(emptyList<ValidationError>())
        inspection.checkFile(mockTextFile("Hello"), mock<InspectionManager>(), true)
        verify(redPen).parse(DocumentParser.PLAIN, "Hello")
    }

    @Test
    fun markdownIsSupported() {
        whenever<List<ValidationError>>(redPen.validate(Matchers.any(Document::class.java))).thenReturn(emptyList<ValidationError>())
        inspection.checkFile(mockFileOfType("Markdown", "Hello"), mock<InspectionManager>(), true)
        verify(redPen).parse(DocumentParser.MARKDOWN, "Hello")
    }

    @Test
    fun asciiDocIsSupported() {
        whenever<List<ValidationError>>(redPen.validate(Matchers.any(Document::class.java))).thenReturn(emptyList<ValidationError>())
        inspection.checkFile(mockFileOfType("AsciiDoc", "Hello"), mock<InspectionManager>(), true)
        verify(redPen).parse(DocumentParser.ASCIIDOC, "Hello")
    }

    @Test
    fun toGlobalOffset_noOffset() {
        assertEquals(0, inspection.toGlobalOffset(null, arrayOf("")))
    }

    @Test
    fun toGlobalOffset_singleLine() {
        assertEquals(3, inspection.toGlobalOffset(LineOffset(1, 3), arrayOf("Hello")))
    }

    @Test
    fun toGlobalOffset_multiLine() {
        assertEquals(8, inspection.toGlobalOffset(LineOffset(2, 3), arrayOf("Hello", "World")))
    }

    @Test
    fun toRange() {
        val textRange = inspection.toRange(errorGenerator.at(5, 5), arrayOf("Hello"))
        assertEquals(TextRange(5, 5), textRange)
    }

    @Test
    fun toRange_sentenceLevelError() {
        val sentence = Sentence("Hello.", listOf(LineOffset(1, 25)), emptyList<String>())
        val textRange = inspection.toRange(errorGenerator.sentence(sentence), arrayOf(sentence.content))
        assertEquals(TextRange(25, 26), textRange)
    }

    @Test
    fun checkFile_convertsRedPenErrorsIntoIDEAProblemDescriptors() {
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello")
        whenever(redPen.validate(doc)).thenReturn(asList(errorGenerator.at(0, 3), errorGenerator.at(3, 5)))

        val problems = inspection.checkFile(mockTextFile("Hello"), mock<InspectionManager>(), true)
        assertNotNull(problems)
        assertEquals(2, problems?.size)
    }

    @Test
    fun checkFile_splitsTextIntoLinesPreservingAllCharacters() {
        inspection = spy(inspection)
        val doc = redPen.parse(DocumentParser.PLAIN, "Hello\nworld")
        val error = errorGenerator.at(1, 2)
        whenever(redPen.validate(doc)).thenReturn(listOf(error))

        inspection.checkFile(mockTextFile("Hello\nworld"), mock<InspectionManager>(), true)

        val captor = ArgumentCaptor.forClass(Array<String>::class.java)
        verify(inspection).toRange(eq(error), captor.capture())

        assertArrayEquals(arrayOf("Hello\n", "world"), captor.value)
    }

    private fun mockTextFile(text: String): PsiFile {
        return mockFileOfType("PLAIN_TEXT", text)
    }

    private fun mockFileOfType(typeName: String, text: String): PsiFile {
        val file = Mockito.mock(PsiFile::class.java, RETURNS_DEEP_STUBS)
        whenever(file.fileType.name).thenReturn(typeName)
        whenever(file.text).thenReturn(text)
        whenever(file.children).thenReturn(arrayOf(mock<PsiElement>()))
        return file
    }

    internal class ErrorGenerator : WordFrequencyValidator() {
        fun at(start: Int, end: Int): ValidationError {
            val errors = ArrayList<ValidationError>()
            setErrorList(errors)
            addErrorWithPosition("Hello", Sentence("Hello", 1), start, end)
            return errors[0]
        }

        fun sentence(sentence: Sentence): ValidationError {
            val errors = ArrayList<ValidationError>()
            setErrorList(errors)
            addError("Hello", sentence)
            return errors[0]
        }
    }
}