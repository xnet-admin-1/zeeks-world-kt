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
                val speed = 30f * dt
                val yawRad = Math.toRadians(orbit.horizontalRotation.toDouble()).toFloat()
                val fwdX = -sin(yawRad)
                val fwdZ = -cos(yawRad)
                val rightX = cos(yawRad)
                val rightZ = -sin(yawRad)

                var dx = 0f; var dy = 0f; var dz = 0f
                if (KEY_W in keys) { dx += fwdX * speed; dz += fwdZ * speed }
                if (KEY_S in keys) { dx -= fwdX * speed; dz -= fwdZ * speed }
                if (KEY_A in keys) { dx -= rightX * speed; dz -= rightZ * speed }
                if (KEY_D in keys) { dx += rightX * speed; dz += rightZ * speed }
                if (KEY_Q in keys || KEY_SHIFT in keys) { dy -= speed }
                if (KEY_E in keys || KEY_SPACE in keys) { dy += speed }

                if (dx != 0f || dy != 0f || dz != 0f) {
                    val t = orbit.translation
                    orbit.setTranslation(
                        (t.x + dx).toFloat(),
                        (t.y + dy).toFloat(),
                        (t.z + dz).toFloat()
                    )
                }
            }

            lighting.singleDirectionalLight {
                setup(Vec3f(-1f, -2f, -1f))
                setColor(Color.WHITE, 5f)
            }

            val worldMesh = addColorMesh {
                generate {
                    for ((pos, chunk) in world.chunks) {
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
            }
            var rebuildTime = 3.0
            onUpdate {
                if (worldDirty || (rebuildTime > 0 && Time.gameTime > rebuildTime)) {
                    worldDirty = false
                    rebuildTime = -1.0
                    worldMesh.generate {
                        for ((pos, chunk) in world.chunks) {
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
