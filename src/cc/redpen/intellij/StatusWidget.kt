package cc.redpen.intellij

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.LangDataKeys.PSI_FILE
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.psi.PsiManager
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseEvent

open class StatusWidget constructor(project: Project, var provider: RedPenProvider) : EditorBasedWidget(project), CustomStatusBarWidget {
    var actionGroup = DefaultActionGroup()
    private val component = object: TextPanel.ExtraSize() {
        protected override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (text != null) {
                val arrows = AllIcons.Ide.Statusbar_arrows
                arrows.paintIcon(this, g, bounds.width - insets.right - arrows.iconWidth - 2,
                        bounds.height / 2 - arrows.iconHeight / 2)
            }
        }
    }

    init {
        object : ClickListener() {
            override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
                showPopup(e)
                return true
            }
        }.installOn(component)
        component.border = StatusBarWidget.WidgetBorder.WIDE

        registerActions()
    }

    open fun registerActions() {
        val actionManager = ActionManager.getInstance() ?: return
        provider.configs.forEach {
            actionGroup.add(object : AnAction() {
                init {
                    templatePresentation.text = it.key
                }

                override fun actionPerformed(e: AnActionEvent) {
                    provider.setConfig(e.getData(PSI_FILE)!!, it.value)
                    DaemonCodeAnalyzer.getInstance(e.project).restart()
                }
            })
        }
        actionManager.registerAction("RedPen", actionGroup)
    }

    override fun ID(): String {
        return "RedPen"
    }

    override fun getPresentation(platformType: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return null
    }

    open fun update(configKey: String) {
        component.text = "RedPen: " + configKey
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (ApplicationManager.getApplication().isUnitTestMode || project == null || event.newFile == null) return

        val file = PsiManager.getInstance(project!!).findFile(event.newFile!!)
        if (file == null || provider.getParser(file) == null) return
        update(provider.getConfigKeyFor(file))
    }

    override fun getComponent(): TextPanel {
        return component
    }

    private fun showPopup(e: MouseEvent) {
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "RedPen", actionGroup, getContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
        val dimension = popup.content.preferredSize
        val at = Point(0, -dimension.height)
        popup.show(RelativePoint(e.component, at))
        Disposer.register(this, popup)
    }

    private fun getContext(): DataContext {
        val editor = editor
        val parent = DataManager.getInstance().getDataContext(myStatusBar as Component)
        return SimpleDataContext.getSimpleContext(
                CommonDataKeys.VIRTUAL_FILE_ARRAY.name,
                arrayOf(selectedFile!!),
                SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.name,
                        project,
                        SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.name,
                                editor?.component, parent)))
    }
}
