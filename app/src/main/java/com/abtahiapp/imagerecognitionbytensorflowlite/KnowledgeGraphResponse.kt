package com.abtahiapp.imagerecognitionbytensorflowlite

data class KnowledgeGraphResponse(
    val itemListElement: List<ItemListElement>
)

data class ItemListElement(
    val result: Result
)

data class Result(
    val name: String,
    val description: String,
    val detailedDescription: DetailedDescription?
)

data class DetailedDescription(
    val articleBody: String
)
