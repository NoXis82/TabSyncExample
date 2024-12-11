package com.noxis.tabsyncsample.model

data class Category(
    val name: String,
    val subCategories: List<Category> = emptyList()
)
