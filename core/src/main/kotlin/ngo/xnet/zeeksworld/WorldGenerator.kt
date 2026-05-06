package ngo.xnet.zeeksworld

import kotlin.math.*

object WorldGenerator {
    private const val WORLD_RADIUS = 50
    private const val METERS_PER_DEG_LAT = 111320.0

    fun generate(osm: OsmData, centerLat: Double, centerLon: Double, world: World) {
        val metersPerDegLon = METERS_PER_DEG_LAT * cos(centerLat * PI / 180)

        fun toBlock(ll: LatLon): Pair<Int, Int> {
            val dx = (ll.lon - centerLon) * metersPerDegLon / 2.0
            val dz = (ll.lat - centerLat) * METERS_PER_DEG_LAT / 2.0
            return dx.roundToInt() to dz.roundToInt()
        }

        fun inBounds(x: Int, z: Int) = x in -WORLD_RADIUS until WORLD_RADIUS && z in -WORLD_RADIUS until WORLD_RADIUS

        // Ground layer aligned to chunks
        val groundRadius = ((WORLD_RADIUS / CHUNK_SIZE) + 1) * CHUNK_SIZE
        for (x in -groundRadius until groundRadius)
            for (z in -groundRadius until groundRadius)
                world.setBlock(x, 0, z, Block.GRASS)

        // Water
        for (w in osm.water)
            fillPolygon(w.outline, ::toBlock, ::inBounds) { x, z -> world.setBlock(x, 1, z, Block.WATER) }

        // Parks
        for (p in osm.parks) {
            fillPolygon(p.outline, ::toBlock, ::inBounds) { x, z -> world.setBlock(x, 1, z, Block.GRASS) }
            if (p.outline.isNotEmpty()) {
                val (bx, bz) = toBlock(p.outline[0])
                for (dx in intArrayOf(-3, 3))
                    for (dz in intArrayOf(-3, 3))
                        if (inBounds(bx + dx, bz + dz)) placeTree(world, bx + dx, bz + dz)
            }
        }

        // Roads
        for (r in osm.roads) {
            val halfW = ceil(r.width / 2).toInt()
            for (i in 0 until r.points.size - 1) {
                val (x0, z0) = toBlock(r.points[i])
                val (x1, z1) = toBlock(r.points[i + 1])
                plotLine(x0, z0, x1, z1) { x, z ->
                    for (dx in -halfW..halfW)
                        for (dz in -halfW..halfW)
                            if (inBounds(x + dx, z + dz)) world.setBlock(x + dx, 1, z + dz, Block.STONE)
                }
            }
        }

        // Buildings
        for (b in osm.buildings) {
            val height = b.height.roundToInt().coerceIn(3, 5)
            for (i in 0 until b.outline.size - 1) {
                val (x0, z0) = toBlock(b.outline[i])
                val (x1, z1) = toBlock(b.outline[i + 1])
                plotLine(x0, z0, x1, z1) { x, z ->
                    if (!inBounds(x, z)) return@plotLine
                    for (y in 1..height)
                        world.setBlock(x, y, z, if (y > 1 && x % 2 == 0) Block.GLASS else Block.STONE)
                }
            }
            fillPolygon(b.outline, ::toBlock, ::inBounds) { x, z -> world.setBlock(x, 1, z, Block.WOOD) }
        }
    }

    private fun placeTree(world: World, x: Int, z: Int) {
        world.setBlock(x, 1, z, Block.WOOD)
        world.setBlock(x, 2, z, Block.WOOD)
        for (dx in -1..1) for (dz in -1..1) world.setBlock(x + dx, 3, z + dz, Block.LEAF)
    }

    private fun plotLine(x0: Int, z0: Int, x1: Int, z1: Int, fn: (Int, Int) -> Unit) {
        var cx = x0; var cz = z0
        val dx = abs(x1 - x0); val dz = abs(z1 - z0)
        val sx = if (x0 < x1) 1 else -1
        val sz = if (z0 < z1) 1 else -1
        var err = dx - dz
        while (true) {
            fn(cx, cz)
            if (cx == x1 && cz == z1) break
            val e2 = 2 * err
            if (e2 > -dz) { err -= dz; cx += sx }
            if (e2 < dx) { err += dx; cz += sz }
        }
    }

    private fun fillPolygon(
        outline: List<LatLon>,
        toBlock: (LatLon) -> Pair<Int, Int>,
        inBounds: (Int, Int) -> Boolean,
        fn: (Int, Int) -> Unit
    ) {
        if (outline.size < 3) return
        val pts = outline.map { toBlock(it) }
        val xs = pts.map { it.first }
        val zs = pts.map { it.second }
        for (z in zs.min()..zs.max())
            for (x in xs.min()..xs.max())
                if (inBounds(x, z) && pointInPolygon(x, z, pts)) fn(x, z)
    }

    private fun pointInPolygon(x: Int, z: Int, pts: List<Pair<Int, Int>>): Boolean {
        var inside = false
        var j = pts.size - 1
        for (i in pts.indices) {
            val (xi, zi) = pts[i]; val (xj, zj) = pts[j]
            if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi)
                inside = !inside
            j = i
        }
        return inside
    }
}
