package privateering.rules

import com.fs.graphics.util.Fader
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.command.CustomProductionPanel
import com.fs.starfarer.loading.specs.FactionProduction
import com.fs.starfarer.ui.impl.StandardTooltipV2
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import lunalib.lunaExtensions.addLunaElement
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.input.Keyboard
import org.magiclib.kotlin.getMercs
import org.magiclib.kotlin.getStorage
import org.magiclib.kotlin.setFullySurveyed
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.misc.ReflectionUtils
import privateering.scripts.baseOrModSpec
import privateering.scripts.getChildrenCopy
import privateering.scripts.getChildrenNonCopy
import privateering.scripts.getParent
import privateering.ui.FactionProductionOverwrite
import privateering.ui.RequisitionProductionPicker
import privateering.ui.element.*
import java.util.*

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

    lateinit var parent: UIPanelAPI
    lateinit var panel: CustomPanelAPI
    lateinit var element: TooltipMakerAPI
    lateinit var reqBar: RequisitionBar

    var orbitPanel: CustomPanelAPI? = null

    override fun init(dialog: InteractionDialogAPI) {

        this.dialog = dialog

        dialog.textPanel.addPara("\"Which of our services do you require today?\"")

        //dialog.visualPanel.hideFirstPerson()
        dialog.visualPanel.showPersonInfo(person, false, false)

        var visual = dialog.visualPanel as UIPanelAPI
        var children = visual.getChildrenCopy()

        parent = children.last() as UIPanelAPI


        panel = Global.getSettings().createCustom(1000f, 1000f, null)
        parent.addComponent(panel)
        panel.position.inTL(0f, 0f)

        element = panel.createUIElement(1000f, 1000f, false)
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

        if (orbitPanel != null) {
            orbitPanel!!.getParent()!!.removeComponent(orbitPanel)
            orbitPanel = null
            toBuildOrbit = null
            orbitPicker = null
        }

        var faction = Misc.getCommissionFaction()
        var data = PrivateeringUtils.getCommissionData(faction)
        var bonds = data.bonds

        dialog.optionPanel.addOption("Request faction nanoforge production (25% off)", "NANOFORGE_FACTION")
        dialog.optionPanel.setTooltip("NANOFORGE_FACTION", "Use your requisition bonds to order custom production. Only ${faction.displayName} blueprints are available. Capital ships and large weapons are only available after the \"Arsenal Authorization\" stage of the favorability event has been reached.")
        dialog.optionPanel.setTooltipHighlights("NANOFORGE_FACTION", faction.displayName, "Capital ships", "large weapons", "Arsenal Authorization")
        dialog.optionPanel.setTooltipHighlightColors("NANOFORGE_FACTION", faction.color, Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())

        dialog.optionPanel.addOption("Request custom nanoforge production", "NANOFORGE_ALL")
        dialog.optionPanel.setTooltip("NANOFORGE_ALL", "Use your requisition bonds to order custom production. Only blueprints that you know are available. Capital ships and large weapons are only available after the \"Arsenal Authorization\" stage of the favorability event has been reached.")
        dialog.optionPanel.setTooltipHighlights("NANOFORGE_ALL", "Capital ships", "large weapons", "Arsenal Authorization")
        dialog.optionPanel.setTooltipHighlightColors("NANOFORGE_ALL", Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())

        dialog.optionPanel.addOption("Request ship repairs (D-Mod Removal)", "DMOD_REMOVAL")

        generateMercs()
        dialog.optionPanel.addOption("Request mercenary officers", "MERC_OFFICERS")
        if (data.mercs.isEmpty()) {
            dialog.optionPanel.setEnabled("MERC_OFFICERS", false)
            dialog.optionPanel.setTooltip("MERC_OFFICERS", "There are currently no new mercenaries available.")
        }

        dialog.optionPanel.addOption("Request the construction of a personal station in ${faction.displayName} space", "BUILD_STATION")
        if (data.hasBuildStation) {
            dialog.optionPanel.setEnabled("BUILD_STATION", false)
            dialog.optionPanel.setTooltip("BUILD_STATION", "You can not build more than one station.")
        }


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

    override fun optionSelected(optionText: String?, optionData: Any?) {

        var data = PrivateeringUtils.getCommissionData()
        var faction = Misc.getCommissionFaction()

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

        if (optionData == "DMOD_REMOVAL") {
            memberToRepair = null
            dialog.textPanel.addPara("Request ship repairs", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            showDmodDialog()
        }

        if (optionData == "DMOD_SELECT_SHIP") {


            dialog.showFleetMemberPickerDialog("Pick a ship to repair", "Confirm", "Cancel", 3, 10, 64f,
                true, false, getRestorableShipsWithDmods(), object : FleetMemberPickerListener {
                    override fun pickedFleetMembers(members: MutableList<FleetMemberAPI>?) {
                        var first = members!!.firstOrNull()
                        if (first != null) {
                            memberToRepair = first
                            recreateDmodOptions()
                        }
                    }

                    override fun cancelledFleetMemberPicking() {

                    }
                })

        }

        if (optionData == "DMOD_PERFORM_REPAIR" && memberToRepair != null) {

            dialog.textPanel.addPara("Repair the ${memberToRepair!!.shipName}", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())

            var cost = getRestoreCost(memberToRepair!!)

            for (dmod in getDmods(memberToRepair!!.variant)) {
                DModManager.removeDMod(memberToRepair!!.variant, dmod.id)
            }

            dialog.textPanel.setFontSmallInsignia()
            dialog.textPanel.addPara("Spent ${cost.toInt()} requisition bonds",  Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), "${cost.toInt()}")
            dialog.textPanel.addPara("Removed all d-mods from the ${memberToRepair!!.shipName}",  Misc.getStoryOptionColor(), Misc.getStoryOptionColor(), "")
            dialog.textPanel.setFontInsignia()

            data.bonds = MathUtils.clamp(data.bonds-cost, 0f, CommissionData.maxBonds)
            Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

            memberToRepair = null
            recreateDmodOptions()
        }

        if (optionData == "MERC_OFFICERS") {
            dialog.textPanel.addPara("Request mercenary officers", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())

            showMercDialog()
        }


        if (optionData == "BUILD_STATION") {
            dialog.textPanel.addPara("Request the construction of a personal station in ${faction.displayName} space", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            showStationBuildDialog()
        }


        if (optionData == "SELECT_STARSYSTEM") {
            var systems = Global.getSector().economy.marketsCopy.filter { it.faction == faction }.map { it.starSystem }.distinct().toMutableSet()
            var entities = systems.map { it.center }

            dialog.showCampaignEntityPicker("Select a starsystem to build in.", "Build a station in", "Confirm", faction, entities, PrivateerSystemPicker(systems, this))
        }

        if (optionData == "BUILD_STATION_DONE") {
            dialog.textPanel.addPara("Order the construction of the station", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            construct()
        }

        if (optionData == "INSIGNIA") {

            dialog.textPanel.setFontSmallInsignia()
            dialog.textPanel.addPara("Spent 750 requisition bonds",  Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), "750")
            dialog.textPanel.addPara("Gained a story point",  Misc.getStoryOptionColor(), Misc.getStoryOptionColor(), "")
            dialog.textPanel.setFontInsignia()

            Global.getSector().playerPerson.stats.storyPoints += 1
            data.bonds = MathUtils.clamp(data.bonds-750, 0f, CommissionData.maxBonds)
            Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

            if (data.bonds < 750) {
                dialog.optionPanel.setEnabled("INSIGNIA", false)
                dialog.optionPanel.setTooltip("INSIGNIA", "You do not have enough bonds.")
            }

        }



        if (optionData == "RECREATE") {
            dialog.textPanel.addPara("Back", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
            recreateOptions()
        }



        if (optionData is PersonAPI) {
            var merc = optionData
            var cost = getMercCost(merc)

            data.bonds = MathUtils.clamp(data.bonds-cost, 0f, CommissionData.maxBonds)

            dialog.textPanel.setFontSmallInsignia()
            var label = dialog.textPanel.addPara("Spent ${cost} requisition bonds",  Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), "${cost}")
            dialog.textPanel.setFontInsignia()

            Global.getSector().playerFleet.fleetData.addOfficer(merc)
            Misc.setMercHiredNow(merc)

            mercsInSelection.remove(merc)
            data.mercs.remove(merc)

            AddRemoveCommodity.addOfficerGainText(merc, dialog.textPanel)
            Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

            recreateMercOptions()
        }

        if (optionData == "BACK") {
            returnToPrevious()
        }
    }

    var toBuildOrbit: OrbitSelectorWidget.SelectedOrbit? = null
    var orbitPicker: OrbitSelectorWidget? = null
    fun showStationBuildDialog() {

        dialog.optionPanel.clearOptions()
        var faction = Misc.getCommissionFaction()

        dialog.textPanel.addPara("\"All successful ${faction.displayName} commission members are permitted to request the construction of a small space station to operate from. " +
                "The station can only be build in ${faction.displayName} space.", Misc.getTextColor(), faction.color, faction.displayName + " space")

        dialog.textPanel.addPara("The station acts as a size 3 colony under your own command. It can grow up to a maximum size of 4. " +
                "Only one such station can be requested per captain. The station continues to belong to you even if you were to renounce your commission. \"", Misc.getTextColor(), Misc.getHighlightColor(), "3", "4", "one")

        recreateStationOptions()
    }

    fun recreateStationOptions() {
        dialog.optionPanel.clearOptions()

        var data = PrivateeringUtils.getCommissionData()

        dialog.optionPanel.addOption("Select a starsystem", "SELECT_STARSYSTEM")

        var cost = CommissionData.stationBuildCost.toInt()
        dialog.optionPanel.addOption("Order the construction of the station (-$cost bonds)", "BUILD_STATION_DONE")
        dialog.optionPanel.addOptionConfirmation("BUILD_STATION_DONE", "Are you sure you want to construct the station?", "Confirm", "Cancel")

        //Add the "no system selected" or "Invalid location" tooltips above this
        if (toBuildOrbit == null) {
            dialog.optionPanel.setEnabled("BUILD_STATION_DONE", false)
            dialog.optionPanel.setTooltip("BUILD_STATION_DONE", "Select a system and a location within the system to start construction.")
        }
        else if (data.bonds < cost) {
            dialog.optionPanel.setEnabled("BUILD_STATION_DONE", false)
            dialog.optionPanel.setTooltip("BUILD_STATION_DONE", "You do not have enough bonds to construct the station")
        }



        addBackToRecreateOption()
    }

    fun construct() {
        var orbit = toBuildOrbit ?: return

        var data = PrivateeringUtils.getCommissionData()
        data.bonds = MathUtils.clamp(data.bonds-CommissionData.stationBuildCost, 0f, CommissionData.maxBonds)

        data.hasBuildStation = true

        dialog.textPanel.setFontSmallInsignia()
        dialog.textPanel.addPara("Spent ${CommissionData.stationBuildCost.toInt()} requisition bonds",  Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), "${CommissionData.stationBuildCost.toInt()}")
        dialog.textPanel.addPara("Build an outpost for your own faction",  Misc.getStoryOptionColor(), Misc.getStoryOptionColor(), "")
        dialog.textPanel.setFontInsignia()

        Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

        var system = orbit.focus.starSystem
        var station = system.addCustomEntity("${CommissionData.stationSpecId}_${Misc.genUID()}", "Outpost", CommissionData.stationSpecId, Factions.PLAYER)
        Misc.fadeIn(station, 1.5f)

        var orbitRadius = orbit.distance
        var days = orbitRadius / (5f + Random().nextFloat() * 10f);
        station.setCircularOrbitWithSpin(orbit.focus, orbit.angle - 180f, orbit.distance, days, -1.5f, 1.5f)

        var market = PrivateeringUtils.addMarketplace(Factions.PLAYER,
            station,
            arrayListOf(),
            "Outpost",
            3,
            arrayListOf("priv_commStation"),
            arrayListOf(Submarkets.LOCAL_RESOURCES, Submarkets.SUBMARKET_STORAGE),
            arrayListOf(Industries.POPULATION, Industries.SPACEPORT, Industries.WAYSTATION),
            0.3f,
            false,
            false)

        var storage = market.getStorage()
        if (storage is StoragePlugin) {
            storage.setPlayerPaidToUnlock(true)
        }

        //Global.getSector().economy.addMarket(market, false)
        station.setMarket(market);
        station.setFaction(Global.getSector().playerFaction.id);

        Misc.setFullySurveyed(market, null, false)
        market.isPlayerOwned = true

        recreateOptions()
    }

    fun selectedOrbitForStation(orbit: OrbitSelectorWidget.SelectedOrbit) {
        toBuildOrbit = orbit
        recreateStationOptions()
    }

    //Required because alex hasnt made the overwrites work for ships and fighters
    fun createProductionPicker(faction: FactionAPI) {
        var production = FactionProductionOverwrite(faction)
        var picker = RequisitionProductionPicker(faction, dialog.textPanel, dialog.interactionTarget.market)
        production.delegate = picker
        production.costMult = picker.costMult

        dialog.showCustomProductionPicker(picker)
        var parent = (dialog as UIPanelAPI).getChildrenNonCopy().last()
        var custom = ReflectionUtils.get(null, parent, CustomProductionPanel::class.java)
        ReflectionUtils.set(null, custom!!, production, FactionProduction::class.java)
    }


    fun showDmodDialog() {


        dialog.textPanel.addPara("\"Our restoration teams are able to repair damages ships in your fleet, " +
                "removing their d-mods. This process is prohibitively expensive, more so than acquiring a new ship.")

        dialog.textPanel.addPara("While still costly, our expert teams are capable of providing this service at a much cheaper price than your usual port service can.\"")

        recreateDmodOptions()
    }

    var memberToRepair: FleetMemberAPI? = null
    fun recreateDmodOptions() {
        var data = PrivateeringUtils.getCommissionData()
        dialog.optionPanel.clearOptions()

        dialog.optionPanel.addOption("Select a ship", "DMOD_SELECT_SHIP")
        if (getRestorableShipsWithDmods().isEmpty()) {
            dialog.optionPanel.setEnabled("DMOD_SELECT_SHIP", false)
            dialog.optionPanel.setTooltip("DMOD_SELECT_SHIP", "You do not have any ships with d-mods that can be restored. Certain special ships are unfixable.")
        }

        if (memberToRepair != null) {
            var cost = getRestoreCost(memberToRepair!!)
            dialog.optionPanel.addOption("Repair the ${memberToRepair!!.shipName} (-${cost.toInt()} bonds)", "DMOD_PERFORM_REPAIR")
            if (data.bonds < cost) {
                dialog.optionPanel.setEnabled("DMOD_PERFORM_REPAIR", false)
                dialog.optionPanel.setTooltip("DMOD_PERFORM_REPAIR", "You do not have enough bonds to repair this ship.")

            }

        }



        addBackToRecreateOption()

    }

    val DMOD_BASE_COST: Float = Global.getSettings().getFloat("baseRestoreCostMult") - 0.25f
    //val DMOD_COST_PER_MOD: Float = Global.getSettings().getFloat("baseRestoreCostMultPerDMod")
    val DMOD_COST_PER_MOD: Float = MathUtils.clamp(Global.getSettings().getFloat("baseRestoreCostMultPerDMod") - 0.1f, 1f, Float.MAX_VALUE)
    fun getRestoreCost(member: FleetMemberAPI) : Float {
        var cost = member.baseValue
        cost *= DMOD_BASE_COST
        var dmodsCount = getDmods(member.variant).count()

        //Cost per dmod
        cost *= Math.pow(DMOD_COST_PER_MOD.toDouble(), dmodsCount.toDouble()).toFloat()

        cost /= CommissionData.bondValue

        return cost
    }

    //Gray out ship selector if empty
    fun getRestorableShipsWithDmods() : List<FleetMemberAPI> {
        var list = ArrayList<FleetMemberAPI>()

        //check if it can be restored first
        for (member in Global.getSector().playerFleet.fleetData.membersListCopy) {
            var variant = member.variant
            var dmods = getDmods(variant)
            if (dmods.isNotEmpty() && !variant.hasTag(Tags.VARIANT_UNRESTORABLE) && !member.baseOrModSpec().hasTag(Tags.HULL_UNRESTORABLE)) {
                list.add(member)
            }
        }

        return list
    }

    fun getDmods(variant: ShipVariantAPI) : List<HullModSpecAPI> {
        var list = ArrayList<HullModSpecAPI>()

        for (id in variant.hullMods) {
            if (DModManager.getMod(id).hasTag(Tags.HULLMOD_DMOD)) {
                if (variant.hullSpec.builtInMods.contains(id)) continue
                list.add(Global.getSettings().getHullModSpec(id))
            }
        }

        return list
    }


    //Mercs
    var mercsInSelection = ArrayList<PersonAPI>()
    fun generateMercs() {
        var data = PrivateeringUtils.getCommissionData()

        var shouldReset = false
        if (data.lastMercTimestamp == null) shouldReset = true
        else if (Global.getSector().clock.getElapsedDaysSince(data.lastMercTimestamp!!) >= 30) {
            shouldReset = true
        }

        if (shouldReset) {
            data.lastMercTimestamp = Global.getSector().clock.timestamp
            data.mercs.clear()

            for (i in 0 until 2) {
                var officer = OfficerManagerEvent.createOfficer(Misc.getCommissionFaction(), MathUtils.getRandomNumberInRange(4,6),
                    OfficerManagerEvent.SkillPickPreference.ANY, false, null, true, true, MathUtils.getRandomNumberInRange(0, 2), Random())

                data.mercs.add(officer)
                officer.addTag("privateers_merc")
                officer.rankId = Ranks.SPACE_CAPTAIN
                officer.postId = Ranks.POST_MERCENARY
                Misc.setMercenary(officer, true)
            }
        }

    }

    fun showMercDialog() {
        var data = PrivateeringUtils.getCommissionData()

        dialog.optionPanel.clearOptions()


        var days = Global.getSettings().getInt("officerMercContractDur")
        var base = Global.getSettings().getInt("officerSalaryBase")
        var perLevel = Global.getSettings().getInt("officerSalaryPerLevel")
        var pay = (base + (perLevel*4)) * Global.getSettings().getFloat("officerMercPayMult")

        dialog.textPanel.addPara("\"We have lists of vetted freelance mercenaries that are available on request. We will only offer you up to 2 active contracts at a time. " +
                "Their typical contract length is $days days.", Misc.getTextColor(), Misc.getHighlightColor(), "2", "$days")

        dialog.textPanel.addPara("They expect a salary of atleast ${Misc.getDGSCredits(pay)} per month. These mercenaries are highly professional and do not count towards the usual officer limitations. \"",
            Misc.getTextColor(), Misc.getHighlightColor(), "${Misc.getDGSCredits(pay)}")

        mercsInSelection.clear()
        for (merc in data.mercs) {
            mercsInSelection.add(merc)
        }

        addMercsToDialog(data.mercs)

        recreateMercOptions()
    }

    fun getMercCost(merc: PersonAPI) : Int{
        var cost = when(merc.stats.level) {
            4 -> 50
            5 -> 75
            6 -> 100
            else -> 100
        }
        for (skill in merc.stats.skillsCopy) {
            if (skill.level >= 2) {
                cost += 10
            }
        }
        return cost
    }

    fun isAtMaxMercs() : Boolean{
        var count = 0
        var fleet = Global.getSector().playerFleet
        var mercs = fleet.getMercs()
        for (merc in mercs) {
            if (merc.person.hasTag("privateers_merc")) {
                count++
            }
        }
        return count >= 2
    }

    fun recreateMercOptions() {
        var data = PrivateeringUtils.getCommissionData()
        var bonds = data.bonds

        dialog.optionPanel.clearOptions()

        for (merc in mercsInSelection) {
            dialog.optionPanel.addOption("Hire ${merc.nameString} (-${getMercCost(merc)} bonds)", merc)
            if (isAtMaxMercs()) {
                dialog.optionPanel.setEnabled(merc, false)
                dialog.optionPanel.setTooltip(merc, "You can not have more than 2 contracts with mercenaries from your commission at once.")
            } else if (bonds < getMercCost(merc)) {
                dialog.optionPanel.setEnabled(merc, false)
                dialog.optionPanel.setTooltip(merc, "You do not have enough bonds to hire this mercenary.")
            }
        }

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
            var cost = 750
            var usage = MathUtils.clamp(cost / data.bonds, 0f, 1f)
            reqBar.useage = reqBar.current * (1-usage)
        }
        else if (optionText.contains("Repair the") && memberToRepair != null) {
            var cost = getRestoreCost(memberToRepair!!)
            var usage = MathUtils.clamp(cost / data.bonds, 0f, 1f)
            reqBar.useage = reqBar.current * (1-usage)
        }
        else if (optionText.contains("Order the construction of the station")) {
            var cost = CommissionData.stationBuildCost
            var usage = MathUtils.clamp(cost / data.bonds, 0f, 1f)
            reqBar.useage = reqBar.current * (1-usage)
        }
        else {
            reqBar.useage = data.bonds/ CommissionData.maxBonds
        }

        for (merc in mercsInSelection) {
            if (optionText.contains(merc.nameString)) {
                var cost = getMercCost(merc)
                var usage = MathUtils.clamp(cost / data.bonds, 0f, 1f)
                reqBar.useage = reqBar.current * (1-usage)
            }
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
            skills = skills.sortedBy { it.skill.governingAptitudeOrder }
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
                var isElite = skill.level >= 2f

                var skillElement = SkillWidgetElement(skill.skill.id, true, false, true, skill.skill.spriteName, color, isElite, element, 58f, 58f)

                var tooltip = ReflectionUtils.invokeStatic(8, "createSkillTooltip", StandardTooltipV2::class.java,
                    skill.skill, officer.stats,
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
            namePara.position.inTL(0f, 0f)

        }
        dialog.textPanel.addTooltip()
    }

    class PrivateerSystemPicker(var systems: MutableSet<StarSystemAPI>, var dialog: SupervisorDialogDelegate) : BaseCampaignEntityPickerListener() {
        override fun getMenuItemNameOverrideFor(entity: SectorEntityToken?): String? {
            return entity?.starSystem?.nameWithNoType
        }

        override fun canConfirmSelection(entity: SectorEntityToken?): Boolean {
            return entity != null
        }

        override fun pickedEntity(entity: SectorEntityToken?) {
            var system = entity?.starSystem ?: return

            //Only show this dialog the first time picking a system
            if (dialog.orbitPanel == null) {

                dialog.dialog.textPanel.addPara("Select a starsystem", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())
                dialog.dialog.textPanel.addPara("Select an orbit for the new station to inhabit on the right. " +
                        "Click to select an orbit and drag to move the map around. The line being red implies that the orbit is to close to its celestial body.",
                    Misc.getTextColor(), Misc.getNegativeHighlightColor(), "red")
            }

            if (dialog.orbitPanel != null) {
                dialog.orbitPanel!!.getParent()!!.removeComponent(dialog.orbitPanel)
                dialog.orbitPicker = null
                dialog.toBuildOrbit = null
            }

            //var parent = dialog.dialog.visualPanel as UIPanelAPI
            dialog.orbitPanel = Global.getSettings().createCustom(1000f, 1000f, null)
            dialog.parent.addComponent(dialog.orbitPanel)
            dialog.orbitPanel!!.position.inTL(0f, 0f)

            var orbitElement = dialog.orbitPanel!!.createUIElement(1000f, 1000f, false)
            dialog.orbitPanel!!.addUIElement(orbitElement)
            orbitElement.position.inTL(0f, 0f)
            dialog.orbitPicker = OrbitSelectorWidget(dialog, system, orbitElement, 365f, 220f)

            dialog.orbitPicker!!.position.inTL(3f, 225f)
        }

        override fun getStarSystemsToShow(): MutableSet<StarSystemAPI> {
            return systems
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