package privateering.ui.element

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import lunalib.lunaUI.elements.LunaElement
import org.lwjgl.opengl.GL11
import privateering.scripts.getAndLoadSprite
import java.awt.Color

class SkillWidgetElement(var id: String, var activated: Boolean, var canChangeState: Boolean, var preAcquired: Boolean, var iconPath: String, var color: Color, var isElite: Boolean, tooltip: TooltipMakerAPI, width: Float, height: Float) : LunaElement(tooltip, width, height) {

    var sprite = Global.getSettings().getSprite(iconPath)
    var inactiveBorder = Global.getSettings().getAndLoadSprite("graphics/ui/priv_skillBorderInactive.png")
    var activeBorder = Global.getSettings().getAndLoadSprite("graphics/ui/priv_skillBorderActive.png")

    var hoverFade = 0f
    var time = 0f

    var eliteStars = Global.getSettings().getAndLoadSprite("graphics/ui/priv_elite_stars.png")
    var eliteBackground = Global.getSettings().getAndLoadSprite("graphics/ui/priv_elite_background.png")


    //var border = Global.getSettings().getSprite("test")

    init {
        enableTransparency = true
        backgroundAlpha = 0f
        borderAlpha = 0f

        onHoverEnter {
            playSound("ui_button_mouseover")
        }

        /*onClick {
            if (!activated) {
                playSound("leadership1")
            }
            else {
                playSound("ui_char_decrease_skill")
            }
            activated = !activated
        }*/
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        time += 1 * amount

        if (isHovering) {
            hoverFade += 10f * amount
        } else {
            hoverFade -= 3f * amount
        }
        hoverFade = hoverFade.coerceIn(0f, 1f)


    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        sprite.setNormalBlend()
        sprite.setSize(width-8, height-8)
        sprite.alphaMult = alphaMult


        sprite.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())

        if (isElite) {
            eliteBackground.setNormalBlend()
            eliteBackground.color = color.darker()
            eliteBackground.setSize(width, height)
            eliteBackground.alphaMult = alphaMult * 0.7f
            eliteBackground.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())
        }

        if (activated) {

            activeBorder.setNormalBlend()
            activeBorder.color = color
            activeBorder.setSize(width, height)
            activeBorder.alphaMult = alphaMult
            activeBorder.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())


            activeBorder.setAdditiveBlend()
            activeBorder.color = color
            activeBorder.setSize(width, height)
            activeBorder.alphaMult = alphaMult * 0.2f
            activeBorder.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())

        }
        else {
            inactiveBorder.setNormalBlend()
            inactiveBorder.color = color
            inactiveBorder.setSize(width, height)
            inactiveBorder.alphaMult = alphaMult
            inactiveBorder.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())

            inactiveBorder.setAdditiveBlend()
            inactiveBorder.color = color
            inactiveBorder.setSize(width, height)
            inactiveBorder.alphaMult = alphaMult * 0.2f
            inactiveBorder.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())
        }

        sprite.setAdditiveBlend()
        sprite.setSize(width-8, height-8)
        sprite.alphaMult = alphaMult * 0.5f * hoverFade
        sprite.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt())

        if (isElite) {

            eliteStars.setNormalBlend()
            eliteStars.setSize(width+12, height+12)
            eliteStars.alphaMult = alphaMult
            eliteStars.renderAtCenter(x + (width / 2).toInt(), y + (height / 2).toInt()+7f)
        }
    }

    override fun renderBelow(alphaMult: Float) {
        super.renderBelow(alphaMult)

        var backgroundColor = Color(0, 0, 0)

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)


        if (alphaMult <= 0.8f) {
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        } else {
            GL11.glDisable(GL11.GL_BLEND)
        }




        GL11.glColor4f(backgroundColor.red / 255f,
            backgroundColor.green / 255f,
            backgroundColor.blue / 255f,
            backgroundColor.alpha / 255f * (alphaMult * backgroundAlpha))

        GL11.glRectf(x, y , x + width, y + height)

        GL11.glPopMatrix()
    }
}