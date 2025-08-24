package privateering.rules

import com.fs.graphics.util.Fader
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.misc.ReflectionUtils
import privateering.scripts.getChildrenCopy
import privateering.scripts.getParent
import privateering.ui.element.RequisitionBar

class PrivateerSupervisorDialog : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI, params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog.optionPanel.clearOptions()

        var plugin = SupervisorDialogDelegate(dialog.plugin, dialog.interactionTarget.activePerson)
        dialog.plugin = plugin
        plugin.init(dialog)

        return true
    }
}

class SupervisorDialogDelegate(var original: InteractionDialogPlugin, var person: PersonAPI) : InteractionDialogPlugin {

    lateinit var dialog: InteractionDialogAPI

    lateinit var panel: CustomPanelAPI
    lateinit var reqBar: RequisitionBar

    override fun init(dialog: InteractionDialogAPI) {

        this.dialog = dialog

        //dialog.visualPanel.hideFirstPerson()
        dialog.visualPanel.showPersonInfo(person, false, false)

        var visual = dialog.visualPanel as UIPanelAPI
        var children = visual.getChildrenCopy()

        var parent = children.last() as UIPanelAPI


        panel = Global.getSettings().createCustom(1000f, 1000f, null)
        parent.addComponent(panel)
        panel.position.inTL(0f, 0f)

        var element = panel.createUIElement(1000f, 1000f, false)
        panel.addUIElement(element)
        element.position.inTL(0f, 0f)

        var faction = Misc.getCommissionFaction()
        var data = PrivateeringUtils.getCommissionData(faction)
        reqBar = RequisitionBar(faction.color, data.bonds/ CommissionData.maxBonds/*-0.1f*/, data.bonds/ CommissionData.maxBonds, element, 180f, 30f)

        reqBar.elementPanel.position.inTL(190f-1f, 180f)




        dialog.optionPanel.clearOptions()

        dialog.optionPanel.addOption("Test1", "1")
        dialog.optionPanel.addOption("Test2", "2")
        dialog.optionPanel.addOption("Test3", "3")

        dialog.optionPanel.addOption("Back", "BACK")
        dialog.optionPanel.setShortcut("BACK", Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    fun returnToPrevious() {
        dialog.optionPanel.clearOptions()
        dialog.textPanel.addPara("End the conversation", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
        dialog.textPanel.addPara("You cut the comm-link.")

        panel.getParent()?.removeComponent(panel)

        //Return to visual without the req bar
        //dialog.visualPanel.hideFirstPerson()
        dialog.visualPanel.showPersonInfo(person, false, false)

        dialog.plugin = original
       /* dialog.visualPanel.hideFirstPerson()
        dialog.interactionTarget.activePerson = null
        (dialog.plugin as RuleBasedDialog).notifyActivePersonChanged()*/

        FireAll.fire(null, dialog, memoryMap, "PopulateOptions")
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {

        if (optionData == "sc_convo_question") {

        }

        if (optionData == "BACK") {
            returnToPrevious()
        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {

    }

    override fun advance(amount: Float) {

        var faction = Misc.getCommissionFaction()
        var data = PrivateeringUtils.getCommissionData(faction)

        var optionText: String = ""
        var any = false
        //Check if an option is being hovered over
        var panel = dialog.optionPanel as UIPanelAPI
        var children = panel.getChildrenCopy()
        for (option in children) {
            if (option is ButtonAPI) {
                var fader = ReflectionUtils.invoke("getGlowFader", option) as Fader
                if (fader.isFadedIn) {
                    any = true
                    optionText = option.text
                    break
                }
            }
        }

        if (any) {
            reqBar.delay = 0f
            //reqBar.fadeIn = reqBar.useage
        }

        if (optionText.contains("Test1")) {
            reqBar.useage = data.bonds/ CommissionData.maxBonds - 0.2f
        }
        else if (optionText.contains("Test2")) {
            reqBar.useage = data.bonds/ CommissionData.maxBonds - 0.3f
        }
        else if (optionText.contains("Test3")) {
            reqBar.useage = data.bonds/ CommissionData.maxBonds - 0.1f
        }
        else {
            reqBar.useage = data.bonds/ CommissionData.maxBonds
        }


    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {

    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
        return original.memoryMap
    }

}