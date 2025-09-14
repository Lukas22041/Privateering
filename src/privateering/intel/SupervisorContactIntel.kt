package privateering.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.scripts.addTooltip
import privateering.ui.element.RequisitionBar
import java.util.*

class SupervisorContactIntel(person: PersonAPI, market: MarketAPI) : ContactIntel(person, market) {

    init {
        isImportant = true
    }

    override fun relocateToMarket(other: MarketAPI?, withIntelUpdate: Boolean) {
        if (wasAddedToCommDirectory != null && wasAddedToCommDirectory && market != null && market.commDirectory != null) {
            market.commDirectory.removePerson(person)
            wasAddedToCommDirectory = null
        }
        market = other
        person.market = other
        marketWasDeciv = other!!.hasCondition(Conditions.DECIVILIZED)
        ensureIsInCommDirectory()
        ensureIsAddedToMarket()
        //person.importance = person.importance.prev()
        if (withIntelUpdate) {
            sendUpdateIfPlayerHasIntel(UPDATE_RELOCATED_CONTACT, false)
        }
    }

    override fun getName(): String {
        if (state == ContactState.LOST_CONTACT_DECIV) {
            return "Lost Contact: " + person.nameString
        } else if (state == ContactState.LOST_CONTACT) {
            return "Lost Contact: " + person.nameString
        } else if (state == ContactState.POTENTIAL) {
            return "Potential Contact: " + person.nameString
        } else if (state == ContactState.SUSPENDED) {
            return "Suspended Contact: " + person.nameString
        } else if (state == ContactState.PRIORITY) {
            return "Priority Contact: " + person.nameString
        }
        return "Supervisor: " + person.nameString
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        var tags = super.getIntelTags(map)
        tags.add(person.faction.id)
        tags.add("Commission")
        return tags
    }

    //Remove Suspend and Delete buttons
    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val pName = Misc.getPersonalityName(person)

        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f


        //info.addImage(person.getPortraitSprite(), width, 128, opad);
        var faction = person.faction
        info.addImages(width, 128f, opad, opad, person.portraitSprite, faction.crest)

        var relBarWidth = 128f * 2f + 10f
        val importanceBarWidth = relBarWidth

        val indent = 25f
        info.addSpacer(0f).position.setXAlignOffset(indent)


        //info.addRelationshipBar(person, relBarWidth, opad);
        relBarWidth = (relBarWidth - 10f) / 2f
        info.addRelationshipBar(person, relBarWidth, opad)
        val barHeight = info.prev.position.height
        info.addRelationshipBar(person.faction, relBarWidth, 0f)
        val prev = info.prev
        prev.position.setYAlignOffset(barHeight)
        prev.position.setXAlignOffset(relBarWidth + 10f)
        info.addSpacer(0f)
        info.prev.position.setXAlignOffset(-(relBarWidth + 10f))

        info.addImportanceIndicator(person.importance, importanceBarWidth, opad)
        addImportanceTooltip(info)
        //		faction = Global.getSector().getPlayerFaction();
//		ButtonAPI button = info.addAreaCheckbox("Priority contact", BUTTON_PRIORITY, faction.getBaseUIColor(),
//				faction.getDarkUIColor(), faction.getBrightUIColor(), relBarWidth, 25f, opad);
//		button.setChecked(state == ContactState.PRIORITY);
//		faction = person.getFaction();
        info.addSpacer(0f).position.setXAlignOffset(-indent)

        if (state == ContactState.NON_PRIORITY || state == ContactState.PRIORITY) {
            //info.addSpacer(0).getPosition().setXAlignOffset(indent);
            faction = Global.getSector().playerFaction
            val button = info.addAreaCheckbox("Priority contact",
                BUTTON_PRIORITY,
                faction.baseUIColor,
                faction.darkUIColor,
                faction.brightUIColor,
                width,
                25f,
                opad)
            button.isChecked = state == ContactState.PRIORITY
            addPriorityTooltip(info)
            faction = person.faction
            //info.addSpacer(0).getPosition().setXAlignOffset(-indent);
        }

