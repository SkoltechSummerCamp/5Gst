package ru.scoltech.openran.speedtest.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.R

class NetworkInfoTab : Fragment() {

    private lateinit var consoleTextView: TextView

    private lateinit var consoleScrollView: ScrollView

    private lateinit var listenerPermissionRequester: ActivityResultLauncher<Array<String>>

    private lateinit var telephonyManager: TelephonyManager

    private var networkInfoUpdaterJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.network_info_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        consoleTextView = view.findViewById(R.id.console_layout_text)
        consoleScrollView = view.findViewById(R.id.console_layout_scroll)

        listenerPermissionRequester = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if (permissions.values.all { it }) {
                launchNetworkInfoUpdaterJob()
            } else {
                showLoadError("Not enough permissions")
            }
        }

        telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: run {
                showLoadError("Android Telephony Manager is not available")
                return
            }

        launchNetworkInfoUpdaterJob()
    }

    override fun onDestroyView() {
        runBlocking {
            networkInfoUpdaterJob?.cancel()
        }
        super.onDestroyView()
    }

    private fun launchNetworkInfoUpdaterJob() {
        networkInfoUpdaterJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                requestNetworkInfoUpdate()
                delay(1000L)
            }
        }
    }

    private fun requestNetworkInfoUpdate() {
        val activity = requireActivity()

        val accessFineLocationPermissionState =
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val readPhoneStatePermissionState =
            activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)

        if (accessFineLocationPermissionState != PackageManager.PERMISSION_GRANTED
            || readPhoneStatePermissionState != PackageManager.PERMISSION_GRANTED
        ) {
            listenerPermissionRequester.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                )
            )
            runBlocking {
                networkInfoUpdaterJob?.cancel()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager.requestCellInfoUpdate(
                Dispatchers.Main.asExecutor(),
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        updateNetworkInfo(cellInfo)
                    }

                    override fun onError(errorCode: Int, detail: Throwable?) {
                        val detailMessage = detail?.let {
                            ", detail: ${it.message}"
                        }
                        appendLineToConsole(
                            "Could not update cell info, " +
                                    "error code is $errorCode$detailMessage"
                        )
                    }
                },
            )
        } else {
            updateNetworkInfo(telephonyManager.allCellInfo)
        }
    }

    private fun updateNetworkInfo(cellsInfo: List<CellInfo>) {
        val scrollPosition = consoleScrollView.scrollX to consoleScrollView.scrollY
        clearConsole()

        if (cellsInfo.isEmpty()) {
            showLoadError(
                "No cell info found. " +
                        "Probably, you should turn on location access in your status bar."
            )
            return
        }

        cellsInfo.filter { it.isRegistered }.forEach { cellInfo ->
            when {
                cellInfo is CellInfoGsm -> showGsmCellInfo(cellInfo)
                cellInfo is CellInfoCdma -> showCdmaCellInfo(cellInfo)
                cellInfo is CellInfoLte -> showLteCellInfo(cellInfo)
                cellInfo is CellInfoWcdma -> showWcdmaCellInfo(cellInfo)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                    showTdscdmaCellInfo(cellInfo)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr ->
                    showNrCellInfo(cellInfo)
                else -> appendLineToConsole("Unknown cell info ${cellInfo::class}")
            }
            appendLineToConsole("")
        }

        consoleTextView.post {
            consoleScrollView.scrollTo(scrollPosition.first, scrollPosition.second)
        }
    }

    private fun showCommonCellSignalStrength(cellSignalStrength: CellSignalStrength) {
        appendMeasurementToConsole("asu level") {
            cellSignalStrength.asuLevel.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole("dBm", unit = "dBm") {
            cellSignalStrength.dbm.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole("signal level") {
            when (cellSignalStrength.level) {
                CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN -> "none or unknown"
                CellSignalStrength.SIGNAL_STRENGTH_POOR -> "poor"
                CellSignalStrength.SIGNAL_STRENGTH_MODERATE -> "moderate"
                CellSignalStrength.SIGNAL_STRENGTH_GOOD -> "good"
                CellSignalStrength.SIGNAL_STRENGTH_GREAT -> "great"
                else -> "unrecognized value (it is a bug)"
            }
        }
    }

    private fun showGsmCellInfo(cellInfo: CellInfoGsm) {
        appendLineToConsole("cell type: GSM")
        val signalStrength = cellInfo.cellSignalStrength
        showCommonCellSignalStrength(signalStrength)
    }

    private fun showLteCellInfo(cellInfo: CellInfoLte) {
        appendLineToConsole("cell type: LTE")

        val identity = cellInfo.cellIdentity
        appendMeasurementToConsole("mcc") {
            identity.mcc.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole("mnc") {
            identity.mnc.takeIfAvbailable()?.toString()
        }

        val signalStrength = cellInfo.cellSignalStrength
        showCommonCellSignalStrength(signalStrength)

        appendMeasurementToConsole(
            "cqi table index",
            minAndroidVersion = Build.VERSION_CODES.S,
        ) @SuppressLint("NewApi") {
            signalStrength.cqiTableIndex.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "rssi",
            unit = "dBm",
            minAndroidVersion = Build.VERSION_CODES.Q,
        ) @SuppressLint("NewApi") {
            signalStrength.rssi.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "cqi",
            minAndroidVersion = Build.VERSION_CODES.O,
        ) @SuppressLint("NewApi") {
            signalStrength.cqi.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "rsrp",
            unit = "dBm",
            minAndroidVersion = Build.VERSION_CODES.O,
        ) @SuppressLint("NewApi") {
            signalStrength.rsrp.toString()
        }
        appendMeasurementToConsole(
            "rsrq",
            minAndroidVersion = Build.VERSION_CODES.O,
        ) @SuppressLint("NewApi") {
            signalStrength.rsrq.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "rssnr",
            minAndroidVersion = Build.VERSION_CODES.O,
        ) @SuppressLint("NewApi") {
            signalStrength.rssnr.takeIfAvbailable()?.toString()
        }
    }

    private fun showCdmaCellInfo(cellInfo: CellInfoCdma) {
        appendLineToConsole("cell type: CDMA")
        val signalStrength = cellInfo.cellSignalStrength
        showCommonCellSignalStrength(signalStrength)
    }

    private fun showWcdmaCellInfo(cellInfo: CellInfoWcdma) {
        appendLineToConsole("cell type: WCDMA")
        val signalStrength = cellInfo.cellSignalStrength
        showCommonCellSignalStrength(signalStrength)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showTdscdmaCellInfo(cellInfo: CellInfoTdscdma) {
        appendLineToConsole("cell type: TDSCDMA")
        val signalStrength = cellInfo.cellSignalStrength
        showCommonCellSignalStrength(signalStrength)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showNrCellInfo(cellInfo: CellInfoNr) {
        appendLineToConsole("cell type: TDSCDMA")

        val identity = cellInfo.cellIdentity as CellIdentityNr
        appendMeasurementToConsole("mcc") {
            identity.mccString
        }
        appendMeasurementToConsole("mnc") {
            identity.mncString
        }

        val signalStrength = cellInfo.cellSignalStrength as CellSignalStrengthNr
        showCommonCellSignalStrength(signalStrength)

        appendMeasurementToConsole(
            "CSI CQI for all subbands",
            minAndroidVersion = Build.VERSION_CODES.S,
        ) @SuppressLint("NewApi") {
            signalStrength.csiCqiReport.toString()
        }
        appendMeasurementToConsole(
            "CSI CQI table index",
            minAndroidVersion = Build.VERSION_CODES.S,
        ) @SuppressLint("NewApi") {
            signalStrength.csiCqiTableIndex.takeIfAvbailable()?.toString()
        }

        appendMeasurementToConsole(
            "csi rsrp",
            unit = "dBm",
        ) {
            signalStrength.csiRsrp.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "csi rsrq",
            unit = "dBm",
        ) {
            signalStrength.csiRsrq.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "csi sinr",
            unit = "dBm",
        ) {
            signalStrength.csiSinr.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "ss rsrp",
            unit = "dBm",
        ) {
            signalStrength.ssRsrp.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "ss rsrq",
            unit = "dBm",
        ) {
            signalStrength.ssRsrq.takeIfAvbailable()?.toString()
        }
        appendMeasurementToConsole(
            "ss sinr",
            unit = "dBm",
        ) {
            signalStrength.ssSinr.takeIfAvbailable()?.toString()
        }
    }

    private fun Int.takeIfAvbailable(): Int? = if (this == CellInfo.UNAVAILABLE) null else this

    private fun showLoadError(error: String) {
        clearConsole()
        appendLineToConsole(error)
    }

    private fun appendMeasurementToConsole(
        name: String,
        unit: String = "",
        minAndroidVersion: Int? = null,
        getMeasurement: () -> String?,
    ) {
        val currentApiVersion = Build.VERSION.SDK_INT
        if (minAndroidVersion != null && currentApiVersion < minAndroidVersion) {
            appendLineToConsole(
                "$name: not supported in Android API $currentApiVersion " +
                        "(needs at least $minAndroidVersion)"
            )
            return
        }

        val measurement = getMeasurement()
        if (measurement == null) {
            appendLineToConsole("$name: unavailable (Android did not provide)")
            return
        }

        appendLineToConsole("$name: $measurement $unit")
    }

    private fun appendLineToConsole(line: String) {
        consoleTextView.append(line)
        consoleTextView.append(System.lineSeparator())
    }

    private fun clearConsole() {
        consoleTextView.text = ""
    }
}
