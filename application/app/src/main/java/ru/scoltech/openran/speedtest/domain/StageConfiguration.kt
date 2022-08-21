package ru.scoltech.openran.speedtest.domain

data class StageConfiguration(
    val name: String,
    val serverArgs: String,
    val deviceArgs: String,
) {
    companion object {
        val EMPTY = StageConfiguration("", "", "")
    }
}
