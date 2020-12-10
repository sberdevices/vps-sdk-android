package ru.arvrlab.vps.service

data class Settings(
    val url: String,
    val locationID: String,
    var onlyForce: Boolean = true,
    val timerInterval: Long,
    val needLocation: Boolean
)