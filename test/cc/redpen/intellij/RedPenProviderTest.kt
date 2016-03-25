package cc.redpen.intellij

import cc.redpen.config.Configuration
import cc.redpen.config.ConfigurationLoader
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType.AMPERSAND
import com.intellij.psi.PsiFile
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
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
        val basePath = File(System.getProperty("java.io.tmpdir"), "redpen-tmp-config")
        whenever(project.basePath).thenReturn(basePath.absolutePath)
        provider = RedPenProvider(project)
    }

    @After
    fun tearDown() {
        provider.configDir.deleteRecursively()
    }

    @Test
    fun allConfigFilesAreLoaded() {
        assertEquals("en", provider.configs["en"]!!.key)
        assertEquals("ja", provider.configs["ja"]!!.key)
        assertEquals("ja.hankaku", provider.configs["ja.hankaku"]!!.key)
        assertEquals("ja.zenkaku2", provider.configs["ja.zenkaku2"]!!.key)
        provider.configs.values.forEach { assertEquals(provider.configDir, it.base) }
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
        provider.configKeysByFile["path/to/foo"] = "ja"
        whenever(project.basePath).thenReturn("/foo")
        whenever(file.virtualFile.path).thenReturn("/foo/path/to/foo")

        var redPen = provider.getRedPenFor(file)
        assertEquals("ja", redPen.configuration.key)
    }

    @Test
    fun saveAndLoad() {
        provider.configs["ja"]!!.symbolTable.overrideSymbol(Symbol(AMPERSAND, '*'))
        provider.configKeysByFile["hello.txt"] = "ja"
        provider.save()

        assertFalse(File(provider.configDir, "en.xml").exists())
        assertEquals(provider.configs["ja"], ConfigurationLoader().load(File(provider.configDir, "ja.xml")))

        provider.configLastModifiedTimes["ja"] = 0
        provider.loadConfig("ja")
        assertNotEquals(0, provider.configLastModifiedTimes["ja"])
        assertEquals('*', provider.configs["ja"]!!.symbolTable.getSymbol(AMPERSAND).value)
        assertFalse(provider.initialConfigs["ja"] == provider.configs["ja"])
        assertTrue(provider.initialConfigs["en"] == provider.configs["en"])

        provider.configKeysByFile.remove("hello.txt")
        provider.loadConfigKeysByFile()
        assertEquals("ja", provider.configKeysByFile["hello.txt"])
    }

    @Test
    fun removeSavedConfigIfSameAsInitial() {
        provider.configDir.mkdirs()
        val enFile = File(provider.configDir, "en.xml")
        FileOutputStream(enFile).use { it.write("<redpen-conf/>".toByteArray()) }
        provider.configLastModifiedTimes["en"] = enFile.lastModified()

        provider.save()
        assertFalse(enFile.exists())
        assertFalse(provider.configDir.exists())
    }

    @Test
    fun alwaysSaveNonDefaultConfigs() {
        provider.initialConfigs["za"] = Configuration.builder("za").build()
        provider.configs["za"] = Configuration.builder("za").build()
        provider.configLastModifiedTimes["za"] = 0

        provider.save()

        assertTrue(File(provider.configDir, "za.xml").exists())
    }

    @Test
    fun loadConfigIfItWasModifiedManuallySinceLastSave() {
        provider.configDir.mkdirs()
        provider.configs["za"] = config("za")
        provider.configLastModifiedTimes["za"] = 0
        val za = File(provider.configDir, "za.xml")
        FileOutputStream(za).use { it.write("<redpen-conf lang=\"za\">".toByteArray()) }

        provider.save()
        assertNotNull(provider.configs["za"])
    }

    @Test
    fun loadJustImportedCustomConfig() {
        provider.configDir.mkdirs()
        provider.configs["za"] = config("za")
        val za = File(provider.configDir, "za.xml")
        FileOutputStream(za).use { it.write("<redpen-conf lang=\"za\">".toByteArray()) }

        provider.save()
        assertNotNull(provider.configs["za"])
    }

    @Test
    fun availableConfigKeys() {
        provider.configDir.mkdirs()
        val zaFile = File(provider.configDir, "za.xml")
        FileOutputStream(zaFile).use { it.write("<redpen-conf/>".toByteArray()) }
        val filesFile = File(provider.configDir, "files.xml")
        FileOutputStream(filesFile).use { it.write("<properties/>".toByteArray()) }
        val nonXmlFile = File(provider.configDir, "blah.txt")
        FileOutputStream(nonXmlFile).use { it.write("blah".toByteArray()) }

        val configKeys = provider.availableConfigKeys()
        assertTrue(configKeys.containsAll(RedPenProvider.defaultConfigKeys))
        assertTrue("za" in configKeys)
        assertEquals(RedPenProvider.defaultConfigKeys.size + 1, configKeys.size)
    }

    @Test
    fun loadConfigForNonDefault() {
        provider.configDir.mkdirs()
        val zaFile = File(provider.configDir, "za.xml")
        FileOutputStream(zaFile).use { it.write("<redpen-conf lang=\"za\"/>".toByteArray()) }

        provider.loadConfig("za")
        assertTrue("za" in provider.configs)
        assertTrue("za" in provider.initialConfigs)
    }

    @Test
    fun setFileConfig() {
        val file = mock<PsiFile>(RETURNS_DEEP_STUBS)
        whenever(project.basePath).thenReturn("/foo")
        whenever(file.virtualFile.path).thenReturn("/foo/path/to/foo")

        provider.setConfigFor(file, "en")

        assertEquals("en", provider.configKeysByFile["path/to/foo"])
    }

    @Test
    fun addingOfNewConfigRebuildsStatusWidget() {
        provider += cloneableConfig("za")

        val order = inOrder(statusWidget)
        order.verify(statusWidget).unregisterActions()
        order.verify(statusWidget).registerActions()
    }

    @Test
    fun buildsAgainstCorrectRedPenVersion() {
        assertEquals("1.5.3", cc.redpen.RedPen.VERSION)
    }
}