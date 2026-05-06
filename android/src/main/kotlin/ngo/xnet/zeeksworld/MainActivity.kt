package ngo.xnet.zeeksworld

import android.app.Activity
import android.os.Bundle
import de.fabmax.kool.createDefaultKoolContext
import de.fabmax.kool.platform.KoolContextAndroid

class MainActivity : Activity() {
    private lateinit var koolCtx: KoolContextAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        koolCtx = createDefaultKoolContext()
        val game = ZeeksGame()
        koolCtx.scenes += game.createScene(koolCtx)
        koolCtx.run()
        setContentView(koolCtx.surfaceView)
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
