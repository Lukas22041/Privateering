package privateering.ui.element

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.RingRenderer
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.CombatViewport
import com.fs.starfarer.combat.entities.terrain.Planet
import com.fs.starfarer.loading.specs.PlanetSpec
import lunalib.lunaUI.elements.LunaElement
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ui.LazyFont
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import org.magiclib.bounty.ui.drawOutlined
import privateering.CommissionData
import privateering.rules.SupervisorDialogDelegate
import privateering.scripts.getAndLoadSprite
import privateering.scripts.levelBetween
import java.awt.Color

class OrbitSelectorWidget(var dialog: SupervisorDialogDelegate, var system: StarSystemAPI, tooltip: TooltipMakerAPI, width: Float, height: Float) : LunaElement(tooltip, width, height) {

    var factor = 0.05f

    class SelectedOrbit(var focus: SectorEntityToken, var distance: Float, var angle: Float) {
        fun getWorldLoc() = MathUtils.getPointOnCircumference(focus.location, distance, angle-180)
    }

    var stationSpec = Global.getSettings().getCustomEntitySpec(CommissionData.stationSpecId)

    var zoomMult = 2f
    var offset = Vector2f(0f, 0f)

    var systemWidth = 52000f
    var systemHeight = 52000f

    var shader = 0

    var time: Float = 0f

    var mousePosLastFrame = Vector2f() //used for only dragging, not tracking mouse position on screen

    var distanceMult = 0.33f

    var cursorLoc = Vector2f()

    var recentFocus: SectorEntityToken? = null

    var influenceSprite = Global.getSettings().getAndLoadSprite("graphics/ui/priv_sphere_of_influence.png")

    var selectedOrbit: SelectedOrbit? = null

    var font: LazyFont? = LazyFont.loadFont(Fonts.INSIGNIA_VERY_LARGE)
    var planetText: LazyFont.DrawableString? = font!!.createText("", Color.white, 800f)

 /*   var planetRotations = HashMap<PlanetAPI, Float>()
    var cloudRotations = HashMap<PlanetAPI, Float>()*/

    var planets = HashMap<PlanetAPI, Planet>()
    var lastFocus: SectorEntityToken? = null

    init {
        enableTransparency = true
        borderAlpha = 0.5f
        backgroundAlpha = 1f
        backgroundColor = Color(0, 0, 0, 255)

        if (shader == 0) {
            shader = ShaderLib.loadShader(Global.getSettings().loadText("data/shaders/priv_baseVertex.shader"),
                Global.getSettings().loadText("data/shaders/priv_glitchFragment.shader"))
            if (shader != 0) {
                GL20.glUseProgram(shader)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0)
                GL20.glUseProgram(0)
            } else {
                var test = ""
            }
        }

        var starColor = Misc.getBasePlayerColor()
        if (system.star != null) starColor = system.star.spec.iconColor

