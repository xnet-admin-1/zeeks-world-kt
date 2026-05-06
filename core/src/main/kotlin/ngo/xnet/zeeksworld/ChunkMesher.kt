package ngo.xnet.zeeksworld

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder

object ChunkMesher {
    private val FACE_NORMALS = arrayOf(
        Vec3f(1f, 0f, 0f), Vec3f(-1f, 0f, 0f),
        Vec3f(0f, 1f, 0f), Vec3f(0f, -1f, 0f),
        Vec3f(0f, 0f, 1f), Vec3f(0f, 0f, -1f)
    )

    private val FACE_OFFSETS = arrayOf(
        intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0),
        intArrayOf(0, 1, 0), intArrayOf(0, -1, 0),
        intArrayOf(0, 0, 1), intArrayOf(0, 0, -1)
    )

    // CCW winding for each face (verified for standard OpenGL front-face)
    private val FACE_VERTS = arrayOf(
        // +X
        arrayOf(Vec3f(1f,0f,1f), Vec3f(1f,0f,0f), Vec3f(1f,1f,0f), Vec3f(1f,1f,1f)),
        // -X
        arrayOf(Vec3f(0f,0f,0f), Vec3f(0f,0f,1f), Vec3f(0f,1f,1f), Vec3f(0f,1f,0f)),
        // +Y
        arrayOf(Vec3f(0f,1f,1f), Vec3f(1f,1f,1f), Vec3f(1f,1f,0f), Vec3f(0f,1f,0f)),
        // -Y
        arrayOf(Vec3f(0f,0f,0f), Vec3f(1f,0f,0f), Vec3f(1f,0f,1f), Vec3f(0f,0f,1f)),
        // +Z
        arrayOf(Vec3f(0f,0f,1f), Vec3f(1f,0f,1f), Vec3f(1f,1f,1f), Vec3f(0f,1f,1f)),
        // -Z
        arrayOf(Vec3f(1f,0f,0f), Vec3f(0f,0f,0f), Vec3f(0f,1f,0f), Vec3f(1f,1f,0f))
    )

    fun buildMesh(chunk: Chunk, pos: ChunkPos, world: World): Mesh {
        val mesh = Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.COLORS)
        val builder = MeshBuilder(mesh.geometry)

        val ox = pos.x * CHUNK_SIZE
        val oz = pos.z * CHUNK_SIZE

        for (x in 0 until CHUNK_SIZE) {
            for (y in 0 until CHUNK_SIZE) {
                for (z in 0 until CHUNK_SIZE) {
                    val block = chunk.getBlock(x, y, z)
                    if (block == Block.AIR) continue

                    for (face in 0..5) {
                        val nx = x + FACE_OFFSETS[face][0]
                        val ny = y + FACE_OFFSETS[face][1]
                        val nz = z + FACE_OFFSETS[face][2]

                        val neighbor = world.getBlock(ox + nx, ny, oz + nz)
                        if (neighbor.solid) continue

                        val normal = FACE_NORMALS[face]
                        val verts = FACE_VERTS[face]
                        val c = block.color

                        builder.color.set(c[0], c[1], c[2], c[3])

                        val i0 = builder.vertex(Vec3f(ox + x + verts[0].x, y + verts[0].y, oz + z + verts[0].z), normal)
                        val i1 = builder.vertex(Vec3f(ox + x + verts[1].x, y + verts[1].y, oz + z + verts[1].z), normal)
                        val i2 = builder.vertex(Vec3f(ox + x + verts[2].x, y + verts[2].y, oz + z + verts[2].z), normal)
                        val i3 = builder.vertex(Vec3f(ox + x + verts[3].x, y + verts[3].y, oz + z + verts[3].z), normal)

                        builder.geometry.addTriIndices(i0, i1, i2)
                        builder.geometry.addTriIndices(i0, i2, i3)
                    }
                }
            }
        }
        return mesh
    }
}
