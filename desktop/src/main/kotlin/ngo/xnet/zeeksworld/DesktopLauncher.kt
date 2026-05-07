package ngo.xnet.zeeksworld

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm

fun main() = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = "Zeek's World"
    )
) {
    val game = ZeeksGame()
    game.createScenes(ctx).forEach { ctx.scenes += it }
}
