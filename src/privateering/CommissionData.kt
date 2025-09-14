package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.characters.PersonAPI
import org.lazywizard.lazylib.MathUtils
import privateering.intel.event.CommissionEventIntel
import privateering.misc.PrivSettings
import privateering.scripts.SupervisorScript

class CommissionData(var faction: FactionAPI) {

    companion object {
        var minBonds = 0f
        var maxBonds = PrivSettings.bondsMax!! //Default 1500
        var bondValue = 500f //1 bond = x credits
        //var bondsPerFrigate = 600f
        var bondsImportantMult = 1.25f

        var stationBuildCost = 1000f
        var stationSpecId = "station_lowtech1"
    }

    fun readResolve() : CommissionData {
        if (hasBuildStation == null) {
            hasBuildStation = false
        }

        return this
    }

    var bonds = 0f

    //private var costsCovered = 0.5f
    fun getCostsCovered() : Float {
        var covered = PrivSettings.baseMaintenanceCovered!!
        if (CommissionEventIntel.get()?.isStageActive(CommissionEventIntel.Stage.PROMOTION) == true) covered += 0.2f
        return MathUtils.clamp(covered, 0f, 1f)
    }
    fun getCostsCoveredPercent() = Math.round(getCostsCovered() * 100)

    var lastMercTimestamp: Long? = null
    var mercs = ArrayList<PersonAPI>()

    var hasBuildStation = false

    init {

    }


}