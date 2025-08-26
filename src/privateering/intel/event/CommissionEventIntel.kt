package privateering.intel.event

import com.fs.graphics.Sprite
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.setAlpha
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.misc.ReflectionUtils
import privateering.scripts.getChildrenCopy
import privateering.scripts.loadTextureCached
import java.awt.Color


class CommissionEventIntel(var faction: FactionAPI) : BaseEventIntel() {

    enum class Stage(var progress: Int) {
        START(0), WARFLEET(250), ARSENAL_AUTHORIZATION(450), PROMOTION(650), IMPORTANT(900)
    }

    companion object {
        @JvmStatic
        var KEY = "\$privateering_commission_event_ref"

        @JvmStatic
        fun addFactorCreateIfNecessary(factor: EventFactor?, dialog: InteractionDialogAPI?) {
            var comm = Misc.getCommissionFaction()

            if (comm != null) {
                if (get() == null) {
                    CommissionEventIntel(comm)
                }
                if (get() != null) {
                    get()!!.addFactor(factor, dialog)
                }
            }


        }

        @JvmStatic
        fun get(): CommissionEventIntel? {
            return Global.getSector().memoryWithoutUpdate[KEY] as CommissionEventIntel?
        }
    }

    init {
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);

        setMaxProgress(Stage.IMPORTANT.progress);

        addStage(Stage.START, Stage.START.progress, StageIconSize.SMALL);
        addStage(Stage.WARFLEET, Stage.WARFLEET.progress, StageIconSize.MEDIUM);
        addStage(Stage.ARSENAL_AUTHORIZATION, Stage.ARSENAL_AUTHORIZATION.progress, StageIconSize.MEDIUM);
        addStage(Stage.PROMOTION, Stage.PROMOTION.progress, StageIconSize.MEDIUM);
        addStage(Stage.IMPORTANT, Stage.IMPORTANT.progress, StageIconSize.MEDIUM);

        getDataFor(Stage.WARFLEET).keepIconBrightWhenLaterStageReached = true;
        getDataFor(Stage.ARSENAL_AUTHORIZATION).keepIconBrightWhenLaterStageReached = true;
        getDataFor(Stage.PROMOTION).keepIconBrightWhenLaterStageReached = true;
        getDataFor(Stage.IMPORTANT).keepIconBrightWhenLaterStageReached = true;

        isImportant = true

