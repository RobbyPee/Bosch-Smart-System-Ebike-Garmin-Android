package com.RobPlow.BoschEbikeMonitor.models

data class BikeConfig(
    val bike: BikeInfo,
    val bluetooth: BluetoothConfig,
    val dataParsing: DataParsingConfig
)

data class BikeInfo(
    val macAddress: String,
    val name: String
)

data class BluetoothConfig(
    val services: ServiceConfig,
    val scanTimeoutMs: Long = 15000L
)

data class ServiceConfig(
    val statusServiceUuid: String,
    val statusCharacteristicUuid: String
)

data class DataParsingConfig(
    val batteryPattern: List<Int>,
    val assistPattern: List<Int>
)