package privateering.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MonthlyReport
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.intel.event.CommissionEventIntel
import privateering.intel.event.FoughtFleetFactor
import privateering.rules.PrivateeringCommission
import privateering.scripts.levelBetween
import privateering.ui.element.RequisitionBar

class PrivateeringCommissionIntel(faction: FactionAPI) : FactionCommissionIntel(faction) {


    class PrivateeringBountyResult(var bonds: Float, payment: Int, fraction: Float, rep: ReputationActionResponsePlugin.ReputationAdjustmentResult?) : CommissionBountyResult(payment, fraction,
        rep) {

    }

    fun updateBaseBounty() {
        baseBounty = Global.getSettings().getFloat("factionCommissionBounty")
        if (CommissionEventIntel.get()?.isStageActive(CommissionEventIntel.Stage.WARFLEET) == true) baseBounty += 200;
    }

    override fun missionAccepted() {
        super.missionAccepted()

        if (CommissionEventIntel.get() == null || CommissionEventIntel.get()?.faction != Misc.getCommissionFaction()) {
            CommissionEventIntel(faction)
        }
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI) {
        if (isEnded || isEnding) return

        if (!battle.isPlayerInvolved) return

        var data = PrivateeringUtils.getCommissionData()
        var reached = CommissionEventIntel.get()?.isStageActive(CommissionEventIntel.Stage.IMPORTANT) ?: false
        var bondsMult = 1f
        if (reached) bondsMult = CommissionData.bondsImportantMult
        var bountyBonds = CommissionData.bondsPerFrigate

        var payment = 0
        var paymentForBonds = 0
        var fpDestroyed = 0f
        for (otherFleet in battle.nonPlayerSideSnapshot) {
            if (!faction.isHostileTo(otherFleet.faction)) continue

            var bounty = 0f
            var bountyForBonds = 0f //Higher by default
            for (loss in Misc.getSnapshotMembersLost(otherFleet)) {
                val mult = Misc.getSizeNum(loss.hullSpec.hullSize)
                bounty += mult * baseBounty
                bountyForBonds += mult * bountyBonds
                fpDestroyed += loss.fleetPointCost.toFloat()
            }

            payment += (bounty * battle.playerInvolvementFraction).toInt()
            paymentForBonds += (bountyForBonds * battle.playerInvolvementFraction * bondsMult).toInt()
        }

        if (payment > 0) {
            Global.getSector().playerFleet.cargo.credits.add(payment.toFloat())

            var bonds = (paymentForBonds / CommissionData.bondValue)
            if (bonds > 0){
                data.bonds = MathUtils.clamp(data.bonds+bonds, 0f, CommissionData.maxBonds)
            }

            val repFP = (fpDestroyed * battle.playerInvolvementFraction).toInt().toFloat()
            val rep = Global.getSector().adjustPlayerReputation(RepActionEnvelope(RepActions.COMMISSION_BOUNTY_REWARD,
                repFP,
                null,
                null,
                true,
                false), faction.id)
            latestResult = PrivateeringBountyResult(bonds, payment, battle.playerInvolvementFraction, rep)
            sendUpdateIfPlayerHasIntel(latestResult, false)
        }

        //Event Progress
        if (fpDestroyed > 0) {
            var level = fpDestroyed.levelBetween(0f, 400f)
            var points = (100 * level).toInt()
            FoughtFleetFactor(points, null)
        }

    }

