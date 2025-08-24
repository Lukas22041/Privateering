package privateering.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.FactionProductionAPI
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.ShipQuality
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel.ProductionData
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.CountingMap
import com.fs.starfarer.api.util.Misc
import privateering.scripts.addPara
import java.awt.Color
import java.util.*
import kotlin.math.max

class CustomProductionIntel(var supervisor: PersonAPI, var data: ProductionData, var market: MarketAPI, var faction: FactionAPI) : BaseIntelPlugin() {

    var cost: Int = 0
    var prod_days: Float = 60.3f
    var timestamp = Global.getSector().clock.timestamp

    override fun getName(): String {
        return "Custom Production Order"
    }


    fun getDaysSinceOrder() = Global.getSector().clock.getElapsedDaysSince(timestamp)

    fun getDaysRemaining() = (prod_days-getDaysSinceOrder()).toInt()

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?,  isUpdate: Boolean,  tc: Color?,initPad: Float) {
        bullet(info)

        if (getDaysRemaining() >= 1) {
            info!!.addPara("${getDaysRemaining()} ${getDayOrDays(getDaysRemaining())} until delivery", 0f, tc, Misc.getHighlightColor(), "${getDaysRemaining()}")
        } else {
            info!!.addPara("Delivered to ${market.name}", 0f, tc, faction.color, "${market.name}")
        }
        unindent(info)
    }

    override fun getIcon(): String {
        return "graphics/icons/missions/custom_production.png"
    }

    override fun hasSmallDescription(): Boolean {
        return true
    }

    fun getDayOrDays(days: Int): String {
        var daysStr = "days"
        if (days == 1) {
            daysStr = "day"
        }
        return daysStr
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {

        info!!.addSpacer(10f)

        info.addImages(width, 128f, 0f, 0f, supervisor.portraitSprite, faction.crest)

        info.addSpacer(10f)

        info.addPara("Contract given by commission supervisor ${supervisor.nameString}.")

        info.addSpacer(10f)

        var days = getDaysRemaining()
        if (days >= 1) {
            var label = info.addPara("The order will be delivered to storage ${market.onOrAt} ${market.name} in $days ${getDayOrDays(days)}.")
            label.setHighlight(market.name, "$days")
            label.setHighlightColors(faction.color, Misc.getHighlightColor())
        } else {
            var label = info.addPara("The order was delivered to storage ${market.onOrAt} ${market.name}.")
            label.setHighlight(market.name)
            label.setHighlightColors(faction.color)
        }

        showCargoContents(info!!, width, height)

        info.addSpacer(10f)
    }

    override fun hasLargeDescription(): Boolean {
        return false
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_PRODUCTION)
        tags.add("Commission")
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken {
        return market.primaryEntity
    }

    override fun advance(amount: Float) {
        if (getDaysRemaining() < 1 && !isEnding) {
            deliver()
        }
    }



    fun deliver() {
        val plugin = Misc.getStorage(market) as StoragePlugin ?: return
        plugin.setPlayerPaidToUnlock(true)

        val cargo = plugin.cargo
        for (curr in data.data.values) {
            cargo.addAll(curr, true)
        }
        Global.getSector().campaignUI.addMessage(this, MessageClickAction.INTEL_TAB, this)
        endAfterDelay(14f)
    }

    public fun convertProdToCargo(prod: FactionProductionAPI) {
        var genRandom = Random()

        cost = prod.totalCurrentCost
        data = ProductionData()
        val cargo: CargoAPI = data.getCargo("Order manifest")

        var quality = ShipQuality.getShipQuality(market, market.getFactionId())
        quality = max(quality, 1.5f) // high enough (with some margin, at that) for no d-mods

        val ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true)
        ships.commander = Global.getSector().playerPerson
        ships.fleetData.shipNameRandom = genRandom
        val p = DefaultFleetInflaterParams()
        p.quality = quality
        p.mode = ShipPickMode.PRIORITY_THEN_ALL
        p.persistent = false
        p.seed = genRandom.nextLong()
        p.timestamp = null
        p.blockHullmodsWithItemReqs = true

        val inflater = Misc.getInflater(ships, p)
        ships.inflater = inflater

        for (item in prod.current) {
            val count = item.quantity
            if (item.type == ProductionItemType.SHIP) {
                for (i in 0..<count) {
                    ships.fleetData.addFleetMember(item.specId + "_Hull")
                }
            } else if (item.type == ProductionItemType.FIGHTER) {
                cargo.addFighters(item.specId, count)
            } else if (item.type == ProductionItemType.WEAPON) {
                cargo.addWeapons(item.specId, count)
            }
        }

        // so that it adds d-mods
        ships.inflateIfNeeded()
        for (member in ships.fleetData.membersListCopy) {
            // it should be due to the inflateIfNeeded() call, this is just a safety check
            if (member.variant.source == VariantSource.REFIT) {
                member.variant.clear()
            }
            cargo.mothballedShips.addFleetMember(member)
        }
    }

    fun showCargoContents(info: TooltipMakerAPI, width: Float, height: Float) {
        if (data == null) return

        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val small = 3f
        val opad = 10f

        val keys: List<String> = ArrayList<String>(data.data.keys)
        Collections.sort(keys, Comparator { o1, o2 -> o1.compareTo(o2) })

        for (key in keys) {
            val cargo: CargoAPI = data.data.get(key)!!
            if (cargo.isEmpty && ((cargo.mothballedShips == null || cargo.mothballedShips.membersListCopy.isEmpty()))) {
                continue
            }

            info.addSectionHeading(key, faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad)

            if (!cargo.stacksCopy.isEmpty()) {
                info.addPara("Ship weapons and fighters:", opad)
                info.showCargo(cargo, 20, true, opad)
            }

            if (!cargo.mothballedShips.membersListCopy.isEmpty()) {
                val counts = CountingMap<String>()
                for (member in cargo.mothballedShips.membersListCopy) {
                    counts.add(member.variant.hullSpec.hullName + " " + member.variant.designation)
                }

                info.addPara("Ship hulls:", opad)
                info.showShips(cargo.mothballedShips.membersListCopy,
                    20,
                    true,
                    false,
                    opad)
            }
        }
    }


}