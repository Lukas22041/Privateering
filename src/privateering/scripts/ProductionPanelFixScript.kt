package privateering.scripts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.ui.impl.StandardTooltipV2
import com.fs.state.AppDriver
import privateering.misc.ReflectionUtils

class ProductionPanelFixScript : EveryFrameScript {

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }


    override fun advance(amount: Float) {

        return

        var state = AppDriver.getInstance().currentState
        if (state !is CampaignState) return

        var core: UIPanelAPI? = null
        var dialog = ReflectionUtils.invoke("getEncounterDialog", state)
        if (dialog != null)
        {
            core = ReflectionUtils.invoke("getCoreUI", dialog) as UIPanelAPI?
        }

        if (core == null) {
            core = ReflectionUtils.invoke("getCore", state) as UIPanelAPI?
        }

        if (core == null) return

        //var screenPanel = ReflectionUtils.get("screenPanel", state) as UIPanelAPI ?: return
        var screenPanel = ReflectionUtils.get("screenPanel", state) as UIPanelAPI ?: return
    var test = ""
    }

}