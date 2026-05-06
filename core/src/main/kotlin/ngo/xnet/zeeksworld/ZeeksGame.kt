package ngo.xnet.zeeksworld

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Color

class ZeeksGame {
    val world = World()

    fun createScene(ctx: KoolContext): Scene {
        // Generate world
        world.generateFlat(50)
        // Add some buildings
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
        // Trees
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
            camera.apply {
                position.set(0f, 20f, 30f)
                lookAt.set(Vec3f.ZERO)
            }

            lighting.singleDirectionalLight {
                setup(Vec3f(-1f, -2f, -1f))
                setColor(Color.WHITE, 1f)
            }

            addColorMesh {
                generate {
                    // Mesh all chunks
                    for ((pos, chunk) in world.chunks) {
                        val chunkMesh = ChunkMesher.buildMesh(chunk, pos, world)
                        // Add chunk geometry to this mesh
                        geometry.addGeometry(chunkMesh.geometry)
                    }
                }
            }

            orbitInputTransform {
                setMouseRotation(20f, -30f)
                zoom = 40.0
                addNode(camera)
            }
        }
    }
}
