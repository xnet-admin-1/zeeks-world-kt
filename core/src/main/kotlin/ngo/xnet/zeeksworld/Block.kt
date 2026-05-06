package ngo.xnet.zeeksworld

enum class Block(val color: FloatArray, val solid: Boolean = true) {
    AIR(floatArrayOf(0f, 0f, 0f, 0f), solid = false),
    GRASS(floatArrayOf(0.3f, 0.8f, 0.2f, 1f)),
    DIRT(floatArrayOf(0.4f, 0.25f, 0.1f, 1f)),
    STONE(floatArrayOf(0.5f, 0.5f, 0.5f, 1f)),
    WOOD(floatArrayOf(0.55f, 0.35f, 0.15f, 1f)),
    LEAF(floatArrayOf(0.1f, 0.6f, 0.1f, 1f)),
    SAND(floatArrayOf(0.9f, 0.85f, 0.6f, 1f)),
    WATER(floatArrayOf(0.2f, 0.4f, 0.9f, 0.8f), solid = false),
    AMETHYST(floatArrayOf(0.6f, 0.2f, 0.8f, 1f)),
    RUBY(floatArrayOf(0.8f, 0.1f, 0.1f, 1f)),
    CLOUD(floatArrayOf(1f, 1f, 1f, 0.9f)),
    GLOW(floatArrayOf(1f, 0.9f, 0.3f, 1f)),
    GLASS(floatArrayOf(0.7f, 0.8f, 0.9f, 0.5f));
}
