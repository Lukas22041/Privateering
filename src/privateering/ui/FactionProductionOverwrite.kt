package privateering.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType
import com.fs.starfarer.loading.specs.FactionProduction

class FactionProductionOverwrite(faction: FactionAPI?) : FactionProduction(faction) {


    override fun getTotalCurrentCost(): Int {
        var var1 = 0

        for (var2 in this.current) {
            var var4 = Math.round((var2.baseCost * var2.quantity).toFloat() * this.costMult)
            if (this.delegate != null) {
                if (var2.type == ProductionItemType.WEAPON) {
                    val var5 = Global.getSettings().getWeaponSpec(var2.specId)
                    if (var5 != null) {
                        val var6 = delegate.getCostOverride(var5)
                        if (var6 >= 0) {
                            var4 = var6 * var2.quantity
                        }
                    }
                }
                if (var2.type == ProductionItemType.SHIP) {
                    val var5 = Global.getSettings().getHullSpec(var2.specId)
                    if (var5 != null) {
                        val var6 = delegate.getCostOverride(var5)
                        if (var6 >= 0) {
                            var4 = var6 * var2.quantity
                        }
                    }
                }
                if (var2.type == ProductionItemType.FIGHTER) {
                    val var5 = Global.getSettings().getFighterWingSpec(var2.specId)
                    if (var5 != null) {
                        val var6 = delegate.getCostOverride(var5)
                        if (var6 >= 0) {
                            var4 = var6 * var2.quantity
                        }
                    }
                }

            }

            var1 += var4
            if (var1 > 2000000000) {
                return var1
            }
        }

        return var1
    }

    override fun getUnitCost(var1: ProductionItemType, var2: String?): Int {
        val var3 = ItemInProduction(var1, var2, 1)
        if (this.delegate != null) {

            if (var1 == ProductionItemType.WEAPON) {
                val var4 = Global.getSettings().getWeaponSpec(var2)
                if (var4 != null) {
                    val var5 = delegate.getCostOverride(var4)
                    if (var5 >= 0) {
                        return var5
                    }
                }
            }
            if (var1 == ProductionItemType.SHIP) {
                val var4 = Global.getSettings().getHullSpec(var2)
                if (var4 != null) {
                    val var5 = delegate.getCostOverride(var4)
                    if (var5 >= 0) {
                        return var5
                    }
                }
            }
            if (var1 == ProductionItemType.FIGHTER) {
                val var4 = Global.getSettings().getFighterWingSpec(var2)
                if (var4 != null) {
                    val var5 = delegate.getCostOverride(var4)
                    if (var5 >= 0) {
                        return var5
                    }
                }
            }

        }

        return Math.round(var3.baseCost.toFloat() * this.costMult)
    }
}