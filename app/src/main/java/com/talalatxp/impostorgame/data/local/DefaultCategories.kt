package com.talalatxp.impostorgame.data.local

import com.talalatxp.impostorgame.domain.model.Category
import com.talalatxp.impostorgame.domain.model.WordItem

object DefaultCategories {
    val items = listOf(
        category("art", "Arte y espectáculo", "Escenarios, películas y artes visuales", "Teatro" to "Máscara", "Cine" to "Butaca", "Circo" to "Carpa", "Mago" to "Chistera"),
        category("food", "Gastronomía", "Sabores y platos del mundo", "Pizza" to "Masa", "Paella" to "Azafrán", "Sushi" to "Palillos", "Taco" to "Picante"),
        category("places", "Lugares", "Espacios que todos reconocemos", "Hospital" to "Blanco", "Aeropuerto" to "Maleta", "Playa" to "Marea", "Biblioteca" to "Silencio"),
        category("objects", "Objetos cotidianos", "Cosas presentes en el día a día", "Paraguas" to "Gota", "Reloj" to "Segundero", "Brújula" to "Aguja", "Espejo" to "Reflejo"),
        category("jobs", "Profesiones", "Oficios y vocaciones", "Astronauta" to "Gravedad", "Detective" to "Lupa", "Juez" to "Mazo", "Arqueólogo" to "Pincel"),
    )

    private fun category(id: String, name: String, description: String, vararg entries: Pair<String, String>) =
        Category(id, name, description, entries.mapIndexed { index, (word, clue) -> WordItem("$id-$index", word, clue) })
}

