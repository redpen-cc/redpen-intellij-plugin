package cc.redpen.intellij

import cc.redpen.config.ConfigurationLoader
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType.AMPERSAND
import com.intellij.psi.PsiFile
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.File
import java.io.FileOutputStream

class RedPenProviderTest : BaseTest() {
    val file = mockTextFile("hello")

    @Before
    fun setUp() {
        provider = RedPenProvider(project)
    }

    @Test
    fun allConfigFilesAreLoaded() {
        assertEquals("en", provider.getConfig("en")!!.key)
        assertEquals("ja", provider.getConfig("ja")!!.key)
        assertEquals("ja.hankaku", provider.getConfig("ja.hankaku")!!.key)
        assertEquals("ja.zenkaku2", provider.getConfig("ja.zenkaku2")!!.key)
    }

    @Test
    fun getRedPenFor_autodetectsLanguage() {
        whenever(file.text).thenReturn("Hello")
        var redPen = provider.getRedPenFor(file)
        assertEquals("en", redPen.configuration.key)

        whenever(file.text).thenReturn("こんにちは")
        redPen = provider.getRedPenFor(file)
        assertEquals("ja", redPen.configuration.key)
    }

    @Test
    fun getRedPenFor_autodetectsLanguageOnlyIfLanguageWasNotAlreadySetManually() {
        val file = mock<PsiFile>(RETURNS_DEEP_STUBS)
        provider.configKeysByFile["/path/to/foo"] = "ja"
        whenever(file.virtualFile.path).thenReturn("/path/to/foo")

        var redPen = provider.getRedPenFor(file)
        assertEquals("ja", redPen.configuration.key)
    }

    @Test
    fun saveAndLoad() {
        provider.configDir = File(System.getProperty("java.io.tmpdir"), "redpen-tmp-config")
        provider.getConfig("ja")!!.symbolTable.overrideSymbol(Symbol(AMPERSAND, '*'))
        provider.configKeysByFile["hello.txt"] = "ja"
        provider.save()

        assertFalse(File(provider.configDir, "en.xml").exists())
        assertEquals(provider.getConfig("ja"), ConfigurationLoader().load(File(provider.configDir, "ja.xml")))

        provider.loadConfig("ja.xml")
        assertEquals('*', provider.getConfig("ja")!!.symbolTable.getSymbol(AMPERSAND).value)
        assertFalse(provider.getInitialConfig("ja") == provider.getConfig("ja"))
        assertTrue(provider.getInitialConfig("en") == provider.getConfig("en"))

        provider.configKeysByFile.remove("hello.txt")
        provider.loadConfigKeysByFile()
        assertEquals("ja", provider.configKeysByFile["hello.txt"])

        provider.configDir.deleteRecursively()
    }

    @Test
    fun removeSavedConfigIfSameAsInitial() {
        provider.configDir = File(System.getProperty("java.io.tmpdir"), "redpen-tmp-config")
        provider.configDir.mkdirs()
        val enConf = File(provider.configDir, "en.xml")
        FileOutputStream(enConf).use { it.write("<redpen-conf/>".toByteArray()) }

        provider.save()
        assertFalse(enConf.exists())
        assertFalse(provider.configDir.exists())
    }

    @Test
    fun setConfig() {
        val config = config("en")
        val file = mock<PsiFile>(RETURNS_DEEP_STUBS)
        whenever(file.virtualFile.path).thenReturn("/path/to/foo")

        provider.setConfig(file, config)

        assertEquals("en", provider.configKeysByFile["/path/to/foo"])
    }
}