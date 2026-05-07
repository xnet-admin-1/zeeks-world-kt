package ngo.xnet.zeeksworld

import android.app.Activity
import android.os.Bundle
import de.fabmax.kool.createDefaultKoolContext
import de.fabmax.kool.platform.KoolContextAndroid

class MainActivity : Activity() {
    lateinit var koolCtx: KoolContextAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        koolCtx = createDefaultKoolContext()
        val game = ZeeksGame()
        koolCtx.scenes += game.createScene(koolCtx)
        setContentView(koolCtx.surfaceView)
        koolCtx.run()
    }

    override fun onResume() {
        super.onResume()
        koolCtx.onResume()
    }

    override fun onPause() {
        super.onPause()
        koolCtx.onPause()
    }

    override fun onDestroy() {
        koolCtx.onDestroy()
        super.onDestroy()
    }
}
