package ngo.xnet.zeeksworld

import de.fabmax.kool.scene.OrbitInputTransform
import de.fabmax.kool.math.deg
import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.UniversalKeyCode
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.math.cos
import kotlin.math.sin

class ZeeksGame {
    val world = World()
    @Volatile var worldDirty = false
    var oliverGreeted = false
    var btnForward = false; var btnBack = false; var btnLeft = false; var btnRight = false; var btnJump = false

    val hud = Hud()

    fun createScenes(ctx: KoolContext): List<Scene> {
        val lat = 43.6057601
        val lon = -116.3932135
        val radius = 100.0

        // Generate flat world immediately
        world.generateFlat(50)

        // Fetch OSM in background
        Thread {
            try {
                println("Fetching OSM data...")
                val osmData = OsmFetcher.fetchArea(lat, lon, radius)
                println("OSM: ${osmData.buildings.size} buildings, ${osmData.roads.size} roads")
                WorldGenerator.generate(osmData, lat, lon, world)
                GeoapifyEnricher.enrichWorld(world, lat, lon)
                worldDirty = true
            } catch (e: Exception) {
                println("OSM fetch failed: ${e.message}")
            }
        }.start()

        val mainScene = scene {
            val keys = mutableSetOf<Int>()
            val inputHandler = InputStack.InputHandler("fly-cam")
            inputHandler.keyboardListeners += InputStack.KeyboardListener { keyEvents, _ ->
                for (ev in keyEvents) {
                    val code = ev.keyCode.code
                    if (ev.isPressed || ev.isRepeated) keys += code
                    if (ev.isReleased) keys -= code
                }
            }
            InputStack.pushTop(inputHandler)

            val orbit = orbitCamera {
                setRotation(20f, -30f)
                setZoom(40.0)
                setTranslation(0f, 5f, 0f)
                rightDragMethod = OrbitInputTransform.DragMethod.NONE
                middleDragMethod = OrbitInputTransform.DragMethod.NONE
            }

            onUpdate {
                val dt = Time.deltaT
                val speed = 8f * dt
                val yawRad = Math.toRadians(orbit.verticalRotation.toDouble()).toFloat()
                val fwdX = -sin(yawRad)
                val fwdZ = -cos(yawRad)
                val rightX = cos(yawRad)
                val rightZ = -sin(yawRad)

                var dx = 0f; var dz = 0f
                // Desktop: WASD
                if (KEY_W in keys) { dx += fwdX * speed; dz += fwdZ * speed }
                if (KEY_S in keys) { dx -= fwdX * speed; dz -= fwdZ * speed }
                if (KEY_A in keys) { dx -= rightX * speed; dz -= rightZ * speed }
                if (KEY_D in keys) { dx += rightX * speed; dz += rightZ * speed }

                // Android: physical buttons
                
                    if (btnForward) { dx -= fwdX * speed; dz -= fwdZ * speed }
                    if (btnBack) { dx += fwdX * speed; dz += fwdZ * speed }
                    if (btnLeft) { dx -= rightX * speed; dz -= rightZ * speed }
                    if (btnRight) { dx += rightX * speed; dz += rightZ * speed }

                // Apply movement
                val t = orbit.translation
                val newX = (t.x + dx).toFloat()
                val newZ = (t.z + dz).toFloat()

                // Ground collision: always snap to ground
                var groundY = 0f
                for (y in 15 downTo 0) {
                    if (world.getBlock(newX.toInt(), y, newZ.toInt()).solid) {
                        groundY = (y + 1).toFloat()
                        break
                    }
                }

                // Jump: lift while held
                if (btnJump || KEY_SPACE in keys) groundY += 3f

                orbit.setTranslation(newX, groundY, newZ)
            }

            lighting.singleDirectionalLight {
                setup(Vec3f(-1f, -2f, -1f))
                setColor(Color.WHITE, 5f)
            }

            val worldMesh = addColorMesh {
                generate {
                    for ((pos, chunk) in world.chunks.toMap()) {
                        ChunkMesher.buildGeometry(chunk, pos, world, this)
                    }
                }
                shader = KslPbrShader {
                    color { vertexColor() }
                    metallic(0f)
                    roughness(0.25f)
                }
            }

            val playerMesh = addColorMesh("player") {
                generate { }
                shader = KslPbrShader { color { vertexColor() } }
            }

            val oliverMesh = addColorMesh("oliver") {
                generate { }
                shader = KslPbrShader { color { vertexColor() } }
            }

            var lastPx = 0f; var lastPz = 0f; var walkPhase = 0f

            onUpdate {
                val t = orbit.translation
                val yaw = orbit.verticalRotation.toFloat()
                val px = t.x.toFloat(); val pz = t.z.toFloat(); val py = t.y.toFloat()

                // Walk animation phase
                val moved = (px - lastPx) * (px - lastPx) + (pz - lastPz) * (pz - lastPz) > 0.0001f
                if (moved) walkPhase += Time.deltaT * 8f else walkPhase = 0f
                lastPx = px; lastPz = pz
                val swing = if (moved) sin(walkPhase) * 0.4f else 0f

                // Rebuild player with animated limbs
                playerMesh.generate {
                    // Left leg
                    color = Color(0.2f, 0.3f, 0.7f, 1f)
                    cube { origin.set(-0.1f, swing * 0.5f, 0f); size.set(0.35f, 1f, 0.4f) }
                    // Right leg
                    cube { origin.set(0.45f, -swing * 0.5f, 0f); size.set(0.35f, 1f, 0.4f) }
                    // Body
                    color = Color(0.5f, 0.2f, 0.8f, 1f)
                    cube { origin.set(0.05f, 1f, 0f); size.set(0.7f, 1.5f, 0.5f) }
                    // Left arm
                    color = Color(0.9f, 0.7f, 0.5f, 1f)
                    cube { origin.set(-0.35f, 1.2f + (-swing * 0.3f), 0f); size.set(0.25f, 1f, 0.25f) }
                    // Right arm
                    cube { origin.set(0.9f, 1.2f + (swing * 0.3f), 0f); size.set(0.25f, 1f, 0.25f) }
                    // Head
                    color = Color(0.9f, 0.7f, 0.5f, 1f)
                    cube { origin.set(0.05f, 2.5f, 0f); size.set(0.7f, 0.7f, 0.7f) }
                }
                playerMesh.transform.setIdentity()
                    .translate(px, py, pz)
                    .rotate(yaw.deg, Vec3f.Y_AXIS)

                // Oliver with animated legs + tail
                val oRad = Math.toRadians((yaw + 150.0)).toFloat()
                val ox = px + sin(oRad) * 3f
                val oz = pz + cos(oRad) * 3f
                val oSwing = if (moved) sin(walkPhase * 1.3f) * 0.3f else 0f
                

                oliverMesh.generate {
                    // Body
                    color = Color(1f, 0.6f, 0.2f, 1f)
                    cube { origin.set(0f, 0.5f, 0f); size.set(0.6f, 0.6f, 1.0f) }
                    // Front legs
                    color = Color(1f, 0.55f, 0.15f, 1f)
                    cube { origin.set(0f, oSwing * 0.2f, 0.7f); size.set(0.15f, 0.5f, 0.15f) }
                    cube { origin.set(0.45f, -oSwing * 0.2f, 0.7f); size.set(0.15f, 0.5f, 0.15f) }
                    // Back legs
                    cube { origin.set(0f, -oSwing * 0.2f, -0.1f); size.set(0.15f, 0.5f, 0.15f) }
                    cube { origin.set(0.45f, oSwing * 0.2f, -0.1f); size.set(0.15f, 0.5f, 0.15f) }
                    // Head
                    color = Color(1f, 0.7f, 0.3f, 1f)
                    cube { origin.set(0.05f, 0.7f, 0.9f); size.set(0.5f, 0.5f, 0.5f) }
                    // Ears
                    color = Color(1f, 0.5f, 0.1f, 1f)
                    cube { origin.set(0.05f, 1.15f, 1.0f); size.set(0.15f, 0.2f, 0.1f) }
                    cube { origin.set(0.4f, 1.15f, 1.0f); size.set(0.15f, 0.2f, 0.1f) }
                    // Tail (wagging)
                    color = Color(1f, 0.6f, 0.2f, 1f)
                    cube { origin.set(0.2f, 0.8f, -0.7f); size.set(0.15f, 0.15f, 0.6f) }
                }
                oliverMesh.transform.setIdentity()
                    .translate(ox, py, oz)
                    .rotate(yaw.deg, Vec3f.Y_AXIS)

                // Oliver speaks once on first movement
                if (!oliverGreeted && Time.gameTime > 5.0) {
                    oliverGreeted = true
                    OliverLlm.generateResponse("Zeek just started exploring the neighborhood! Greet him.") { response ->
                        println("[Oliver] $response")
                    }
                }
            }
            var rebuildTime = 3.0
            var rebuilds = 0
            onUpdate {
                if (worldDirty || (rebuilds < 3 && Time.gameTime > rebuildTime)) {
                    worldDirty = false
                    rebuilds++
                    rebuildTime = Time.gameTime + 3.0
                    worldMesh.generate {
                        for ((pos, chunk) in world.chunks.toMap()) {
                            ChunkMesher.buildGeometry(chunk, pos, world, this)
                        }
                    }
                }
            }
        }

        return listOf(mainScene)
    }

    companion object {
        private val KEY_W = UniversalKeyCode('W').code
        private val KEY_A = UniversalKeyCode('A').code
        private val KEY_S = UniversalKeyCode('S').code
        private val KEY_D = UniversalKeyCode('D').code
        private val KEY_Q = UniversalKeyCode('Q').code
        private val KEY_E = UniversalKeyCode('E').code
        private val KEY_SPACE = UniversalKeyCode(' ').code
        private val KEY_SHIFT = KeyboardInput.KEY_SHIFT_LEFT.code
    }
}
