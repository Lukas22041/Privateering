package privateering.intel.event

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import privateering.intel.event.CommissionEventIntel

class SpendBondsFactor(var points: Int, dialog: InteractionDialogAPI?) : BaseOneTimeFactor(points) {

    init {
        CommissionEventIntel.addFactorCreateIfNecessary(this, dialog)
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Spend requisition bonds"
    }

    override fun getMainRowTooltip(): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara("You used your requisition bonds to move up within the faction.",
                    0f,
                    Misc.getHighlightColor(),
                    "")
            }
        }
    }

}