    companion object {

        /*@JvmStatic
        fun getRelationMult(faction: FactionAPI) : Float{
            var relationMult = 0.3f
            if (faction.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) relationMult = 0.4f
            if (faction.relToPlayer.isAtWorst(RepLevel.FAVORABLE)) relationMult = 0.5f
            if (faction.relToPlayer.isAtWorst(RepLevel.WELCOMING)) relationMult = 0.6f
            if (faction.relToPlayer.isAtWorst(RepLevel.FRIENDLY)) relationMult = 0.7f
            if (faction.relToPlayer.isAtWorst(RepLevel.COOPERATIVE)) relationMult = 0.7f
            return relationMult
        }*/

        @JvmStatic
        fun getMonthlyBaseIncome() : Float {
            var base = 15000f
            if (CommissionEventIntel.get()?.isStageActive(CommissionEventIntel.Stage.PROMOTION) == true) base += 5000f
            return base
        }

        var maxSupplyCompensation = 20000f
        fun getSupplyCompensation(faction: FactionAPI) : Float {
            var spec = Global.getSettings().getCommoditySpec(Commodities.SUPPLIES)

            var coveredMult = PrivateeringUtils.getCommissionData(faction).costsCovered

            var player = Global.getSector().playerFleet
            var monthly = player.totalSupplyCostPerDay * 30

            var compensation = (monthly * spec.basePrice) * coveredMult
            compensation = MathUtils.clamp(compensation, 0f, maxSupplyCompensation)
            return compensation
        }

        var maxCrewCompensation = 5000f
        fun getCrewCompensation(faction: FactionAPI) : Float {
            val crewSalary = Global.getSettings().getInt("crewSalary")
            var coveredMult = PrivateeringUtils.getCommissionData(faction).costsCovered

            var player = Global.getSector().playerFleet
            val crewCost = player.getCargo().getCrew() * crewSalary

            var compensation = crewCost * coveredMult
            compensation = MathUtils.clamp(compensation, 0f, maxCrewCompensation)
            return compensation
        }

        var maxOfficerCompensation = 10000f
        fun getOfficerCompensation(faction: FactionAPI) : Float {
            var coveredMult = PrivateeringUtils.getCommissionData(faction).costsCovered

            var player = Global.getSector().playerFleet
            var maxCompensation = 10000f

            var salary = 0f
            for (officer in player.getFleetData().getOfficersCopy()) {
                salary += Misc.getOfficerSalary(officer.person)
            }

            var compensation = salary * coveredMult
            compensation = MathUtils.clamp(compensation, 0f, maxOfficerCompensation)
            return compensation
        }

        @JvmStatic
        fun getTotalCompensation(faction: FactionAPI) = getSupplyCompensation(faction) + getCrewCompensation(faction) + getOfficerCompensation(faction)
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.remove(Tags.INTEL_ACCEPTED)
        tags.remove(Tags.INTEL_MISSIONS)
        tags.add(Tags.INTEL_AGREEMENTS)
        tags.add(faction.id)
        tags.add("Commission")
        return tags
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {

        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f

        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad

        val tc = getBulletColorForMode(mode)

        bullet(info)
        val isUpdate = getListInfoParam() != null

        if (getListInfoParam() === UPDATE_PARAM_ACCEPTED) {
            return
        }

        if (missionResult != null && missionResult.payment < 0) {
//			info.addPara("Annulled by " + faction.getDisplayNameWithArticle(), initPad, tc,
//					faction.getBaseUIColor(), faction.getDisplayNameWithArticleWithoutArticle());
        } else if (isUpdate && latestResult != null) {
            var credits = Misc.getDGSCredits(latestResult.payment.toFloat())
            var bonds = (latestResult as PrivateeringBountyResult).bonds.toInt()
            var label = info!!.addPara("$credits received, +$bonds bonds", initPad, tc, h, credits)
            label.setHighlight(credits, "$bonds")
            label.setHighlightColors(Misc.getHighlightColor(), faction.color)

            if (Math.round(latestResult.fraction * 100f) < 100f) {
                info!!.addPara("%s share based on damage dealt",
                    0f,
                    tc,
                    h,
                    "" + Math.round(latestResult.fraction * 100f) + "%")
            }
            CoreReputationPlugin.addAdjustmentMessage(latestResult.rep1.delta,  faction,null,null,  null,  info,  tc, isUpdate,
                0f)
        } else if (mode == ListInfoMode.IN_DESC) {
            var coveredPercent = PrivateeringUtils.getCommissionData().getCostsCoveredPercent()
            var bondsPer = CommissionData.bondsPerFrigate
            if (CommissionEventIntel.get()?.isStageActive(CommissionEventIntel.Stage.IMPORTANT) == true) bondsPer *= CommissionData.bondsImportantMult

            info!!.addPara("%s base bounty per frigate", initPad, tc, h, Misc.getDGSCredits(baseBounty))
            info!!.addPara("%s worth of bonds per frigate", 0f, tc, h, Misc.getDGSCredits(bondsPer))
            info.addPara("%s monthly stipend", 0f, tc, h, Misc.getDGSCredits(getMonthlyBaseIncome()))
            info.addPara("$coveredPercent%% fleet upkeep covered", 0f, tc, h, "$coveredPercent%")
        } else {
//			info.addPara("Faction: " + faction.getDisplayName(), initPad, tc,
//					faction.getBaseUIColor(), faction.getDisplayName());
//			initPad = 0f;
            var coveredPercent = PrivateeringUtils.getCommissionData().getCostsCoveredPercent()
            info!!.addPara("%s base reward per frigate", initPad, tc, h, Misc.getDGSCredits(baseBounty))
            info.addPara("%s monthly stipend", 0f, tc, h, Misc.getDGSCredits(getMonthlyBaseIncome()))
            info.addPara("$coveredPercent%% fleet upkeep covered", 0f, tc, h, "$coveredPercent%")
        }
        unindent(info)
    }


    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float, forMarketConditionTooltip: Boolean) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f

