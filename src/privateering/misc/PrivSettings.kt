package privateering.misc

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

object PrivSettings : LunaSettingsListener {

    var MOD_ID = "privateering"

    var baseCommIncome = LunaSettings.getInt(MOD_ID, "priv_commissionBase")
    var baseCommIncomePerLevel = LunaSettings.getInt(MOD_ID, "priv_commissionPerLevel")
    var baseBountyPay = LunaSettings.getInt(MOD_ID, "priv_commissionBountyBase")
    var baseBondsPay = LunaSettings.getInt(MOD_ID, "priv_bondsBountyBase")
    var baseMaintenanceCovered = LunaSettings.getFloat(MOD_ID, "priv_maintenanceCovered")
    var favorabilityMult = LunaSettings.getFloat(MOD_ID, "priv_favorMult")

    init {
        updateDefaults()
    }

    override fun settingsChanged(modID: String) {
        if (modID == MOD_ID) {
            baseCommIncome = LunaSettings.getInt(MOD_ID, "priv_commissionBase")
            baseCommIncomePerLevel = LunaSettings.getInt(MOD_ID, "priv_commissionPerLevel")
            baseBountyPay = LunaSettings.getInt(MOD_ID, "priv_commissionBountyBase")
            baseBondsPay = LunaSettings.getInt(MOD_ID, "priv_bondsBountyBase")
            baseMaintenanceCovered = LunaSettings.getFloat(MOD_ID, "priv_maintenanceCovered")
            favorabilityMult = LunaSettings.getFloat(MOD_ID, "priv_favorMult")
            updateDefaults()
        }
    }

    //So that other mods and certain places will have things display correctly
    fun updateDefaults() {
        Global.getSettings().setFloat("factionCommissionStipendBase", baseCommIncome!!.toFloat())
        Global.getSettings().setFloat("factionCommissionStipendPerLevel", baseCommIncomePerLevel!!.toFloat())
        Global.getSettings().setFloat("factionCommissionBounty", baseBountyPay!!.toFloat())
    }

}