package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.characters.PersonAPI
import privateering.scripts.SupervisorScript

class CommissionData(var faction: FactionAPI) {

    companion object {
        var minBonds = 0f
        var maxBonds = 1500f
        var bondValue = 500f //1 bond = x credits
        var bondsPerFrigate = 600f
        var bondsImportantMult = 1.25f
    }

    var bonds = 0f

    var costsCovered = 0.5f
    fun getCostsCoveredPercent() = Math.round(costsCovered * 100)

    var lastMercTimestamp: Long? = null
    var mercs = ArrayList<PersonAPI>()

    init {

    }


}