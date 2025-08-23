package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import privateering.scripts.SupervisorScript

object PrivateeringUtils {

    var COMMISSION_KEY = "\$privateering_commission_key"

    @JvmStatic
    fun getCommissionData(faction: String) : CommissionData {
        return getCommissionData(Global.getSector().getFaction(faction))
    }

    @JvmStatic
    fun getCommissionData(faction: FactionAPI) : CommissionData {
        var data = faction.memoryWithoutUpdate.get(COMMISSION_KEY) as CommissionData?
        if (data == null) {
            data = CommissionData(faction)
            faction.memoryWithoutUpdate.set(COMMISSION_KEY, data)
        }
        return data
    }

    @JvmStatic
    fun getSupervisorScript() = Global.getSector().scripts.find { it::class.java == SupervisorScript::class.java } as SupervisorScript?

}