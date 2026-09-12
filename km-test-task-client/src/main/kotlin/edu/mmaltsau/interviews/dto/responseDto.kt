package edu.mmaltsau.interviews.dto

import kotlinx.serialization.Serializable

@Serializable
data class CounterV1ResponseDto(val name: String, val value: Int)