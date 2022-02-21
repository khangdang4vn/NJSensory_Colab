package edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApi.DeviceStreamingFeature
import com.polar.sdk.api.model.PolarHrBroadcastData
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import edu.ucsd.calab.extrasensory.ESApplication
import edu.ucsd.calab.extrasensory.R

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.core.Observable
import java.util.*
import android.content.Intent

import edu.ucsd.calab.extrasensory.ui.MainActivity
import android.R.array
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Supplier
import android.R.string
import android.annotation.SuppressLint
import android.app.AlertDialog
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable.*
import io.reactivex.rxjava3.internal.util.NotificationLite.getValue
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.AsyncSubject
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.Flow
import javax.security.auth.Subject
import kotlin.collections.ArrayList
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.Menu
import androidx.core.content.ContextCompat.startActivity
import com.polar.sdk.impl.BDBleApiImpl
import edu.ucsd.calab.extrasensory.data.*
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity.Companion.api
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity.Companion.isPolarConnected
import edu.ucsd.calab.extrasensory.ui.BaseActivity
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.properties.Delegates


class PolarActivity : BaseActivity() {
    companion object {
        @kotlin.jvm.JvmField
        var batterylevel: Int = 0
        var polarhrMeasurements = HashMap<String, ArrayList<Int>>()
        private const val TAG = "PolarActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
        @SuppressLint("StaticFieldLeak")
        private var theSinglePolarProcessor: PolarActivity? = null
        @SuppressLint("StaticFieldLeak")
        private lateinit var broadcastButton: Button
        @SuppressLint("StaticFieldLeak")
        private lateinit var connectButton: Button
        private lateinit var broadcastDisposable: Disposable
        private var deviceConnected = false

        private val POLAR_HR = "polar_heart_rate"
        private var polarhrList = ArrayList<Int>()

        // ATTENTION! Replace with the device ID from your device.
        //private var deviceId = "A Polar Device"

        private val api: PolarBleApi by lazy {
            // Notice PolarBleApi.ALL_FEATURES are enabled
            PolarBleApiDefaultImpl.defaultImplementation(AppCompatActivity(), PolarBleApi.ALL_FEATURES)
        }
        /**
         * Get the single instance of this class
         * @return PolarActivity
         */
        @JvmStatic
        fun getPolarProcessor(): PolarActivity? {
            if (theSinglePolarProcessor == null) {
                theSinglePolarProcessor =
                    PolarActivity()
            }
            return theSinglePolarProcessor
        }
        /* Function to let app know if a Polar device is connected */
        @JvmStatic
        fun isPolarConnected(): Boolean {
            Log.i(TAG, "Polar is " + if (deviceConnected) "connected" else "not connected")
            return deviceConnected
        }
        @JvmStatic
        fun startHRBroadcast() {
                broadcastDisposable = api.startListenForPolarHrBroadcasts(null)
                    .doFinally{
                        // close the file once stream is either completed, error has stop the stream or stream is disposed
                        polarhrMeasurements[POLAR_HR] = polarhrList
                        Log.i(
                            TAG,
                            "doFinally: $polarhrMeasurements "
                        )
                    }
                    .subscribe(
                        { polarBroadcastData: PolarHrBroadcastData ->
                            polarhrList += polarBroadcastData.hr
                            Log.i(
                                TAG,
                                "HR BROADCAST ${polarBroadcastData.polarDeviceInfo.deviceId} " +
                                        "HR: ${polarBroadcastData.hr} " +
                                        "batt: ${polarBroadcastData.batteryStatus} " +
                                        "polarhrListCollected: $polarhrList "
                            )
                        },
                        { error: Throwable ->
                            showToast("Broadcast listener failed. Reason $error. Please try again or check your Polar device.")
                            Log.e(TAG, "Broadcast listener failed. Reason $error")
                        },
                        { Log.d(TAG, "complete") }
                    )
        }
        @JvmStatic
        fun startHRBackground() {
            api.setApiCallback(object : PolarBleApiCallback() {
                override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                    polarhrList += data.hr
                    Log.d(
                        TAG,
                        "HR value: ${data.hr} rrsMs: ${data.rrsMs} rr: ${data.rrs} contact: ${data.contactStatus} , ${data.contactStatusSupported}"
                    )
                }
            })
        }

        private fun showToast(s: String) {
            showToast(s)

        }

