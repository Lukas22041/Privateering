package privateering.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MonthlyReport
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.Commission
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils

class PrivateeringCommissionIntel(faction: FactionAPI) : FactionCommissionIntel(faction) {

    fun getMonthlyBaseIncome() = 15000f

    fun getRelationMult() : Float{
        var relationMult = 0.3f
        if (faction.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) relationMult = 0.4f
        if (faction.relToPlayer.isAtWorst(RepLevel.FAVORABLE)) relationMult = 0.5f
        if (faction.relToPlayer.isAtWorst(RepLevel.WELCOMING)) relationMult = 0.6f
        if (faction.relToPlayer.isAtWorst(RepLevel.FRIENDLY)) relationMult = 0.7f
        if (faction.relToPlayer.isAtWorst(RepLevel.COOPERATIVE)) relationMult = 0.7f
        return relationMult
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
            info!!.addPara("%s received", initPad, tc, h, Misc.getDGSCredits(latestResult.payment.toFloat()))
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
            var relationMult = (getRelationMult() * 100).toInt()
            info!!.addPara("%s base bounty per hostile frigate", initPad, tc, h, Misc.getDGSCredits(baseBounty))
            info.addPara("%s monthly stipend", 0f, tc, h, Misc.getDGSCredits(getMonthlyBaseIncome()))
            info.addPara("$relationMult%% fleet upkeep covered", 0f, tc, h, "$relationMult%")
        } else {
//			info.addPara("Faction: " + faction.getDisplayName(), initPad, tc,
//					faction.getBaseUIColor(), faction.getDisplayName());
//			initPad = 0f;
            var relationMult = (getRelationMult() * 100).toInt()
            info!!.addPara("%s base reward per frigate", initPad, tc, h, Misc.getDGSCredits(baseBounty))
            info.addPara("%s monthly stipend", 0f, tc, h, Misc.getDGSCredits(getMonthlyBaseIncome()))
            info.addPara("$relationMult%% fleet upkeep covered", 0f, tc, h, "$relationMult%")
        }
        unindent(info)
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

                CoreReputationPlugin.addRequiredStanding(faction, Commission.COMMISSION_REQ,    null,  null,   info,  tc, opad,true)
                CoreReputationPlugin.addCurrentStanding(faction, null, null, info, tc, opad)
            } else {
                info.addPara("You've resigned your commission with " + faction.displayNameWithArticle + ".", opad,  faction.baseUIColor,  faction.displayNameWithArticleWithoutArticle)
            }
        } else {
            info.addPara("You've accepted a %s commission.",   opad,  faction.baseUIColor,  Misc.ucFirst(faction.personNamePrefix))

            addBulletPoints(info, ListInfoMode.IN_DESC)

            info.addPara("The combat bounty payment depends on the number and size of ships destroyed.", opad)

            info.addSpacer(opad)

            var relationMult = (getRelationMult() * 100).toInt()
            info.addPara("The commission covers $relationMult%% of your monthly fleet expanses. Check the Income tab to see more details.", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "$relationMult%", "Income")
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
        var basePay = 15000f
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

        var relationMult = getRelationMult()

        var maxCompensation = 20000f

        var player = Global.getSector().playerFleet
        var monthly = player.totalSupplyCostPerDay * 30

        var compensation = (monthly * spec.basePrice) * relationMult
        compensation = MathUtils.clamp(compensation, 0f, maxCompensation)

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
                        "The compensation is based on the base cost of supplies (${Misc.getDGSCredits(spec.basePrice)}) and how many supplies per month your fleet requires. \n\n" +
                        "" +
                        "The percentage compensated depends on your relation to the faction: \n" +
                        "   - 40% at neutral relation\n" +
                        "   - 50% at favorable relation\n" +
                        "   - 60% at welcoming relation\n" +
                        "   - 70% at friendly relation or higher\n" +
                        "\n" +
                        "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxCompensation)} credits for your supply costs.",
                    0f)

                label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(spec.basePrice)}", "40%", "neutral", "50%", "favorable", "60%","welcoming", "70%", "friendly", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxCompensation)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.NEUTRAL),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.FAVORABLE),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.WELCOMING),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.FRIENDLY),
                    Misc.getHighlightColor(), Misc.getHighlightColor())
            }
        }

    }

    fun addCrewNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

        val crewSalary = Global.getSettings().getInt("crewSalary")
        var relationMult = getRelationMult()

        var maxCompensation = 5000f

        var player = Global.getSector().playerFleet
        val crewCost = player.getCargo().getCrew() * crewSalary

        var compensation = crewCost * relationMult
        compensation = MathUtils.clamp(compensation, 0f, maxCompensation)

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
                        "Your current crew salary is ${Misc.getDGSCredits(crewCost.toFloat())} credits per month. \n\n" +
                        "" +
                        "The percentage compensated depends on your relation to the faction: \n" +
                        "   - 40% at neutral relation\n" +
                        "   - 50% at favorable relation\n" +
                        "   - 60% at welcoming relation\n" +
                        "   - 70% at friendly relation or higher\n" +
                        "\n" +
                        "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxCompensation)} credits for crew salary.",
                    0f)

                label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(crewCost.toFloat())}", "40%", "neutral", "50%", "favorable", "60%","welcoming", "70%", "friendly", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxCompensation)}")
                label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.NEUTRAL),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.FAVORABLE),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.WELCOMING),
                    Misc.getHighlightColor(), faction.getRelColor(RepLevel.FRIENDLY),
                    Misc.getHighlightColor(), Misc.getHighlightColor())
            }
        }
    }

    fun addOfficerNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

        var relationMult = getRelationMult()

        var maxCompensation = 10000f

        var player = Global.getSector().playerFleet

        var salary = 0f
        for (officer in player.getFleetData().getOfficersCopy()) {
            salary += Misc.getOfficerSalary(officer.person)
        }

        var compensation = salary * relationMult
        compensation = MathUtils.clamp(compensation, 0f, maxCompensation)

        var officerNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_officer")
        officerNode.income += compensation * mult

        if (officerNode.name == null) {
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
                            "Your current officer salary is ${Misc.getDGSCredits(salary)} credits per month. \n\n" +
                            "" +
                            "The percentage compensated depends on your relation to the faction: \n" +
                            "   - 40% at neutral relation\n" +
                            "   - 50% at favorable relation\n" +
                            "   - 60% at welcoming relation\n" +
                            "   - 70% at friendly relation or higher\n" +
                            "\n" +
                            "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxCompensation)} credits for officer salary.",
                        0f)

                    label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(salary)}", "40%", "neutral", "50%", "favorable", "60%","welcoming", "70%", "friendly", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxCompensation)}")
                    label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor(),
                        Misc.getHighlightColor(), faction.getRelColor(RepLevel.NEUTRAL),
                        Misc.getHighlightColor(), faction.getRelColor(RepLevel.FAVORABLE),
                        Misc.getHighlightColor(), faction.getRelColor(RepLevel.WELCOMING),
                        Misc.getHighlightColor(), faction.getRelColor(RepLevel.FRIENDLY),
                        Misc.getHighlightColor(), Misc.getHighlightColor())
                }
            }
        }

    }

}