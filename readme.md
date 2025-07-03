# WIP - Bosch eBike Monitor

A simple Android app to connect to Bosch eBikes via Bluetooth Low Energy (BLE) and monitor real-time data including battery level and assist mode.

Creates an android notification that can be pushed to any connected devices like Garmin Smartwatches. 

<img src="images\ebikeMonitorScreenshot.jpg" alt="drawing" width="200"/> <img src="images\FlowScreenshot.jpg" alt="drawing" width="200"/>


Created after getting frustrated with Bosches lack of connectivity to other devices.

Others have had the same frustrations!
https://www.emtbforums.com/threads/project-to-enable-bosch-garmin-integration.37793/

<img src="images\notification.jpg" alt="drawing" width="400"/>

Example notification


## Features

- 🔗 Direct connection to Bosch eBikes via BLE
- 🔋 Real-time battery monitoring (two formats)
- ⚡ Live assist mode display
- 📊 Data logging with timestamps
- 📱 Clean, user-friendly interface

## Prerequisites

- Android device with Bluetooth LE support
- Bosch eBike with Smart System
- **nRF Connect app** (for finding your bike's MAC address)

## Setup

### 1. Find Your Bike's MAC Address

1. **Download nRF Connect** from Google Play Store
2. **Turn on your bike** and ensure Bluetooth is enabled
3. **Open nRF Connect** and scan for devices
4. **Look for "SMART SYSTEM EBIKE"** in the device list
5. **Note the MAC address** (format: `XX:XX:XX:XX:XX:XX`)
<img src="images\nrf.jpg" alt="drawing" width="400"/>
### 2. Configure the App

1. **Clone this repository**
2. **Open in Android Studio**
3. **Build and install** the app on your Android device
4. **Grant Bluetooth permissions** when prompted

## Usage

### Connection Methods

**Option 1: Direct Connection (Fastest)**
1. Open the app
2. Enter your bike's MAC address in the "Direct Connection" field
3. Tap "Connect to This MAC"

**Option 2: Device Scanning**
1. Tap "Scan Devices"
2. Wait for your bike to appear in the list
3. Tap on your bike to connect

### Live Data Display

Once connected, you'll see:
- **🚴 Live Bike Status** - Current assist mode and battery levels
- **📡 Raw Data** - Latest data packet received
- **📋 Data Log** - Timestamped history of all data

## Supported Data

The app currently decodes:
- **Battery Level**: Two different battery value formats
- **Assist Mode**: Current motor assistance level (0-5)
- **Raw Telemetry**: All received data packets for analysis

## Troubleshooting

- **Can't find bike**: Ensure bike is on and close to your phone
- **Connection fails**: Try the direct MAC address method
- **No data**: Check that Bosch Flow app isn't blocking the connection

## Development

Built with:
- Kotlin
- Android Jetpack Compose
- Android Bluetooth LE APIs

- CLAUDE.ai

# TO DO LIST

- Hardcoded UUIDs: Bosch service UUIDs look suspicious - custom/placeholder UUIDs rather than official Bosch ones - investigate? read from BLE service?
- Data parsing assumptions: The battery/assist parsing logic makes assumptions about packet structure that may not be correct
- No error recovery: Limited handling of connection failures or data corruption

*UI*
- choose which fields are output as BLE sources (speed/cadence/ebike data)
- choose which fields are output as notifications upon change (battery percentage?)

## License

MIT License - feel free to modify and distribute.

---

*Note: This app works alongside the official Bosch Flow app. Make sure your bike is authenticated and unlocked via Flow before connecting.*