        @JvmStatic
        fun completeHRBroadcast(): HashMap<String, ArrayList<Int>> {
            //broadcastDisposable.dispose()
            polarhrMeasurements[POLAR_HR] = polarhrList
            Log.d(TAG, "complete, doFinally: $polarhrMeasurements ")
            return polarhrMeasurements
        }
        @JvmStatic
        fun cleanPolarMeasurements() {
            polarhrList = ArrayList()
        }

    }

    //static part of class
    private val POLAR_HR = "polar_heart_rate"

    private val POLAR_ACC_X = "polar_acc_x"
    private val POLAR_ACC_Y = "polar_acc_y"
    private val POLAR_ACC_Z = "polar_acc_z"
    private val POLAR_GYRO_X = "polar_gyro_x"
    private val POLAR_GYRO_Y = "polar_gyro_y"
    private val POLAR_GYRO_Z = "polar_gyro_z"
    private val POLAR_MAGNET_X = "polar_magnet_x"
    private val POLAR_MAGNET_Y = "polar_magnet_y"
    private val POLAR_MAGNET_Z = "polar_magnet_z"
    private val POLAR_PPG_0 = "polar_ppg_0"
    private val POLAR_PPG_1 = "polar_ppg_1"
    private val POLAR_PPG_2 = "polar_ppg_2"
    private val POLAR_PPG_AMBIENT = "polar_ppg_ambient"
    private val POLAR_PPI = "polar_ppi"
    private val POLAR_PPI_BLOCKER = "polar_ppi_blocker"
    private val POLAR_PPI_ERRORESTIMATE = "polar_ppi_error_estimate"

    // non-static part
    private var _polarMeasurements: HashMap<String, Any>? = null

    //private var _polarhrMeasurements = HashMap<String, ArrayList<Int>>()
    private var _polarhrList = AsyncSubject.create<ArrayList<Int>>()
    private var polarhrList = ArrayList<Int>()

    private var _polaraccxMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _polaraccyMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _polaracczMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _polargyroxMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polargyroyMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polargyrozMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polarmagnetxMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polarmagnetyMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polarmagnetzMeasurements: HashMap<String, ArrayList<Float>>? = null
    private var _polarppg0Measurements: HashMap<String, ArrayList<Int>>? = null
    private var _polarppg1Measurements: HashMap<String, ArrayList<Int>>? = null
    private var _polarppg2Measurements: HashMap<String, ArrayList<Int>>? = null
    private var _polarppgambientMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _polarppiMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _polarppiblockerMeasurements: HashMap<String, ArrayList<Boolean>>? = null
    private var _polarppierrorestimateMeasurements: HashMap<String, ArrayList<Int>>? = null
    private var _theApplication: ESApplication? = null

    private fun getTheApplicationContext(): Context? {
        return ESApplication.getTheAppContext()
    }

    private val _timestampLatestNotification: ESTimestamp? = null

    fun setTheESApplicationReference(esApplicationReference: ESApplication) {
        _theApplication = esApplicationReference
    }

    // ATTENTION! Replace with the device ID from your device.
    private var deviceId = "Polar Device"

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.ALL_FEATURES)
    }

    //private lateinit var broadcastDisposable: Disposable
    private var scanDisposable: Disposable? = null
    private var autoConnectDisposable: Disposable? = null
    private var ecgDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private var gyrDisposable: Disposable? = null
    private var magDisposable: Disposable? = null
    private var ppgDisposable: Disposable? = null
    private var ppiDisposable: Disposable? = null
    private var sdkModeEnableDisposable: Disposable? = null

    private var sdkModeEnabledStatus = false
    //private var deviceConnected = false
    private var bluetoothEnabled = false
    private var exerciseEntry: PolarExerciseEntry? = null

    //private lateinit var broadcastButton: Button
    private var broadcastButtonclicked = false
  //  private lateinit var connectButton: Button
   // private lateinit var autoConnectButton: Button
   // private lateinit var scanButton: Button
   /* private lateinit var ecgButton: Button
    private lateinit var accButton: Button
    private lateinit var gyrButton: Button
    private lateinit var magButton: Button
    private lateinit var ppgButton: Button
    private lateinit var ppiButton: Button
    private lateinit var listExercisesButton: Button
    private lateinit var readExerciseButton: Button
    private lateinit var removeExerciseButton: Button
    private lateinit var startH10RecordingButton: Button
    private lateinit var stopH10RecordingButton: Button
    private lateinit var readH10RecordingStatusButton: Button
    private lateinit var setTimeButton: Button
    private lateinit var toggleSdkModeButton: Button*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(R.layout.activity_polar)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
     //   broadcastButton = findViewById(R.id.broadcast_button)
        connectButton = findViewById(R.id.connect_button)
     //   autoConnectButton = findViewById(R.id.auto_connect_button)
      //  scanButton = findViewById(R.id.scan_button)
     /*   ecgButton = findViewById(R.id.ecg_button)
        accButton = findViewById(R.id.acc_button)
        gyrButton = findViewById(R.id.gyr_button)
        magButton = findViewById(R.id.mag_button)
        ppgButton = findViewById(R.id.ohr_ppg_button)
        ppiButton = findViewById(R.id.ohr_ppi_button)
        listExercisesButton = findViewById(R.id.list_exercises)
        readExerciseButton = findViewById(R.id.read_exercise)
        removeExerciseButton = findViewById(R.id.remove_exercise)
        startH10RecordingButton = findViewById(R.id.start_h10_recording)
        stopH10RecordingButton = findViewById(R.id.stop_h10_recording)
        readH10RecordingStatusButton = findViewById(R.id.h10_recording_status)
        setTimeButton = findViewById(R.id.set_time)
        toggleSdkModeButton = findViewById(R.id.toggle_SDK_mode)*/

        api.setPolarFilter(false)
        api.setApiLogger { s: String? ->
            if (s != null) {
                Log.d(API_LOGGER_TAG, s)
            }
        }
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth is on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth is off")

                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId)
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, deviceId)
                toggleButtonDown(connectButton, buttonText)
                showToast("Your Polar device is connected")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId)
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
                toggleButtonUp(connectButton, buttonText)
                //toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
                showToast("Your Polar device is disconnected")
            }

            override fun streamingFeaturesReady(
                identifier: String, features: Set<DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            override fun hrFeatureReady(identifier: String) {
                Log.d(TAG, "HR READY: $identifier")
                // hr notifications are about to start
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                batterylevel = level
                if(level < 5){
                    showToast("Your Polar device has a low battery level of $level%. Please charge.")
                }
                showToast("The battery level of your Polar device is $level%")
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                Log.d(
                    TAG,
                    "HR value: ${data.hr} rrsMs: ${data.rrsMs} rr: ${data.rrs} contact: ${data.contactStatus} , ${data.contactStatusSupported}"
                )
            }

            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

       /* broadcastButton.setOnClickListener {
            if (!this::broadcastDisposable.isInitialized || broadcastDisposable.isDisposed) {
                toggleButtonDown(broadcastButton, R.string.listening_broadcast)
                //broadcastButtonclicked = true
                //createpolarhrList()
                 broadcastDisposable = api.startListenForPolarHrBroadcasts(null)
                     .doFinally{
                         // close the file once stream is either completed, error has stop the stream or stream is disposed
                         polarhrMeasurements[POLAR_HR] = polarhrList
                         Log.d(
                             TAG,
                             "doFinally: $polarhrMeasurements "
                         )
                     }
                    .subscribe(
                        { polarBroadcastData: PolarHrBroadcastData ->
                            polarhrList += polarBroadcastData.hr

                            Log.d(
                                TAG,
                                "HR BROADCAST ${polarBroadcastData.polarDeviceInfo.deviceId} " +
                                        "HR: ${polarBroadcastData.hr} " +
                                        "batt: ${polarBroadcastData.batteryStatus} " +
                                        "polarhrListCollected: $polarhrList " +
                                        "publish:  "
                            )
                        },
                        { error: Throwable ->
                            toggleButtonUp(
                                broadcastButton,
                                R.string.listen_broadcast
                            )
                            Log.e(TAG, "Broadcast listener failed. Reason $error")
                        },
                        { Log.d(TAG, "complete") }
                    )
            } else {
                toggleButtonUp(broadcastButton, R.string.listen_broadcast)
                broadcastDisposable.dispose()
                getPolarhrMeasurements()
            }
        }*/
        //createpolarhrList()
        //val myList: List<ArrayList<Int>> = createpolarhrList().toList().blockingGet()
        //_polarhrList.onNext(createpolarhrList().subscribe(emitter => emitter)
        //createpolarhrList().subscribe(_polarhrList)
        //val abc = myList
        //Log.d(
        //    TAG,
        //    "publish: $abc ")

        connectButton.text = getString(R.string.connect_to_device, deviceId)
        connectButton.setOnClickListener {
            try {
                if (autoConnectDisposable != null) {
                autoConnectDisposable?.dispose()
            }
                if((autoConnectDisposable == null) && (!deviceConnected)){
                    showToast("Connecting to your Polar device")}
                if((autoConnectDisposable != null) && (!deviceConnected)){
                    showToast("Connecting to your Polar device")}
            autoConnectDisposable = api.autoConnectToDevice(-50, "180D", null)
                .subscribe(
                    { Log.d(TAG, "auto connect search complete") },
                    { throwable: Throwable -> Log.e(TAG, "" + throwable.toString()) }
                )
                if (deviceConnected) {
                    api.disconnectFromDevice(deviceId)
                } else {
                    api.connectToDevice(deviceId)
                }
            } catch (polarInvalidArgument: PolarInvalidArgument) {
                val attempt = if (deviceConnected) {
                    "disconnect"
                } else {
                    "connect"
                }
                Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
            }
        }

       /* autoConnectButton.setOnClickListener {
            if (autoConnectDisposable != null) {
                autoConnectDisposable?.dispose()
            }
            autoConnectDisposable = api.autoConnectToDevice(-50, "180D", null)
                .subscribe(
                    { Log.d(TAG, "auto connect search complete") },
                    { throwable: Throwable -> Log.e(TAG, "" + throwable.toString()) }
                )
        }*/

      /*  scanButton.setOnClickListener {
            val isDisposed = scanDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(scanButton, R.string.scanning_devices)
                scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarDeviceInfo: PolarDeviceInfo ->
                            Log.d(
                                TAG,
                                "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable
                            )
                        },
                        { error: Throwable ->
                            toggleButtonUp(scanButton, "Scan devices")
                            Log.e(TAG, "Device scan failed. Reason $error")
                        },
                        { Log.d(TAG, "complete") }
                    )
            } else {
                toggleButtonUp(scanButton, "Scan devices")
                scanDisposable?.dispose()
            }
        }

        ecgButton.setOnClickListener {
            val isDisposed = ecgDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ecgButton, R.string.stop_ecg_stream)
                ecgDisposable = requestStreamSettings(deviceId, DeviceStreamingFeature.ECG)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startEcgStreaming(deviceId, settings)
                    }
                    .subscribe(
                        { polarEcgData: PolarEcgData ->
                            for (microVolts in polarEcgData.samples) {
                                Log.d(TAG, "    yV: $microVolts")
                            }
                        },
                        { error: Throwable ->
                            toggleButtonUp(ecgButton, R.string.start_ecg_stream)
                            Log.e(TAG, "ECG stream failed. Reason $error")
                        },
                        { Log.d(TAG, "ECG stream complete") }
                    )
            } else {
                toggleButtonUp(ecgButton, R.string.start_ecg_stream)
                // NOTE stops streaming if it is "running"
                ecgDisposable?.dispose()
            }
        }

        accButton.setOnClickListener {
            val isDisposed = accDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(accButton, R.string.stop_acc_stream)
                accDisposable = requestStreamSettings(deviceId, DeviceStreamingFeature.ACC)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startAccStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            for (data in polarAccelerometerData.samples) {
                                //_polaraccxMeasurements!![POLAR_ACC_X]!!.add(data.x)
                                //_polaraccyMeasurements!![POLAR_ACC_Y]!!.add(data.y)
                                //_polaracczMeasurements!![POLAR_ACC_Z]!!.add(data.z)
                                Log.d(TAG, "ACC    x: ${data.x} y:  ${data.y} z: ${data.z}")
                            }
                        },
                        { error: Throwable ->
                            toggleButtonUp(accButton, R.string.start_acc_stream)
                            Log.e(TAG, "ACC stream failed. Reason $error")
                        },
                        {
                            showToast("ACC stream complete")
                            Log.d(TAG, "ACC stream complete")
                        }
                    )
            } else {
                toggleButtonUp(accButton, R.string.start_acc_stream)
                // NOTE dispose will stop streaming if it is "running"
                accDisposable?.dispose()
            }
        }

        gyrButton.setOnClickListener {
            val isDisposed = gyrDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(gyrButton, R.string.stop_gyro_stream)
                gyrDisposable =
                    requestStreamSettings(deviceId, DeviceStreamingFeature.GYRO)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startGyroStreaming(deviceId, settings)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { polarGyroData: PolarGyroData ->
                                for (data in polarGyroData.samples) {
                                    //_polargyroxMeasurements!![POLAR_GYRO_X]!!.add(data.x)
                                    //_polargyroyMeasurements!![POLAR_GYRO_Y]!!.add(data.y)
                                    //_polargyrozMeasurements!![POLAR_GYRO_Z]!!.add(data.z)
                                    Log.d(TAG, "GYR    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(gyrButton, R.string.start_gyro_stream)
                                Log.e(TAG, "GYR stream failed. Reason $error")
                            },
                            { Log.d(TAG, "GYR stream complete") }
                        )
            } else {
                toggleButtonUp(gyrButton, R.string.start_gyro_stream)
                // NOTE dispose will stop streaming if it is "running"
                gyrDisposable?.dispose()
            }
        }

        magButton.setOnClickListener {
            val isDisposed = magDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(magButton, R.string.stop_mag_stream)
                magDisposable =
                    requestStreamSettings(deviceId, DeviceStreamingFeature.MAGNETOMETER)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startMagnetometerStreaming(deviceId, settings)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { polarMagData: PolarMagnetometerData ->
                                for (data in polarMagData.samples) {
                                    //_polarmagnetxMeasurements!![POLAR_MAGNET_X]!!.add(data.x)
                                    //_polarmagnetyMeasurements!![POLAR_MAGNET_Y]!!.add(data.y)
                                    //_polarmagnetzMeasurements!![POLAR_MAGNET_Z]!!.add(data.z)
                                    Log.d(TAG, "MAG    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(magButton, R.string.start_mag_stream)
                                Log.e(TAG, "MAGNETOMETER stream failed. Reason $error")
                            },
                            { Log.d(TAG, "MAGNETOMETER stream complete") }
                        )
            } else {
                toggleButtonUp(magButton, R.string.start_mag_stream)
                // NOTE dispose will stop streaming if it is "running"
                magDisposable!!.dispose()
            }
        }

        ppgButton.setOnClickListener {
            val isDisposed = ppgDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ppgButton, R.string.stop_ppg_stream)
                ppgDisposable =
                    requestStreamSettings(deviceId, DeviceStreamingFeature.PPG)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startOhrStreaming(deviceId, settings)
                        }
                        .subscribe(
                            { polarOhrPPGData: PolarOhrData ->
                                if (polarOhrPPGData.type == PolarOhrData.OHR_DATA_TYPE.PPG3_AMBIENT1) {
                                    for (data in polarOhrPPGData.samples) {
                                        //_polarppg0Measurements!![POLAR_PPG_0]!!.add(data.channelSamples[0])
                                        //_polarppg1Measurements!![POLAR_PPG_1]!!.add(data.channelSamples[1])
                                        //_polarppg2Measurements!![POLAR_PPG_2]!!.add(data.channelSamples[2])
                                        //_polarppgambientMeasurements!![POLAR_PPG_AMBIENT]!!.add(data.channelSamples[3])
                                        Log.d(
                                            TAG,
                                            "PPG    ppg0: ${data.channelSamples[0]} ppg1: ${data.channelSamples[1]} ppg2: ${data.channelSamples[2]} ambient: ${data.channelSamples[3]}"
                                        )
                                    }
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                                Log.e(TAG, "PPG stream failed. Reason $error")
                            },
                            { Log.d(TAG, "PPG stream complete") }
                        )
            } else {
                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                // NOTE dispose will stop streaming if it is "running"
                ppgDisposable?.dispose()
            }
        }

        ppiButton.setOnClickListener {
            val isDisposed = ppiDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ppiButton, R.string.stop_ppi_stream)
                ppiDisposable = api.startOhrPPIStreaming(deviceId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { ppiData: PolarOhrPPIData ->
                            for (sample in ppiData.samples) {
                                //_polarppiMeasurements!![POLAR_PPI]!!.add(sample.ppi)
                                //_polarppiblockerMeasurements!![POLAR_PPI_BLOCKER]!!.add(sample.blockerBit)
                                //_polarppierrorestimateMeasurements!![POLAR_PPI_ERRORESTIMATE]!!.add(sample.errorEstimate)
                                Log.d(
                                    TAG,
                                    "PPI    ppi: ${sample.ppi} blocker: ${sample.blockerBit} errorEstimate: ${sample.errorEstimate}"
                                )
                            }
                        },
                        { error: Throwable ->
                            toggleButtonUp(ppiButton, R.string.start_ppi_stream)
                            Log.e(TAG, "PPI stream failed. Reason $error")
                        },
                        { Log.d(TAG, "PPI stream complete") }
                    )
            } else {
                toggleButtonUp(ppiButton, R.string.start_ppi_stream)
                // NOTE dispose will stop streaming if it is "running"
                ppiDisposable?.dispose()
            }
        }

        listExercisesButton.setOnClickListener {
            api.listExercises(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarExerciseEntry: PolarExerciseEntry ->
                        Log.d(
                            TAG,
                            "next: ${polarExerciseEntry.date} path: ${polarExerciseEntry.path} id: ${polarExerciseEntry.identifier}"
                        )
                        exerciseEntry = polarExerciseEntry
                    },
                    { error: Throwable -> Log.e(TAG, "Failed to list exercises: $error") },
                    { Log.d(TAG, "list exercises complete") }
                )
        }

        readExerciseButton.setOnClickListener {
            exerciseEntry?.let { entry ->
                api.fetchExercise(deviceId, entry)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarExerciseData: PolarExerciseData ->
                            Log.d(
                                TAG,
                                "exercise data count: ${polarExerciseData.hrSamples.size} samples: ${polarExerciseData.hrSamples}"
                            )
                        },
                        { error: Throwable ->
                            val errorDescription = "Failed to read exercise. Reason: $error"
                            Log.e(TAG, errorDescription)
                            showToast(errorDescription)
                        }
                    )
            } ?: run {
                val help = "No exercise to read, please list the exercises first"
                showToast(help)
                Log.e(TAG, help)
            }
        }

        removeExerciseButton.setOnClickListener {
            exerciseEntry?.let { entry ->
                api.removeExercise(deviceId, entry)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            exerciseEntry = null
                            Log.d(TAG, "ex removed ok")
                        },
                        { error: Throwable ->
                            Log.d(TAG, "ex remove failed: $error")
                        }
                    )
            } ?: run {
                val help = "No exercise to remove, please list the exercises first"
                showToast(help)
                Log.e(TAG, help)
            }
        }

        startH10RecordingButton.setOnClickListener {
            api.startRecording(
                deviceId,
                "TEST_APP_ID",
                PolarBleApi.RecordingInterval.INTERVAL_1S,
                PolarBleApi.SampleType.HR
            )
                .subscribe(
                    { Log.d(TAG, "recording started") },
                    { error: Throwable ->
                        Log.e(TAG, "recording start failed: $error")
                    })
        }

        stopH10RecordingButton.setOnClickListener {
            api.stopRecording(deviceId)
                .subscribe(
                    { Log.d(TAG, "recording stopped") },
                    { error: Throwable -> Log.e(TAG, "recording stop failed: $error") }
                )
        }

        readH10RecordingStatusButton.setOnClickListener {
            api.requestRecordingStatus(deviceId)
                .subscribe(
                    { pair: Pair<Boolean, String> ->
                        Log.d(TAG, "recording on: ${pair.first} ID: ${pair.second}")
                    },
                    { error: Throwable -> Log.e(TAG, "recording status failed: $error") }
                )
        }

        setTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            api.setLocalTime(deviceId, calendar)
                .subscribe(
                    { Log.d(TAG, "time ${calendar.time} set to device") },
                    { error: Throwable -> Log.d(TAG, "set time failed: $error") }
                )
        }

        toggleSdkModeButton.setOnClickListener {
            toggleSdkModeButton.isEnabled = false
            if (!sdkModeEnabledStatus) {
                sdkModeEnableDisposable = api.enableSDKMode(deviceId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(TAG, "SDK mode enabled")
                            // at this point dispose all existing streams. SDK mode enable command
                            // stops all the streams but client is not informed. This is workaround
                            // for the bug.
                            disposeAllStreams()
                            toggleSdkModeButton.isEnabled = true
                            sdkModeEnabledStatus = true
                            toggleButtonDown(toggleSdkModeButton, R.string.disable_sdk_mode)
                        },
                        { error ->
                            toggleSdkModeButton.isEnabled = true
                            val errorString = "SDK mode enable failed: $error"
                            showToast(errorString)
                            Log.e(TAG, errorString)
                        }
                    )
            } else {
                sdkModeEnableDisposable = api.disableSDKMode(deviceId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(TAG, "SDK mode disabled")
                            toggleSdkModeButton.isEnabled = true
                            sdkModeEnabledStatus = false
                            toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
                        },
                        { error ->
                            toggleSdkModeButton.isEnabled = true
                            val errorString = "SDK mode disable failed: $error"
                            showToast(errorString)
                            Log.e(TAG, errorString)
                        }
                    )
            }
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ), PERMISSION_REQUEST_CODE
                    )
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }

    }

   /* private fun writeFile(filename: String, content: String): Boolean {
        var fos: FileOutputStream? = null
        try {
            val outFile = File(ESDataFilesAccessor.getLabelFilesDir(), filename)
            fos = FileOutputStream(outFile)
            fos.write(content.toByteArray())
            fos.close()
            Log.d(TAG, "File: $fos")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, e.message!!)
            return false
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
            return false
        }
        return true
    } */

   /* override fun onBackPressed() {
        val intent = Intent(this@PolarActivity, MainActivity::class.java)
        startActivity(intent)
    }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            Log.d(TAG, "bt ready")
        }
    }

   // public override fun onStop() {
   //     super.onStop()
   //     api.backgroundEntered()
   //  }

    //public override fun onPause() {
    //    super.onPause()
    //    api.backgroundEntered()
   // }

    public override fun onResume() {
        super.onResume()
        if (deviceConnected) {
        val buttonText = getString(R.string.disconnect_from_device, deviceId)
        toggleButtonDown(connectButton, buttonText)}
        else {
            val buttonText = getString(R.string.connect_to_device, deviceId)
            toggleButtonUp(connectButton, buttonText)}
        //api.foregroundEntered()
    }

   // public override fun onDestroy() {
    //    super.onDestroy()
   //     api.shutDown()
   // }

    private fun toggleButtonDown(button: Button, text: String? = null) {
        toggleButton(button, true, text)
    }

   // private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
   //     toggleButton(button, true, getString(resourceId))
    //}

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

   // private fun toggleButtonUp(button: Button, @StringRes resourceId: Int) {
   //     toggleButton(button, false, getString(resourceId))
   // }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun requestStreamSettings(
        identifier: String,
        feature: DeviceStreamingFeature
    ): Flowable<PolarSensorSetting> {

        val availableSettings = api.requestStreamSettings(identifier, feature)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn { error: Throwable ->
                val errorString = "Settings are not available for feature $feature. REASON: $error"
                Log.w(TAG, errorString)
                showToast(errorString)
                PolarSensorSetting(emptyMap())
            }
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(
                    TAG,
                    "Full stream settings are not available for feature $feature. REASON: $error"
                )
                PolarSensorSetting(emptyMap())
            }
        return Single.zip(
            availableSettings,
            allSettings,
            { available: PolarSensorSetting, all: PolarSensorSetting ->
                if (available.settings.isEmpty()) {
                    throw Throwable("Settings are not available")
                } else {
                    Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
                    Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
                    return@zip android.util.Pair(available, all)
                }
            }
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable()
            .flatMap(
                Function { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                    DialogUtility.showAllSettingsDialog(
                        this@PolarActivity,
                        sensorSettings.first.settings,
                        sensorSettings.second.settings
                    ).toFlowable()
                } as Function<android.util.Pair<PolarSensorSetting, PolarSensorSetting>, Flowable<PolarSensorSetting>>
            )
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun disableAllButtons() {
      ///  broadcastButton.isEnabled = false
        connectButton.isEnabled = false
      //  autoConnectButton.isEnabled = false
      //  scanButton.isEnabled = false
     /*   ecgButton.isEnabled = false
        accButton.isEnabled = false
        gyrButton.isEnabled = false
        magButton.isEnabled = false
        ppgButton.isEnabled = false
        ppiButton.isEnabled = false
        listExercisesButton.isEnabled = false
        readExerciseButton.isEnabled = false
        removeExerciseButton.isEnabled = false
        startH10RecordingButton.isEnabled = false
        stopH10RecordingButton.isEnabled = false
        readH10RecordingStatusButton.isEnabled = false
        setTimeButton.isEnabled = false
        toggleSdkModeButton.isEnabled = false*/
    }

    private fun enableAllButtons() {
       // broadcastButton.isEnabled = true
        connectButton.isEnabled = true
     //   autoConnectButton.isEnabled = true
      //  scanButton.isEnabled = true
     /*   ecgButton.isEnabled = true
        accButton.isEnabled = true
        gyrButton.isEnabled = true
        magButton.isEnabled = true
        ppgButton.isEnabled = true
        ppiButton.isEnabled = trueadb piar
        listExercisesButton.isEnabled = true
        readExerciseButton.isEnabled = true
        removeExerciseButton.isEnabled = true
        startH10RecordingButton.isEnabled = true
        stopH10RecordingButton.isEnabled = true
        readH10RecordingStatusButton.isEnabled = true
        setTimeButton.isEnabled = true
        toggleSdkModeButton.isEnabled = true */
    }

    private fun disposeAllStreams() {
        ecgDisposable?.dispose()
        accDisposable?.dispose()
        gyrDisposable?.dispose()
        magDisposable?.dispose()
        ppgDisposable?.dispose()
        ppgDisposable?.dispose()
    }


   /* fun startPolarCollection() {
        Log.i(
            TAG,
            "Resetting Polar bundle data structures."
        )
        broadcastButton.performClick()
        //cleanPolarMeasurements()

        //launchPolarApp();
    } */

   // fun cleanPolarMeasurements() {
        //_polarMeasurements = HashMap(17)
     //   polarhrList = ArrayList()
        //_polarhrList = ArrayList(60)
        /*   _polaraccxMeasurements = HashMap(500)
        _polaraccyMeasurements = HashMap(500)
        _polaracczMeasurements = HashMap(500)
        _polargyroxMeasurements = HashMap(500)
        _polargyroyMeasurements = HashMap(500)
        _polargyrozMeasurements = HashMap(500)
        _polarmagnetxMeasurements = HashMap(500)
        _polarmagnetyMeasurements = HashMap(500)
        _polarmagnetzMeasurements = HashMap(500)
        _polarppg0Measurements = HashMap(500)
        _polarppg1Measurements = HashMap(500)
        _polarppg2Measurements = HashMap(500)
        _polarppgambientMeasurements = HashMap(500)
        _polarppiMeasurements = HashMap(500)
        _polarppiblockerMeasurements = HashMap(500)
        _polarppierrorestimateMeasurements = HashMap(500) */
   // }

    // Convert the array into a MutableList, add the specified element at the end of the list,
    // and finally return an array list containing all the elements in this list.
    fun append(arr: ArrayList<Int>, element: Int): ArrayList<Int> {
        val list: MutableList<Int> = arr.toMutableList()
        list.add(element)
        return list.toCollection(ArrayList())
    }

