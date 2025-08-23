package privateering.scripts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.isMilitary

class SupervisorScript : EveryFrameScript {

    var supervisor: PersonAPI? = null
    var market: MarketAPI? = null

    var supervisors = HashMap<FactionAPI, Pair<PersonAPI, MarketAPI>>()

    var interval = IntervalUtil(0.2f, 0.2f)

    override fun isDone(): Boolean {
        return false
    }


    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {


        interval.advance(amount)
        if (interval.intervalElapsed()) {
            var faction = Misc.getCommissionFaction()

            if ((supervisor == null) || supervisor!!.faction != faction) {
                replaceSupervisor(faction)
            }



        }

    }

    fun replaceSupervisor(faction: FactionAPI?) {
        if (supervisor != null) {
            if (market != null) {
                var comm = market!!.commDirectory.getEntryForPerson(supervisor)
                if (comm != null)
                {
                    ContactIntel.removeContact(supervisor, null)
                    comm.isHidden = true
                    supervisor = null
                    market = null
                }
            }
        }

        if (faction == null) return

        var existing = supervisors.get(faction)
        if (existing != null) {
            supervisor = existing.first
            market = existing.second

            market!!.commDirectory.getEntryForPerson(supervisor)?.isHidden = false;
        }
        else {
            var newSupervisor = faction.createRandomPerson()
            newSupervisor!!.addTag(Tags.CONTACT_MILITARY)
            newSupervisor.postId = "privateering_commission_supervisor"
            newSupervisor.rankId = "spaceLieutenant"
            newSupervisor.addTag("privateering_supervisor")
            newSupervisor.memoryWithoutUpdate.set("\$privateering_supervisor", true)
            newSupervisor.importance = PersonImportance.MEDIUM

            var markets = Misc.getFactionMarkets(faction)
            markets = markets.filterNotNull()
            markets = markets.sortedWith(compareByDescending<MarketAPI?>({ it!!.isMilitary() } ).thenByDescending { it!!.size })

            var newMarket = markets.firstOrNull()
            if (newMarket != null) {
                supervisor = newSupervisor
                market = newMarket

                market!!.commDirectory.addPerson(supervisor)

                supervisors.set(faction, Pair(supervisor!!, market!!))

                //ContactIntel.addPotentialContact(1f, supervisor, market, null)
                /*var contact = ContactIntel.getContactIntel(supervisor)
                if (contact != null) {

                }*/
            }
        }

        if (supervisor != null) {
            val intel = ContactIntel(supervisor, market)
            intel.state = ContactIntel.ContactState.NON_PRIORITY
            Global.getSector().intelManager.addIntel(intel, false, null)

            intel.develop(null)
        }


    }

}