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

    fun createScene(ctx: KoolContext): Scene {
        // Fetch and generate Zeek's neighborhood from OSM
        val lat = 43.6121
        val lon = -116.3915
        val radius = 100.0 // meters
        try {
            println("Fetching OSM data for Zeek's neighborhood...")
            val osmData = OsmFetcher.fetchArea(lat, lon, radius)
            println("OSM: ${osmData.buildings.size} buildings, ${osmData.roads.size} roads, ${osmData.parks.size} parks")
            WorldGenerator.generate(osmData, lat, lon, world)
            // Enrich with Geoapify POIs
            GeoapifyEnricher.enrichWorld(world, lat, lon)
        } catch (e: Exception) {
            println("OSM fetch failed: ${e.message}, using flat world")
            world.generateFlat(50)
        }

        return scene {
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

            addColorMesh {
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
        }
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