        if (market != null && state == ContactState.LOST_CONTACT_DECIV) {
            info.addPara(person.nameString + " was " + person.postArticle + " " + person.post.lowercase(Locale.getDefault()) + " " + market.onOrAt + " " + market.name + ", a colony controlled by " + marketFaction.displayNameWithArticle + ".",
                opad,
                marketFaction.baseUIColor,
                Misc.ucFirst(marketFaction.displayNameWithArticleWithoutArticle))
            info.addPara("This colony has decivilized, and you've since lost contact with " + person.himOrHer + ".",
                opad)
        } else if (state == ContactState.LOST_CONTACT) {
            info.addPara("You've lost this contact.", opad)
        } else {
            if (market != null) {
                val label =
                    info.addPara(person.nameString + " is " + person.postArticle + " " + person.post.lowercase(Locale.getDefault()) + " and can be found " + market.onOrAt + " " + market.name + ", a size %s colony controlled by " + market.faction.displayNameWithArticle + ".",
                        opad,
                        market.faction.baseUIColor,
                        "" + market.size,
                        market.faction.displayNameWithArticleWithoutArticle)
                label.setHighlightColors(h, market.faction.baseUIColor)
                //				LabelAPI label = info.addPara(Misc.ucFirst(person.getPost().toLowerCase()) +
//						", found " + market.getOnOrAt() + " " + market.getName() +
//						", a size %s colony controlled by " + market.getFaction().getDisplayNameWithArticle() + ".",
//						opad, market.getFaction().getBaseUIColor(),
//						"" + (int)market.getSize(), Misc.ucFirst(market.getFaction().getPersonNamePrefix()));
//				label.setHighlightColors(h, market.getFaction().getBaseUIColor());
            }
        }

        if (state == ContactState.POTENTIAL) {
            info.addPara("If this contact is developed, " + person.heOrShe + " will periodically " + "have work for you. As the relationship improves, you may gain " + "access to better opportunities.",
                opad)
        } else if (state == ContactState.SUSPENDED) {
            info.addPara("Your contact with " + person.nameString + " is currently suspended.",
                Misc.getNegativeHighlightColor(),
                opad)
        }

        addBulletPoints(info, ListInfoMode.IN_DESC)

        if (state == ContactState.PRIORITY || state == ContactState.NON_PRIORITY || state == ContactState.SUSPENDED) {
            val ts = BaseMissionHub.getLastOpenedTimestamp(person)
            if (ts <= Long.MIN_VALUE) {
                //info.addPara("Never visited.", opad);
                info.addSpacer(30f)
            } else {
                info.addPara("Last visited: %s.", opad, h, Misc.getDetailedAgoString(ts))
            }
        }

        info.addSpacer(40f)

        var data = PrivateeringUtils.getCommissionData(faction)
        var reqBar = RequisitionBar(faction.color, data.bonds/ CommissionData.maxBonds/*-0.1f*/, data.bonds/ CommissionData.maxBonds, info, 180f, 30f)

        reqBar.position.setXAlignOffset(width/2-reqBar.width/2)
        info.addSpacer(-5f).position.setXAlignOffset(-(width/2-reqBar.width/2))

        var relocateButton = info.addButton("Relocate (-250 bonds)", "SUP_RELOCATE", faction.baseUIColor, faction.darkUIColor, Alignment.MID, CutStyle.BOTTOM, reqBar.width, 20f, 0f)
        relocateButton.position.belowLeft(reqBar.elementPanel, 5f)
        if (PrivateeringUtils.getCommissionData(faction).bonds < 250) {
            relocateButton.isEnabled = false
        }

