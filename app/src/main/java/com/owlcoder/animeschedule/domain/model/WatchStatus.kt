package com.owlcoder.animeschedule.domain.model

enum class WatchStatus(val malValue: String) {
    WATCHING("watching"),
    COMPLETED("completed"),
    ON_HOLD("on_hold"),
    DROPPED("dropped"),
    PLAN_TO_WATCH("plan_to_watch"),
    NOT_IN_LIST("");

    companion object {
        fun fromMal(value: String): WatchStatus =
            entries.find { it.malValue == value } ?: NOT_IN_LIST
    }
}
