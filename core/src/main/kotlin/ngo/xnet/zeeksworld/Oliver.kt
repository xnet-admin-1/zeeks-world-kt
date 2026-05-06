package ngo.xnet.zeeksworld

import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.util.Color
import kotlin.math.sin
import kotlin.math.sqrt

class Oliver(startPos: Vec3f = Vec3f(3f, 1f, 3f)) {
    enum class State { IDLE, FOLLOWING, CELEBRATING, SLEEPING }

    val position = MutableVec3f(startPos)
    var state = State.IDLE
    private var time = 0f
    private var lastSpeakTime = -10f

    fun update(dt: Float, playerPos: Vec3f) {
        time += dt
        val dx = playerPos.x - position.x
        val dz = playerPos.z - position.z
        val dist = sqrt(dx * dx + dz * dz)

        state = if (dist > 3f) State.FOLLOWING else State.IDLE

        when (state) {
            State.FOLLOWING -> {
                val speed = 4f * dt
                val nx = dx / dist
                val nz = dz / dist
                position.x += nx * speed
                position.z += nz * speed
                position.y = playerPos.y + sin(time * 6f) * 0.15f
            }
            State.IDLE -> {
                position.y = playerPos.y + sin(time * 2f) * 0.2f
            }
            else -> {}
        }
    }

    fun <T : de.fabmax.kool.util.Struct> buildMesh(builder: MeshBuilder<T>) {
        val ox = position.x; val oy = position.y; val oz = position.z; val s = 0.5f
        // Body
        builder.color = Color(1f, 0.6f, 0.2f, 1f)
        for (bx in 0..1) for (by in 0..2) for (bz in 0..1) {
            builder.cube { origin.set(ox + bx * s, oy + by * s, oz + bz * s); size.set(s, s, s) }
        }
        // Head
        builder.color = Color(1f, 0.7f, 0.3f, 1f)
        for (bx in 0..1) for (by in 0..1) for (bz in 0..1) {
            builder.cube { origin.set(ox + bx * s, oy + 3 * s + by * s, oz + bz * s); size.set(s, s, s) }
        }
        // Ears
        builder.color = Color(1f, 0.5f, 0.1f, 1f)
        builder.cube { origin.set(ox, oy + 5 * s, oz); size.set(s, s, s) }
        builder.cube { origin.set(ox + s, oy + 5 * s, oz); size.set(s, s, s) }
        // Tail
        builder.color = Color(1f, 0.6f, 0.2f, 1f)
        builder.cube { origin.set(ox - s, oy, oz); size.set(s, s, s) }
        builder.cube { origin.set(ox - 2 * s, oy + s * 0.5f, oz); size.set(s, s, s) }
    }

    fun shouldSpeak(): Boolean {
        if (time - lastSpeakTime >= 10f) {
            lastSpeakTime = time
            return true
        }
        return false
    }
}
