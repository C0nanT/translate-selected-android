package com.conan.tradutor

data class Size(val width: Int, val height: Int)
data class Position(val x: Int, val y: Int)

// Keeps the window inside the screen. Pure function, used by the popup and the bubble.
// centeredX = false: x is the distance from the left edge (bubble, gravity TOP|START).
// centeredX = true: x is the offset from the center (popup, gravity BOTTOM|CENTER_HORIZONTAL).
// y always goes from 0 to (screen - window): distance from the top (bubble) or the bottom (popup).
fun clampPosition(pos: Position, screen: Size, window: Size, centeredX: Boolean): Position {
    val slackX = maxOf(0, screen.width - window.width)
    val x = if (centeredX) pos.x.coerceIn(-slackX / 2, slackX / 2) else pos.x.coerceIn(0, slackX)
    val y = pos.y.coerceIn(0, maxOf(0, screen.height - window.height))
    return Position(x, y)
}
