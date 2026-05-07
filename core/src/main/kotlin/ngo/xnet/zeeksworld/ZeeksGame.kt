package ngo.xnet.zeeksworld

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
            }

            onUpdate {
                val dt = Time.deltaT
                val speed = 8f * dt
                val yawRad = Math.toRadians(orbit.horizontalRotation.toDouble()).toFloat()
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

                // Touch: left-side drag = move in all directions
                val ptr = de.fabmax.kool.input.PointerInput.primaryPointer
                if (ptr.isValid && ptr.isDrag && ptr.pos.x < 640f) {
                    val tdx = ptr.delta.x * 0.02f
                    val tdz = ptr.delta.y * 0.02f
                    dx += rightX * tdx + fwdX * -tdz
                    dz += rightZ * tdx + fwdZ * -tdz
                }


                if (dx != 0f || dz != 0f) {
                    val t = orbit.translation
                    val newX = (t.x + dx).toFloat()
                    val newZ = (t.z + dz).toFloat()

                    // Ground collision: find highest solid block at player position
                    var groundY = 0f
                    for (y in 15 downTo 0) {
                        if (world.getBlock(newX.toInt(), y, newZ.toInt()).solid) {
                            groundY = (y + 1).toFloat()
                            break
                        }
                    }

                    orbit.setTranslation(newX, groundY, newZ)
                }
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
                generate {
                    // Legs (blue)
                    color = Color(0.2f, 0.3f, 0.7f, 1f)
                    cube { origin.set(0f, 0f, 0f); size.set(0.8f, 1f, 0.8f) }
                    // Body (purple)
                    color = Color(0.5f, 0.2f, 0.8f, 1f)
                    cube { origin.set(0f, 1f, 0f); size.set(0.8f, 1f, 0.8f) }
                    cube { origin.set(0f, 2f, 0f); size.set(0.8f, 1f, 0.8f) }
                    // Head (skin)
                    color = Color(0.9f, 0.7f, 0.5f, 1f)
                    cube { origin.set(0f, 3f, 0f); size.set(0.8f, 1f, 0.8f) }
                }
                shader = KslPbrShader { color { vertexColor() } }
            }

            // Oliver the cat (orange, 3 blocks tall, offset from player)
            val oliverMesh = addColorMesh("oliver") {
                generate {
                    color = Color(1f, 0.6f, 0.2f, 1f) // orange body
                    cube { origin.set(0f, 0f, 0f); size.set(0.8f, 1f, 1.2f) }
                    color = Color(1f, 0.7f, 0.3f, 1f) // head
                    cube { origin.set(0f, 1f, 0.1f); size.set(0.7f, 0.7f, 0.7f) }
                    color = Color(1f, 0.5f, 0.1f, 1f) // ears
                    cube { origin.set(0f, 1.6f, 0.1f); size.set(0.2f, 0.3f, 0.2f) }
                    cube { origin.set(0.5f, 1.6f, 0.1f); size.set(0.2f, 0.3f, 0.2f) }
                    color = Color(1f, 0.6f, 0.2f, 1f) // tail
                    cube { origin.set(0.2f, 0.5f, -0.8f); size.set(0.2f, 0.2f, 0.8f) }
                }
                shader = KslPbrShader { color { vertexColor() } }
            }

            onUpdate {
                val t = orbit.translation
                playerMesh.transform.setIdentity().translate(t.x.toFloat(), t.y.toFloat(), t.z.toFloat())
                // Oliver follows 3 blocks behind player
                oliverMesh.transform.setIdentity().translate(t.x.toFloat() + 3f, t.y.toFloat(), t.z.toFloat() + 2f)

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
