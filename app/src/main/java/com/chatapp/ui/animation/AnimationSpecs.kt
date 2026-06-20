package com.chatapp.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally

/** Standard enter transition: slide + fade with tween, no spring bounce */
fun smoothEnter() = slideInVertically(
    initialOffsetY = { it / 8 },
    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearEasing))

/** Standard exit transition: slide + fade with tween, no spring bounce */
fun smoothExit() = slideOutVertically(
    targetOffsetY = { it / 8 },
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(durationMillis = 180, easing = LinearEasing))

/** Vertical expand with tween, no bounce */
fun smoothExpandVertically() = expandVertically(
    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
)

/** Vertical shrink with tween, no bounce */
fun smoothShrinkVertically() = shrinkVertically(
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
)

/** Horizontal expand with tween, no bounce */
fun smoothExpandHorizontally() = expandHorizontally(
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
)

/** Horizontal shrink with tween, no bounce */
fun smoothShrinkHorizontally() = shrinkHorizontally(
    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
)

/** Simple fade-in with tween */
fun smoothFadeIn() = fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearEasing))

/** Simple fade-out with tween */
fun smoothFadeOut() = fadeOut(animationSpec = tween(durationMillis = 150, easing = LinearEasing))
