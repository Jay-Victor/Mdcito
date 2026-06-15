package com.mdcito.app.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

object MdcitoTransitions {

    private const val FORWARD_DURATION = 350
    private const val BACKWARD_DURATION = 350
    private const val TAB_DURATION = 250
    private const val REDUCED_DURATION = 100

    fun forwardTransition(reduceMotion: Boolean = false): ContentTransform {
        val duration = if (reduceMotion) REDUCED_DURATION else FORWARD_DURATION
        return if (reduceMotion) {
            fadeIn(animationSpec = tween(duration)) togetherWith fadeOut(
                animationSpec = tween(duration),
                targetAlpha = 0.8f,
            )
        } else {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(duration),
            ) + fadeIn(
                animationSpec = tween(duration),
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth / 3) },
                animationSpec = tween(duration),
            ) + fadeOut(
                animationSpec = tween(duration),
                targetAlpha = 0.8f,
            )
        }
    }

    fun backwardTransition(reduceMotion: Boolean = false): ContentTransform {
        val duration = if (reduceMotion) REDUCED_DURATION else BACKWARD_DURATION
        return if (reduceMotion) {
            fadeIn(animationSpec = tween(duration)) togetherWith fadeOut(
                animationSpec = tween(duration),
            )
        } else {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth / 3) },
                animationSpec = tween(duration),
            ) + fadeIn(
                animationSpec = tween(duration),
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(duration),
            ) + fadeOut(
                animationSpec = tween(duration),
            )
        }
    }

    fun tabTransition(reduceMotion: Boolean = false): ContentTransform {
        val duration = if (reduceMotion) REDUCED_DURATION else TAB_DURATION
        return fadeIn(
            animationSpec = tween(duration),
        ) togetherWith fadeOut(
            animationSpec = tween(duration),
        )
    }

    fun AnimatedContentTransitionScope<*>.mdcitoEnterTransition(
        isForward: Boolean = true,
        reduceMotion: Boolean = false,
    ) = if (reduceMotion) {
        fadeIn(tween(REDUCED_DURATION))
    } else if (isForward) {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(FORWARD_DURATION),
        ) + fadeIn(tween(FORWARD_DURATION))
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(BACKWARD_DURATION),
        ) + fadeIn(tween(BACKWARD_DURATION))
    }

    fun AnimatedContentTransitionScope<*>.mdcitoExitTransition(
        isForward: Boolean = true,
        reduceMotion: Boolean = false,
    ) = if (reduceMotion) {
        fadeOut(tween(REDUCED_DURATION))
    } else if (isForward) {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(FORWARD_DURATION),
        ) + fadeOut(tween(FORWARD_DURATION), targetAlpha = 0.8f)
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(BACKWARD_DURATION),
        ) + fadeOut(tween(BACKWARD_DURATION))
    }

    fun AnimatedContentTransitionScope<*>.mdcitoPopEnterTransition(reduceMotion: Boolean = false) =
        mdcitoEnterTransition(isForward = false, reduceMotion = reduceMotion)

    fun AnimatedContentTransitionScope<*>.mdcitoPopExitTransition(reduceMotion: Boolean = false) =
        mdcitoExitTransition(isForward = false, reduceMotion = reduceMotion)
}
