package privateering.intel.event

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class AcquiredFavorFactor(var points: Int, dialog: InteractionDialogAPI?) : BaseOneTimeFactor(points) {

    init {
        CommissionEventIntel.addFactorCreateIfNecessary(this, dialog)
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Acquired favor."
    }

    override fun getMainRowTooltip(): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara("You acquired more favor within the faction.",
                    0f,
                    Misc.getHighlightColor(),
                    "")
            }
        }
    }

}