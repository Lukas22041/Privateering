package privateering.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomProductionPickerDelegateImpl
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionProductionAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.intel.event.CommissionEventIntel

class RequisitionProductionPicker(var text: TextPanelAPI, var faction: FactionAPI, var market: MarketAPI) : BaseCustomProductionPickerDelegateImpl() {

    override fun getMaximumValue(): Float {
        return PrivateeringUtils.getCommissionData().bonds
    }

    override fun isUseCreditSign(): Boolean {
        return true
    }

    override fun withQuantityLimits(): Boolean {
        return false
    }

    override fun getMaximumOrderValueLabelOverride(): String {
        return "Requisition bonds available"
    }

    override fun getCurrentOrderValueLabelOverride(): String {
        return "Requisition bonds required"
    }

    override fun getItemGoesOverMaxValueStringOverride(): String {
        return "Not enough requisition bonds"
    }

    override fun notifyProductionSelected(production: FactionProductionAPI?) {
        if (production != null ) {
            var data = ProductionReportIntel.ProductionData()
            var intel = CustomProductionIntel(PrivateeringUtils.getSupervisorScript()!!.supervisor!!, data, market, faction)
            intel.convertProdToCargo(production!!)

            var cost = production.totalCurrentCost
            text.setFontSmallInsignia()
            var label = text.addPara("Spent ${production.totalCurrentCost} requisition bonds",  Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), "${production.totalCurrentCost}")
            text.setFontInsignia()

            var commData = PrivateeringUtils.getCommissionData()
            commData.bonds = MathUtils.clamp(commData.bonds-cost, 0f, CommissionData.maxBonds)


            Global.getSoundPlayer().playUISound("ui_cargo_machinery_drop", 1.0f, 1.0f)
            Global.getSector().intelManager.addIntel(intel)
            Global.getSector().intelManager.addIntelToTextPanel(intel, text)
            Global.getSector().addScript(intel)
        }

    }

    override fun getCostOverride(item: Any?): Int {
        if (item is WeaponSpecAPI) {
            return Math.max((item.baseValue * costMult / CommissionData.bondValue).toInt(), 1)
        } else if (item is ShipHullSpecAPI) {
            return Math.max((item.baseValue * costMult / CommissionData.bondValue).toInt(), 1)
        } else if (item is FighterWingSpecAPI) {
            return Math.max((item.baseValue * costMult / CommissionData.bondValue).toInt(), 1)
        }
       return super.getCostOverride(item)
    }

    override fun getCostMult(): Float {
        var value = 1f
        if (faction == Misc.getCommissionFaction()) value = 0.75f
        return value
    }

    override fun getAvailableFighters(): MutableSet<String> {
        return faction.knownFighters
    }


    override fun getAvailableShipHulls(): MutableSet<String> {
        var comm = Misc.getCommissionFaction()
        var event = CommissionEventIntel.get()
        var allowCapitals = event?.isStageActive(CommissionEventIntel.Stage.ARSENAL_AUTHORIZATION) ?: false

        var ships = faction.knownShips.toMutableSet()
        for (ship in ArrayList(ships)) {
            var spec = Global.getSettings().getHullSpec(ship)
            if (spec.hullSize == ShipAPI.HullSize.CAPITAL_SHIP && !allowCapitals) {
                ships.remove(ship)
            }
        }

        return ships
    }

    override fun getAvailableWeapons(): MutableSet<String> {
        var comm = Misc.getCommissionFaction()
        var event = CommissionEventIntel.get()
        var allowCapitals = event?.isStageActive(CommissionEventIntel.Stage.ARSENAL_AUTHORIZATION) ?: false

        var weapons = faction.knownWeapons.toMutableSet()
        for (weapon in ArrayList(weapons)) {
            var spec = Global.getSettings().getWeaponSpec(weapon)
            if (spec.size == WeaponAPI.WeaponSize.LARGE && !allowCapitals) {
                weapons.remove(weapon)
            }
        }

        return weapons
    }

}