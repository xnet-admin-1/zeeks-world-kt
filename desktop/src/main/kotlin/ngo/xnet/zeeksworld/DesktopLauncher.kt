package ngo.xnet.zeeksworld

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig

fun main() {
    KoolApplication(
        KoolConfig(
            windowTitle = "Zeek's World",
            windowWidth = 1280,
            windowHeight = 720
        )
    ) { ctx ->
        val game = ZeeksGame()
        ctx.scenes += game.createScene(ctx)
    }
}
