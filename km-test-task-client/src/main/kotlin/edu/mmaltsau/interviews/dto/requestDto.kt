package edu.mmaltsau.interviews.dto

import kotlinx.serialization.Serializable

@Serializable
data class CounterCreateRequestDto(var initialValue: Int) {}

@Serializable
data class CounterIncrementRequestDto(val count: Int?)
