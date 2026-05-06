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
        world.generateFlat(50)
        for (x in -10..10 step 8) {
            for (z in -10..10 step 8) {
                for (y in 1..3) {
                    world.setBlock(x, y, z, Block.STONE)
                    world.setBlock(x+1, y, z, Block.STONE)
                    world.setBlock(x, y, z+1, Block.STONE)
                    world.setBlock(x+1, y, z+1, Block.GLASS)
                }
                world.setBlock(x, 1, z, Block.WOOD)
                world.setBlock(x+1, 1, z+1, Block.WOOD)
            }
        }
        for (x in -20..20 step 12) {
            for (z in -20..20 step 12) {
                world.setBlock(x, 1, z, Block.WOOD)
                world.setBlock(x, 2, z, Block.WOOD)
                world.setBlock(x, 3, z, Block.LEAF)
                world.setBlock(x-1, 3, z, Block.LEAF)
                world.setBlock(x+1, 3, z, Block.LEAF)
                world.setBlock(x, 3, z-1, Block.LEAF)
                world.setBlock(x, 3, z+1, Block.LEAF)
            }
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
                val vertRad = Math.toRadians(orbit.verticalRotation).toFloat()
                val fwdX = -sin(vertRad)
                val fwdZ = cos(vertRad)
                val rightX = fwdZ
                val rightZ = fwdX

                var dx = 0f; var dy = 0f; var dz = 0f
                if (KEY_W in keys) { dx += fwdX * speed; dz += fwdZ * speed }
                if (KEY_S in keys) { dx -= fwdX * speed; dz -= fwdZ * speed }
                if (KEY_A in keys) { dx -= rightX * speed; dz += rightZ * speed }
                if (KEY_D in keys) { dx += rightX * speed; dz -= rightZ * speed }
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
