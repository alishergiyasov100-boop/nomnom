package com.korvus.nomnom.data

import kotlinx.serialization.Serializable

@Serializable
data class FoodEntry(
    val id: String,
    val timestamp: Long,            // epoch millis
    val dish: String,
    val kcal: Int,
    val proteinG: Int = 0,
    val fatG: Int = 0,
    val carbsG: Int = 0,
    val comment: String = "",
    val confidence: String = "medium",
    val imagePath: String? = null,  // local cached image
    val meal: String? = null,       // Завтрак / Обед / Ужин / Перекус; null = авто по часу
)

@Serializable
data class AnalysisResult(
    val dish: String,
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val comment: String,
    val confidence: String,
    val isFood: Boolean = true,
    val components: List<Component> = emptyList(),
    val description: String = "",
)

@Serializable
data class Component(
    val name: String,
    val grams: Int,
    val kcal: Int,
    val proteinG: Int = 0,
    val fatG: Int = 0,
    val carbsG: Int = 0,
)
