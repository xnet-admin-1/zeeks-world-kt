package ngo.xnet.zeeksworld

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.Time

class Hud {
    private var speechText = mutableStateOf("")
    private var speechTimer = 0f

    private val inventoryColors = listOf(
        MdColor.GREEN, MdColor.BROWN, MdColor.GREY,
        MdColor.BLUE, MdColor.AMBER, MdColor.RED
    )

    fun showSpeech(text: String) {
        speechText.set(text)
        speechTimer = 5f
    }

    fun createScene(ctx: KoolContext): Scene {
        return Scene("hud").apply {
            setupUiScene()

            // Crosshair
            addPanelSurface {
                modifier.size(Grow.Std, Grow.Std)
                Text("+") {
                    modifier
                        .align(AlignmentX.Center, AlignmentY.Center)
                        .font(sizes.largeText)
                        .textColor(Color.WHITE)
                }
            }

            // Inventory bar
            addPanelSurface {
                modifier.size(Grow.Std, Grow.Std)
                Row {
                    modifier.align(AlignmentX.Center, AlignmentY.Bottom).margin(bottom = 16.dp)
                    for (color in inventoryColors) {
                        Box {
                            modifier
                                .size(40.dp, 40.dp)
                                .margin(horizontal = 4.dp)
                                .background(RoundRectBackground(color, 4.dp))
                        }
                    }
                }
            }

            // Speech bubble
            addPanelSurface {
                modifier.size(Grow.Std, Grow.Std)
                val text = remember { speechText }
                if (text.value.isNotEmpty()) {
                    Box {
                        modifier
                            .align(AlignmentX.Center, AlignmentY.Top)
                            .margin(top = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(RoundRectBackground(Color(0f, 0f, 0f, 0.7f), 8.dp))
                        Text(text.value) {
                            modifier.textColor(Color.WHITE)
                        }
                    }
                }
            }

            onUpdate {
                if (speechTimer > 0f) {
                    speechTimer -= Time.deltaT
                    if (speechTimer <= 0f) {
                        speechText.set("")
                    }
                }
            }
        }
    }
}
