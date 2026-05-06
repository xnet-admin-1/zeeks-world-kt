package ngo.xnet.zeeksworld

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.UniversalKeyCode
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.*
import de.fabmax.kool.scene.geometry.Usage
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.math.cos
import kotlin.math.sin

class ZeeksGame {
    val world = World()
    val oliver = Oliver(Vec3f(3f, 2f, 3f))
    val playerPos = MutableVec3f(0f, 2f, 0f)
    val touchControls = TouchControls()
    var cameraYaw = 0f

    fun createScenes(ctx: KoolContext): List<Scene> {
        val lat = 43.6057601
        val lon = -116.3932135
        val radius = 100.0
        try {
            println("Fetching OSM data for Zeek's neighborhood...")
            val osmData = OsmFetcher.fetchArea(lat, lon, radius)
            println("OSM: ${osmData.buildings.size} buildings, ${osmData.roads.size} roads, ${osmData.parks.size} parks")
            WorldGenerator.generate(osmData, lat, lon, world)
            GeoapifyEnricher.enrichWorld(world, lat, lon)
        } catch (e: Exception) {
            println("OSM fetch failed: ${e.message}, using flat world")
            world.generateFlat(50)
        }

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

            // Third-person camera
            val cam = PerspectiveCamera()
            cam.clipNear = 0.5f
            cam.clipFar = 500f
            camera = cam

            lighting.singleDirectionalLight {
                setup(Vec3f(-1f, -2f, -1f))
                setColor(Color.WHITE, 5f)
            }

            // Static world mesh
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

            // Dynamic Oliver mesh
            val oliverMesh = addColorMesh("oliver") {
                generate { oliver.buildMesh(this) }
                shader = KslPbrShader {
                    color { vertexColor() }
                    metallic(0f)
                    roughness(0.25f)
                }
            }

            onUpdate {
                val dt = Time.deltaT
                val speed = 8f * dt

                // Touch controls (mobile) + keyboard (desktop)

                var dx = 0f
                var dz = 0f

                // WASD movement (desktop)
                if (KEY_W in keys) dz -= speed
                if (KEY_S in keys) dz += speed
                if (KEY_A in keys) dx -= speed
                if (KEY_D in keys) dx += speed

                playerPos.x += dx
                playerPos.z += dz

                // Touch camera rotation
                

                // Update Oliver
                oliver.update(dt, playerPos)

                // Rebuild Oliver mesh
                oliverMesh.generate { oliver.buildMesh(this) }

                // Third-person camera: rotates around player
                val camDist = 15f
                val camHeight = 10f
                val rad = Math.toRadians(cameraYaw.toDouble()).toFloat()
                cam.position.set(
                    playerPos.x + kotlin.math.sin(rad) * camDist,
                    playerPos.y + camHeight,
                    playerPos.z + kotlin.math.cos(rad) * camDist
                )
                cam.lookAt.set(playerPos)
            }
        }

        val hud = Hud()
        val scenes = mutableListOf(mainScene, hud.createScene(ctx))
        touchControls.createOverlay()?.let { scenes += it }
        return scenes
    }

    companion object {
        private val KEY_W = UniversalKeyCode('W').code
        private val KEY_A = UniversalKeyCode('A').code
        private val KEY_S = UniversalKeyCode('S').code
        private val KEY_D = UniversalKeyCode('D').code
    }
}
