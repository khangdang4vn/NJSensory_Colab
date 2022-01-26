/*package edu.ucsd.calab.extrasensory.sensors.WatchProcessing;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import android.util.Log;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallbackProvider;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.model.PolarExerciseData;

import edu.ucsd.calab.extrasensory.ui.MainActivity;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Pair;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApi.DeviceStreamingFeature;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.*;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import java.util.*;

public class ESPolarSensor extends AppCompatActivity {
    // ATTENTION! Replace with the device ID from your device.
    private String deviceId = "93170422";

    private final Lazy api$delegate = LazyKt.lazy((Function0)(new Function0() {
        // $FF: synthetic method
        // $FF: bridge method
        public Object invoke() {
            return this.invoke();
        }

        @NotNull
        public final PolarBleApi invoke() {
            PolarBleApi var10000 = PolarBleApiDefaultImpl.defaultImplementation((Context) MainActivity.this, 255);
            Intrinsics.checkNotNullExpressionValue(var10000, "PolarBleApiDefaultImpl.d…PolarBleApi.ALL_FEATURES)");
            return var10000;
        }
    }));

    private Disposable broadcastDisposable;
    private Disposable scanDisposable;
    private Disposable autoConnectDisposable;
    private Disposable ecgDisposable;
    private Disposable accDisposable;
    private Disposable gyrDisposable;
    private Disposable magDisposable;
    private Disposable ppgDisposable;
    private Disposable ppiDisposable;
    private Disposable sdkModeEnableDisposable;
    private boolean sdkModeEnabledStatus;
    private boolean deviceConnected;
    private boolean bluetoothEnabled;
    private PolarExerciseEntry exerciseEntry;
    private Button broadcastButton;
    private Button connectButton;
    private Button autoConnectButton;
    private Button scanButton;
    private Button ecgButton;
    private Button accButton;
    private Button gyrButton;
    private Button magButton;
    private Button ppgButton;
    private Button ppiButton;
    private Button listExercisesButton;
    private Button readExerciseButton;
    private Button removeExerciseButton;
    private Button startH10RecordingButton;
    private Button stopH10RecordingButton;
    private Button readH10RecordingStatusButton;
    private Button setTimeButton;
    private Button toggleSdkModeButton;
    private static final String TAG = "MainActivity";
    private static final String API_LOGGER_TAG = "API LOGGER";
    @NotNull
    public static final MainActivity.Companion Companion = new MainActivity.Companion((DefaultConstructorMarker)null);

    private final PolarBleApi getApi() {
        Lazy var1 = this.api$delegate;
        Object var3 = null;
        boolean var4 = false;
        return (PolarBleApi)var1.getValue();
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(1300085);
        Log.d("MainActivity", "version: " + PolarBleApiDefaultImpl.versionInfo());
        View var10001 = this.findViewById(1000248);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.broadcast_button)");
        this.broadcastButton = (Button)var10001;
        var10001 = this.findViewById(1000113);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.connect_button)");
        this.connectButton = (Button)var10001;
        var10001 = this.findViewById(1000055);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.auto_connect_button)");
        this.autoConnectButton = (Button)var10001;
        var10001 = this.findViewById(1000004);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.scan_button)");
        this.scanButton = (Button)var10001;
        var10001 = this.findViewById(1000285);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.ecg_button)");
        this.ecgButton = (Button)var10001;
        var10001 = this.findViewById(1000127);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.acc_button)");
        this.accButton = (Button)var10001;
        var10001 = this.findViewById(1000204);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.gyr_button)");
        this.gyrButton = (Button)var10001;
        var10001 = this.findViewById(1000336);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.mag_button)");
        this.magButton = (Button)var10001;
        var10001 = this.findViewById(1000129);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.ohr_ppg_button)");
        this.ppgButton = (Button)var10001;
        var10001 = this.findViewById(1000297);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.ohr_ppi_button)");
        this.ppiButton = (Button)var10001;
        var10001 = this.findViewById(1000357);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.list_exercises)");
        this.listExercisesButton = (Button)var10001;
        var10001 = this.findViewById(1000109);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.read_exercise)");
        this.readExerciseButton = (Button)var10001;
        var10001 = this.findViewById(1000153);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.remove_exercise)");
        this.removeExerciseButton = (Button)var10001;
        var10001 = this.findViewById(1000239);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.start_h10_recording)");
        this.startH10RecordingButton = (Button)var10001;
        var10001 = this.findViewById(1000302);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.stop_h10_recording)");
        this.stopH10RecordingButton = (Button)var10001;
        var10001 = this.findViewById(1000298);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.h10_recording_status)");
        this.readH10RecordingStatusButton = (Button)var10001;
        var10001 = this.findViewById(1000105);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.set_time)");
        this.setTimeButton = (Button)var10001;
        var10001 = this.findViewById(1000213);
        Intrinsics.checkNotNullExpressionValue(var10001, "findViewById(R.id.toggle_SDK_mode)");
        this.toggleSdkModeButton = (Button)var10001;
        PolarBleApi var10000 = PolarBleApiDefaultImpl.defaultImplementation((Context)this, 255);
        Intrinsics.checkNotNullExpressionValue(var10000, "PolarBleApiDefaultImpl.d…PolarBleApi.ALL_FEATURES)");
        final PolarBleApi api = var10000;
        api.setPolarFilter(false);
        api.setApiLogger((PolarBleApiLogger)null.INSTANCE);
        api.setApiCallback((PolarBleApiCallbackProvider)(new PolarBleApiCallback() {
            public void blePowerStateChanged(boolean powered) {
                Log.d("MainActivity", "BLE power: " + powered);
                MainActivity.this.bluetoothEnabled = powered;
                if (powered) {
                    MainActivity.this.enableAllButtons();
                    MainActivity.this.showToast("Phone Bluetooth on");
                } else {
                    MainActivity.this.disableAllButtons();
                    MainActivity.this.showToast("Phone Bluetooth off");
                }

            }

            public void deviceConnected(@NotNull PolarDeviceInfo polarDeviceInfo) {
                Intrinsics.checkNotNullParameter(polarDeviceInfo, "polarDeviceInfo");
                Log.d("MainActivity", "CONNECTED: " + polarDeviceInfo.deviceId);
                MainActivity var10000 = MainActivity.this;
                String var10001 = polarDeviceInfo.deviceId;
                Intrinsics.checkNotNullExpressionValue(var10001, "polarDeviceInfo.deviceId");
                var10000.deviceId = var10001;
                MainActivity.this.deviceConnected = true;
                String var3 = MainActivity.this.getString(1900029, new Object[]{MainActivity.this.deviceId});
                Intrinsics.checkNotNullExpressionValue(var3, "getString(R.string.disco…ct_from_device, deviceId)");
                String buttonText = var3;
                MainActivity.this.toggleButtonDown(MainActivity.access$getConnectButton$p(MainActivity.this), buttonText);
            }

            public void deviceConnecting(@NotNull PolarDeviceInfo polarDeviceInfo) {
                Intrinsics.checkNotNullParameter(polarDeviceInfo, "polarDeviceInfo");
                Log.d("MainActivity", "CONNECTING: " + polarDeviceInfo.deviceId);
            }

            public void deviceDisconnected(@NotNull PolarDeviceInfo polarDeviceInfo) {
                Intrinsics.checkNotNullParameter(polarDeviceInfo, "polarDeviceInfo");
                Log.d("MainActivity", "DISCONNECTED: " + polarDeviceInfo.deviceId);
                MainActivity.this.deviceConnected = false;
                String var10000 = MainActivity.this.getString(1900106, new Object[]{MainActivity.this.deviceId});
                Intrinsics.checkNotNullExpressionValue(var10000, "getString(R.string.connect_to_device, deviceId)");
                String buttonText = var10000;
                MainActivity.this.toggleButtonUp(MainActivity.access$getConnectButton$p(MainActivity.this), buttonText);
                MainActivity.this.toggleButtonUp(MainActivity.access$getToggleSdkModeButton$p(MainActivity.this), 1900097);
            }

            public void streamingFeaturesReady(@NotNull String identifier, @NotNull Set features) {
                Intrinsics.checkNotNullParameter(identifier, "identifier");
                Intrinsics.checkNotNullParameter(features, "features");
                Iterator var4 = features.iterator();

                while(var4.hasNext()) {
                    DeviceStreamingFeature feature = (DeviceStreamingFeature)var4.next();
                    Log.d("MainActivity", "Streaming feature " + feature + " is ready");
                }

            }

            public void hrFeatureReady(@NotNull String identifier) {
                Intrinsics.checkNotNullParameter(identifier, "identifier");
                Log.d("MainActivity", "HR READY: " + identifier);
            }

            public void disInformationReceived(@NotNull String identifier, @NotNull UUID uuid, @NotNull String value) {
                Intrinsics.checkNotNullParameter(identifier, "identifier");
                Intrinsics.checkNotNullParameter(uuid, "uuid");
                Intrinsics.checkNotNullParameter(value, "value");
                Log.d("MainActivity", "uuid: " + uuid + " value: " + value);
            }

            public void batteryLevelReceived(@NotNull String identifier, int level) {
                Intrinsics.checkNotNullParameter(identifier, "identifier");
                Log.d("MainActivity", "BATTERY LEVEL: " + level);
            }

            public void hrNotificationReceived(@NotNull String identifier, @NotNull PolarHrData data) {
                Intrinsics.checkNotNullParameter(identifier, "identifier");
                Intrinsics.checkNotNullParameter(data, "data");
                Log.d("MainActivity", "HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + " , " + data.contactStatusSupported);
            }

            public void polarFtpFeatureReady(@NotNull String s) {
                Intrinsics.checkNotNullParameter(s, "s");
                Log.d("MainActivity", "FTP ready");
            }
        }));
        Button var3 = this.broadcastButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("broadcastButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                if (MainActivity.this.broadcastDisposable != null && !MainActivity.access$getBroadcastDisposable$p(MainActivity.this).isDisposed()) {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getBroadcastButton$p(MainActivity.this), 1900126);
                    MainActivity.access$getBroadcastDisposable$p(MainActivity.this).dispose();
                } else {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getBroadcastButton$p(MainActivity.this), 1900120);
                    MainActivity var10000 = MainActivity.this;
                    Disposable var10001 = api.startListenForPolarHrBroadcasts((Set)null).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getBroadcastButton$p(MainActivity.this), 1900126);
                            Log.e("MainActivity", "Broadcast listener failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                    Intrinsics.checkNotNullExpressionValue(var10001, "api.startListenForPolarH…) }\n                    )");
                    var10000.broadcastDisposable = var10001;
                }

            }
        }));
        var3 = this.connectButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("connectButton");
        }

        var3.setText((CharSequence)this.getString(1900106, new Object[]{this.deviceId}));
        var3 = this.connectButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("connectButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                try {
                    if (MainActivity.this.deviceConnected) {
                        api.disconnectFromDevice(MainActivity.this.deviceId);
                    } else {
                        api.connectToDevice(MainActivity.this.deviceId);
                    }
                } catch (PolarInvalidArgument var4) {
                    String attempt = MainActivity.this.deviceConnected ? "disconnect" : "connect";
                    Log.e("MainActivity", "Failed to " + attempt + ". Reason " + var4 + ' ');
                }

            }
        }));
        var3 = this.autoConnectButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("autoConnectButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                if (MainActivity.this.autoConnectDisposable != null) {
                    Disposable var10000 = MainActivity.this.autoConnectDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

                MainActivity.this.autoConnectDisposable = api.autoConnectToDevice(-50, "180D", (String)null).subscribe((Action)null.INSTANCE, (Consumer)null.INSTANCE);
            }
        }));
        var3 = this.scanButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scanButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.scanDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getScanButton$p(MainActivity.this), 1900033);
                    MainActivity.this.scanDisposable = api.searchForDevice().observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getScanButton$p(MainActivity.this), "Scan devices");
                            Log.e("MainActivity", "Device scan failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getScanButton$p(MainActivity.this), "Scan devices");
                    var10000 = MainActivity.this.scanDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.ecgButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ecgButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.ecgDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getEcgButton$p(MainActivity.this), 1900079);
                    MainActivity.this.ecgDisposable = MainActivity.this.requestStreamSettings(MainActivity.this.deviceId, DeviceStreamingFeature.ECG).flatMap((Function)(new Function() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public Object apply(Object var1) {
                            return this.apply((PolarSensorSetting)var1);
                        }

                        public final Publisher apply(@NotNull PolarSensorSetting settings) {
                            Intrinsics.checkNotNullParameter(settings, "settings");
                            return (Publisher)api.startEcgStreaming(MainActivity.this.deviceId, settings);
                        }
                    })).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getEcgButton$p(MainActivity.this), 1900129);
                            Log.e("MainActivity", "ECG stream failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getEcgButton$p(MainActivity.this), 1900129);
                    var10000 = MainActivity.this.ecgDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.accButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("accButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.accDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getAccButton$p(MainActivity.this), 1900036);
                    MainActivity.this.accDisposable = MainActivity.this.requestStreamSettings(MainActivity.this.deviceId, DeviceStreamingFeature.ACC).flatMap((Function)(new Function() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public Object apply(Object var1) {
                            return this.apply((PolarSensorSetting)var1);
                        }

                        public final Publisher apply(@NotNull PolarSensorSetting settings) {
                            Intrinsics.checkNotNullParameter(settings, "settings");
                            return (Publisher)api.startAccStreaming(MainActivity.this.deviceId, settings);
                        }
                    })).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getAccButton$p(MainActivity.this), 1900090);
                            Log.e("MainActivity", "ACC stream failed. Reason " + error);
                        }
                    }), (Action)(new Action() {
                        public final void run() {
                            MainActivity.this.showToast("ACC stream complete");
                            Log.d("MainActivity", "ACC stream complete");
                        }
                    }));
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getAccButton$p(MainActivity.this), 1900090);
                    var10000 = MainActivity.this.accDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.gyrButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("gyrButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.gyrDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getGyrButton$p(MainActivity.this), 1900026);
                    MainActivity.this.gyrDisposable = MainActivity.this.requestStreamSettings(MainActivity.this.deviceId, DeviceStreamingFeature.GYRO).flatMap((Function)(new Function() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public Object apply(Object var1) {
                            return this.apply((PolarSensorSetting)var1);
                        }

                        public final Publisher apply(@NotNull PolarSensorSetting settings) {
                            Intrinsics.checkNotNullParameter(settings, "settings");
                            return (Publisher)api.startGyroStreaming(MainActivity.this.deviceId, settings);
                        }
                    })).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getGyrButton$p(MainActivity.this), 1900003);
                            Log.e("MainActivity", "GYR stream failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getGyrButton$p(MainActivity.this), 1900003);
                    var10000 = MainActivity.this.gyrDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.magButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("magButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.magDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getMagButton$p(MainActivity.this), 1900100);
                    MainActivity.this.magDisposable = MainActivity.this.requestStreamSettings(MainActivity.this.deviceId, DeviceStreamingFeature.MAGNETOMETER).flatMap((Function)(new Function() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public Object apply(Object var1) {
                            return this.apply((PolarSensorSetting)var1);
                        }

                        public final Publisher apply(@NotNull PolarSensorSetting settings) {
                            Intrinsics.checkNotNullParameter(settings, "settings");
                            return (Publisher)api.startMagnetometerStreaming(MainActivity.this.deviceId, settings);
                        }
                    })).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getMagButton$p(MainActivity.this), 1900025);
                            Log.e("MainActivity", "MAGNETOMETER stream failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getMagButton$p(MainActivity.this), 1900025);
                    var10000 = MainActivity.this.magDisposable;
                    Intrinsics.checkNotNull(var10000);
                    var10000.dispose();
                }

            }
        }));
        var3 = this.ppgButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppgButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.ppgDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getPpgButton$p(MainActivity.this), 1900107);
                    MainActivity.this.ppgDisposable = MainActivity.this.requestStreamSettings(MainActivity.this.deviceId, DeviceStreamingFeature.PPG).flatMap((Function)(new Function() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public Object apply(Object var1) {
                            return this.apply((PolarSensorSetting)var1);
                        }

                        public final Publisher apply(@NotNull PolarSensorSetting settings) {
                            Intrinsics.checkNotNullParameter(settings, "settings");
                            return (Publisher)api.startOhrStreaming(MainActivity.this.deviceId, settings);
                        }
                    })).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getPpgButton$p(MainActivity.this), 1900135);
                            Log.e("MainActivity", "PPG stream failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getPpgButton$p(MainActivity.this), 1900135);
                    var10000 = MainActivity.this.ppgDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.ppiButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppiButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                Disposable var10000 = MainActivity.this.ppiDisposable;
                boolean isDisposed = var10000 != null ? var10000.isDisposed() : true;
                if (isDisposed) {
                    MainActivity.this.toggleButtonDown(MainActivity.access$getPpiButton$p(MainActivity.this), 1900050);
                    MainActivity.this.ppiDisposable = api.startOhrPPIStreaming(MainActivity.this.deviceId).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)null.INSTANCE, (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(@NotNull Throwable error) {
                            Intrinsics.checkNotNullParameter(error, "error");
                            MainActivity.this.toggleButtonUp(MainActivity.access$getPpiButton$p(MainActivity.this), 1900064);
                            Log.e("MainActivity", "PPI stream failed. Reason " + error);
                        }
                    }), (Action)null.INSTANCE);
                } else {
                    MainActivity.this.toggleButtonUp(MainActivity.access$getPpiButton$p(MainActivity.this), 1900064);
                    var10000 = MainActivity.this.ppiDisposable;
                    if (var10000 != null) {
                        var10000.dispose();
                    }
                }

            }
        }));
        var3 = this.listExercisesButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("listExercisesButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                api.listExercises(MainActivity.this.deviceId).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)(new Consumer() {
                    // $FF: synthetic method
                    // $FF: bridge method
                    public void accept(Object var1) {
                        this.accept((PolarExerciseEntry)var1);
                    }

                    public final void accept(@NotNull PolarExerciseEntry polarExerciseEntry) {
                        Intrinsics.checkNotNullParameter(polarExerciseEntry, "polarExerciseEntry");
                        Log.d("MainActivity", "next: " + polarExerciseEntry.date + " path: " + polarExerciseEntry.path + " id: " + polarExerciseEntry.identifier);
                        MainActivity.this.exerciseEntry = polarExerciseEntry;
                    }
                }), (Consumer)null.INSTANCE, (Action)null.INSTANCE);
            }
        }));
        var3 = this.readExerciseButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readExerciseButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                PolarExerciseEntry var10000 = MainActivity.this.exerciseEntry;
                boolean var3;
                boolean var4;
                boolean var6;
                if (var10000 != null) {
                    PolarExerciseEntry var2 = var10000;
                    var3 = false;
                    var4 = false;
                    var6 = false;
                    if (api.fetchExercise(MainActivity.this.deviceId, var2).observeOn(AndroidSchedulers.mainThread()).subscribe((Consumer)MainActivity$onCreate$14$1$1.INSTANCE, (Consumer)(new MainActivity$onCreate$14$$special$$inlined$let$lambda$1(this))) != null) {
                        return;
                    }
                }

                MainActivity var8 = MainActivity.this;
                var3 = false;
                var4 = false;
                var6 = false;
                String help = "No exercise to read, please list the exercises first";
                var8.showToast(help);
                Log.e("MainActivity", help);
            }
        }));
        var3 = this.removeExerciseButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("removeExerciseButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                PolarExerciseEntry var10000 = MainActivity.this.exerciseEntry;
                boolean var3;
                boolean var4;
                boolean var6;
                if (var10000 != null) {
                    PolarExerciseEntry var2 = var10000;
                    var3 = false;
                    var4 = false;
                    var6 = false;
                    if (api.removeExercise(MainActivity.this.deviceId, var2).observeOn(AndroidSchedulers.mainThread()).subscribe((Action)(new MainActivity$onCreate$15$$special$$inlined$let$lambda$1(this)), (Consumer)MainActivity$onCreate$15$1$2.INSTANCE) != null) {
                        return;
                    }
                }

                MainActivity var8 = MainActivity.this;
                var3 = false;
                var4 = false;
                var6 = false;
                String help = "No exercise to remove, please list the exercises first";
                var8.showToast(help);
                Log.e("MainActivity", help);
            }
        }));
        var3 = this.startH10RecordingButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("startH10RecordingButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                api.startRecording(MainActivity.this.deviceId, "TEST_APP_ID", RecordingInterval.INTERVAL_1S, SampleType.HR).subscribe((Action)null.INSTANCE, (Consumer)null.INSTANCE);
            }
        }));
        var3 = this.stopH10RecordingButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("stopH10RecordingButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                api.stopRecording(MainActivity.this.deviceId).subscribe((Action)null.INSTANCE, (Consumer)null.INSTANCE);
            }
        }));
        var3 = this.readH10RecordingStatusButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readH10RecordingStatusButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                api.requestRecordingStatus(MainActivity.this.deviceId).subscribe((Consumer)null.INSTANCE, (Consumer)null.INSTANCE);
            }
        }));
        var3 = this.setTimeButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("setTimeButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                final Calendar calendar = Calendar.getInstance();
                Intrinsics.checkNotNullExpressionValue(calendar, "calendar");
                calendar.setTime(new Date());
                api.setLocalTime(MainActivity.this.deviceId, calendar).subscribe((Action)(new Action() {
                    public final void run() {
                        StringBuilder var10001 = (new StringBuilder()).append("time ");
                        Calendar var10002 = calendar;
                        Intrinsics.checkNotNullExpressionValue(var10002, "calendar");
                        Log.d("MainActivity", var10001.append(var10002.getTime()).append(" set to device").toString());
                    }
                }), (Consumer)null.INSTANCE);
            }
        }));
        var3 = this.toggleSdkModeButton;
        if (var3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleSdkModeButton");
        }

        var3.setOnClickListener((OnClickListener)(new OnClickListener() {
            public final void onClick(View it) {
                MainActivity.access$getToggleSdkModeButton$p(MainActivity.this).setEnabled(false);
                if (!MainActivity.this.sdkModeEnabledStatus) {
                    MainActivity.this.sdkModeEnableDisposable = api.enableSDKMode(MainActivity.this.deviceId).observeOn(AndroidSchedulers.mainThread()).subscribe((Action)(new Action() {
                        public final void run() {
                            Log.d("MainActivity", "SDK mode enabled");
                            MainActivity.this.disposeAllStreams();
                            MainActivity.access$getToggleSdkModeButton$p(MainActivity.this).setEnabled(true);
                            MainActivity.this.sdkModeEnabledStatus = true;
                            MainActivity.this.toggleButtonDown(MainActivity.access$getToggleSdkModeButton$p(MainActivity.this), 1900104);
                        }
                    }), (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(Throwable error) {
                            MainActivity.access$getToggleSdkModeButton$p(MainActivity.this).setEnabled(true);
                            String errorString = "SDK mode enable failed: " + error;
                            MainActivity.this.showToast(errorString);
                            Log.e("MainActivity", errorString);
                        }
                    }));
                } else {
                    MainActivity.this.sdkModeEnableDisposable = api.disableSDKMode(MainActivity.this.deviceId).observeOn(AndroidSchedulers.mainThread()).subscribe((Action)(new Action() {
                        public final void run() {
                            Log.d("MainActivity", "SDK mode disabled");
                            MainActivity.access$getToggleSdkModeButton$p(MainActivity.this).setEnabled(true);
                            MainActivity.this.sdkModeEnabledStatus = false;
                            MainActivity.this.toggleButtonUp(MainActivity.access$getToggleSdkModeButton$p(MainActivity.this), 1900097);
                        }
                    }), (Consumer)(new Consumer() {
                        // $FF: synthetic method
                        // $FF: bridge method
                        public void accept(Object var1) {
                            this.accept((Throwable)var1);
                        }

                        public final void accept(Throwable error) {
                            MainActivity.access$getToggleSdkModeButton$p(MainActivity.this).setEnabled(true);
                            String errorString = "SDK mode disable failed: " + error;
                            MainActivity.this.showToast(errorString);
                            Log.e("MainActivity", errorString);
                        }
                    }));
                }

            }
        }));
        if (VERSION.SDK_INT >= 23 && savedInstanceState == null) {
            this.requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 1);
        }

        if (VERSION.SDK_INT >= 23 && savedInstanceState == null) {
            this.requestPermissions(new String[]{"android.permission.BLUETOOTH"}, 1);
        }

        if (VERSION.SDK_INT >= 23 && savedInstanceState == null) {
            this.requestPermissions(new String[]{"android.permission.BLUETOOTH_ADMIN"}, 1);
        }

    }

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            Log.d("MainActivity", "bt ready");
        }

    }

    public void onPause() {
        super.onPause();
        this.getApi().backgroundEntered();
    }

    public void onResume() {
        super.onResume();
        this.getApi().foregroundEntered();
    }

    public void onDestroy() {
        super.onDestroy();
        this.getApi().shutDown();
    }

    private final void toggleButtonDown(Button button, String text) {
        this.toggleButton(button, true, text);
    }

    // $FF: synthetic method
    static void toggleButtonDown$default(MainActivity var0, Button var1, String var2, int var3, Object var4) {
        if ((var3 & 2) != 0) {
            var2 = (String)null;
        }

        var0.toggleButtonDown(var1, var2);
    }

    private final void toggleButtonDown(Button button, @StringRes int resourceId) {
        this.toggleButton(button, true, this.getString(resourceId));
    }

    private final void toggleButtonUp(Button button, String text) {
        this.toggleButton(button, false, text);
    }

    // $FF: synthetic method
    static void toggleButtonUp$default(MainActivity var0, Button var1, String var2, int var3, Object var4) {
        if ((var3 & 2) != 0) {
            var2 = (String)null;
        }

        var0.toggleButtonUp(var1, var2);
    }

    private final void toggleButtonUp(Button button, @StringRes int resourceId) {
        this.toggleButton(button, false, this.getString(resourceId));
    }

    private final void toggleButton(Button button, boolean isDown, String text) {
        if (text != null) {
            button.setText((CharSequence)text);
        }

        Drawable buttonDrawable = button.getBackground();
        Intrinsics.checkNotNull(buttonDrawable);
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, this.getResources().getColor(500007));
        } else {
            DrawableCompat.setTint(buttonDrawable, this.getResources().getColor(500054));
        }

        button.setBackground(buttonDrawable);
    }

    // $FF: synthetic method
    static void toggleButton$default(MainActivity var0, Button var1, boolean var2, String var3, int var4, Object var5) {
        if ((var4 & 4) != 0) {
            var3 = (String)null;
        }

        var0.toggleButton(var1, var2, var3);
    }

    private final Flowable requestStreamSettings(String identifier, final DeviceStreamingFeature feature) {
        Single availableSettings = this.getApi().requestStreamSettings(identifier, feature).observeOn(AndroidSchedulers.mainThread()).onErrorReturn((Function)(new Function() {
            // $FF: synthetic method
            // $FF: bridge method
            public Object apply(Object var1) {
                return this.apply((Throwable)var1);
            }

            public final PolarSensorSetting apply(@NotNull Throwable error) {
                Intrinsics.checkNotNullParameter(error, "error");
                String errorString = "Settings are not available for feature " + feature + ". REASON: " + error;
                Log.w("MainActivity", errorString);
                MainActivity.this.showToast(errorString);
                return new PolarSensorSetting(MapsKt.emptyMap());
            }
        }));
        Single allSettings = this.getApi().requestFullStreamSettings(identifier, feature).onErrorReturn((Function)(new Function() {
            // $FF: synthetic method
            // $FF: bridge method
            public Object apply(Object var1) {
                return this.apply((Throwable)var1);
            }

            public final PolarSensorSetting apply(@NotNull Throwable error) {
                Intrinsics.checkNotNullParameter(error, "error");
                Log.w("MainActivity", "Full stream settings are not available for feature " + feature + ". REASON: " + error);
                return new PolarSensorSetting(MapsKt.emptyMap());
            }
        }));
        Flowable var10000 = Single.zip((SingleSource)availableSettings, (SingleSource)allSettings, (BiFunction)(new BiFunction() {
            // $FF: synthetic method
            // $FF: bridge method
            public Object apply(Object var1, Object var2) {
                return this.apply((PolarSensorSetting)var1, (PolarSensorSetting)var2);
            }

            public final Pair apply(@NotNull PolarSensorSetting available, @NotNull PolarSensorSetting all) {
                Intrinsics.checkNotNullParameter(available, "available");
                Intrinsics.checkNotNullParameter(all, "all");
                if (available.settings.isEmpty()) {
                    throw new Throwable("Settings are not available");
                } else {
                    Log.d("MainActivity", "Feature " + feature + " available settings " + available.settings);
                    Log.d("MainActivity", "Feature " + feature + " all settings " + all.settings);
                    return new Pair(available, all);
                }
            }
        })).observeOn(AndroidSchedulers.mainThread()).toFlowable().flatMap((Function)(new Function() {
            // $FF: synthetic method
            // $FF: bridge method
            public Object apply(Object var1) {
                return this.apply((Pair)var1);
            }

            public final Flowable apply(@NotNull Pair sensorSettings) {
                Intrinsics.checkNotNullParameter(sensorSettings, "sensorSettings");
                DialogUtility var10000 = DialogUtility.INSTANCE;
                Activity var10001 = (Activity)MainActivity.this;
                Map var10002 = ((PolarSensorSetting)sensorSettings.first).settings;
                Intrinsics.checkNotNullExpressionValue(var10002, "sensorSettings.first.settings");
                Map var10003 = ((PolarSensorSetting)sensorSettings.second).settings;
                Intrinsics.checkNotNullExpressionValue(var10003, "sensorSettings.second.settings");
                return var10000.showAllSettingsDialog(var10001, var10002, var10003).toFlowable();
            }
        }));
        Intrinsics.checkNotNullExpressionValue(var10000, "Single.zip(\n            …orSetting>>\n            )");
        return var10000;
    }

    private final void showToast(String message) {
        Toast toast = Toast.makeText(this.getApplicationContext(), (CharSequence)message, 1);
        toast.show();
    }

    private final void disableAllButtons() {
        Button var10000 = this.broadcastButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("broadcastButton");
        }

        var10000.setEnabled(false);
        var10000 = this.connectButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("connectButton");
        }

        var10000.setEnabled(false);
        var10000 = this.autoConnectButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("autoConnectButton");
        }

        var10000.setEnabled(false);
        var10000 = this.scanButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scanButton");
        }

        var10000.setEnabled(false);
        var10000 = this.ecgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ecgButton");
        }

        var10000.setEnabled(false);
        var10000 = this.accButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("accButton");
        }

        var10000.setEnabled(false);
        var10000 = this.gyrButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("gyrButton");
        }

        var10000.setEnabled(false);
        var10000 = this.magButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("magButton");
        }

        var10000.setEnabled(false);
        var10000 = this.ppgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppgButton");
        }

        var10000.setEnabled(false);
        var10000 = this.ppiButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppiButton");
        }

        var10000.setEnabled(false);
        var10000 = this.listExercisesButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("listExercisesButton");
        }

        var10000.setEnabled(false);
        var10000 = this.readExerciseButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readExerciseButton");
        }

        var10000.setEnabled(false);
        var10000 = this.removeExerciseButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("removeExerciseButton");
        }

        var10000.setEnabled(false);
        var10000 = this.startH10RecordingButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("startH10RecordingButton");
        }

        var10000.setEnabled(false);
        var10000 = this.stopH10RecordingButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("stopH10RecordingButton");
        }

        var10000.setEnabled(false);
        var10000 = this.readH10RecordingStatusButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readH10RecordingStatusButton");
        }

        var10000.setEnabled(false);
        var10000 = this.setTimeButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("setTimeButton");
        }

        var10000.setEnabled(false);
        var10000 = this.toggleSdkModeButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleSdkModeButton");
        }

        var10000.setEnabled(false);
    }

    private final void enableAllButtons() {
        Button var10000 = this.broadcastButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("broadcastButton");
        }

        var10000.setEnabled(true);
        var10000 = this.connectButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("connectButton");
        }

        var10000.setEnabled(true);
        var10000 = this.autoConnectButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("autoConnectButton");
        }

        var10000.setEnabled(true);
        var10000 = this.scanButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scanButton");
        }

        var10000.setEnabled(true);
        var10000 = this.ecgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ecgButton");
        }

        var10000.setEnabled(true);
        var10000 = this.accButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("accButton");
        }

        var10000.setEnabled(true);
        var10000 = this.gyrButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("gyrButton");
        }

        var10000.setEnabled(true);
        var10000 = this.magButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("magButton");
        }

        var10000.setEnabled(true);
        var10000 = this.ppgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppgButton");
        }

        var10000.setEnabled(true);
        var10000 = this.ppiButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppiButton");
        }

        var10000.setEnabled(true);
        var10000 = this.listExercisesButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("listExercisesButton");
        }

        var10000.setEnabled(true);
        var10000 = this.readExerciseButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readExerciseButton");
        }

        var10000.setEnabled(true);
        var10000 = this.removeExerciseButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("removeExerciseButton");
        }

        var10000.setEnabled(true);
        var10000 = this.startH10RecordingButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("startH10RecordingButton");
        }

        var10000.setEnabled(true);
        var10000 = this.stopH10RecordingButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("stopH10RecordingButton");
        }

        var10000.setEnabled(true);
        var10000 = this.readH10RecordingStatusButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("readH10RecordingStatusButton");
        }

        var10000.setEnabled(true);
        var10000 = this.setTimeButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("setTimeButton");
        }

        var10000.setEnabled(true);
        var10000 = this.toggleSdkModeButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleSdkModeButton");
        }

        var10000.setEnabled(true);
    }

    private final void disposeAllStreams() {
        Disposable var10000 = this.ecgDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

        var10000 = this.accDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

        var10000 = this.gyrDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

        var10000 = this.magDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

        var10000 = this.ppgDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

        var10000 = this.ppgDisposable;
        if (var10000 != null) {
            var10000.dispose();
        }

    }

    // $FF: synthetic method
    public static final boolean access$getBluetoothEnabled$p(MainActivity $this) {
        return $this.bluetoothEnabled;
    }

    // $FF: synthetic method
    public static final Button access$getConnectButton$p(MainActivity $this) {
        Button var10000 = $this.connectButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("connectButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setConnectButton$p(MainActivity $this, Button var1) {
        $this.connectButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getToggleSdkModeButton$p(MainActivity $this) {
        Button var10000 = $this.toggleSdkModeButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleSdkModeButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setToggleSdkModeButton$p(MainActivity $this, Button var1) {
        $this.toggleSdkModeButton = var1;
    }

    // $FF: synthetic method
    public static final Disposable access$getBroadcastDisposable$p(MainActivity $this) {
        Disposable var10000 = $this.broadcastDisposable;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("broadcastDisposable");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setBroadcastDisposable$li(MainActivity $this, Disposable var1) {
        $this.broadcastDisposable = var1;
    }

    // $FF: synthetic method
    public static final Button access$getBroadcastButton$p(MainActivity $this) {
        Button var10000 = $this.broadcastButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("broadcastButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setBroadcastButton$p(MainActivity $this, Button var1) {
        $this.broadcastButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getScanButton$p(MainActivity $this) {
        Button var10000 = $this.scanButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scanButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setScanButton$p(MainActivity $this, Button var1) {
        $this.scanButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getEcgButton$p(MainActivity $this) {
        Button var10000 = $this.ecgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ecgButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setEcgButton$p(MainActivity $this, Button var1) {
        $this.ecgButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getAccButton$p(MainActivity $this) {
        Button var10000 = $this.accButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("accButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setAccButton$p(MainActivity $this, Button var1) {
        $this.accButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getGyrButton$p(MainActivity $this) {
        Button var10000 = $this.gyrButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("gyrButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setGyrButton$p(MainActivity $this, Button var1) {
        $this.gyrButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getMagButton$p(MainActivity $this) {
        Button var10000 = $this.magButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("magButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setMagButton$p(MainActivity $this, Button var1) {
        $this.magButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getPpgButton$p(MainActivity $this) {
        Button var10000 = $this.ppgButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppgButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setPpgButton$p(MainActivity $this, Button var1) {
        $this.ppgButton = var1;
    }

    // $FF: synthetic method
    public static final Button access$getPpiButton$p(MainActivity $this) {
        Button var10000 = $this.ppiButton;
        if (var10000 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("ppiButton");
        }

        return var10000;
    }

    // $FF: synthetic method
    public static final void access$setPpiButton$p(MainActivity $this, Button var1) {
        $this.ppiButton = var1;
    }

    // $FF: synthetic method
    public static final Disposable access$getSdkModeEnableDisposable$p(MainActivity $this) {
        return $this.sdkModeEnableDisposable;
    }

    @Metadata(
            mv = {1, 5, 1},
            k = 1,
            d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n\u0000¨\u0006\u0006"},
            d2 = {"Lcom/polar/androidblesdk/MainActivity$Companion;", "", "()V", "API_LOGGER_TAG", "", "TAG", "app_debug"}
    )
    public static final class Companion {
        private Companion() {
        }

        // $FF: synthetic method
        public Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }
    }
}
// MainActivity$onCreate$14$$special$$inlined$let$lambda$1.java
package com.polar.androidblesdk;

        import android.util.Log;
        import io.reactivex.rxjava3.functions.Consumer;
        import kotlin.Metadata;
        import kotlin.jvm.internal.Intrinsics;
        import org.jetbrains.annotations.NotNull;

@Metadata(
        mv = {1, 5, 1},
        k = 3,
        d1 = {"\u0000\u0010\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004¨\u0006\u0005"},
        d2 = {"<anonymous>", "", "error", "", "accept", "com/polar/androidblesdk/MainActivity$onCreate$14$1$2"}
)
final class MainActivity$onCreate$14$$special$$inlined$let$lambda$1 implements Consumer {
    // $FF: synthetic field
    final <undefinedtype> this$0;

    MainActivity$onCreate$14$$special$$inlined$let$lambda$1(Object var1) {
        this.this$0 = var1;
    }

    // $FF: synthetic method
    // $FF: bridge method
    public void accept(Object var1) {
        this.accept((Throwable)var1);
    }

    public final void accept(@NotNull Throwable error) {
        Intrinsics.checkNotNullParameter(error, "error");
        String errorDescription = "Failed to read exercise. Reason: " + error;
        Log.e("MainActivity", errorDescription);
        MainActivity.access$showToast(this.this$0.this$0, errorDescription);
    }
}
// MainActivity$onCreate$3$1.java
package com.polar.androidblesdk;

        import io.reactivex.rxjava3.disposables.Disposable;
        import kotlin.Metadata;
        import kotlin.jvm.internal.MutablePropertyReference0Impl;
        import org.jetbrains.annotations.Nullable;

// $FF: synthetic class
@Metadata(
        mv = {1, 5, 1},
        k = 3
)
final class MainActivity$onCreate$3$1 extends MutablePropertyReference0Impl {
    MainActivity$onCreate$3$1(MainActivity var1) {
        super(var1, MainActivity.class, "broadcastDisposable", "getBroadcastDisposable()Lio/reactivex/rxjava3/disposables/Disposable;", 0);
    }

    @Nullable
    public Object get() {
        return MainActivity.access$getBroadcastDisposable$p((MainActivity)this.receiver);
    }

    public void set(@Nullable Object value) {
        MainActivity.access$setBroadcastDisposable$p((MainActivity)this.receiver, (Disposable)value);
    }
}
// MainActivity$onCreate$15$$special$$inlined$let$lambda$1.java
package com.polar.androidblesdk;

        import android.util.Log;
        import com.polar.sdk.api.model.PolarExerciseEntry;
        import io.reactivex.rxjava3.functions.Action;
        import kotlin.Metadata;

@Metadata(
        mv = {1, 5, 1},
        k = 3,
        d1 = {"\u0000\n\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\u0010\u0000\u001a\u00020\u0001H\n¢\u0006\u0002\b\u0002¨\u0006\u0003"},
        d2 = {"<anonymous>", "", "run", "com/polar/androidblesdk/MainActivity$onCreate$15$1$1"}
)
final class MainActivity$onCreate$15$$special$$inlined$let$lambda$1 implements Action {
    // $FF: synthetic field
    final <undefinedtype> this$0;

    MainActivity$onCreate$15$$special$$inlined$let$lambda$1(Object var1) {
        this.this$0 = var1;
    }

    public final void run() {
        MainActivity.access$setExerciseEntry$p(this.this$0.this$0, (PolarExerciseEntry)null);
        Log.d("MainActivity", "ex removed ok");
    }
}
// MainActivity$onCreate$15$1$2.java
package com.polar.androidblesdk;

        import android.util.Log;
        import io.reactivex.rxjava3.functions.Consumer;
        import kotlin.Metadata;
        import kotlin.jvm.internal.Intrinsics;
        import org.jetbrains.annotations.NotNull;

@Metadata(
        mv = {1, 5, 1},
        k = 3,
        d1 = {"\u0000\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0003\n\u0000\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004"},
        d2 = {"<anonymous>", "", "error", "", "accept"}
)
final class MainActivity$onCreate$15$1$2 implements Consumer {
    public static final MainActivity$onCreate$15$1$2 INSTANCE = new MainActivity$onCreate$15$1$2();

    // $FF: synthetic method
    // $FF: bridge method
    public void accept(Object var1) {
        this.accept((Throwable)var1);
    }

    public final void accept(@NotNull Throwable error) {
        Intrinsics.checkNotNullParameter(error, "error");
        Log.d("MainActivity", "ex remove failed: " + error);
    }
}
*/

