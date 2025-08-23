package privateering.scripts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.Misc
import privateering.intel.PrivateeringCommissionIntel

class UtilScript : EveryFrameScript {

    override fun isDone(): Boolean {
       return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        var comm = Misc.getCommissionIntel()
        if (comm is PrivateeringCommissionIntel) {
            comm.updateBaseBounty()
        }
    }

}