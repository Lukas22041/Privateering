package privateering.ui.element

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.combat.CombatViewport
import com.fs.starfarer.combat.entities.terrain.Planet
import com.fs.starfarer.loading.specs.PlanetSpec
import lunalib.lunaUI.elements.LunaElement
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import privateering.misc.ReflectionUtils

class PlanetRenderElement(var planet: PlanetAPI, tooltip: TooltipMakerAPI, width: Float, height: Float) : LunaElement(tooltip, width, height) {

    init {
        enableTransparency = true
        renderBackground = false
        renderBorder = false
    }

    //
    //var rPlanet = Planet(planet.spec as PlanetSpec, width* 0.9f, 1f, Vector2f(0f, 0f))
    var rPlanet = ReflectionUtils.get("graphics", planet) as Planet //To improve performance

    override fun processInput(events: MutableList<InputEventAPI>?) {
        //super.processInput(events)
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        //rPlanet.advance(amount)
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        var llx = rPlanet.location.x
        var lly = rPlanet.location.y
        var radius = planet.radius


        var pLoc = Vector2f(llx, lly)

        var viewport = CombatViewport(llx - radius / 2.0F,lly - radius / 2.0F, radius, radius);
        //var viewport = CombatViewport(llx - radius ,lly - radius, radius, radius);


        rPlanet.scale = width / rPlanet.radius / 2 * 0.85f

        GL11.glPushMatrix();
        //GL11.glTranslatef(sWidth / 2f, sHeight / 2f, 0.0F);
        GL11.glTranslatef(x + width / 2, y + height / 2, 0.0F);
        GL11.glTranslatef(-rPlanet.location.x, -rPlanet.location.y, 0.0F);
        viewport.alphaMult = alphaMult

        //rPlanet.renderSphere(viewport)
        rPlanet.render(CombatEngineLayers.PLANET_LAYER, viewport)

        GL11.glPopMatrix();
    }

}