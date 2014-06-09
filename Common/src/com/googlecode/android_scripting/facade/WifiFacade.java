package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.net.ConnectException;
import java.util.List;

/**
 * WifiManager functions.
 *
 */
//TODO: make methods handle various wifi states properly
//e.g. wifi connection result will be null when flight mode is on
public class WifiFacade extends RpcReceiver {
  private final Service mService;
  private final WifiManager mWifi;
  private final IntentFilter mIntentFilter;
  private final WifiScanReceiver mReceiver;
  private WifiLock mLock;
  private static int WifiScanCnt;

  public WifiFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mWifi = (WifiManager) mService.getSystemService(Context.WIFI_SERVICE);
    mLock = null;
    mIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    mReceiver = new WifiScanReceiver(manager.getReceiver(EventFacade.class));
  }

  private void makeLock(int wifiMode) {
    if (mLock == null) {
      mLock = mWifi.createWifiLock(wifiMode, "sl4a");
      mLock.acquire();
    }
  }

  /**
   * Handle Brodacst receiver for Scan Result
   * @parm eventFacade
   *        Object of EventFacade
   */
  class WifiScanReceiver extends BroadcastReceiver {
    private final static String mEventType = "WiFiScan";
    private final EventFacade mEventFacade;
    private final Bundle mResults;

    WifiScanReceiver(EventFacade eventFacade){
      mEventFacade = eventFacade;
      mResults = new Bundle();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
      Log.d("WifiScanReceiver  "+ mEventType);
      mResults.putLong("Timestamp", System.currentTimeMillis()/1000);
      mResults.putString("Type", "onWifiScanReceive");
      mEventFacade.postEvent(mEventType, mResults.clone());
      mResults.clear();
    }
  }

  @Rpc(description = "Returns the list of access points found during the most recent Wifi scan.")
  public List<ScanResult> wifiGetScanResults() {
    mService.unregisterReceiver(mReceiver);
    return mWifi.getScanResults();
  }

  @Rpc(description = "Acquires a full Wifi lock.")
  public void wifiLockAcquireFull() {
    makeLock(WifiManager.WIFI_MODE_FULL);
  }

  @Rpc(description = "Acquires a scan only Wifi lock.")
  public void wifiLockAcquireScanOnly() {
    makeLock(WifiManager.WIFI_MODE_SCAN_ONLY);
  }

  @Rpc(description = "Releases a previously acquired Wifi lock.")
  public void wifiLockRelease() {
    if (mLock != null) {
      mLock.release();
      mLock = null;
    }
  }

  @Rpc(description = "Starts a scan for Wifi access points.", returns = "True if the scan was initiated successfully.")
  public Boolean wifiStartScan() {
    mService.registerReceiver(mReceiver, mIntentFilter);
    return mWifi.startScan();
  }

  @Rpc(description = "Checks Wifi state.", returns = "True if Wifi is enabled.")
  public Boolean checkWifiState() {
    return mWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
  }

  @Rpc(description = "Toggle Wifi on and off.", returns = "True if Wifi is enabled.")
  public Boolean toggleWifiState(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
    if (enabled == null) {
      enabled = !checkWifiState();
    }
    mWifi.setWifiEnabled(enabled);
    return enabled;
  }

  @Rpc(description = "Disconnects from the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiDisconnect() {
    return mWifi.disconnect();
  }

  @Rpc(description = "Returns information about the currently active access point.")
  public WifiInfo wifiGetConnectionInfo() {
    return mWifi.getConnectionInfo();
  }

  @Rpc(description = "Reassociates with the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiReassociate() {
    return mWifi.reassociate();
  }

  @Rpc(description = "Reconnects to the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiReconnect() {
    return mWifi.reconnect();
  }

  /**
   * Connects to a WPA protected wifi network
   * 
   * @param wifiSSID
   *          SSID of the wifi network
   * @param wifiPassword
   *          password for the wifi network
   * @return true on success
   * @throws ConnectException
   */
  @Rpc(description = "Connects a WPA protected wifi network by ssid", returns = "True if the operation succeeded.")
  public Boolean wifiConnectWPA(@RpcParameter(name = "wifiSSID") String wifiSSID,
      @RpcParameter(name = "wifiPassword") String wifiPassword) throws ConnectException {
    WifiConfiguration wifiConfig = new WifiConfiguration();
    wifiConfig.SSID = "\"" + wifiSSID + "\"";
    if(wifiPassword == null)
      wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    else
      wifiConfig.preSharedKey = "\"" + wifiPassword + "\"";

    mWifi.addNetwork(wifiConfig);
    Boolean status = false;
    Boolean found = false;
    for (ScanResult sr : mWifi.getScanResults()) {
      if (sr.SSID.equals(wifiSSID)) {
        found = true;
        break;
      }
    }
    if (found == false) {
      Log.e("Could not find wifi network with ssid " + wifiSSID);
      throw new ConnectException("Could not find wifi network with ssid " + wifiSSID);
    }
    for (WifiConfiguration conf : mWifi.getConfiguredNetworks()) {
      if (conf.SSID != null && conf.SSID.equals("\"" + wifiSSID + "\"")) {
        mWifi.disconnect();
        mWifi.enableNetwork(conf.networkId, true);
        status = mWifi.reconnect();
        break;
      }
    }
    return status;
  }

  @Override
  public void shutdown() {
    wifiLockRelease();
  }
}
