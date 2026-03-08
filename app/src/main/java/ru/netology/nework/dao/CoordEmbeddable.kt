package ru.netology.nework.dao

import ru.netology.nework.dto.Coordinates

data class CoordEmbeddable(
    val latitude : String?,
    val longitude : String?,
) {
    fun toDto() =
        Coordinates(latitude, longitude)

    companion object {
        fun fromDto(dto: Coordinates?) = dto?.let {
            CoordEmbeddable(it.lat, it.lng)
        }
    }
}