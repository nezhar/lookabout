package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Walk

/** The lock-screen notice during a walk: the current picture's text and the position, such as "2 of 5". */
data class WalkNotice(val picture: String, val position: String)

/**
 * The notice to show for [walk] on [screen], or null when there is none: no walk, a screen other
 * than Walk (Summary, or Setup after New walk), or a finished walk.
 */
fun walkNotice(walk: Walk?, screen: Screen): WalkNotice? {
    if (walk == null || screen != Screen.Walk || walk.isFinished) return null
    val shot = walk.current ?: return null
    return WalkNotice(picture = shot.text, position = positionText(walk.currentIndex, walk.shots.size))
}

/**
 * True when the current picture waits for its photo: it has none and the camera is not already
 * taking one. A tap on the notice then opens the camera straight away.
 */
fun Walk.waitsForPhoto(): Boolean = pendingPhotoPath == null && current?.hasPhoto == false