/*
    private fun createpolarhrListObservable(): Observable<ArrayList<Int>> {
        return create { emitter ->
            if (broadcastButtonclicked) {
                broadcastDisposable = api.startListenForPolarHrBroadcasts(null)
                    .subscribe(
                        { polarBroadcastData: PolarHrBroadcastData ->
                            polarhrList += polarBroadcastData.hr
                            _polarhrList.onNext(polarhrList)
                            emitter.onNext(polarhrList)
                            Log.d(
                                TAG,
                                "HR BROADCAST ${polarBroadcastData.polarDeviceInfo.deviceId} " +
                                        "HR: ${polarBroadcastData.hr} " +
                                        "batt: ${polarBroadcastData.batteryStatus} " +
                                        "polarhrListCollected: $polarhrList " +
                                        "publish:  "
                            )
                        },
                        { error: Throwable ->
                            Log.e(TAG, "Broadcast listener failed. Reason $error")
                        },
                        {
                            Log.d(
                                TAG, "complete" +
                                        "publish: $polarhrList "
                            )
                        }
                    )
            } else {
                Log.e(TAG, "Need to turn on Broadcast Button")
            }
        }
    }*/
/*
    fun createpolarhrList(): ArrayList<Int> {
            createpolarhrListObservable().subscribe(_polarhrList)
            _polarhrList.onComplete()
            val myList: ArrayList<Int> = _polarhrList.getValue()
            Log.d(
                TAG,
                "publish: $myList "
            )
            return myList
    }
*/

    fun getPolarhrMeasurements(): HashMap<String, ArrayList<Int>> {
       // _polarhrMeasurements[POLAR_HR] = createpolarhrList()
        Log.d(
            TAG,
            "getPolarhrMeasurements: $polarhrMeasurements "
        )
        return polarhrMeasurements
    }

  /*  fun getPolaraccxMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polaraccxMeasurements
        return _polaraccxMeasurements
    }

    fun getPolaraccyMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polaraccyMeasurements
        return _polaraccyMeasurements
    }

    fun getPolaracczMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polaracczMeasurements
        return _polaracczMeasurements
    }

    fun getPolargyroxMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polargyroxMeasurements
        return _polargyroxMeasurements
    }

    fun getPolargyroyMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polargyroyMeasurements
        return _polargyroyMeasurements
    }

    fun getPolargyrozMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polargyrozMeasurements
        return _polargyrozMeasurements
    }

    fun getPolarmagnetxMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polarmagnetxMeasurements
        return _polarmagnetxMeasurements
    }

    fun getPolarmagnetyMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polarmagnetyMeasurements
        return _polarmagnetyMeasurements
    }

    fun getPolarmagnetzMeasurements(): HashMap<String, ArrayList<Float>>? {
        _polarmagnetzMeasurements
        return _polarmagnetzMeasurements
    }

    fun getPolarppg0Measurements(): HashMap<String, ArrayList<Int>>? {
        _polarppg0Measurements
        return _polarppg0Measurements
    }

    fun getPolarppg1Measurements(): HashMap<String, ArrayList<Int>>? {
        _polarppg1Measurements
        return _polarppg1Measurements
    }

    fun getPolarppg2Measurements(): HashMap<String, ArrayList<Int>>? {
        _polarppg2Measurements
        return _polarppg2Measurements
    }

    fun getPolarppgambientMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polarppgambientMeasurements
        return _polarppgambientMeasurements
    }

    fun getPolarppiMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polarppiMeasurements
        return _polarppiMeasurements
    }

    fun getPolarppiblockerMeasurements(): HashMap<String, ArrayList<Boolean>>? {
        _polarppiblockerMeasurements
        return _polarppiblockerMeasurements
    }

    fun getPolarppierrorestimateMeasurements(): HashMap<String, ArrayList<Int>>? {
        _polarppierrorestimateMeasurements
        return _polarppierrorestimateMeasurements
    } */
