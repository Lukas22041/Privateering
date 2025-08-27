package privateering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import privateering.scripts.SupervisorScript
import java.util.ArrayList

object PrivateeringUtils {

    var COMMISSION_KEY = "\$privateering_commission_key"

    @JvmStatic
    fun getCommissionData() : CommissionData {
        return getCommissionData(Global.getSector().getFaction(Misc.getCommissionFactionId()))
    }

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

    fun addMarketplace(factionID: String?, primaryEntity: SectorEntityToken, connectedEntities: ArrayList<SectorEntityToken>?, name: String?,
                       size: Int, marketConditions: ArrayList<String>, submarkets: ArrayList<String>?, industries: ArrayList<String>, tarrif: Float,
                       freePort: Boolean, withJunkAndChatter: Boolean): MarketAPI {

        val globalEconomy = Global.getSector().economy
        val planetID = primaryEntity.id
        val marketID = planetID + "_market"
        val newMarket = Global.getFactory().createMarket(marketID, name, size)
        newMarket.factionId = factionID
        newMarket.primaryEntity = primaryEntity
        newMarket.tariff.modifyFlat("generator", tarrif)

        //Adds submarkets
        if (null != submarkets) {
            for (market in submarkets) {
                newMarket.addSubmarket(market)
            }
        }

        //Adds market conditions
        for (condition in marketConditions) {
            newMarket.addCondition(condition)
        }

        //Add market industries
        for (industry in industries) {
            newMarket.addIndustry(industry)
        }

        //Sets us to a free port, if we should
        newMarket.isFreePort = freePort

        //Adds our connected entities, if any
        if (null != connectedEntities) {
            for (entity in connectedEntities) {
                newMarket.connectedEntities.add(entity)
            }
        }
        globalEconomy.addMarket(newMarket, withJunkAndChatter)
        primaryEntity.market = newMarket
        primaryEntity.setFaction(factionID)
        if (null != connectedEntities) {
            for (entity in connectedEntities) {
                entity.market = newMarket
                entity.setFaction(factionID)
            }
        }

        //Finally, return the newly-generated market
        return newMarket
    }
}