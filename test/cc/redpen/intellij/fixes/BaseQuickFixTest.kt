package cc.redpen.intellij.fixes

import cc.redpen.intellij.BaseTest
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.nhaarman.mockito_kotlin.*
import org.mockito.Mockito

abstract class BaseQuickFixTest(var quickFix: BaseQuickFix) : BaseTest() {
    val problem = mock<ProblemDescriptor>(Mockito.RETURNS_DEEP_STUBS)
    val document = mock<Document>()
    val psiElement = problem.psiElement

    init {
        quickFix = spy(quickFix)
        doReturn(document).whenever(quickFix).containingDocument(psiElement)
        doNothing().whenever(quickFix).writeAction(any(), any())
    }
}