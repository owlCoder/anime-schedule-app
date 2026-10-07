package com.owlcoder.animeschedule.domain.model

enum class CharacterRole { ALL, MAIN, SUPPORTING, BACKGROUND }
fun List<Character>.findCharacters(query: String, role: CharacterRole): List<Character> {
    val search = query.trim()
    return filter { character ->
        (search.isBlank() || character.name.contains(search, true) || character.nativeName?.contains(search, true) == true) &&
            (role == CharacterRole.ALL || character.role.equals(role.name, true))
    }
}
