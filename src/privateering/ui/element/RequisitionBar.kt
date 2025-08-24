package privateering.ui.element

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaUI.elements.LunaElement
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import privateering.CommissionData
import privateering.PrivateeringUtils
import privateering.scripts.addPara
import privateering.scripts.addTooltip
import privateering.scripts.getAndLoadSprite
import privateering.scripts.getWidth
import java.awt.Color

class RequisitionBar(var color: Color, var useage: Float, var current: Float, tooltip: TooltipMakerAPI, width: Float, height: Float) : LunaElement(tooltip, width, height) {

    var bar = Global.getSettings().getAndLoadSprite("graphics/ui/priv_requisition_bar.png")
    var barFill = Global.getSettings().getAndLoadSprite("graphics/ui/priv_requisition_bar_fill.png")
    var barFillFull = Global.getSettings().getAndLoadSprite("graphics/ui/priv_requisition_bar_fill_dark.png")


    init {
        renderBorder = false
        renderBackground = false
        enableTransparency = true

        onHoverEnter {
            playScrollSound()
        }

        var data = PrivateeringUtils.getCommissionData(Misc.getCommissionFaction())
        var label = innerElement.addTitle("Requisition Bonds: ", color)
        //label.position.inTMid(-1f-label.computeTextHeight(label.text))

        //innerElement.setParaFont("graphics/fonts/victor14.fnt")
        var number = innerElement.addPara(""+data.bonds.toInt(), 0f, color, color)


        label.position.inTL(width/2-label.computeTextWidth(label.text)/2-number.computeTextWidth(number.text)/2, -1f-label.computeTextHeight(label.text))
        number.position.rightOfTop(label as UIComponentAPI, 0-1-(label.getWidth()-label.computeTextWidth(label.text)))

        tooltip.addTooltip(elementPanel, TooltipMakerAPI.TooltipLocation.BELOW, 400f) { tooltip ->
            var value = CommissionData.bondValue
            var max = CommissionData.maxBonds.toInt()
            tooltip.addPara("Requisition bonds are a currency rewarded for performing tasks for your commissioned faction. " +
                    "A bond is equal to ${Misc.getDGSCredits(value)}, but can only be spend on services provided by your commission supervisor. You can have at most $max bonds.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "${Misc.getDGSCredits(value)}", "$max")
        }
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        bar.color = color
        barFillFull.color = color
        barFill.color = color

        bar.alphaMult = alphaMult
        bar.setSize(width, height)
        bar.render(x, y)

        current = MathUtils.clamp(current, 0f, 1f)

        startBarStencil(x, y, width, height, current)

        barFillFull.alphaMult = alphaMult
        barFillFull.setSize(width, height)
        barFillFull.render(x, y)

        endStencil()

        useage = MathUtils.clamp(useage, 0f, 1f)

        startBarStencil(x, y, width, height, useage)

        barFill.alphaMult = alphaMult
        barFill.setSize(width, height)
        barFill.render(x, y)

        endStencil()
    }

    fun startBarStencil(x: Float, y: Float, width: Float, height: Float, percent: Float) {
        GL11.glClearStencil(0);
        GL11.glStencilMask(0xff);
        //set everything to 0
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        //disable drawing colour, enable stencil testing
        GL11.glColorMask(false, false, false, false); //disable colour
        GL11.glEnable(GL11.GL_STENCIL_TEST); //enable stencil

        // ... here you render the part of the scene you want masked, this may be a simple triangle or square, or for example a monitor on a computer in your spaceship ...
        //begin masking
        //put 1s where I want to draw
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff); // Do not test the current value in the stencil buffer, always accept any value on there for drawing
        GL11.glStencilMask(0xff);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE); // Make every test succeed

        // <draw a quad that dictates you want the boundaries of the panel to be>

        GL11.glRectf(x, y, x + (width * percent), y + height)

        //GL11.glRectf(x, y, x + width, y + height)

        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP); // Make sure you will no longer (over)write stencil values, even if any test succeeds
        GL11.glColorMask(true, true, true, true); // Make sure we draw on the backbuffer again.

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF); // Now we will only draw pixels where the corresponding stencil buffer value equals 1
        //Ref 0 causes the content to not display in the specified area, 1 causes the content to only display in that area.

        // <draw the lines>
    }

    fun endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}