        info.addImage(faction.logo, width, 128f, opad)

        if (isEnding) {
            if (missionResult != null && missionResult.payment < 0) {
                info.addPara("Your commission was annulled by " + faction.displayNameWithArticle + " due to your standing falling too low.", opad,  faction.baseUIColor, faction.displayNameWithArticleWithoutArticle)

                CoreReputationPlugin.addRequiredStanding(faction, PrivateeringCommission.COMMISSION_REQ,    null,  null,   info,  tc, opad,true)
                CoreReputationPlugin.addCurrentStanding(faction, null, null, info, tc, opad)
            } else {
                info.addPara("You've resigned your commission with " + faction.displayNameWithArticle + ".", opad,  faction.baseUIColor,  faction.displayNameWithArticleWithoutArticle)
            }
        } else {
            info.addPara("You've accepted a %s commission.",   opad,  faction.baseUIColor,  Misc.ucFirst(faction.personNamePrefix))

            addBulletPoints(info, ListInfoMode.IN_DESC)

            info.addPara("The combat bounty payment depends on the number and size of ships destroyed.", opad)

            info.addSpacer(opad)

            var coveredPercent = PrivateeringUtils.getCommissionData().getCostsCoveredPercent()
            info.addPara("The commission covers $coveredPercent%% of your monthly fleet expenses. Check the Income tab to see more details.", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "$coveredPercent%", "Income")
            info.addSpacer(10f)

            var script = PrivateeringUtils.getSupervisorScript()
            var supervisor = script!!.supervisor
            var market = script.market

            info.addPara("Combat with hostile factions rewards you with requisition bonds. " +
                    "Requisition bonds can be spend by meeting with your commission supervisor ${supervisor!!.nameString} on ${market!!.name}.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "requisition bonds", "${supervisor.nameString}", "${market.name}")

            info.addSpacer(30f)
            var data = PrivateeringUtils.getCommissionData(faction)
            var reqBar = RequisitionBar(faction.color, data.bonds/CommissionData.maxBonds/*-0.1f*/, data.bonds/CommissionData.maxBonds, info, 180f, 30f)

            reqBar.position.setXAlignOffset(width/2-reqBar.width/2)
            info.addSpacer(-5f).position.setXAlignOffset(-(width/2-reqBar.width/2))

            info.addSpacer(10f)
        }



        if (latestResult != null) {
            //Color color = faction.getBaseUIColor();
            //Color dark = faction.getDarkUIColor();
            //info.addSectionHeading("Most Recent Reward", color, dark, Alignment.MID, opad);
            info.addPara("Most recent bounty:", opad)
            bullet(info)
            info.addPara("%s received", opad, tc, h, Misc.getDGSCredits(latestResult.payment.toFloat()))
            if (Math.round(latestResult.fraction * 100f) < 100f) {
                info.addPara("%s share based on damage dealt", 0f, tc,   h,  "" + Math.round(latestResult.fraction * 100f) + "%")
            }
            CoreReputationPlugin.addAdjustmentMessage(latestResult.rep1.delta,  faction, null, null,null,     info,    tc, false,   0f)
            unindent(info)
        }

        if (!isEnding && !isEnded) {
            val plMember = PerseanLeagueMembership.isLeagueMember()
            if (!plMember) {
                addAbandonButton(info, width, "Resign commission")
            } else {
                info.addPara("You can not resign your commission while polities under your " + "control are members of the League.",
                    opad)
            }
        }


    }

