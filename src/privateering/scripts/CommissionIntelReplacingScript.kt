package privateering.scripts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel
import privateering.intel.PrivateeringCommissionIntel

class CommissionIntelReplacingScript : EveryFrameScript {



    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }


    override fun advance(amount: Float) {
        var intel = Global.getSector().characterData.memoryWithoutUpdate.get(MemFlags.FCM_EVENT) as FactionCommissionIntel?

        //No Commission Active
        if (intel == null) return

        //Intel has already been replaced
        if (intel is PrivateeringCommissionIntel) return

        var faction = intel.faction
        var replacement = PrivateeringCommissionIntel(faction)

        //Remove original intel
        Global.getSector().removeScript(intel)
        Global.getSector().intelManager.removeIntel(intel)
        Global.getSector().listenerManager.removeListener(intel)

        //Make sure to set new intel as the comm intel, though this should be done by replacement.missionAccepted() anyways
        Global.getSector().characterData.memoryWithoutUpdate.set(MemFlags.FCM_EVENT, replacement)

        replacement.missionAccepted()
    }

}