        /*var title = innerElement.addTitle(system.name, starColor)
        title.position.inTL(0f, -20f)*/


    }

    override fun advance(amount: Float) {

        time += 1 * amount

        for (planet in planets) {
            planet.value.advance(amount)
        }

      /*  for (planet in system.planets) {
            var rot = planetRotations.get(planet)
            if (rot == null) rot = 0f
            planetRotations.set(planet, rot + 10f * amount)

            var cRot = cloudRotations.get(planet)
            if (cRot == null) cRot = 0f
            cloudRotations.set(planet, cRot + 20f * amount)
        }*/

        if (Mouse.isButtonDown(1)) {
            var currentX = Mouse.getX().toFloat()
            var currentY = Mouse.getY().toFloat()

            if (isHovering) {
                var difference = Vector2f(currentX-mousePosLastFrame.x, currentY-mousePosLastFrame.y)


                var newX = offset.x + (difference.x * 50f / zoomMult)
                var newY = offset.y + (difference.y * 50f / zoomMult)

                newX = MathUtils.clamp(newX, -systemWidth/3, systemWidth/3)
                newY = MathUtils.clamp(newY, -systemHeight/3, systemHeight/3)

                offset = Vector2f(newX, newY)
            }



        }
        mousePosLastFrame = Vector2f(Mouse.getX().toFloat(), Mouse.getY().toFloat())

        super.advance(amount)

    }

    override fun processInput(events: MutableList<InputEventAPI>?) {


        for (event in events!!) {
            if (event.isConsumed) continue

            if (event.isLMBDownEvent) {
                if (event.x.toFloat() in x..(x+width) && event.y.toFloat() in y..(y+height)) {


                    var world = toWorldCoordinates(Vector2f(event.x.toFloat(), event.y.toFloat()))
                    var focus = getFocus(world)

                    var toClose = false
                    if (focus is PlanetAPI) {
                        var distanceToFocus = MathUtils.getDistance(world, focus.location)
                        var token = system.createToken(MathUtils.getPointOnCircumference(focus.location, distanceToFocus, 0f))
                        var radius = Math.max(stationSpec.spriteHeight, stationSpec.spriteHeight)
                        var distance = MathUtils.getDistance(focus, system.createToken(MathUtils.getPointOnCircumference(focus.location, distanceToFocus, 0f)))

                        var combined = focus.radius + radius

                        if (distance < 10 + radius/2) {
                            toClose = true
                        }
                    }

                    if (toClose) {
                        Global.getSoundPlayer().playUISound("ui_char_can_not_increase_skill_or_aptitude", 1f, 1f)
                    }
                    else if (focus != null) {
                        Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f)
                        var orbit = SelectedOrbit(focus, MathUtils.getDistance(world, focus.location), Misc.getAngleInDegrees(world, focus.location))
                        //data.orbitPickerLastSelected = orbit
                        selectedOrbit = orbit
                        dialog.selectedOrbitForStation(orbit)
                    }

                    //event.consume()
                    continue
                }
            }
        }

        super.processInput(events)

        for (event in events!!) {
            if (event.isConsumed) {
                continue
            }

            if (event.isMouseMoveEvent) {
                cursorLoc = Vector2f(event.x.toFloat(), event.y.toFloat())
            }

            if (isHovering) {



                if (event.isMouseScrollEvent) {

                    var center = system.center

                    var increase = 0f
                    if (event.eventValue > 0) {
                        increase += 1f * zoomMult / 3
                    } else {
                        increase -= 1f * zoomMult / 3
                    }


                    var screenScale = Global.getSettings().screenScaleMult

                    var mouseX = Mouse.getX() / screenScale
                    var mouseY = Mouse.getY() / screenScale
                    var mouseWorld = toWorldCoordinates(Vector2f(mouseX, mouseY))
                    var mouseLocBefore = Vector2f(mouseWorld)

                    zoomMult += increase
                    zoomMult = MathUtils.clamp(zoomMult, 1f, 60f)

                    mouseWorld = toWorldCoordinates(Vector2f(mouseX, mouseY))
                    var mouseLocAfter = Vector2f(mouseWorld)

                    var diff = mouseLocAfter.minus(mouseLocBefore)
                    offset = offset.plus(diff)


                    event.consume()
                    continue
                }
            }



        }

    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        //factor = width / (systemWidth / 2)

        startStencil()

        renderGrid(alphaMult)

        var screenScale = Global.getSettings().screenScaleMult

        var mouseX = Mouse.getX() / screenScale
        var mouseY = Mouse.getY() / screenScale
        var mouseInWorld = toWorldCoordinates(Vector2f(mouseX, mouseY))
        //mouseInWorld = mouseInWorld.scale(1/distanceMult) as Vector2f
        var focus = getFocus(mouseInWorld)

        if (focus != null && focus != lastFocus && isHovering) {
            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 1f)
        }
        lastFocus = focus

        if (mouseX !in x..(x+width) || mouseY !in y..(y+height)) focus = null //Prevent selection outside of panel

        if (focus != null) {
            var invalid = false
            var distanceToFocus = MathUtils.getDistance(mouseInWorld, focus.location)
            if (distanceToFocus <= focus.radius) invalid = true

            var focusLoc = toMapCoordinates(focus.location)

            if (focus is PlanetAPI && !focus.isStar) {
                var icon = focus.spec.iconTexture
                var sprite = Global.getSettings().getSprite(icon)

                var mult = 2.5f
                if (focus.isGasGiant) mult += 1f

                //influenceSprite.color = sprite.averageBrightColor
                influenceSprite.color = Misc.getDarkPlayerColor()
                influenceSprite.setNormalBlend()
                influenceSprite.alphaMult = alphaMult * 0.9f
                influenceSprite.setSize(focus.radius * factor * zoomMult * (1-distanceMult)*mult, focus.radius * factor * zoomMult * (1-distanceMult)*mult)
                influenceSprite.renderAtCenter(focusLoc.x, focusLoc.y)

            }

            var color: Color? = null

            if (focus is PlanetAPI) {
                var token = system.createToken(MathUtils.getPointOnCircumference(focus.location, distanceToFocus, 0f))
                var radius = Math.max(stationSpec.spriteHeight, stationSpec.spriteHeight)
                var distance = MathUtils.getDistance(focus, system.createToken(MathUtils.getPointOnCircumference(focus.location, distanceToFocus, 0f)))

                var combined = focus.radius + radius

                if (distance < 10 + radius/2) {
                    color = Misc.getNegativeHighlightColor()
                }
            }


            renderOrbit(focusLoc, (distanceToFocus)*factor* zoomMult *distanceMult, alphaMult, colorOverride = color)

            var angle = Misc.getAngleInDegrees(focus.location, mouseInWorld)

            var circleLoc = MathUtils.getPointOnCircumference(focusLoc, (distanceToFocus)*factor* zoomMult *distanceMult , angle)
            renderCircle(circleLoc, 5f, alphaMult, colorOverride = color)

            //println(distanceToFocus)
        }


        //Rings

        var terrainCopy = system.terrainCopy.filter { it.plugin is BaseRingTerrain } as List<CampaignTerrainAPI>

        for (terrain in terrainCopy) {

            var plugin = terrain.plugin

            if (plugin !is AsteroidBeltTerrainPlugin) continue

            var params = plugin.params ?: continue
            var band = plugin.ringParams ?: continue

            var color = Global.getSettings().getColor("ringSystemMapColor")
            if (focus == terrain) color = Misc.interpolateColor(color, Misc.getDarkPlayerColor().brighter().brighter().brighter(), 0.5f)

            var loc = toMapCoordinates(terrain.location)



            var renderer = RingRenderer("systemMap", "map_ring")
            var r = (plugin.ringParams.middleRadius) * factor * zoomMult * (1-distanceMult)*2f

            renderer.render(loc,
                (params.middleRadius - params.bandWidthInEngine * 0.5f) * factor * zoomMult * distanceMult,
                (params.middleRadius + params.bandWidthInEngine * 0.5f) * factor * zoomMult * distanceMult,
                color, false, 1f, alphaMult)

        }

        //Render Asteroids


        for (planet in system.planets) {
            var icon = planet.spec.iconTexture
            var sprite = Global.getSettings().getAndLoadSprite(icon)

            var loc = planet.location

            var convertedLoc = toMapCoordinates(loc)



            var parent = planet.orbitFocus
            //if ((parent is PlanetAPI && parent.isStar) || parent == system.center) {
            if (parent != null) {

                var parentLoc = toMapCoordinates(parent.location)

                var distance = MathUtils.getDistance(parentLoc, convertedLoc)

                renderOrbit(parentLoc, distance, alphaMult * 0.1f)
            }


            //Planet

            var llx = 0f
            var lly = 0f
            var radius = planet.radius

            var pLoc = Vector2f(llx, lly)

            var rPlanet = planets.get(planet)
            if (rPlanet == null) {
                rPlanet = Planet(planet.spec as PlanetSpec, radius, 1f, pLoc)
                planets.set(planet, rPlanet)
            }

            var viewport = CombatViewport(llx - radius / 2.0F,lly - radius / 2.0F, radius, radius);
            //var viewport = CombatViewport(llx - radius ,lly - radius, radius, radius);

            var sWidth = Global.getSettings().screenWidth
            var sHeight = Global.getSettings().screenHeight

            rPlanet.scale = 1f * factor * zoomMult * (1-distanceMult) / 2f //have to use this attribute, otherwise things just break

            GL11.glPushMatrix();
            //GL11.glTranslatef(sWidth / 2f, sHeight / 2f, 0.0F);
            GL11.glTranslatef(0f + convertedLoc.x, 0f + convertedLoc.y, 0.0F);
            GL11.glTranslatef(-rPlanet.location.x, -rPlanet.location.y, 0.0F);
            viewport.alphaMult = alphaMult

            //rPlanet.renderSphere(viewport)
            rPlanet.render(CombatEngineLayers.PLANET_LAYER, viewport)

            GL11.glPopMatrix();



            planetText!!.text = planet.name
            planetText!!.fontSize = 50f * factor * zoomMult * (1-distanceMult)
            planetText!!.baseColor = planet.market?.faction?.color ?: Misc.getBasePlayerColor()
            planetText!!.blendDest = GL11.GL_ONE_MINUS_SRC_ALPHA
            planetText!!.blendSrc = GL11.GL_SRC_ALPHA

            planetText!!.draw((convertedLoc.x-planetText!!.width/2f) ,convertedLoc.y + (planet.radius/2 * factor * zoomMult * (1-distanceMult) + planetText!!.height*1.5f) )
        }


        if (selectedOrbit != null) {

            var focusLocMap = toMapCoordinates(selectedOrbit!!.focus.location)
            var entityLocMap = toMapCoordinates(selectedOrbit!!.getWorldLoc())

            renderOrbit(focusLocMap, selectedOrbit!!.distance *factor* zoomMult *distanceMult, alphaMult * 0.5f, Misc.getBasePlayerColor(), 2f)

            //var plugin = ConstructionPlannerPanel.selectedPlugin
            //var entitySpec = Global.getSettings().getCustomEntitySpec(ConstructionPlannerPanel.entityIndexMap.get(plugin!!.specId))
            var entitySpec = Global.getSettings().getCustomEntitySpec(CommissionData.stationSpecId)
            var sprite = Global.getSettings().getSprite(entitySpec.spriteName)


            GL20.glUseProgram(shader)

            //var colorMult = Vector3f(0.3f , 0.6f + 0.2f, 1.5f + 0.3f)
            var colorMult = Vector3f(0.8f , 0.9f, 1.2f)
            var shaderTime = time / 8
            GL20.glUniform3f(GL20.glGetUniformLocation(shader, "colorMult"), colorMult.x, colorMult.y, colorMult.z)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "iTime"), shaderTime)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "alphaMult"), 0.9f * alphaMult)

            sprite.alphaMult = alphaMult
            sprite.angle = selectedOrbit!!.angle + 90
            sprite.setSize(entitySpec.spriteWidth * factor * zoomMult * distanceMult, entitySpec.spriteHeight * factor * zoomMult * distanceMult)
            sprite.renderAtCenter(entityLocMap.x, entityLocMap.y)

            GL20.glUseProgram(0)


        }


        endStencil()





        //var test = toWorldCoordinates(Vector2f(x+width/2, y+height/2)) //Returns 0,0 in woorld coords


        //println(toWorldCoordinates(Vector2f(Mouse.getX() / scale, Mouse.getY() / scale)))

        //renderCircle(Vector2f(cursorLoc.x, cursorLoc.y), 50f)
        //renderCircle(Vector2f(Mouse.getX() / scale, Mouse.getY() / scale), 50f)
    }

    fun getFocus(cursorWorldCoords: Vector2f) : SectorEntityToken? {
        if (!isHovering) return null //Avoid Making the selector function while the map is not in focus, i.e due to popups
        var pick = system.center
        var shortestDistance = 1000000f

        for (planet in system.planets.filter { it.isStar }) {
            var distance = MathUtils.getDistance(cursorWorldCoords, planet.location)

            //Stars that orbit other stars should not get as large of a selection range bonus
            if (planet.orbitFocus != null && planet.orbitFocus.isStar) {
                if (distance >= planet.radius * 5f) {
                    continue
                }
            }

            if (distance <= shortestDistance) {
                //if (pick == system.center) {
                pick = planet
                shortestDistance = distance


               // }
            }
        }



        //Check for asteroid belts
        var terrainCopy = system.terrainCopy.filter { it.plugin is BaseRingTerrain } as List<CampaignTerrainAPI>

        for (terrain in terrainCopy) {

            var plugin = terrain.plugin

            if (plugin !is AsteroidBeltTerrainPlugin) continue

            var params = plugin.params ?: continue

            var min = params.middleRadius - params.bandWidthInEngine * 0.5f
            var max = params.middleRadius + params.bandWidthInEngine * 0.5f

            var distance = MathUtils.getDistance(cursorWorldCoords, terrain.location)
            if (distance in min..max) {
                pick = terrain
            }

        }

        for (planet in system.planets.filter { !it.isStar }) {
            var distance = MathUtils.getDistance(cursorWorldCoords, planet.location)

            //Increase the radius for gas giants
            var mult = 2.5f
            if (planet.isGasGiant) mult += 1f

            if (distance <= planet.radius * mult) { //Always pick a planet if the cursor is in its range and its the shortest distance
                pick = planet
                shortestDistance = distance
            }
        }

        recentFocus = pick
        return pick
    }

    fun toMapCoordinates(worldCoords: Vector2f) : Vector2f {
        var loc = Vector2f(worldCoords)
        loc = loc.plus(offset)

        loc = loc.scale(factor * zoomMult) as Vector2f
        loc = loc.scale(distanceMult) as Vector2f

        //Center of the UI Element
        var center = Vector2f(x + width / 2, y + height / 2)

        //Add Element Center to Element
        loc = loc.plus(center)
        return loc
    }

    fun toWorldCoordinates(mapCoords: Vector2f) : Vector2f{
        var loc = Vector2f(mapCoords)

        var center = Vector2f(x + width / 2, y + height / 2)
        loc = loc.minus(center)

        loc = loc.scale(1 / distanceMult) as Vector2f
        loc = loc.scale(1 / (factor * zoomMult)) as Vector2f

        loc = loc.minus(offset)
        return loc
    }

    fun renderGrid(alphaMult: Float) {

        var size = 2000f
        var cells = systemWidth.toInt() / size.toInt()

        var offset = Vector2f(-(systemWidth / 2), -(systemHeight / 2))
        var color = Misc.getDarkPlayerColor()

        for (cellY in 0 until cells) {

            var y = size * cellY + offset.y

            for (cellX in 0 until cells) {
                var x = size * cellX + offset.x


                GL11.glPushMatrix()

                GL11.glTranslatef(0f, 0f, 0f)
                GL11.glRotatef(0f, 0f, 0f, 1f)

                GL11.glDisable(GL11.GL_TEXTURE_2D)


                GL11.glEnable(GL11.GL_BLEND)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

                var map = toMapCoordinates(Vector2f(x, y))
                var size = size * factor * zoomMult * distanceMult

                //var center = Vector2f(x+size/2, y+size/2)




                GL11.glEnable(GL11.GL_LINE_SMOOTH)
                GL11.glBegin(GL11.GL_LINE_STRIP)

                var points = ArrayList<Vector2f>()
                points.add(Vector2f(map.x, map.y))
                points.add(Vector2f(map.x + size, map.y))
                points.add(Vector2f(map.x + size, map.y + size))
                points.add(Vector2f(map.x, map.y + size))
                points.add(Vector2f(map.x, map.y))

                for (point in points) {

                    var alpha = 0.2f
                    var distance = MathUtils.getDistance(Vector2f(), Vector2f(x, y))

                    var level = distance.levelBetween(systemWidth, 0f)
                    alpha *= level

                    GL11.glColor4f(color.red / 255f,
                        color.green / 255f,
                        color.blue / 255f,
                        color.alpha / 255f * alphaMult * alpha)

                    GL11.glVertex2f(point.x, point.y)
                }

                /*GL11.glVertex2f(map.x, map.y)
                GL11.glVertex2f(map.x + size, map.y)
                GL11.glVertex2f(map.x + size, map.y + size)
                GL11.glVertex2f(map.x, map.y + size)
                GL11.glVertex2f(map.x, map.y)*/

                GL11.glEnd()
                GL11.glPopMatrix()


            }
        }
    }

    fun renderOrbit(pos: Vector2f, radius: Float, alphaMult: Float, colorOverride: Color? = null, lineWidth: Float = 3f) {
        var color = Color(255, 255, 255)

        if (colorOverride != null) color = colorOverride

        GL11.glPushMatrix()

        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)


        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)


        GL11.glColor4f(color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f * alphaMult)

        GL11.glLineWidth(lineWidth)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glBegin(GL11.GL_LINE_STRIP)


        var circlePoints = 100
        val x = pos.x
        val y = pos.y

        for (i in 0..circlePoints) {
            val angle: Double = (2 * Math.PI * i / circlePoints)
            val vertX: Double = Math.cos(angle) * (radius)
            val vertY: Double = Math.sin(angle) * (radius)
            GL11.glVertex2d(x + vertX, y + vertY)
        }


        GL11.glEnd()
        GL11.glPopMatrix()

        GL11.glLineWidth(1f)

    }

    //Used to render selection dot
    fun renderCircle(pos: Vector2f, radius: Float, alphaMult: Float, colorOverride: Color? = null) {
        var color = Color(255, 255, 255)
        if (colorOverride != null) color = colorOverride

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)

        GL11.glDisable(GL11.GL_BLEND)

        GL11.glColor4f(color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f * alphaMult)


        GL11.glBegin(GL11.GL_POLYGON) // Middle circle

        var circlePoints = 100
        val x = pos.x
        val y = pos.y

        for (i in 0..circlePoints) {

            val angle: Double = (2 * Math.PI * i / circlePoints)
            val vertX: Double = Math.cos(angle) * (radius)
            val vertY: Double = Math.sin(angle) * (radius)
            GL11.glVertex2d(x + vertX, y + vertY)
        }

        GL11.glEnd()


        GL11.glPopMatrix()
    }

    fun startStencil() {

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

        GL11.glRectf(x, y, x+width, y+height)

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