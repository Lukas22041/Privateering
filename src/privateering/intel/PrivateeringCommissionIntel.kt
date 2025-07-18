package privateering.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MonthlyReport
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils

class PrivateeringCommissionIntel(faction: FactionAPI) : FactionCommissionIntel(faction) {

    override fun reportEconomyTick(iterIndex: Int) {
        val numIter = Global.getSettings().getFloat("economyIterPerMonth")
        val mult = 1f / numIter

        //CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        val report = SharedData.getData().currentReport

        val fleetNode = report.getNode(MonthlyReport.FLEET)
        fleetNode.name = "Fleet"
        fleetNode.custom = MonthlyReport.FLEET
        fleetNode.tooltipCreator = report.monthlyReportTooltip

        val stipend = computeStipend()
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

    }

    fun addBaseNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {
        var basePay = 10000f
        val commissionRootNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_base")
        commissionRootNode.income += basePay * mult

        if (commissionRootNode.name == null) {
            commissionRootNode.name = "Base commission pay"
            commissionRootNode.icon = faction.crest
            commissionRootNode.tooltipCreator = object : TooltipMakerAPI.TooltipCreator {
                override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                    return false
                }

                override fun getTooltipWidth(tooltipParam: Any): Float {
                    return 450f
                }

                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                    var label = tooltip.addPara("The ${faction.displayName} pays a base amount of ${Misc.getDGSCredits(basePay)} credits per month for your sarvices", 0f)
                    label.setHighlight(faction.displayName, "${Misc.getDGSCredits(basePay)}")
                    label.setHighlightColors(faction.baseUIColor, Misc.getHighlightColor())
                }
            }
        }
    }

    fun addSupplyNode(report: MonthlyReport, commissionNode: FDNode, mult: Float) {

       /* var suppliesCost = 0f
        var prices = ArrayList<Float>()
        for (market in Global.getSector().economy.marketsCopy) {
            if (market.faction != faction) continue
            var tariff = market.tariff
            var commodity = market.getCommodityData(Commodities.SUPPLIES)
            println(market.name + "_" +commodity.commodityMarketData.marketValue)
            var submarket = market.getSubmarket(Submarkets.SUBMARKET_OPEN)
        }*/

        var spec = Global.getSettings().getCommoditySpec(Commodities.SUPPLIES)

        var relationMult = 0.3f
        if (faction.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) relationMult = 0.4f
        if (faction.relToPlayer.isAtWorst(RepLevel.FAVORABLE)) relationMult = 0.5f
        if (faction.relToPlayer.isAtWorst(RepLevel.WELCOMING)) relationMult = 0.6f
        if (faction.relToPlayer.isAtWorst(RepLevel.FRIENDLY)) relationMult = 0.7f

        var maxCompensation = 30000f

        var player = Global.getSector().playerFleet
        var monthly = player.totalSupplyCostPerDay * 30

        var compensation = (monthly * spec.basePrice) * relationMult
        compensation = MathUtils.clamp(compensation, 0f, maxCompensation)

        var supplyNode = report.getNode(commissionNode, "node_id_stipend_${faction.id}_supplies")
        supplyNode.income += compensation * mult

        if (supplyNode.name == null) {
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
                            "The compensation is based on the base cost of supplies (${Misc.getDGSCredits(spec.basePrice)}) and how many supplies per month your fleet require. \n\n" +
                            "" +
                            "The percentage compensated depends on your relation to the faction: \n" +
                            "   - 50% at neutral relation\n" +
                            "   - 60% at favorable relation\n" +
                            "   - 70% at welcoming relation\n" +
                            "   - 80% at friendly relation or higher\n" +
                            "\n" +
                            "Added up, you currently receive a compensation of around ${Misc.getDGSCredits(compensation)} credits per month. The faction is only willing to provide at most ${Misc.getDGSCredits(maxCompensation)} credits for your supply costs.",
                        0f)

                    label.setHighlight("${faction.displayNameWithArticle.capitalize()}", "${Misc.getDGSCredits(spec.basePrice)}", "50%", "neutral", "60%", "favorable", "70%","welcoming", "80%", "friendly", "${Misc.getDGSCredits(compensation)}", "${Misc.getDGSCredits(maxCompensation)}")
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