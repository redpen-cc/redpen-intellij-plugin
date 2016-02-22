package cc.redpen.intellij

import cc.redpen.config.Configuration
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

    companion object {
        var cachedConfigs: MutableMap<String, Configuration>? = null
    }

    @Before
    fun setUp() {
        if (cachedConfigs == null) {
            provider = RedPenProvider(project)
            cachedConfigs = provider.initialConfigs.map { it.key to it.value.clone() }.toMap().toLinkedMap()
        }
        else
            provider = RedPenProvider(project, cachedConfigs!!)
    }

    @Test
    fun allConfigFilesAreLoaded() {
        assertEquals("en", provider.configs["en"]!!.key)
        assertEquals("ja", provider.configs["ja"]!!.key)
        assertEquals("ja.hankaku", provider.configs["ja.hankaku"]!!.key)
        assertEquals("ja.zenkaku2", provider.configs["ja.zenkaku2"]!!.key)
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
        provider.configs["ja"]!!.symbolTable.overrideSymbol(Symbol(AMPERSAND, '*'))
        provider.configKeysByFile["hello.txt"] = "ja"
        provider.save()

        assertFalse(File(provider.configDir, "en.xml").exists())
        assertEquals(provider.configs["ja"], ConfigurationLoader().load(File(provider.configDir, "ja.xml")))

        provider.loadConfig("ja.xml")
        assertEquals('*', provider.configs["ja"]!!.symbolTable.getSymbol(AMPERSAND).value)
        assertFalse(provider.initialConfigs["ja"] == provider.configs["ja"])
        assertTrue(provider.initialConfigs["en"] == provider.configs["en"])

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