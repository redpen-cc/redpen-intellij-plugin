package cc.redpen.intellij

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.LangDataKeys.PSI_FILE
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.WidgetBorder.WIDE
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.psi.PsiManager
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseEvent

open class StatusWidget constructor(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, ProjectComponent {
    val provider = RedPenProvider.forProject(project)
    var enabled: Boolean = false
    val actionGroupId = "RedPen " + project.basePath

    companion object {
        fun forProject(project: Project) = project.getComponent(StatusWidget::class.java)!!
    }

    var actionGroup: DefaultActionGroup? = null
    private val component = object: TextPanel.ExtraSize() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (enabled && text != null) {
                val arrows = AllIcons.Ide.Statusbar_arrows
                arrows.paintIcon(this, g, bounds.width - insets.right - arrows.iconWidth - 2,
                        bounds.height / 2 - arrows.iconHeight / 2)
            }
        }
    }

    override fun projectOpened() {
        install(WindowManager.getInstance().getStatusBar(project))
        myStatusBar.addWidget(this, "before Encoding")

        object : ClickListener() {
            override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
                showPopup(e)
                return true
            }
        }.installOn(component)
        component.border = WIDE
        component.toolTipText = "RedPen language"
        registerActions()
    }

    override fun projectClosed() {
        myStatusBar.removeWidget(ID())
        unregisterActions()
    }

    override fun getComponentName(): String = "StatusWidget"

    override fun initComponent() {}

    override fun disposeComponent() {}

    open fun registerActions() {
        val actionManager = ActionManager.getInstance() ?: return
        actionGroup = DefaultActionGroup()
        provider.configs.keys.forEach { key ->
            actionGroup!!.add(object : AnAction() {
                init { templatePresentation.text = key }

                override fun actionPerformed(e: AnActionEvent) {
                    provider.setConfigFor(e.getData(PSI_FILE)!!, key)
                    DaemonCodeAnalyzer.getInstance(e.project).restart()
                }
            })
        }
        actionManager.registerAction(actionGroupId, actionGroup!!)
    }

    open internal fun unregisterActions() {
        ActionManager.getInstance()?.unregisterAction(actionGroupId)
    }

    override fun ID(): String {
        return "RedPen"
    }

    override fun getPresentation(platformType: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return null
    }

    open fun update(configKey: String) {
        if (isDisposed) return
        ApplicationManager.getApplication().invokeLater {
            component.text = configKey
            component.foreground = if (enabled) UIUtil.getActiveTextColor() else UIUtil.getInactiveTextColor()
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val file = if (event.newFile == null) null else PsiManager.getInstance(project!!).findFile(event.newFile!!)
        if (file != null && provider.getParser(file) != null) {
            enabled = true
            update(provider.getConfigKeyFor(file))
        }
        else {
            enabled = false
            update("n/a")
        }
    }

    override fun getComponent(): TextPanel {
        return component
    }

    internal fun showPopup(e: MouseEvent) {
        if (!enabled) return
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "RedPen", actionGroup!!, getContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
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

    fun rebuild() {
        unregisterActions()
        registerActions()
    }
}
