package privateering.rules

import com.fs.graphics.util.Fader
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.command.CustomProductionPanel
import com.fs.starfarer.loading.specs.FactionProduction
import com.fs.starfarer.ui.impl.StandardTooltipV2
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import lunalib.lunaExtensions.addLunaElement
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.input.Keyboard
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.misc.ReflectionUtils
import privateering.scripts.getChildrenCopy
import privateering.scripts.getChildrenNonCopy
import privateering.scripts.getParent
import privateering.ui.FactionProductionOverwrite
import privateering.ui.RequisitionProductionPicker
import privateering.ui.element.DialogAptitudeBackgroundElement
import privateering.ui.element.RequisitionBar
import privateering.ui.element.SkillSeperatorElement
import second_in_command.ui.elements.OfficerDisplayElement
import privateering.ui.element.SkillWidgetElement

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

        dialog.textPanel.addPara("\"Which of our services do you require today?\"")

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



        recreateOptions()

    }

    fun recreateOptions() {
        dialog.optionPanel.clearOptions()

        var faction = Misc.getCommissionFaction()
        var data = PrivateeringUtils.getCommissionData(faction)
        var bonds = data.bonds

        dialog.optionPanel.addOption("Request faction-grade nanoforge production (25% off)", "NANOFORGE_FACTION")
        dialog.optionPanel.setTooltip("NANOFORGE_FACTION", "Use your requisition bonds to order custom production. Only ${faction.displayName} blueprints are available. Capital ships and large weapons are only available after the \"Arsenal Authorization\" stage of the favorability event has been reached.")
        dialog.optionPanel.setTooltipHighlights("NANOFORGE_FACTION", faction.displayName, "Capital ships", "large weapons", "Arsenal Authorization")
        dialog.optionPanel.setTooltipHighlightColors("NANOFORGE_FACTION", faction.color, Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())

        dialog.optionPanel.addOption("Request military-grade nanoforge production", "NANOFORGE_ALL")
        dialog.optionPanel.setTooltip("NANOFORGE_ALL", "Use your requisition bonds to order custom production. Only blueprints that you know are available. Capital ships and large weapons are only available after the \"Arsenal Authorization\" stage of the favorability event has been reached.")
        dialog.optionPanel.setTooltipHighlights("NANOFORGE_ALL", "Capital ships", "large weapons", "Arsenal Authorization")
        dialog.optionPanel.setTooltipHighlightColors("NANOFORGE_ALL", Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())

        dialog.optionPanel.addOption("Request ship repairs (D-Mod Removal)", "DMOD_REMOVAL")
        dialog.optionPanel.addOption("Request mercenary officers", "MERC_OFFICERS")
        dialog.optionPanel.addOption("Request the construction of a personal station in ${faction.displayName} space", "STATION")
        dialog.optionPanel.addOption("Request an insignia honoring your achievements (-750 bonds, +1 SP)", "INSIGNIA")
        if (bonds < 750f) {
            dialog.optionPanel.setEnabled("INSIGNIA", false)
            dialog.optionPanel.setTooltip("INSIGNIA", "You do not have enough bonds.")
        }


        dialog.setOptionColor("INSIGNIA", Misc.getStoryOptionColor())

        dialog.optionPanel.addOption("\"I need more time to consider\".", "BACK")
        dialog.optionPanel.setShortcut("BACK", Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    fun addBackToRecreateOption() {
        dialog.optionPanel.addOption("Back", "RECREATE")
        dialog.optionPanel.setShortcut("RECREATE", Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    fun returnToPrevious() {
        dialog.optionPanel.clearOptions()
        dialog.textPanel.addPara("\"I need more time to consider\".", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())

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

    //Required because alex hasnt made the overwrites work for ships and fighters
    fun createProductionPicker(faction: FactionAPI) {
        var production = FactionProductionOverwrite(faction)
        var picker = RequisitionProductionPicker(dialog.textPanel, faction, dialog.interactionTarget.market)
        production.delegate = picker
        production.costMult = picker.costMult

        dialog.showCustomProductionPicker(picker)
        var parent = (dialog as UIPanelAPI).getChildrenNonCopy().last()
        var custom = ReflectionUtils.get(null, parent, CustomProductionPanel::class.java)
        ReflectionUtils.set(null, custom!!, production, FactionProduction::class.java)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {

        if (optionData == "sc_convo_question") {

        }

        if (optionData == "NANOFORGE_FACTION") {
            dialog.textPanel.addPara("Request faction-grade nanoforge production", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            createProductionPicker(Misc.getCommissionFaction())
        }

        if (optionData == "NANOFORGE_ALL") {
            dialog.textPanel.addPara("Request military-grade nanoforge production", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            createProductionPicker(Global.getSector().playerFaction)
        }

        if (optionData == "MERC_OFFICERS") {
            dialog.textPanel.addPara("Request mercenary officers", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            showMercDialog()
        }

        if (optionData == "RECREATE") {
            dialog.textPanel.addPara("Back", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            recreateOptions()
        }

        if (optionData == "BACK") {
            returnToPrevious()
        }
    }


    fun showMercDialog() {
        dialog.optionPanel.clearOptions()

        var officers = ArrayList<PersonAPI>()

        var days = Global.getSettings().getInt("officerMercContractDur")
        var base = Global.getSettings().getInt("officerSalaryBase")
        var perLevel = Global.getSettings().getInt("officerSalaryPerLevel")
        var pay = (base + (perLevel*4)) * Global.getSettings().getFloat("officerMercPayMult")

        dialog.textPanel.addPara("\"We have lists of vetted freelance mercenaries that are available on request. We will only offer you up to 2 active contracts at a time. " +
                "Their typical contract length is $days days.", Misc.getTextColor(), Misc.getHighlightColor(), "2", "$days")

        dialog.textPanel.addPara("They expect a salary of atleast ${Misc.getDGSCredits(pay)} per month. These mercenaries are highly professional and do not count towards the usual officer limitations. \"",
            Misc.getTextColor(), Misc.getHighlightColor(), "${Misc.getDGSCredits(pay)}")

        for (i in 0 until 2) {
            var officer = OfficerManagerEvent.createOfficer(Misc.getCommissionFaction(), MathUtils.getRandomNumberInRange(4,5))
            officers.add(officer)
        }

        addMercsToDialog(officers)

        addBackToRecreateOption()
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

        if (optionText.contains("Request an insignia honoring your achievements")) {
            var used = 750
            var usage = MathUtils.clamp(used / data.bonds, 0f, 1f)
            reqBar.useage = reqBar.current * (1-usage)
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

    fun addMercsToDialog(officers: List<PersonAPI>) {

        var tooltip = dialog!!.textPanel.beginTooltip()
        for (officer in officers) {
            tooltip.addSpacer(0f)

            var width = 500f
            var height = 96f
            var panel = Global.getSettings().createCustom(width, height, null)
            tooltip.addCustom(panel, 0f)
            var element = panel.createUIElement(width, height, false)
            panel.addUIElement(element)

            var skills = officer.stats.skillsCopy
            var color = Global.getSettings().getSkillSpec(Skills.HELMSMANSHIP).governingAptitudeColor

            element.addSpacer(10f)

            var officerPickerElement = OfficerDisplayElement(officer, color, element, 80f, 80f)
            //officerPickerElement.position.inTL(10f, 10f)

            officerPickerElement.onHoverEnter {
                officerPickerElement.playScrollSound()
            }

            officerPickerElement.onClick {
                officerPickerElement.playClickSound()
            }


            var offset = 6f
            var offsetElement = element.addLunaElement(0f, 0f)
            offsetElement.elementPanel.position.rightOfMid(officerPickerElement.elementPanel, -8f)

            var background = DialogAptitudeBackgroundElement(color, element, 7f)
            background.elementPanel.position.belowLeft(offsetElement.elementPanel, offset)

            var previous: CustomPanelAPI? = null

            var first: SkillWidgetElement? = null

            for (skill in skills) {
                if (skill.level <= 0) continue
                if (!skill.skill.isCombatOfficerSkill) continue

                var isFirst = skills.first() == skill
                var isLast = skills.last() == skill

                var skillElement = SkillWidgetElement(skill.skill.id, true, false, true, skill.skill.spriteName, color, element, 58f, 58f)

                var tooltip = ReflectionUtils.invokeStatic(8, "createSkillTooltip", StandardTooltipV2::class.java,
                    skill.skill, Global.getSector().playerPerson.stats,
                    800f, 10f, true, false, 1000, null)

                ReflectionUtils.invokeStatic(2, "addTooltipBelow", StandardTooltipV2Expandable::class.java, skillElement.elementPanel, tooltip)

                skillElement.onClick {
                    skillElement.playClickSound()
                }


                if (previous != null) {
                    skillElement.elementPanel.position.rightOfTop(previous, 3f)
                } else {
                    first = skillElement
                    skillElement.elementPanel.position.rightOfMid(background.elementPanel, 20f)
                }


                if (!isLast) {
                    var seperator = SkillSeperatorElement(color, element, 58f)
                    seperator.elementPanel.position.rightOfTop(skillElement.elementPanel, 3f)
                    previous = seperator.elementPanel
                }
            }

            var paraElement = element.addLunaElement(300f, 20f).apply {
                renderBorder = false
                renderBackground = false
            }
            paraElement.elementPanel.position.aboveLeft(first!!.elementPanel, 0f)

            paraElement.innerElement.setParaFont("graphics/fonts/victor14.fnt")
            var namePara = paraElement.innerElement.addPara("${officer.nameString}: lv ${officer.stats.level}", 0f, color, color)
            namePara.position.inTL(0f,  0f,)

        }
        dialog.textPanel.addTooltip()
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