        // now that the event is fully constructed, add it and send notification
        Global.getSector().getIntelManager().addIntel(this, true);
    }


    override fun addStageDescriptionWithImage(main: TooltipMakerAPI, stageId: Any) {
        val data = createDisplayData(stageId)
        val icon = if (data != null) {
            data.icon
        } else {
            icon
        }
        val imageSize = getImageSizeForStageDesc(stageId)
        val opad = 10f
        var indent = 0f
        indent = 10f
        indent += getImageIndentForStageDesc(stageId)
        val width = barWidth - indent * 2f


        val info = main.beginImageWithText(icon, imageSize, width, true)
        addStageDescriptionText(info, width - imageSize - opad, stageId)
        if (info.heightSoFar > 0) {
            var img = main.addImageWithText(opad).position.setXAlignOffset(indent)

            //Dont modify first image
            if (stageId != Stage.START) {
                var prev = main.prev as UIPanelAPI
                var spriteElement = prev.getChildrenCopy().first()
                var sprite = ReflectionUtils.invoke("getSprite", spriteElement) as Sprite
                sprite.color = faction.color
            }

            main.addSpacer(0f).position.setXAlignOffset(-indent)
        }
    }


    override fun getStageIconImpl(stageId: Any?): String {

        var spritePath = when(stageId) {
            Stage.START -> PrivateeringUtils.getSupervisorScript()?.supervisor?.portraitSprite ?: faction.crest
            Stage.WARFLEET -> "graphics/icons/intel/privateer_warfleet.png"
            Stage.ARSENAL_AUTHORIZATION -> "graphics/icons/intel/privateer_auth.png"
            Stage.PROMOTION -> "graphics/icons/intel//privateer_promotion.png"
            Stage.IMPORTANT -> "graphics/icons/intel/privateer_important.png"
            else -> "graphics/icons/intel/privateer_important.png"
        }

        Global.getSettings().loadTextureCached(spritePath)
        return spritePath
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return mutableSetOf("Commission", Tags.INTEL_MAJOR_EVENT)
    }



    override fun addBulletPoints(info: TooltipMakerAPI?,  mode: IntelInfoPlugin.ListInfoMode?,isUpdate: Boolean, tc: Color?,initPad: Float) {

        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
            return
        }

        val h = Misc.getHighlightColor()
        if (isUpdate && getListInfoParam() is EventStageData) {
            val esd = getListInfoParam() as EventStageData
            if (esd.id == Stage.WARFLEET) {
                info!!.addPara("Base bounty per destroyed frigate increased by ${Misc.getDGSCredits(200f)}.", initPad, tc, h, Misc.getDGSCredits(200f))
            }
            if (esd.id == Stage.ARSENAL_AUTHORIZATION) {
                //info!!.addPara("25%% reduced supply useage in the abyss.", initPad, tc, h, "25%")
                info!!.addPara("Gained access to capital ships and large weapons from custom production.", initPad, tc, h, "capital ships", "large weapons")
            }
            if (esd.id == Stage.PROMOTION) {
                //info!!.addPara("Base commission pay increased by ${Misc.getDGSCredits(10000f)}.", initPad, tc, h, Misc.getDGSCredits(10000f))
                info!!.addPara("The faction covers 20%% more of your monthly supply needs and pays you ${Misc.getDGSCredits(5000f)} more per month.", initPad, tc, h, "20%", Misc.getDGSCredits(5000f))
            }
            if (esd.id == Stage.IMPORTANT) {
                var percent = ((CommissionData.bondsImportantMult-1) * 100).toInt()
                info!!.addPara("Requisition bonds gained from battles increased by $percent%%.", initPad, tc, h, "$percent%")
            }

            return
        }
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        val opad = 10f
        val small = 0f
        val h = Misc.getHighlightColor()

        val stage = getDataFor(stageId) ?: return

        if (isStageActive(stageId)) {
            addStageDesc(info!!, stageId, small, false)
        }
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator {
        val esd = getDataFor(stageId)

        return object: BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)

                when (stageId) {
                    Stage.START -> tooltip!!.addTitle("")
                    Stage.WARFLEET -> tooltip!!.addTitle("War Fleet")
                    Stage.ARSENAL_AUTHORIZATION -> tooltip!!.addTitle("Arsenal Authorization")
                    Stage.PROMOTION -> tooltip!!.addTitle("Promotion")
                    Stage.IMPORTANT -> tooltip!!.addTitle("Important")
                }

                addStageDesc(tooltip!!, stageId, 10f, true)

                esd.addProgressReq(tooltip, 10f)
            }
        }

    }

    fun addStageDesc(info: TooltipMakerAPI, stageId: Any?, initPad: Float, forTooltip: Boolean)
    {
        if (stageId == Stage.START)
        {
            var supervisorScript = PrivateeringUtils.getSupervisorScript()!!

            var supervisor = supervisorScript.supervisor
            var market = supervisorScript.market

            var label = info.addPara("You are an active part within ${faction.displayNameWithArticle}'s defensive measures. Defeating fleets from opposing factions rewards you with credits, requisition bonds and favorability from ${faction.displayNameWithArticle}. " +
                    "Requisition bonds can be spend by visting your commission supervisor, ${supervisor!!.nameString}, who is available on ${market!!.name}.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "")

            label.setHighlight("${faction.displayNameWithArticle}'s", "Requisition bonds", supervisor.nameString, market.name)
            label.setHighlightColors(faction.color, Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())
        }
        if (stageId == Stage.WARFLEET)
        {
            info.addPara("Your fleets effort have made a sizeable impact. The base bounty per destroyed frigate is increased by ${Misc.getDGSCredits(200f)}.", 0f,
            Misc.getTextColor(), Misc.getHighlightColor(), Misc.getDGSCredits(200f))
        }
        if (stageId == Stage.ARSENAL_AUTHORIZATION)
        {
            info.addPara("With increased favorability of your fleet within ${faction.displayNameWithArticle}'s ranks, " +
                    "you have been given authorisation to access custom production for capital ships and large weapons from your commission supervisor.",
                0f,  Misc.getTextColor(), Misc.getHighlightColor(),"capital ships", "large weapons")
        }
        if (stageId == Stage.PROMOTION)
        {
            info.addPara("Your fleet is recognised as a vital part of the factions goals. To alleviate logistical issues, " +
                    "the faction is willing to cover 20%% more of your basic maintenance costs and increase its base commission pay by ${Misc.getDGSCredits(5000f)}.", 0f,  Misc.getTextColor(), Misc.getHighlightColor(),
                "20%", "${Misc.getDGSCredits(5000f)}")
        }
        if (stageId == Stage.IMPORTANT)
        {
            var percent = ((CommissionData.bondsImportantMult-1) * 100).toInt()
            info.addPara("You are important to the preservation of the factions future. Worried about other factions poaching you, " +
                    "${faction.displayNameWithArticle} increased the rewarded amount of requisition bonds from battles by $percent%%.", 0f,
                Misc.getTextColor(), Misc.getHighlightColor(), "requisition bonds", "$percent%")
        }
    }

    override fun getName(): String {
        return "${faction.displayName} Favorability"
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount) //Used to cause issues, need to check if that remains the case after uncomment

        //End if no longer commissioned
        if (Misc.getCommissionFaction() != faction) {
            endAfterDelay(0f)
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()
        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)

        if (stage!!.id == Stage.WARFLEET)
        {

        }

        if (stage!!.id == Stage.ARSENAL_AUTHORIZATION)
        {

        }



        if (stage!!.id == Stage.PROMOTION) {
            PrivateeringUtils.getCommissionData().costsCovered = 0.7f
        }

        if (stage!!.id == Stage.IMPORTANT)
        {

        }
    }

    override fun getBarColor(): Color {
        return faction.baseUIColor.darker()
    }

    override fun getBarProgressIndicatorColor(): Color {
        return faction.color
    }

    override fun getBarBracketColor(): Color {
        return faction.color
    }

    override fun getProgressColor(delta: Int): Color {
        return Misc.getHighlightColor()
    }

    override fun getBarProgressIndicatorLabelColor(): Color {
        return faction.color
    }

    override fun getTitleColor(mode: IntelInfoPlugin.ListInfoMode?): Color {
        return Misc.getBasePlayerColor()
    }

    override fun getStageColor(stageId: Any?): Color {
        return faction.color
    }

    override fun getBaseStageColor(stageId: Any?): Color {
        return faction.color
    }

    override fun getDarkStageColor(stageId: Any?): Color {
        return faction.darkUIColor
    }

   /* override fun getStageIconColor(stageId: Any?): Color {
        return Color(255, 255, 255)
    }*/

    override fun getStageIconColor(stageId: Any): Color {
        val req = getRequiredProgress(stageId)

        val last = getLastActiveStage(false)
        val esd = getDataFor(stageId)
        var grayItOut = false
        if (last != null && esd != null && last !== esd && !esd.isOneOffEvent && !esd.keepIconBrightWhenLaterStageReached && esd.progress < last.progress) {
            grayItOut = true
        }

        if (esd != null && esd.randomized && esd.rollData != null) {
            faction.color
        }

        if (req > progress || grayItOut) {
            return faction.color.setAlpha(155)
        }
        return faction.color
    }

    override fun getStageLabelColor(stageId: Any?): Color {
        return faction.color
    }


    override fun getCircleBorderColorOverride(): Color {
        return faction.color
    }

    //This sets the color for the headers, because alex.
    //Replace with boss factions color once ready
    override fun getFactionForUIColors(): FactionAPI {


        return faction
    }

    override fun getBulletColorForMode(mode: IntelInfoPlugin.ListInfoMode?): Color {
        return Misc.getTextColor()
    }

    override fun withMonthlyFactors(): Boolean {
        return false
    }

    override fun withOneTimeFactors(): Boolean {
        return true
    }

    override fun getIcon(): String {
        return faction.crest
    }
}