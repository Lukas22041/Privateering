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
    }

    var bonds = 800f

    var lastMercTimestamp: Long? = null
    var mercs = ArrayList<PersonAPI>()

    init {

    }


}