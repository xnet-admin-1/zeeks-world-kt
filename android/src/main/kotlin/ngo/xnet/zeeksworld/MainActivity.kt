package ngo.xnet.zeeksworld

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import de.fabmax.kool.createDefaultKoolContext
import de.fabmax.kool.platform.KoolContextAndroid

class MainActivity : Activity() {
    lateinit var koolCtx: KoolContextAndroid
    val game = ZeeksGame()

    // Movement state from buttons

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        koolCtx = createDefaultKoolContext()
        game.createScenes(koolCtx).forEach { koolCtx.scenes += it }

        // Create layout with GL view + button overlay
        val root = FrameLayout(this)
        root.addView(koolCtx.surfaceView)

        // D-pad + jump buttons
        val btnSize = 120
        val margin = 20

        // Forward
        addButton(root, "▲", margin + btnSize, margin, btnSize) { game.btnForward = it }
        // Back
        addButton(root, "▼", margin + btnSize, margin + btnSize * 2, btnSize) { game.btnBack = it }
        // Left
        addButton(root, "◀", margin, margin + btnSize, btnSize) { game.btnLeft = it }
        // Right
        addButton(root, "▶", margin + btnSize * 2, margin + btnSize, btnSize) { game.btnRight = it }
        // Jump (right side)
        addButton(root, "⬆", -margin - btnSize, margin + btnSize, btnSize, rightAlign = true) { game.btnJump = it }

        setContentView(root)
        koolCtx.run()

        // Feed button state into game
        
    }

    private fun addButton(parent: FrameLayout, text: String, x: Int, y: Int, size: Int, rightAlign: Boolean = false, onState: (Boolean) -> Unit) {
        val btn = Button(this).apply {
            this.text = text
            textSize = 24f
            alpha = 0.6f
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { onState(true); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { onState(false); true }
                    else -> false
                }
            }
        }
        val lp = FrameLayout.LayoutParams(size, size)
        lp.gravity = Gravity.BOTTOM or (if (rightAlign) Gravity.END else Gravity.START)
        lp.leftMargin = if (!rightAlign) x else 0
        lp.rightMargin = if (rightAlign) -x else 0
        lp.bottomMargin = y
        parent.addView(btn, lp)
    }

    override fun onResume() { super.onResume(); koolCtx.onResume() }
    override fun onPause() { super.onPause(); koolCtx.onPause() }
    override fun onDestroy() { koolCtx.onDestroy(); super.onDestroy() }
}
