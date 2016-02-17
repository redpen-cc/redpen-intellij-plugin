package cc.redpen.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class StatusWidgetTest {
    val psiManager = mock<PsiManager>(RETURNS_DEEP_STUBS)
    val project = mock<Project>(RETURNS_DEEP_STUBS)
    val widget = StatusWidget(project)
    val newFile = mock<VirtualFile>()

    @Before
    fun setUp() {
        widget.provider = mock(RETURNS_DEEP_STUBS)
        whenever(project.getComponent(PsiManager::class.java)).thenReturn(psiManager)
        ApplicationManager.setApplication(mock(), mock())
    }

    @Test
    fun selectionChanged() {
        whenever(psiManager.findFile(newFile)!!.text).thenReturn("Hello")
        whenever(widget.provider.getConfigKeyFor("Hello")).thenReturn("ja")

        widget.selectionChanged(FileEditorManagerEvent(mock(), mock(), mock(), newFile, mock()))
        assertEquals("RedPen: ja", widget.component.text)
    }

    @Test
    fun selectionChanged_noParser() {
        whenever(widget.provider.getParser(psiManager.findFile(newFile)!!)).thenReturn(null)
        widget.selectionChanged(FileEditorManagerEvent(mock(), mock(), mock(), newFile, mock()))
        assertNull(widget.component.text)
    }
}