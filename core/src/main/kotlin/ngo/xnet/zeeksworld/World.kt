package ngo.xnet.zeeksworld

data class ChunkPos(val x: Int, val z: Int)

class World {
    val chunks = mutableMapOf<ChunkPos, Chunk>()

    fun getBlock(x: Int, y: Int, z: Int): Block {
        val cx = if (x >= 0) x / CHUNK_SIZE else (x + 1) / CHUNK_SIZE - 1
        val cz = if (z >= 0) z / CHUNK_SIZE else (z + 1) / CHUNK_SIZE - 1
        val chunk = chunks[ChunkPos(cx, cz)] ?: return Block.AIR
        val lx = x - cx * CHUNK_SIZE
        val lz = z - cz * CHUNK_SIZE
        if (y < 0 || y >= CHUNK_SIZE) return Block.AIR
        return chunk.getBlock(lx, y, lz)
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Block) {
        val cx = if (x >= 0) x / CHUNK_SIZE else (x + 1) / CHUNK_SIZE - 1
        val cz = if (z >= 0) z / CHUNK_SIZE else (z + 1) / CHUNK_SIZE - 1
        val chunk = chunks.getOrPut(ChunkPos(cx, cz)) { Chunk() }
        val lx = x - cx * CHUNK_SIZE
        val lz = z - cz * CHUNK_SIZE
        chunk.setBlock(lx, y, lz, block)
    }

    fun generateFlat(radius: Int) {
        for (x in -radius until radius) {
            for (z in -radius until radius) {
                setBlock(x, 0, z, Block.GRASS)
            }
        }
    }
}
