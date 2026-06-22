# WIP - Bosch eBike Monitor

Current Status - **Not being Developed**

Whilst its possible to read the Bosch ebike data on the phone, there are Garmin firmware restrictions that stop the data being broadcast from the phone back to the Garmin.

The Garmin device is **unable** to hold two simulatenous connections to the phone:
1. Garmin <-> Phone connection for Garmin Connect
2. Garmin <-> Phone emulating emtb or power sensor so data can display data fields and log the data
This is the reason I halted progress.

I'd urge anyone interested in this to look at https://github.com/Nilogax/SmartBridge who has continued this work with a hardware bridge
Bosch now implements a similar feature to link Garmin devices to their Bosch motor, but you need a more recent Garmin https://support.garmin.com/en-US/?faq=rrXMisEjAh22sJZLsRMChA

A simple Android app to connect to Bosch eBikes via Bluetooth Low Energy (BLE) and monitor real-time data and re-broadcast as a BLE sensor so that a Garmin Edge device can read it. 

Older updates from June 2025:

Currently I have decoded fields for
- Battery percentage
- Assist mode
- Human Power (watts)
- Speed
- Cadence
- Motor Power (watts)

It also creates a notification when battery percentage changes so you can see it on any smartwatch or other connected device

<img src="images\ebikeMonitorScreenshot.jpg" alt="drawing" width="200"/> <img src="images\FlowScreenshot.jpg" alt="drawing" width="200"/>


Created after getting frustrated with Bosches lack of connectivity to other devices.

Others have had the same frustrations!
https://www.emtbforums.com/threads/project-to-enable-bosch-garmin-integration.37793/

<img src="images\notification.jpg" alt="drawing" width="400"/>

Example notification using assist mode

## TO DO LIST

Features to be added
- Fix disconnect button
- Fix SCAN + connect 
- UI to allow notifications (e.g. for battery percentage) to be turned on/off
- Test that all values return to zero correctly (e.g. motor power)
- Run as an android service in background
- Test BLE Ebike and Speed/Cadence broadcast using Garmin Edge (or Zwift / MyWhoosh)
- Run on alternative phones / bikes / check uuids - are they fixed or variable? 
- Error checking - deal with disconnects etc. gracefully
- Remove debug logs

## How does Bosch send the information via Bluetooth??

Sample message
| Message                  | Power (watts) |
|--------------------------|----------------|
| `30-04-98-5B-08-4F`      | 79 W           |
| `30-05-98-5B-08-B2-04`   | 562 W          |
| `30-02-98-5B`            | 0 W            |

More information - ➡️ [BLE Decoding](BLEdata.md)

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

## Troubleshooting

- **Can't find bike**: Ensure bike is on and close to your phone
- **Connection fails**: Try the direct MAC address method
- **No data**: Check that Bosch Flow app isn't blocking the connection

## Development

Built with:
- Kotlin
- Android Jetpack Compose
- Android Bluetooth LE APIs

- CLAUDE.ai / Gemini (better!) / chatgpt etc.
- I found it best to reset the chat for each incremental code change. Remove unneeded debugging and functions as soon as possible. Keep code size small. Keep chat purpose small and contained, with clear end goal (add feature X), start a new chat once finished, pasting updated code in. 

## License
This project is licensed under the GNU GPL v3.0 - see the LICENSE file for details.

---

*Note: This app works alongside the official Bosch Flow app. Make sure your bike is authenticated and unlocked via Flow before connecting.*
