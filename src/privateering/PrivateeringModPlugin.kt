package privateering

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.DelayedActionScript
import privateering.intel.event.SpendBondsFactor
import privateering.scripts.CommDirectoryRecolorScript
import privateering.scripts.CommissionIntelReplacingScript
import privateering.scripts.CommissionIntelReplacingScript.Companion.replace
import privateering.scripts.SupervisorScript
import privateering.scripts.UtilScript

class PrivateeringModPlugin : BaseModPlugin() {

    override fun onApplicationLoad() {

        //AoTD does its own replacement of the Commission Intel, so not compatible.
        if (Global.getSettings().modManager.isModEnabled("aotd_qol")) {
            throw Exception("The mods \"AoTD - Question of Loyalty\" and \"Privateering - Commission Overhaul\" are not compatible with eachother " +
                    "due to overhauling the same system. Disable either one of them to launch the game.\n")
        }


    }

    override fun onNewGameAfterEconomyLoad() {

        CommissionIntelReplacingScript.replace()
    }

    private fun DelayedActionScript(fl: Float, function: () -> Any) {

    }

    override fun onGameLoad(newGame: Boolean) {
        if (!Global.getSector().hasScript(SupervisorScript::class.java)) {
            Global.getSector().addScript(SupervisorScript())
        }
        Global.getSector().addTransientScript(UtilScript())
        Global.getSector().addTransientScript(CommDirectoryRecolorScript())
        Global.getSector().addTransientScript(CommissionIntelReplacingScript())


        SpendBondsFactor(910, null)

    }

}