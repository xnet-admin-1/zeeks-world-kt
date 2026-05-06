package ngo.xnet.zeeksworld

const val CHUNK_SIZE = 16

class Chunk {
    val blocks = ByteArray(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE)

    fun getBlock(x: Int, y: Int, z: Int): Block {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) return Block.AIR
        val id = blocks[x + y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE].toInt() and 0xFF
        return Block.entries.getOrElse(id) { Block.AIR }
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) return
        blocks[x + y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE] = block.ordinal.toByte()
    }
}
