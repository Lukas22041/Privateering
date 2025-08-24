package privateering.ui.element

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import lunalib.lunaUI.elements.LunaElement
import privateering.scripts.getAndLoadSprite
import java.awt.Color

class OfficerDisplayElement(var officer: PersonAPI?, var color: Color, tooltip: TooltipMakerAPI, width: Float, height: Float) : LunaElement(tooltip, width, height) {

    var noOfficerSprite = Global.getSettings().getAndLoadSprite("graphics/ui/priv_no_officer.png")
    var officerSprite: SpriteAPI? = null

    var hoverFade = 0f

    init {
        enableTransparency = true
        borderAlpha = 0.5f
        borderColor = color
        renderBackground = false

        if (officer != null) {
            officerSprite = Global.getSettings().getSprite(officer!!.portraitSprite)
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (isHovering) {
            hoverFade += 10f * amount
        } else {
            hoverFade -= 3f * amount
        }
        hoverFade = hoverFade.coerceIn(0f, 1f)
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

    }

    override fun renderBelow(alphaMult: Float) {
        super.renderBelow(alphaMult)

        if (officer == null) {
            noOfficerSprite.setNormalBlend()
            noOfficerSprite.alphaMult = alphaMult * 0.7f
            noOfficerSprite.setSize(width, height)
            noOfficerSprite.render(x, y)

            noOfficerSprite.setAdditiveBlend()
            noOfficerSprite.alphaMult = alphaMult * 0.3f * hoverFade
            noOfficerSprite.setSize(width, height)
            noOfficerSprite.render(x, y)
        } else {

            var alpha = 1f

            officerSprite!!.setNormalBlend()
            officerSprite!!.alphaMult = alphaMult * 0.7f * alpha
            officerSprite!!.setSize(width, height)
            officerSprite!!.render(x, y)

            officerSprite!!.setAdditiveBlend()
            officerSprite!!.alphaMult = alphaMult * 0.3f * hoverFade
            officerSprite!!.setSize(width, height)
            officerSprite!!.render(x, y)
        }


    }
}