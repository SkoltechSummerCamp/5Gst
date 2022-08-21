package ru.scoltech.openran.speedtest.parser

import android.content.SharedPreferences
import android.util.Log
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.domain.StageConfiguration

class StageConfigurationParser {
    companion object {
        private val LOG_TAG = StageConfigurationParser::class.simpleName
    }

    fun deserializeStage(stagePresentation: String?): StageConfiguration {
        if (stagePresentation == null) {
            return StageConfiguration.EMPTY
        }

        val slashNsCount = stagePresentation.count { it == '\n' }
        if (slashNsCount != 2) {
            Log.e(
                LOG_TAG,
                "Configuration string '$stagePresentation' " +
                        "should have exactly 2 \\n symbols",
            )
            return StageConfiguration.EMPTY
        }

        val (name, serverArgs, deviceArgs) = stagePresentation.split('\n')
        return StageConfiguration(name, serverArgs, deviceArgs)
    }

    fun serializeStage(stageConfiguration: StageConfiguration): String {
        val (name, serverArgs, deviceArgs) = stageConfiguration
        return "$name\n$serverArgs\n$deviceArgs"
    }

    fun parseFromPreferences(
        pipelinePreferences: SharedPreferences,
        getString: (Int) -> String,
    ): List<FromPreferencesStageConfiguration> {
        val downloadStageConfiguration = StageConfiguration(
            "Download Speed Test",
            getString(R.string.download_server_iperf_args),
            getString(R.string.download_device_iperf_args),
        )
        val uploadStageConfiguration = StageConfiguration(
            "Upload Speed Test",
            getString(R.string.upload_server_iperf_args),
            getString(R.string.upload_device_iperf_args),
        )

        val stageCount = 5
        return List(stageCount) { index ->
            val preferencesKey = "stage$index"
            val serializedStage = pipelinePreferences.getString(preferencesKey, null)
            if (index == 0 && serializedStage == null) {
                FromPreferencesStageConfiguration(preferencesKey, downloadStageConfiguration)
            } else if (index == 1 && serializedStage == null) {
                FromPreferencesStageConfiguration(preferencesKey, uploadStageConfiguration)
            } else {
                FromPreferencesStageConfiguration(preferencesKey, deserializeStage(serializedStage))
            }
        }
    }

    data class FromPreferencesStageConfiguration(
        val preferencesKey: String,
        val stageConfiguration: StageConfiguration,
    )
}
