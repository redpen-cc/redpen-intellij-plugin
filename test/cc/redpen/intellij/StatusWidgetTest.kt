package cc.redpen.intellij

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class StatusWidgetTest : BaseTest() {
    val psiManager = mock<PsiManager>(RETURNS_DEEP_STUBS)
    val project = mock<Project>(RETURNS_DEEP_STUBS)
    val provider = mock<RedPenProvider>(RETURNS_DEEP_STUBS)
    val widget = StatusWidget(project, provider)
    val newFile = mock<VirtualFile>()

    @Before
    fun setUp() {
        whenever(project.getComponent(PsiManager::class.java)).thenReturn(psiManager)
    }

    @Test
    fun selectionChanged() {
        whenever(psiManager.findFile(newFile)!!.text).thenReturn("Hello")
        whenever(provider.getConfigKeyFor("Hello")).thenReturn("ja")

        widget.selectionChanged(FileEditorManagerEvent(mock(), mock(), mock(), newFile, mock()))
        assertEquals("RedPen: ja", widget.component.text)
    }

    @Test
    fun selectionChanged_noParser() {
        whenever(provider.getParser(psiManager.findFile(newFile)!!)).thenReturn(null)
        widget.selectionChanged(FileEditorManagerEvent(mock(), mock(), mock(), newFile, mock()))
        assertNull(widget.component.text)
    }

    @Test
    fun remembersManuallySelectedFile() {
        val event = mock<AnActionEvent>(RETURNS_DEEP_STUBS)
        val file = mock<PsiFile>(RETURNS_DEEP_STUBS)
        val config = config("en")
        val codeAnalyzer = mock<DaemonCodeAnalyzer>()
        whenever(ApplicationManager.getApplication().getComponent(ActionManager::class.java)).thenReturn(mock())
        whenever(event.getData(LangDataKeys.PSI_FILE)).thenReturn(file)
        whenever(event.project!!.getComponent(DaemonCodeAnalyzer::class.java)).thenReturn(codeAnalyzer)
        whenever(provider.getConfigs()).thenReturn(mapOf("en" to config))

        widget.registerActions()
        widget.actionGroup.childActionsOrStubs[0].actionPerformed(event)

        verify(provider).setConfig(file, config)
        verify(codeAnalyzer).restart()
    }
}