        info.addTooltip(relocateButton, TooltipMakerAPI.TooltipLocation.BELOW, 250f) {
            it.addPara("Relocate your supervisor to another market. Can only select markets from the same faction. ", 0f, Misc.getTextColor(), Misc.getHighlightColor())
        }


    }



    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        super.buttonPressConfirmed(buttonId, ui)

        if (buttonId == "SUP_RELOCATE") {

            ui?.showDialog(Global.getSector().playerFleet, MoveSupervisorDialog(this, ui))

            /*ui?.updateIntelList()
            ui?.recreateIntelUI()*/
        }
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: ListInfoMode) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = getBulletColorForMode(mode)

        val pad = 3f
        val opad = 10f

        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad

        bullet(info)

        if (getListInfoParam() === UPDATE_RELOCATED_CONTACT) {
            info.addPara("Relocated to " + market.name, tc, initPad)
            initPad = 0f
            //info.addPara("Importance reduced to: %s", initPad, tc, h, person.importance.displayName)
            initPad = 0f
            unindent(info)
            return
        }
        if (state == ContactState.LOST_CONTACT_DECIV) {
            if (mode != ListInfoMode.IN_DESC) {
                info.addPara(market.name + " decivilized", tc, initPad)
                initPad = 0f
            }
            unindent(info)
            return
        }

        if (state == ContactState.LOST_CONTACT) {
            unindent(info)
            return
        }

        addFactionPara(info, tc, initPad)
        initPad = 0f

        addTypePara(info, tc, initPad)
        initPad = 0f

        if (mode != ListInfoMode.IN_DESC) {
            info.addPara("Importance: %s", initPad, tc, h, person.importance.displayName)
            initPad = 0f

            if (state == ContactState.PRIORITY || state == ContactState.NON_PRIORITY || state == ContactState.SUSPENDED) {
                val ts = BaseMissionHub.getLastOpenedTimestamp(person)
                if (ts <= Long.MIN_VALUE) {
                    //info.addPara("Never visited.", opad);
                } else {
                    info.addPara("Last visited: %s.", initPad, tc, h, Misc.getDetailedAgoString(ts))
                    initPad = 0f
                }
            }
        }


//		info.addPara("Rank: %s", initPad, tc, h, person.getRank());
//		initPad = 0f;

//		info.addPara("Post: %s", initPad, tc, h, person.getPost());
//		initPad = 0f;
        if (state == ContactState.POTENTIAL && POTENTIAL_EXPIRES) {
            if (mode != ListInfoMode.IN_DESC) {
                val days = DURATION - daysSincePlayerVisible
                info.addPara("%s " + getDaysString(days) + " left to develop", initPad, tc, h, getDays(days))
                initPad = 0f
            }
        }


        //info.addPara("Personality: %s", initPad, tc, h, pName);
        unindent(info)
    }

    class MoveSupervisorDialog(var intel: SupervisorContactIntel, var ui: IntelUIAPI) : InteractionDialogPlugin {
        override fun init(dialog: InteractionDialogAPI) {
            dialog?.promptText = ""

            var markets = Misc.getFactionMarkets(intel.marketFaction)
            markets = markets.filterNotNull()
            markets = markets.filter { !it.isHidden }
            markets = markets.filter { it != intel.market }
            var marketEntities = markets.map { it.primaryEntity }.filterNotNull()

            dialog.showCampaignEntityPicker("Select a new market", "Relocate to", "Confirm", intel.marketFaction, marketEntities, object : BaseCampaignEntityPickerListener() {
                override fun pickedEntity(entity: SectorEntityToken?) {

                    if (entity != null) {
                        var data = PrivateeringUtils.getCommissionData()
                        data.bonds = MathUtils.clamp(data.bonds-250f,0f, CommissionData.maxBonds)
                        Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

                        var market = entity.market

                        var script = PrivateeringUtils.getSupervisorScript()
                        script?.market?.commDirectory?.removePerson(intel.person)

                        script?.market = market
                        script?.supervisors!!.put(intel.marketFaction, Pair(intel.person, market))
                        ContactIntel.getContactIntel(intel.person).relocateToMarket(market, true)
                    }

                    ui.recreateIntelUI()
                    dialog.dismiss()
                }

                override fun cancelledEntityPicking() {
                    dialog.dismiss()
                }

                override fun canConfirmSelection(entity: SectorEntityToken?): Boolean {
                    return entity != null
                }

                override fun getMenuItemNameOverrideFor(entity: SectorEntityToken?): String {
                    return entity!!.market.name
                }
            })
        }

        override fun optionSelected(optionText: String?, optionData: Any?) {

        }

        override fun optionMousedOver(optionText: String?, optionData: Any?) {

        }

        override fun advance(amount: Float) {

        }

        override fun backFromEngagement(battleResult: EngagementResultAPI?) {

        }

        override fun getContext(): Any? {
            return null
        }

        override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
            return hashMapOf()
        }

    }

}