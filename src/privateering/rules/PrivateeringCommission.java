package privateering.rules;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionResult;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionState;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.EndConversation;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import privateering.PrivateeringUtils;
import privateering.intel.PrivateeringCommissionIntel;
import privateering.scripts.SupervisorScript;

//Replaces the original Commission Rules Command.
//"CommissionIntelReplacingScript" exists as a backup in case another mod uses something else than this to give a commission.

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class PrivateeringCommission extends BaseCommandPlugin {
	
	public static RepLevel COMMISSION_REQ = RepLevel.FAVORABLE;
	
	protected CampaignFleetAPI playerFleet;
	protected SectorEntityToken entity;
	protected FactionAPI playerFaction;
	protected FactionAPI entityFaction;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;
	protected CargoAPI playerCargo;
	protected MemoryAPI memory;
	protected InteractionDialogAPI dialog;
	protected Map<String, MemoryAPI> memoryMap;
	protected PersonAPI person;
	protected FactionAPI faction;

	protected boolean offersCommissions;
	
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		this.dialog = dialog;
		this.memoryMap = memoryMap;
		
		String command = params.get(0).getString(memoryMap);
		if (command == null) return false;
		
		memory = getEntityMemory(memoryMap);
		
		entity = dialog.getInteractionTarget();
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		
		playerFleet = Global.getSector().getPlayerFleet();
		playerCargo = playerFleet.getCargo();
		
		playerFaction = Global.getSector().getPlayerFaction();
		entityFaction = entity.getFaction();
		
		person = dialog.getInteractionTarget().getActivePerson();
		faction = person.getFaction();
		
		offersCommissions = faction.getCustomBoolean("offersCommissions");
		
		
		//printInfo
		//accept
		
		if (command.equals("printRequirements")) {
			printRequirements();
		} else if (command.equals("playerMeetsCriteria")) {
			return playerMeetsCriteria();
		} else if (command.equals("printInfo")) {
			printInfo();
		} else if (command.equals("hasFactionCommission")) {
			return hasFactionCommission();
		} else if (command.equals("hasOtherCommission")) {
			if (hasOtherCommission()) {
				memory.set("$theOtherCommissionFaction", Misc.getCommissionFaction().getDisplayNameWithArticle(), 0);
				memory.set("$otherCommissionFaction", Misc.getCommissionFaction().getPersonNamePrefix(), 0);
				return true;
			}
			return false;
		} else if (command.equals("accept")) {
			accept();
		} else if (command.equals("resign")) {
			resign(true);
		} else if (command.equals("resignNoPenalty")) {
			resign(false);
		} else if (command.equals("personCanGiveCommission")) {
			return personCanGiveCommission();
		}
		else if (command.equals("commissionFactionIsAtWarWith")) {
			if (hasOtherCommission()) {
				if (params.size() >= 1) {
					String target_faction_id = params.get(0).getString(memoryMap);
					FactionAPI target_faction = Global.getSector().getFaction(target_faction_id);
					if(target_faction != null )
					{
						return Misc.getCommissionFaction().isHostileTo(target_faction);
					}
				}
			}
			return false;
		}
		else if (command.equals("isCargoPodsScam")) {
			MarketAPI market = dialog.getInteractionTarget().getMarket();
			if(market == null) 
				return false;
			//Misc.getStorage(market)
			for (SectorEntityToken entity : market.getContainingLocation().getAllEntities()) {
				if (Entities.CARGO_PODS.equals(entity.getCustomEntityType())) {
					
					// use player fleet 'cause it's in market range, right? And therefore scan range.
					// market is otherwise attached to a station or planet entity (who knows!)
					float dist = Misc.getDistance(entity.getLocation(), playerFleet.getLocation()); 
					if( dist < 500f)
						if (entity.getCargo().getSupplies() >= 10)
						{
							return true;
						}
							
				}
			}
			return false;
		}
		else if (command.equals("recalcFreeSupplyDaysRemaining")) {
			Object obj1 = person.getFaction().getMemoryWithoutUpdate().get("$playerReceivedCommissionResupplyOn");
			Object obj2 = Global.getSector().getMemoryWithoutUpdate().get("$daysSinceStart"); 
			if(obj1 == null) return false;
			if(obj2 == null) return false;

			float d1 = (Float) obj1;
			float d2 = (Float) obj2;
			
			faction.getMemoryWithoutUpdate().set("$daysLeft", (int)d1 + 365 - (int)d2 , 0);
		}
		else if (command.equals("doesPlayerFleetNeedRepairs")) {
	
			
			float fleetCRcurrent = 0f;
			float fleetCRmax = 0f;
			float fleetHullDamage = 0f;

			//playerFleet.getFleetData().getMembersListCopy()
			for (FleetMemberAPI member : playerFleet.getMembersWithFightersCopy()) {
				if(member.isFighterWing()) continue; // no one cares about fighters.
				
				//if (member.canBeRepaired()) {
				fleetHullDamage += 1f - member.getStatus().getHullFraction();
				fleetCRcurrent += member.getRepairTracker().getCR();
				fleetCRmax += member.getRepairTracker().getMaxCR();

			}
			

			//System.out.println("doesPlayerFleetNeedRepairs results:");
			//System.out.println("fleetCRcurrent = " + fleetCRcurrent);
			//System.out.println("fleetCRmax = " + fleetCRmax);
			//System.out.println("fleetHullDamage = " + fleetHullDamage); // ever 1f is about 100% of a ship
			
			boolean needsSupplies = false;
			
			if(fleetHullDamage > 0.5) {
				needsSupplies = true;
				
				Global.getSector().getPlayerMemoryWithoutUpdate().set("$fleetDamaged", true , 0);
				//memory.set("$fleetDamaged", true , 0); // "Looks like you've taken some damage."
			}
			
			if(fleetHullDamage > 2.5) {
				needsSupplies = true;
				Global.getSector().getPlayerMemoryWithoutUpdate().set("$fleetDamagedLots", true , 0); // "Your fleet is in rough shape, captain."
			}
			

			// basically, if the CR percent is less than 60% (of max) for the fleet, acknowledge that supplies are needed.
			if(fleetCRcurrent == 0 || (fleetCRcurrent / fleetCRmax < 0.6f) ) {
				needsSupplies = true;
				Global.getSector().getPlayerMemoryWithoutUpdate().set("$fleetLowCR", true , 0);
			}
			
			//memory.set("$fleetLowCR", true , 0);
			//memory.set("$fleetDamaged", true , 0);
			//memory.set("$fleetDamagedLots", true , 0);
			
			return needsSupplies;
		}
		
		return true;
	}

	protected boolean hasFactionCommission() {
		return faction.getId().equals(Misc.getCommissionFactionId());
	}
	protected boolean hasOtherCommission() {
		return Misc.getCommissionFactionId() != null && !hasFactionCommission();
	}
	
	
	protected boolean personCanGiveCommission() {
		if (person == null || !offersCommissions) return false;
		if (person.getFaction().isPlayerFaction()) return false;
		
		//if (Misc.getCommissionFactionId() != null) return false;
		
		return Ranks.POST_BASE_COMMANDER.equals(person.getPostId()) ||
			   Ranks.POST_STATION_COMMANDER.equals(person.getPostId()) ||
			   Ranks.POST_ADMINISTRATOR.equals(person.getPostId()) ||
			   Ranks.POST_OUTPOST_COMMANDER.equals(person.getPostId()) ||
				"privateering_commission_supervisor".equals(person.getPostId());
	}
	
	protected void resign(boolean withPenalty) {
		FactionCommissionIntel intel = Misc.getCommissionIntel();
		if (intel != null) {
			MissionResult result = intel.createResignedCommissionResult(withPenalty, true, dialog);
			intel.setMissionResult(result);
			intel.setMissionState(MissionState.ABANDONED);
			intel.endMission(dialog);
		}

		//Exit the person interaction if its the commission supervisor
		if (person != null && person.hasTag("privateering_supervisor")) {
			dialog.getVisualPanel().hideFirstPerson();
			dialog.getInteractionTarget().setActivePerson(null);
			/*((RuleBasedDialog) (dialog)).notifyActivePersonChanged();

			FireAll.fire(null, dialog, memoryMap, "PopulateOptions");*/

			new EndConversation().execute(null, dialog, new ArrayList<>(), memoryMap);
		}
	}
	
	protected void accept() {
		if (Misc.getCommissionFactionId() == null) {
			//FactionCommissionIntel intel = new FactionCommissionIntel(faction);
			PrivateeringCommissionIntel intel = new PrivateeringCommissionIntel(faction);
			intel.missionAccepted();
			intel.sendUpdate(FactionCommissionIntel.UPDATE_PARAM_ACCEPTED, dialog.getTextPanel());
			intel.makeRepChanges(dialog);

			SupervisorScript script = PrivateeringUtils.getSupervisorScript();
			if (script != null) {

				script.replaceSupervisor(faction);
				PersonAPI supervisor = script.getSupervisor();
				MarketAPI smarket = script.getMarket();
				ContactIntel contact = ContactIntel.getContactIntel(supervisor);

				dialog.getTextPanel().addPara("Your commission is supervised by " + supervisor.getNameString() + ". " +
						"Meet " + supervisor.getHimOrHer() + " on " + smarket.getName() + " for more information. ",
						Misc.getTextColor(), Misc.getHighlightColor(), ""+supervisor.getNameString(), ""+smarket.getName());

				Global.getSector().getIntelManager().addIntelToTextPanel(contact, text);


				/*
				if (supervisor != null) {
					ContactIntel contact = ContactIntel.getContactIntel(supervisor);
				}*/
			}
		}
	}
	
	protected void printInfo() {
		TooltipMakerAPI info = dialog.getTextPanel().beginTooltip();
		
		//FactionCommissionIntel temp = new FactionCommissionIntel(faction);
		PrivateeringCommissionIntel temp = new PrivateeringCommissionIntel(faction);

		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;
		
		info.setParaSmallInsignia();

		int stipend = (int) temp.computeStipend();
		
		info.addPara("By accepting the commission, you would receive a %s monthly stipend, as well as a modest bounty for destroying enemy ships.",
				0f, h, Misc.getDGSCredits(PrivateeringCommissionIntel.getMonthlyBaseIncome()));

		info.addSpacer(10f);

		var percent = PrivateeringUtils.getCommissionData(faction).getCostsCoveredPercent();
		float compensation = PrivateeringCommissionIntel.getTotalCompensation(faction);
		info.addPara("We offer to compensate %s of your fleets upkeep. For your current fleet this would be an additional %s credits per month.",
				0f, Misc.getTextColor(), Misc.getHighlightColor(), percent+"%", Misc.getDGSCredits(compensation));

		info.addSpacer(10f);

		info.addPara("Performing work for us also rewards you with requisition bonds, which can be traded in with your commission supervisor for additional support.", 0f, Misc.getTextColor(), faction.getColor(), "requisition bonds");


		List<FactionAPI> hostile = temp.getHostileFactions();
		if (hostile.isEmpty()) {
			info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " is not currently hostile to any major factions.", 0f);
		} else {
			info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " is currently hostile to:", opad);
			
			info.setParaFontDefault();
			
			info.setBulletedListMode(BaseIntelPlugin.INDENT);
			float initPad = opad;
			for (FactionAPI other : hostile) {
				info.addPara(Misc.ucFirst(other.getDisplayName()), other.getBaseUIColor(), initPad);
				initPad = 3f;
			}
			info.setBulletedListMode(null);
		}
		
		
		dialog.getTextPanel().addTooltip();
	}
	
	
	protected boolean playerMeetsCriteria() {
		return faction.getRelToPlayer().isAtWorst(COMMISSION_REQ);
	}
	protected void printRequirements() {
		CoreReputationPlugin.addRequiredStanding(entityFaction, COMMISSION_REQ, null, dialog.getTextPanel(), null, null, 0f, true);
		CoreReputationPlugin.addCurrentStanding(entityFaction, null, dialog.getTextPanel(), null, null, 0f);
	}
	
	
	
}















