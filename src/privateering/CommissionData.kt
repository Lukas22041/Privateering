package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import privateering.scripts.SupervisorScript

class CommissionData(var faction: FactionAPI) {

    companion object {
        var minBonds = 0f
        var maxBonds = 1500f
        var bondValue = 500f //1 bond = x credits
    }

    var bonds = 600f

    init {

    }


}