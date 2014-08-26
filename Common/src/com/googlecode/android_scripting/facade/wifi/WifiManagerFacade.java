package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAdapter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;

/**
 * WifiManager functions.
 *
 */
//TODO: make methods handle various wifi states properly
//e.g. wifi connection result will be null when flight mode is on
public class WifiManagerFacade extends RpcReceiver {
  private final static String mEventType = "WifiManager";
  private final Service mService;
  private final WifiManager mWifi;
  private final EventFacade mEventFacade;

  private final IntentFilter mScanFilter;
  private final IntentFilter mStateChangeFilter;
  private final WifiScanReceiver mScanResultsAvailableReceiver;
  private final WifiStateChangeReceiver mStateChangeReceiver;

  private WifiLock mLock;
  private boolean mIsConnected;

  public WifiManagerFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mWifi = (WifiManager) mService.getSystemService(Context.WIFI_SERVICE);
    mLock = null;
    mEventFacade = manager.getReceiver(EventFacade.class);

    mScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    mStateChangeFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    mStateChangeFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
    mStateChangeFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    mStateChangeFilter.setPriority(999);

    mScanResultsAvailableReceiver = new WifiScanReceiver(mEventFacade);
    mStateChangeReceiver = new WifiStateChangeReceiver();
  }

  private void makeLock(int wifiMode) {
    if (mLock == null) {
      mLock = mWifi.createWifiLock(wifiMode, "sl4a");
      mLock.acquire();
    }
  }

  /**
   * Handle Broadcast receiver for Scan Result
   * @parm eventFacade
   *        Object of EventFacade
   */
  class WifiScanReceiver extends BroadcastReceiver {
    private final EventFacade mEventFacade;
    private final Bundle mResults;

    WifiScanReceiver(EventFacade eventFacade){
      mEventFacade = eventFacade;
      mResults = new Bundle();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
      String action = intent.getAction();
      if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
          Log.d("Wifi connection scan finished, results available.");
          mResults.putLong("Timestamp", System.currentTimeMillis()/1000);
          mEventFacade.postEvent(mEventType + "ScanResultsAvailable", mResults);
          mResults.clear();
          mService.unregisterReceiver(mScanResultsAvailableReceiver);
      }
    }
  }

  class WifiActionListener implements WifiManager.ActionListener{
    private final EventFacade mEventFacade;
    private final String TAG;

    public WifiActionListener(EventFacade eventFacade, String tag) {
      mEventFacade = eventFacade;
      this.TAG = tag;
    }

    @Override
    public void onSuccess() {
      mEventFacade.postEvent(mEventType + TAG + "OnSuccess", null);
    }

    @Override
    public void onFailure(int reason) {
      Log.d("WifiActionListener  "+ mEventType);
      Bundle msg = new Bundle();
      msg.putInt("reason", reason);
      mEventFacade.postEvent(mEventType + TAG + "OnFailure", msg);
    }
  }

  public class WifiStateChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d("Wifi network state changed.");
            NetworkInfo nInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo wInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            Log.d("NetworkInfo " + nInfo);
            Log.d("WifiInfo " + wInfo);
            if (wInfo != null) {
              Bundle msg = new Bundle();
              String ssid = wInfo.getSSID();
              if (ssid.charAt(0)=='"' && ssid.charAt(ssid.length()-1)=='"') {
                  msg.putString("ssid", ssid.substring(1, ssid.length()-1));
              } else {
                  msg.putString("ssid", ssid);
              }
              msg.putString("bssid", wInfo.getBSSID());
              Log.d("WifiNetworkConnected");
              mEventFacade.postEvent("WifiNetworkConnected", msg);
            }
        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            Log.d("Supplicant connection state changed.");
            mIsConnected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
            Bundle msg = new Bundle();
            msg.putBoolean("Connected", mIsConnected);
            mEventFacade.postEvent("SupplicantConnectionChanged", msg);
        }
    }
  }

  private WifiConfiguration wifiConfigurationFromScanResult(ScanResult result) {
      if (result == null) return null;
      WifiConfiguration config = new WifiConfiguration();
      config.SSID = "\"" + result.SSID + "\"";
      if (result.capabilities.contains("WEP")) {
          config.allowedKeyManagement.set(KeyMgmt.NONE);
          config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN); //?
          config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
      }
      if (result.capabilities.contains("WPA2-PSK")) {
          config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
      }
      if (result.capabilities.contains("WPA-PSK")) {
          config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
      }
      if (result.capabilities.contains("EAP")) {
          //this is probably wrong, as we don't have a way to enter the enterprise config
          config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
          config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
      }
      if (result.capabilities.length() == 5 && result.capabilities.contains("ESS")) {
          config.allowedKeyManagement.set(KeyMgmt.NONE);
      }
      config.BSSID = result.BSSID;
      config.scanResultCache = new HashMap<String, ScanResult>();
      if (config.scanResultCache == null)
          return null;
      config.scanResultCache.put(result.BSSID, result);
      return config;
  }

  private boolean matchScanResult(ScanResult result, String id) {
      if (result.BSSID.equals(id) || result.SSID.equals(id)) {
          return true;
      }
      return false;
  }

  @Rpc(description = "Check if wifi scanner is supported on this device.")
  public Boolean wifiIsScannerSupported() {
      List<WifiAdapter> adapters = mWifi.getAdapters();
      boolean s = false;
      for(WifiAdapter a : adapters) {
          if (a.isWifiScannerSupported()) {
              s = true;
          }
      }
      return s;
  }

  @Rpc(description = "Add a network.")
  public Integer wifiAddNetwork(@RpcParameter(name = "wifiId") String wifiId) {
      ScanResult target = null;
      for (ScanResult r : mWifi.getScanResults()) {
          if (matchScanResult(r, wifiId)) {
              target = r;
              break;
          }
      }
      return mWifi.addNetwork(wifiConfigurationFromScanResult(target));
  }

  @Rpc(description = "Checks Wifi state.", returns = "True if Wifi is enabled.")
  public Boolean wifiCheckState() {
    return mWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
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
  @Rpc(description = "Connects a wifi network by ssid",
           returns = "True if the operation succeeded.")
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

  @Rpc(description = "Disconnects from the currently active access point.",
           returns = "True if the operation succeeded.")
  public Boolean wifiDisconnect() {
    return mWifi.disconnect();
  }

  @Rpc(description = "Enable a configured network. Initiate a connection if disableOthers is true",
           returns = "True if the operation succeeded.")
  public Boolean wifiEnableNetwork(@RpcParameter(name = "netId") Integer netId,
                                   @RpcParameter(name = "disableOthers") Boolean disableOthers) {
    return mWifi.enableNetwork(netId, disableOthers);
  }
  /**
   * Forget a wifi network with priority
   *
   * @param networkID
   *          Id of wifi network
   */
  @Rpc(description = "Forget a wifi network with priority")
  public void wifiForgetNetwork(@RpcParameter(name = "wifiSSID") Integer newtorkId ) {
    WifiActionListener listener = new WifiActionListener(mEventFacade, "ForgetNetwork");
    mWifi.forget(newtorkId, listener);
  }

  @Rpc(description = "Return a list of all the configured wifi networks.")
  public List<WifiConfiguration> wifiGetConfiguredNetworks() {
      return mWifi.getConfiguredNetworks();
  }

  @Rpc(description = "Returns information about the currently active access point.")
  public WifiInfo wifiGetConnectionInfo() {
    return mWifi.getConnectionInfo();
  }

  @Rpc(description = "Returns the list of access points found during the most recent Wifi scan.")
  public List<ScanResult> wifiGetScanResults() {
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

  /**
   * Connects to a wifi network with priority
   *
   * @param wifiSSID
   *          SSID of the wifi network
   * @param wifiPassword
   *          password for the wifi network
   */
  @Rpc(description = "Connects a wifi network as priority by pasing ssid")
  public void wifiPriorityConnect(@RpcParameter(name = "wifiSSID") String wifiSSID,
      @RpcParameter(name = "wifiPassword") String wifiPassword) {
    WifiConfiguration wifiConfig = new WifiConfiguration();
    wifiConfig.SSID = "\"" + wifiSSID + "\"";
    if(wifiPassword == null)
      wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    else
      wifiConfig.preSharedKey = "\"" + wifiPassword + "\"";
    WifiActionListener listener = new WifiActionListener(mEventFacade, "PriorityConnect");
    mWifi.connect(wifiConfig, listener);
  }

  @Rpc(description = "Reassociates with the currently active access point.",
           returns = "True if the operation succeeded.")
  public Boolean wifiReassociate() {
    return mWifi.reassociate();
  }

  @Rpc(description = "Reconnects to the currently active access point.",
           returns = "True if the operation succeeded.")
  public Boolean wifiReconnect() {
    return mWifi.reconnect();
  }

  @Rpc(description = "Remove a configured network.",
           returns = "True if the operation succeeded.")
   public Boolean wifiRemoveNetwork(@RpcParameter(name = "netId") Integer netId) {
     return mWifi.removeNetwork(netId);
   }

  @Rpc(description = "Starts a scan for Wifi access points.",
           returns = "True if the scan was initiated successfully.")
  public Boolean wifiStartScan() {
    mService.registerReceiver(mScanResultsAvailableReceiver, mScanFilter);
    return mWifi.startScan();
  }

  @Rpc(description = "Start receiving wifi state change related broadcasts.")
  public void wifiStartTrackingStateChange() {
      mService.registerReceiver(mStateChangeReceiver, mStateChangeFilter);
  }

  @Rpc(description = "Stop receiving wifi state change related broadcasts.")
  public void wifiStopTrackingStateChange() {
      mService.unregisterReceiver(mStateChangeReceiver);
  }

  @Rpc(description = "Toggle Wifi on and off.", returns = "True if Wifi is enabled.")
  public Boolean wifiToggleState(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
    if (enabled == null) {
      enabled = !wifiCheckState();
    }
    mWifi.setWifiEnabled(enabled);
    return enabled;
  }

  @Override
  public void shutdown() {
    wifiLockRelease();
    mService.unregisterReceiver(mStateChangeReceiver);
  }
}