    override fun reportEconomyTick(iterIndex: Int) {
        val numIter = Global.getSettings().getFloat("economyIterPerMonth")
        val mult = 1f / numIter

        //CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        val report = SharedData.getData().currentReport

        val fleetNode = report.getNode(MonthlyReport.FLEET)
        fleetNode.name = "Fleet"
        fleetNode.custom = MonthlyReport.FLEET
        fleetNode.tooltipCreator = report.monthlyReportTooltip

        val commissionRootNode = report.getNode(fleetNode, "node_id_stipend_" + faction.id)
        commissionRootNode.income = 0f

        if (commissionRootNode.name == null) {
            commissionRootNode.name = faction.displayName + " Commission"
            commissionRootNode.icon = faction.crest
            commissionRootNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
                override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                    return false
                }

                override fun getTooltipWidth(tooltipParam: Any): Float {
                    return 450f
                }

                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                    tooltip.addPara("Your monthly stipend for holding a " + faction.displayName + " commission", 0f)
                }
            }
        }


        addBaseNode(report, commissionRootNode, mult)
        addSupplyNode(report, commissionRootNode, mult)
        addCrewNode(report, commissionRootNode, mult)
        addOfficerNode(report, commissionRootNode, mult)

    }

    fun addBaseNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {
        var basePay = getMonthlyBaseIncome()
        val basePayNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_base")
        basePayNode.income += basePay * mult

        basePayNode.name = "Base commission pay"
        basePayNode.icon = faction.crest
        basePayNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
            override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                return false
            }

            override fun getTooltipWidth(tooltipParam: Any): Float {
                return 450f
            }

            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                var label = tooltip.addPara("${faction.displayNameWithArticle.capitalize()} pays a base amount of ${Misc.getDGSCredits(basePay)} credits per month for your services", 0f)
                label.setHighlight(faction.displayName, "${Misc.getDGSCredits(basePay)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor())
            }
        }
    }

    fun addSupplyNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

        var spec = Global.getSettings().getCommoditySpec(Commodities.SUPPLIES)
        var data = PrivateeringUtils.getCommissionData()
        var percentCovered = data.costsCovered
        var percent = data.getCostsCoveredPercent()

        var player = Global.getSector().playerFleet
        var monthly = player.totalSupplyCostPerDay * 30

        var compensation = getSupplyCompensation(faction)

        var supplyNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_supplies")
        supplyNode.income += compensation * mult

        supplyNode.name = "Maintenance cost covered"
        supplyNode.icon = "graphics/icons/cargo/supplies.png"
        supplyNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
            override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                return false
            }

            override fun getTooltipWidth(tooltipParam: Any): Float {
                return 500f
            }

            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                var label = tooltip.addPara("${faction.displayNameWithArticle.capitalize()} compensates your monthly need of supplies in exchange for your services. " +
                        "The compensation is based on the base cost of supplies (${Misc.getDGSCredits(spec.basePrice)}) and covers around $percent% of it. \n\n" +
                        "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxSupplyCompensation)} credits for your supply costs.",
                    0f)

                label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(spec.basePrice)}", "$percent%", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxSupplyCompensation)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor(), Misc.getHighlightColor())
            }
        }

    }


    fun addCrewNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

        val crewSalary = Global.getSettings().getInt("crewSalary")
        var player = Global.getSector().playerFleet
        val crewCost = player.getCargo().getCrew() * crewSalary
        var compensation = getCrewCompensation(faction)

        var data = PrivateeringUtils.getCommissionData()
        var percentCovered = data.costsCovered
        var percent = data.getCostsCoveredPercent()

        var crewNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_crew")
        crewNode.income += compensation * mult

        crewNode.name = "Crew salary covered"
        crewNode.icon = "graphics/icons/reports/crew24.png"
        crewNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
            override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                return false
            }

            override fun getTooltipWidth(tooltipParam: Any): Float {
                return 500f
            }

            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                var label = tooltip.addPara("${faction.displayNameWithArticle.capitalize()} compensates the salary of your crew. " +
                        "Your current crew salary is ${Misc.getDGSCredits(crewCost.toFloat())} credits per month and around $percent% of it will be covered by the faction. \n\n" +
                        "" +
                        "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxCrewCompensation)} credits for crew salary.",
                    0f)

                label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(crewCost.toFloat())}", "$percent%", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxCrewCompensation)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor(), Misc.getHighlightColor())
            }
        }
    }



    fun addOfficerNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

        var data = PrivateeringUtils.getCommissionData()
        var percentCovered = data.costsCovered
        var percent = data.getCostsCoveredPercent()

        var player = Global.getSector().playerFleet

        var salary = 0f
        for (officer in player.getFleetData().getOfficersCopy()) {
            salary += Misc.getOfficerSalary(officer.person)
        }

        var compensation = getOfficerCompensation(faction)

        var officerNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_officer")
        officerNode.income += compensation * mult

        officerNode.name = "Officer salary covered"
        officerNode.icon = "graphics/icons/reports/officers24.png"
        officerNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
            override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                return false
            }

            override fun getTooltipWidth(tooltipParam: Any): Float {
                return 500f
            }

            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                var label = tooltip.addPara("${faction.displayNameWithArticle.capitalize()} compensates the salary of your officers. " +
                        "Your current officer salary is ${Misc.getDGSCredits(salary)} credits per month and $percent% of it will be covered by the faction. \n\n" +

                        "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxOfficerCompensation)} credits for officer salary.",
                    0f)

                label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(salary)}", "$percent%", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxOfficerCompensation)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor(), Misc.getHighlightColor())
            }
        }
    }

}