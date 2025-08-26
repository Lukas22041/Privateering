package privateering.misc.commands

import com.fs.starfarer.api.util.Misc
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import privateering.PrivateeringUtils
import privateering.intel.event.AcquiredFavorFactor
import privateering.intel.event.CommissionEventIntel

class AddFavorCommand : BaseCommand {
    /**
     * Called when the player enters your command.
     *
     * @param args    The arguments passed into this command. Will be an empty [String] if no arguments were
     * entered.
     * @param context Where this command was called from (campaign, combat, mission, simulation, etc).
     *
     * @return A [CommandResult] describing the result of execution.
     *
     * @since 2.0
     */
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        if (context == BaseCommand.CommandContext.COMBAT_MISSION || context == BaseCommand.CommandContext.COMBAT_SIMULATION) {
            Console.showMessage("The command can only be used in the campaign.")
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }

        var number = args.toIntOrNull()
        if (number == null || number < 0) {
            Console.showMessage("The command requires a positive integer number.")
            return BaseCommand.CommandResult.BAD_SYNTAX
        }

        var faction = Misc.getCommissionFaction()
        if (faction == null) {
            Console.showMessage("The command can only be run when you have an active commission")
            return BaseCommand.CommandResult.ERROR
        }

        var data = PrivateeringUtils.getCommissionData(faction)
        AcquiredFavorFactor(number, null)
        Console.showMessage("Added ${number} favorability to the event.")

        return BaseCommand.CommandResult.SUCCESS
    }
}