package com.example.samplepolaapi

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.DefaultTab.AlbumsTab.value
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.samplepolaapi.ui.theme.SamplePolaAPITheme
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.concurrent.schedule
import kotlin.text.clear

class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var api: PolarBleApi

    private var isScanning by mutableStateOf(false)
    private val foundDevices = mutableStateListOf<PolarDeviceInfo>()

    private var heartRate by mutableStateOf(0) // 心拍数を保持する状態変数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 権限ランチャーの初期化
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(this, "すべての権限が許可されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "権限が拒否されました", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            SamplePolaAPITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) { // Column を使用して Text を配置
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                        Button(onClick = { startDeviceScan() }) {
                            Text("Scan for Devices")
                        }
                        if (isScanning) {
                            Text("Scanning...")
                        }
                        DeviceList(foundDevices) { device ->
                            connectToDevice(device.deviceId)
                        }
                        Text("Heart Rate: $heartRate")
                    }
                }
            }
        }
        // Polar BLE API の初期化
        initializePolarApi()

        // アプリ起動時に権限リクエストを実行
        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        val permissions = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        permissionLauncher.launch(permissions)
    }

    private fun initializePolarApi() {
        api = PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext, setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )



        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d("MyApp", "BLE power: $powered")
                Toast.makeText(applicationContext, "BLE Power: $powered", Toast.LENGTH_SHORT).show()
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTED: ${polarDeviceInfo.deviceId}")
                Toast.makeText(applicationContext, "Connected to: ${polarDeviceInfo.deviceId}", Toast.LENGTH_SHORT).show()
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                Toast.makeText(applicationContext, "Disconnected from: ${polarDeviceInfo.deviceId}", Toast.LENGTH_SHORT).show()
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d("MyApp", "DIS INFO disInfo: $disInfo value: $value")
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d("MyApp", "Polar BLE SDK feature $feature is ready")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d("MyApp", "BATTERY LEVEL: $level")
                Toast.makeText(applicationContext, "Battery Level: $level%", Toast.LENGTH_SHORT).show()
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                runOnUiThread { // メインスレッドで heartRate を更新
                    heartRate = data.hr
                }
            }

        })



        // デバイスのスキャンを開始（デバイスIDを指定する場合は適宜変更）
        val deviceId = BuildConfig.DEVICE_ID // 実際のデバイスIDに置き換え
        try {
            api.connectToDevice(deviceId)
        } catch (e: Exception) {
            Log.e("MyApp", "Connection failed: ${e.localizedMessage}")
        }
    }
    private fun startDeviceScan() {
        isScanning = true
        foundDevices.clear()

        api.searchForDevice()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { polarDeviceInfo ->
                    foundDevices.add(polarDeviceInfo)
                },
                { error ->
                    Log.e("MyApp", "Device search failed: ${error.message}")
                }
            )
        // Stop scanning after 10 seconds
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    isScanning = false
                }
            }
        }, 10000) // 10 seconds
    }

    private fun connectToDevice(deviceId: String) {
        isScanning = false
        try {
            api.connectToDevice(deviceId)
        } catch (e: Exception) {
            Log.e("MyApp", "Connection failed: ${e.localizedMessage}")
            // Handle connection error, e.g., show an error message
        }
    }
}

@Composable
fun DeviceList(devices: List<PolarDeviceInfo>, onDeviceClick: (PolarDeviceInfo) -> Unit) {
    LazyColumn {
        items(devices) { device ->
            Text(
                text = "${device.name} (${device.deviceId})",
                modifier = Modifier.clickable { onDeviceClick(device) }
            )
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SamplePolaAPITheme {
        Greeting("Android")
    }
}