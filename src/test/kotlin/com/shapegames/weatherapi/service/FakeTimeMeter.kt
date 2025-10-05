package com.shapegames.weatherapi.service

import io.github.bucket4j.TimeMeter


class FakeTimeMeter : TimeMeter {
    private var currentTimeNanos: Long = 0

    override fun currentTimeNanos(): Long = currentTimeNanos
    override fun isWallClockBased(): Boolean = true

    fun addMillis(millis: Long) {
        currentTimeNanos += millis * 1_000_000
    }
}