/*
    private fun applySameLabelForRecentActivity() {
        val timestampUserRespondToWatchNotification = ESTimestamp()
        if (_theApplication == null || _theApplication!!._dataForAlertForPastFeedback == null) {
            Log.i(
                TAG,
                "We have no data for alert about past activity."
            )
            return
        }
        val latestVerified = _theApplication!!._dataForAlertForPastFeedback._latestVerifiedActivity
        if (latestVerified == null) {
            Log.i(
                TAG,
                "We have no latest verified activity."
            )
            return
        }
        val entireRange =
            ESDatabaseAccessor.getESDatabaseAccessor().getSingleContinuousActivityFromTimeRange(
                latestVerified._timestamp,
                _theApplication!!._dataForAlertForPastFeedback._untilTimestamp
            )
        // Apply the labels of latestVerified to all minutes in the range:
        for (minuteActivity in entireRange.minuteActivities) {
            ESDatabaseAccessor.getESDatabaseAccessor().setESActivityValues(
                minuteActivity,
                ESActivity.ESLabelSource.ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT_FROM_POLAR,
                latestVerified._mainActivityUserCorrection,
                latestVerified._secondaryActivities,
                latestVerified._moods,
                null, null,
                _timestampLatestNotification, timestampUserRespondToWatchNotification
            )
        }
    }*/


    /*
    private void displayMaxStorageValue(int maxStorage) {
        TextView maxStorageValue = (TextView)findViewById(R.id.max_storage_value);
        float hours = ((float)maxStorage) / 60f;
        float megaBytes = ((float)maxStorage) * EXAMPLE_SIZE_IN_MEGABYTES;
        String storageString = String.format("%d (~%.2f hr, ~%.2f MB)",maxStorage,hours,megaBytes);
        maxStorageValue.setText(storageString);
    }
*/

}