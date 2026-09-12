package edu.mmaltsau.interviews.domain.dictionary.model

import kotlinx.serialization.Serializable

@Serializable
data class Counter(val name: String, val value: Int)