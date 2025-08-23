package privateering.scripts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.campaign.CommDirectory
import com.fs.starfarer.campaign.CommDirectoryEntry
import com.fs.state.AppDriver
import privateering.misc.ReflectionUtils

class CommDirectoryRecolorScript : EveryFrameScript {

    @Transient var controlPanel: CustomPanelAPI? = null

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
       return true
    }

    override fun advance(amount: Float) {
        if (!Global.getSector().isPaused) return

        var state = AppDriver.getInstance().currentState
        if (state !is CampaignState) return

        var core: UIPanelAPI? = null

        var docked = false

        var dialog = ReflectionUtils.invoke("getEncounterDialog", state)
        if (dialog != null)
        {
            core = ReflectionUtils.invoke("getCoreUI", dialog) as UIPanelAPI?
        }

        if (dialog !is UIPanelAPI) return

        var children = dialog.getChildrenCopy()
        for (child in children) {
            if (ReflectionUtils.hasVariableOfType(CommDirectory::class.java, child) && child is UIPanelAPI)  {
                if (controlPanel != null && child.getChildrenCopy().contains(controlPanel!!)) return

                var panel = child.getChildrenCopy().firstOrNull()
                controlPanel = Global.getSettings().createCustom(0f, 0f, null)
                child.addComponent(controlPanel)

                if (panel != null) {
                    var innerPanel = ReflectionUtils.invoke("getCurr", panel)
                    if (innerPanel is UIPanelAPI) {
                        var innerPanelChildren = innerPanel.getChildrenCopy()
                        var grid = innerPanelChildren.filter { it is UIPanelAPI }.flatMap { (it as UIPanelAPI).getChildrenCopy() }.find { ReflectionUtils.hasMethodOfName("getItems", it) }

                        if (grid != null) {
                            var items = ReflectionUtils.invoke("getItems", grid) as List<*>
                            for (item in items) {
                                if (item is ButtonAPI) {
                                    var renderer = ReflectionUtils.invoke("getRenderer", item)
                                    if (renderer != null) {
                                        var buttonPanel = ReflectionUtils.getFirstDeclaredField(renderer)
                                        if (buttonPanel is UIPanelAPI) {
                                            var buttonChildren = buttonPanel.getChildrenCopy()
                                            var entry = ReflectionUtils.get(null, buttonPanel, CommDirectoryEntry::class.java) as CommDirectoryEntryAPI

                                            var person = entry.entryData as PersonAPI
                                            var color = person.faction.color

                                            for (buttonChild in buttonChildren) {
                                                if (buttonChild is LabelAPI && buttonChild.text.contains("Commission Supervisor")) {
                                                    buttonChild.setHighlight("Commission Supervisor", "Contact")
                                                    buttonChild.setHighlightColors(color, Misc.getBasePlayerColor())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}