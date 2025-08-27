package privateering.campaign.conditions

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import privateering.scripts.addPara


class CommissionStationCondition : BaseMarketConditionPlugin() {

    var MAX_MARKET_SIZE = 4

    override fun apply(id: String?) {
        market.accessibilityMod.modifyFlat(id, 0.20f, condition.name)
        market.stability.modifyFlat(id, 1f, condition.name)

        val maxSize = Misc.getMaxMarketSize(market)
        val mod = MAX_MARKET_SIZE - maxSize
        market.stats.dynamic.getMod(Stats.MAX_MARKET_SIZE).modifyFlat(id, mod.toFloat(), name)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        tooltip?.addPara("This station has been rapidly assembled for your use. " +
                "With the protection of the faction that build it, it gains a permanent +1 stability and +20%% accessibility." +
                "\n\n" +
                "Due to size constrains, the station can not grow beyond a market size of 4.",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "+1", "+20%", "4")
    }

}