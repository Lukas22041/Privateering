package privateering.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.ui.element.RequisitionBar
import java.util.*

class SupervisorContactIntel(person: PersonAPI, market: MarketAPI) : ContactIntel(person, market) {

    init {
        isImportant = true
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

        info.addSpacer(70f)

        var data = PrivateeringUtils.getCommissionData(faction)
        var reqBar = RequisitionBar(faction.color, data.bonds/ CommissionData.maxBonds/*-0.1f*/, data.bonds/ CommissionData.maxBonds, info, 180f, 30f)

        reqBar.position.setXAlignOffset(width/2-reqBar.width/2)
        info.addSpacer(-5f).position.setXAlignOffset(-(width/2-reqBar.width/2))

        if (state == ContactState.PRIORITY || state == ContactState.NON_PRIORITY || state == ContactState.SUSPENDED) {
            val ts = BaseMissionHub.getLastOpenedTimestamp(person)
            if (ts <= Long.MIN_VALUE) {
                //info.addPara("Never visited.", opad);
            } else {
                info.addPara("Last visited: %s.", opad, h, Misc.getDetailedAgoString(ts))
            }


//			Color color = faction.getBaseUIColor();
//			Color dark = faction.getDarkUIColor();
//			info.addSectionHeading("Personality traits", color, dark, Alignment.MID, opad);
//			info.addPara("Suspicous          Ambitious", opad, h, "Suspicous", "Ambitious");
//			info.addPara("Ambitious: will offer missions that further their advancement more frequently. Refusing " +
//					"these missions will damage the relationship.", opad, h, "Ambitious:");
//			info.addPara("Suspicous: reduced reputation gains.", opad, h, "Suspicous:");
        }

/*
        val color = Misc.getStoryOptionColor()
        val dark = Misc.getStoryDarkColor()

        val noDeleteTooltip: TooltipCreator = object : TooltipCreator {
            override fun isTooltipExpandable(tooltipParam: Any): Boolean {
                return false
            }

            override fun getTooltipWidth(tooltipParam: Any): Float {
                return TOOLTIP_WIDTH
            }

            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara("Can not delete or suspend contact at this time.", 0f)
            }
        }*/

        /*if (state == ContactState.POTENTIAL || state == ContactState.SUSPENDED) {
            if (state == ContactState.POTENTIAL && POTENTIAL_EXPIRES) {
                val days = DURATION - daysSincePlayerVisible
                info.addPara("The opportunity to develop this contact will be available for %s more " + getDaysString(
                    days) + ".", opad, tc, h, getDays(days))
            }


            val max = getMaxContacts()
            val curr = getCurrentContacts()

            info.addPara("Active contacts: %s %s %s", opad, h, "" + curr, "/", "" + max)

            var develop: ButtonAPI? = null
            var developText = "Develop contact"
            if (state == ContactState.SUSPENDED) developText = "Resume contact"
            if (curr >= max) {
//				info.addPara("Developing contacts above the maximum will " +
//							 "require a story point per additional contact.", opad,
//							 Misc.getStoryOptionColor(), "story point");
                develop = addGenericButton(info, width, color, dark, developText, BUTTON_DEVELOP)
                addDevelopTooltip(info)
            } else {
                develop = addGenericButton(info, width, developText, BUTTON_DEVELOP)
            }
            develop.setShortcut(Keyboard.KEY_T, true)
        } else if (state == ContactState.NON_PRIORITY || state == ContactState.PRIORITY) {
            val suspend = addGenericButton(info, width, color, dark, "Suspend contact", BUTTON_SUSPEND)
            suspend.setShortcut(Keyboard.KEY_U, true)
            if (Global.getSector().intel.isInShowMap) {
                suspend.isEnabled = false
                info.addTooltipToPrevious(noDeleteTooltip, TooltipLocation.LEFT)
            }
        }*/

        /*info.addSpacer(-10f)
        val delete = addGenericButton(info, width, "Delete contact", BUTTON_DELETE)
        if (Global.getSector().intel.isInShowMap) {
            delete.isEnabled = false
            info.addTooltipToPrevious(noDeleteTooltip, TooltipLocation.LEFT)
        }
        delete.setShortcut(Keyboard.KEY_G, true)*/


    }

}