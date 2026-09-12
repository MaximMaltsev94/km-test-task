package edu.mmaltsau.interviews.domain.greeting

import kotlinx.serialization.Serializable

@Serializable
data class PersonalGreeting(val name: String, val content: String)