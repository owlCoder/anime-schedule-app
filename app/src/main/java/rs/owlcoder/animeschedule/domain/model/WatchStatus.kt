package rs.owlcoder.animeschedule.domain.model

enum class WatchStatus(val malValue: String, val displayName: String) {
    WATCHING("watching", "Gledam"),
    COMPLETED("completed", "Završeno"),
    ON_HOLD("on_hold", "Pauzirao"),
    DROPPED("dropped", "Dropovao"),
    PLAN_TO_WATCH("plan_to_watch", "Planiram"),
    NOT_IN_LIST("", "Nije u listi");

    companion object {
        fun fromMal(value: String): WatchStatus =
            entries.find { it.malValue == value } ?: NOT_IN_LIST
    }
}
