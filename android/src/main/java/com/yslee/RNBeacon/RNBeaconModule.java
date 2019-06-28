package com.yslee.RNBeacon;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.content.Context;
import android.content.Intent;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class RNBeaconModule extends ReactContextBaseJavaModule{
  private static final String TAG = "RNBeaconModule";
  private BluetoothLeScanner mBLEScanner;;
  private ReactApplicationContext mReactContext;
  private Context mApplicationContext;

  private String filter_uuid;
  
  public RNBeaconModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.mReactContext = reactContext;
    this.mApplicationContext = reactContext.getApplicationContext();
  }

  @Override
  public String getName() {
    return "RNBeacon";
  }
  
  private ScanCallback mScanCallback= new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result)
    {
      super.onScanResult(callbackType, result);
      // Log.d(TAG, " onScanResult " + result.getDevice().getAddress());
      // sendEvent(result.getDevice().getAddress());
      processResult(result);
    }
    @Override
    public void onScanFailed(int errorCode)
    {
      super.onScanFailed(errorCode);
      Log.d(TAG, " onScanFailed");
    }
  
    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      super.onBatchScanResults(results);
      for (ScanResult result : results) {
        Log.d(TAG, " onBatchScanResults");
      }
    }

    public void processResult(ScanResult result) {
      if(result.getScanRecord() != null) {
        byte[] bytes = null;
        bytes = result.getScanRecord().getBytes();
        byte_to_ascii(bytes, result.getRssi());
      }
    }

    private void byte_to_ascii(byte[] scanData, int rssi) {

      byte [] uuidBytes = Arrays.copyOfRange(scanData, 9, 9 + 16);
      String uuidString = "";
      for (int i = 0 ; i < uuidBytes.length; i++) {
        if (i == 4 || i == 6 || i == 8 || i == 10) {
          uuidString += "-";
        }
        uuidString += String.format("%02x", uuidBytes[i]);
      }

      
      byte [] uuidBytes_simulator = Arrays.copyOfRange(scanData, 9 - 3, 9 + 16 - 3);
      String uuidString_simulator = "";
      for (int i = 0 ; i < uuidBytes_simulator.length; i++) {
        if (i == 4 || i == 6 || i == 8 || i == 10) {
          uuidString_simulator += "-";
        }
        uuidString_simulator += String.format("%02x", uuidBytes_simulator[i]);
      }

      if (uuidString.equals(filter_uuid)) {

        int major = (((int)scanData[25] & 0x0FF) * 0x100 + ((int)scanData[26] & 0x0FF)) & 0x0FFFF;
        int minor = (((int)scanData[27] & 0x0FF) * 0x100 + ((int)scanData[28] & 0x0FF)) & 0x0FFFF;
        // Log.d(TAG, " byte_to_ascii uuid  : " + uuidString);
        // Log.d(TAG, " byte_to_ascii major : " + major);
        // Log.d(TAG, " byte_to_ascii minor : " + minor);
        // Log.d(TAG, " byte_to_ascii rssi  : " + rssi);
        sendEvent(createReturnData(uuidString, major, minor, rssi));

      } else if (uuidString_simulator.equals(filter_uuid)) {

        int major = (((int)scanData[25 - 3] & 0x0FF) * 0x100 + ((int)scanData[26 - 3] & 0x0FF)) & 0x0FFFF;
        int minor = (((int)scanData[27 - 3] & 0x0FF) * 0x100 + ((int)scanData[28 - 3] & 0x0FF)) & 0x0FFFF;
        // Log.d(TAG, " byte_to_ascii uuid  : " + uuidString);
        // Log.d(TAG, " byte_to_ascii major : " + major);
        // Log.d(TAG, " byte_to_ascii minor : " + minor);
        // Log.d(TAG, " byte_to_ascii rssi  : " + rssi);
        sendEvent(createReturnData(uuidString_simulator, major, minor, rssi));

      }
    }
  };
  
  @ReactMethod
  public void startScanBle(String uuid) {
    filter_uuid = uuid;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();

    ScanFilter scanFilter = new ScanFilter.Builder().build();
    List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    scanFilters.add(scanFilter);
    
    ScanSettings.Builder mScanSettings = new ScanSettings.Builder();
    mScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    ScanSettings scanSettings = mScanSettings.build();
    mBLEScanner.startScan(scanFilters, scanSettings, mScanCallback);
    
  }
  
  @ReactMethod
  public void stopScanBle() {
    mBLEScanner.stopScan(mScanCallback);
  }

  private WritableMap createReturnData(String uuid, int major, int minor, int rssi) {
    WritableMap map = new WritableNativeMap();
    map.putString("uuid", uuid);
    map.putInt("major", major);
    map.putInt("minor", minor);
    map.putInt("rssi", rssi);
    return map;
  }

  private void sendEvent(WritableMap param) {
    mReactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("noticeBeacon", param);
  }
  
}