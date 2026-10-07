package com.owlcoder.animeschedule.domain.model

import java.util.Locale

private val excludedRelations = setOf("CHARACTER", "CONTAINS", "OTHER")

enum class CharacterRole { ALL, MAIN, SUPPORTING, BACKGROUND }
fun List<Character>.findCharacters(query: String, role: CharacterRole): List<Character> {
    val search = query.trim()
    return filter { character ->
        (search.isBlank() || character.name.contains(search, true) || character.nativeName?.contains(search, true) == true) &&
            (role == CharacterRole.ALL || character.role.equals(role.name, true))
    }
}

/** Search only navigable loaded anime relations; duplicate graph edges produce one result. */
fun List<RelatedAnime>.findRelatedAnime(query: String = "", relationType: String? = null): List<RelatedAnime> {
    val search = query.trim()
    return filter { item ->
        item.animeId != 0 && (item.mediaType == null || item.mediaType.equals("ANIME", true)) &&
            item.relationType?.uppercase(Locale.ROOT) !in excludedRelations &&
            (search.isEmpty() || item.title.contains(search, true)) &&
            (relationType == null || item.relationType.equals(relationType, true))
    }.distinctBy { it.animeId }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}
