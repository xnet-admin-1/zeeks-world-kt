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
        val lat = 43.6057601
        val lon = -116.3932135
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
                    // Oliver the cat - oversized (5x scale)
                    val ox = 3f; val oz = 3f; val s = 5f // 5-block scale
                    this.color = de.fabmax.kool.util.Color(1f, 0.6f, 0.2f, 1f) // orange
                    // Body (2x3x2 blocks)
                    for (bx in 0..1) for (by in 0..2) for (bz in 0..1) {
                        cube { origin.set(ox + bx*s, s + by*s, oz + bz*s); size.set(s, s, s) }
                    }
                    // Head (2x2x2)
                    this.color = de.fabmax.kool.util.Color(1f, 0.7f, 0.3f, 1f) // lighter orange
                    for (bx in 0..1) for (by in 0..1) for (bz in 0..1) {
                        cube { origin.set(ox + bx*s, 4*s + by*s, oz + bz*s); size.set(s, s, s) }
                    }
                    // Ears
                    this.color = de.fabmax.kool.util.Color(1f, 0.5f, 0.1f, 1f) // dark orange
                    cube { origin.set(ox, 6*s, oz); size.set(s, s, s) }
                    cube { origin.set(ox + s, 6*s, oz); size.set(s, s, s) }
                    // Tail (3 blocks long)
                    this.color = de.fabmax.kool.util.Color(1f, 0.6f, 0.2f, 1f)
                    cube { origin.set(ox - s, s, oz); size.set(s, s, s) }
                    cube { origin.set(ox - 2*s, s + s*0.5f, oz); size.set(s, s, s) }
                    cube { origin.set(ox - 3*s, 2*s, oz); size.set(s, s, s) }
                    // Eyes (white + green)
                    this.color = de.fabmax.kool.util.Color(1f, 1f, 1f, 1f)
                    cube { origin.set(ox, 5*s, oz + 2*s); size.set(s*0.5f, s*0.5f, s*0.3f) }
                    cube { origin.set(ox + s*0.7f, 5*s, oz + 2*s); size.set(s*0.5f, s*0.5f, s*0.3f) }
                    this.color = de.fabmax.kool.util.Color(0.2f, 0.8f, 0.2f, 1f) // green pupils
                    cube { origin.set(ox + s*0.1f, 5*s, oz + 2.2f*s); size.set(s*0.3f, s*0.3f, s*0.2f) }
                    cube { origin.set(ox + s*0.8f, 5*s, oz + 2.2f*s); size.set(s*0.3f, s*0.3f, s*0.2f) }
                    // Nose (pink)
                    this.color = de.fabmax.kool.util.Color(1f, 0.6f, 0.7f, 1f)
                    cube { origin.set(ox + s*0.4f, 4.5f*s, oz + 2*s); size.set(s*0.4f, s*0.3f, s*0.2f) }
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
