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
    var playerX = 0f; var playerY = 0f; var playerZ = 0f

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

            val cam = PerspectiveCamera().apply {
                clipNear = 0.5f
                clipFar = 500f
            }
            camera = cam
            var camYaw = 0f
            var camPitch = 30f
            var camDist = 20f

            onUpdate {
                val dt = Time.deltaT
                val speed = 8f * dt
                val yawRad = Math.toRadians(camYaw.toDouble()).toFloat()
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

                // Apply movement using player position (not orbit translation)
                playerX += dx
                playerZ += dz

                // Ground collision
                var groundY = 0f
                for (y in 15 downTo 0) {
                    if (world.getBlock(playerX.toInt(), y, playerZ.toInt()).solid) {
                        groundY = (y + 1).toFloat()
                        break
                    }
                }
                if (btnJump || KEY_SPACE in keys) groundY += 3f
                playerY = groundY

                // Camera rotation from touch (ignore bottom-left D-pad area)
                val ptr = de.fabmax.kool.input.PointerInput.primaryPointer
                val inDpadArea = ptr.pos.x < 400f && ptr.pos.y > (2500f)
                if (ptr.isValid && ptr.isDrag && !inDpadArea) {
                    camYaw += ptr.delta.x * 0.3f
                    camPitch = (camPitch - ptr.delta.y * 0.3f).coerceIn(10f, 80f)
                }

                // Position camera behind player
                val pitchRad = Math.toRadians(camPitch.toDouble()).toFloat()
                val camX = playerX + sin(yawRad) * camDist * cos(pitchRad)
                val camZ = playerZ + cos(yawRad) * camDist * cos(pitchRad)
                val camY = playerY + camDist * sin(pitchRad)
                cam.position.set(camX, camY, camZ)
                cam.lookAt.set(playerX, playerY + 2f, playerZ)
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
                val yaw = camYaw
                playerMesh.transform.setIdentity()
                    .translate(playerX, playerY, playerZ)
                    .rotate(yaw.deg, Vec3f.Y_AXIS)
                val oRad = Math.toRadians((yaw + 150.0)).toFloat()
                val ox = playerX + sin(oRad) * 3f
                val oz = playerZ + cos(oRad) * 3f
                oliverMesh.transform.setIdentity()
                    .translate(ox, playerY, oz)
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
