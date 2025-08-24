package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import privateering.scripts.SupervisorScript

class CommissionData(var faction: FactionAPI) {

    companion object {
        var minBonds = 0f
        var maxBonds = 1000f
    }

    var bonds = 400f

    init {

    }


}