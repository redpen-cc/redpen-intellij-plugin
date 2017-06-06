package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.config.Configuration
import cc.redpen.config.Symbol
import cc.redpen.config.ValidatorConfiguration
import cc.redpen.model.Sentence
import cc.redpen.validator.ValidationError
import cc.redpen.validator.document.WordFrequencyValidator
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.BeforeClass
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.util.*

abstract class BaseTest {
    val project = mock<Project>(RETURNS_DEEP_STUBS)
    val redPen: RedPen = mock(RETURNS_DEEP_STUBS)
    var provider: RedPenProvider = mock(RETURNS_DEEP_STUBS)
    var statusWidget: StatusWidget = mock(RETURNS_DEEP_STUBS)

    companion object {
        val application = mock<Application>(RETURNS_DEEP_STUBS)

        @BeforeClass @JvmStatic
        fun initStatics() {
            ApplicationManager.setApplication(application, mock())
        }
    }

    init {
        whenever(project.getComponent(RedPenProvider::class.java)).thenReturn(provider)
        whenever(project.getComponent(StatusWidget::class.java)).thenReturn(statusWidget)
        whenever(provider.getRedPen()).thenReturn(redPen)
        whenever(provider.getRedPenFor(any())).thenReturn(redPen)
        whenever(provider.getParser(any())).thenCallRealMethod()
        whenever(redPen.configuration.key).thenReturn("en")
    }

    protected fun configWithValidators(validatorConfigs: List<ValidatorConfiguration>): Configuration {
        val builder = Configuration.builder()
        validatorConfigs.forEach { builder.addValidatorConfig(it) }
        return builder.build()
    }

    protected fun configWithSymbols(symbols: List<Symbol>): Configuration {
        val builder = Configuration.builder()
        symbols.forEach { builder.addSymbol(it) }
        return builder.build()
    }

    protected fun cloneableConfig(key: String): Configuration {
        val config = config(key)
        val configClone = config(key)
        whenever(config.clone()).thenReturn(configClone)
        whenever(configClone.clone()).thenReturn(configClone)
        return config
    }

    protected fun config(key: String): Configuration {
        val config = mock<Configuration>()
        whenever(config.key).thenReturn(key)
        return config
    }

    protected fun mockTextFile(text: String): PsiFile {
        return mockFileOfType("PLAIN_TEXT", "txt", text)
    }

    protected open fun mockFileOfType(typeName: String, extension: String, text: String): PsiFile {
        val file = mock<PsiFile>(RETURNS_DEEP_STUBS)
        whenever(file.text).thenReturn(text)
        whenever(file.children).thenReturn(arrayOf(mock()))
        whenever(file.virtualFile.path).thenReturn("/path")
        whenever(file.project).thenReturn(project)
        whenever(file.fileType.name).thenReturn(typeName)
        whenever(file.name).thenReturn("sample." + extension)
        return file
    }

    object ErrorGenerator : WordFrequencyValidator() {
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
