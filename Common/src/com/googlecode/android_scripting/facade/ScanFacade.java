package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.*;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

import java.util.Hashtable;
import java.util.concurrent.Callable;

/**
 * ScanManager functions.
 */
public class ScanFacade extends RpcReceiver {
  private final Service mService;
  private final EventFacade mEventFacade;
  private final WifiScanner mScan;
  //These counters are just for indexing;
  //they do not represent the total number of listeners
  private static int WifiScanListenerCnt;
  private static int WifiChangeListenerCnt;
  private static int WifiHotspotListenerCnt;
  private Hashtable<Integer, WifiScanListener> wifiScannerListenerList;
  private Hashtable<Integer, ChangeListener> wifiChangeListenerList;
  private Hashtable<Integer, WifiHotspotListener> wifiHotspotListenerList;

  public ScanFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mScan = (WifiScanner) mService.getSystemService(Context.WIFI_SCANNING_SERVICE);
    mEventFacade = manager.getReceiver(EventFacade.class);
    wifiScannerListenerList = new Hashtable<Integer, WifiScanListener>();
    wifiChangeListenerList = new Hashtable<Integer, ChangeListener>();
    wifiHotspotListenerList = new Hashtable<Integer, WifiHotspotListener>();
  }

  private class WifiActionListener implements WifiScanner.ActionListener {
    private final Bundle mStatus;
    private final Bundle mResults;
    public int index;
    protected String listenerType;

    public WifiActionListener(String type, int idx, Bundle statusBundle, Bundle resultBundle) {
      this.index = idx;
      this.listenerType = type;
      this.mStatus = statusBundle;
      this.mResults = resultBundle;
    }

    @Override
    public void onSuccess(Object result) {
      Log.d("android_scripting change onSuccess " + listenerType + index);
      mStatus.putString("ID", listenerType + index);
      mStatus.putBoolean("Status", true);
      mEventFacade.postEvent("Started", mStatus.clone());
      mStatus.clear();
    }

    @Override
    public void onFailure(int reason, Object exception) {
      Log.d("android_scripting change onFailure " + listenerType + index);
      mStatus.putString("ID", listenerType + index);
      mStatus.putBoolean("Status", false);
      mStatus.putInt("Reason", reason);
      mStatus.putString("Exception", exception.toString());
      mEventFacade.postEvent("Failed", mStatus.clone());
      mStatus.clear();
    }

    public void reportResult(ScanResult[] results, String eventType) {
      Log.d("android_scripting " + eventType + " " + listenerType + index);
      mResults.putString("ID", listenerType + index);
      mResults.putLong("Timestamp", System.currentTimeMillis()/1000);
      mResults.putString("Type", eventType);
      mResults.putParcelableArray("Results", results);
      mEventFacade.postEvent("ScanResults", mResults.clone());
      mResults.clear();
    }
  }

  /**
   * Constructs a wifiScanListener obj and returns it
   * @return WifiScanListener
   */
  private WifiScanListener genWifiScanListener() {
    WifiScanListener mWifiScannerListener = MainThread.run(mService, new Callable<WifiScanListener>() {
      @Override
      public WifiScanListener call() throws Exception {
        return new WifiScanListener();
      }
    });
    wifiScannerListenerList.put(mWifiScannerListener.index, mWifiScannerListener);
    return mWifiScannerListener;
  }

  private class WifiScanListener implements WifiScanner.ScanListener {
    protected final Bundle mScanResults;
    protected final Bundle mScanStatus;
    private final WifiActionListener mWAL;
    public int index;

    public WifiScanListener() {
      mScanStatus = new Bundle();
      mScanResults = new Bundle();
      WifiScanListenerCnt += 1;
      index = WifiScanListenerCnt;
      mWAL = new WifiActionListener("WifiScanListener", index, mScanStatus, mScanResults);
    }

    @Override
    public void onSuccess(Object result) {
      mWAL.onSuccess(result);
    }

    @Override
    public void onFailure(int reason, Object exception) {
      mWAL.onFailure(reason, exception);
    }

    @Override
    public void onPeriodChanged(int periodInMs) {
      mScanStatus.putString("ID", "WifiScanListener" + index);
      mScanStatus.putBoolean("Status", true);
      mScanStatus.putInt("NewPeriod", periodInMs);
      mEventFacade.postEvent("onPeriodChanged", mScanStatus.clone());
      mScanStatus.clear();
    }

    @Override
    public void onResults(ScanResult[] results) {
      mWAL.reportResult(results, "onScanResults");
    }

    @Override
    public void onFullResult(FullScanResult fullScanResult) {
      Log.d("android_scripting onFullResult WifiScanListener " + index);
      mScanResults.putString("ID", "WifiScanListener" + index);
      mScanResults.putString("ScanResult", fullScanResult.result.toString());
      for (InformationElement ie : fullScanResult.informationElements) {
        mScanResults.putInt("ExtraInfoId", ie.id);
        mScanResults.putByteArray("ExtraInfoBytes", ie.bytes);
      }
      mEventFacade.postEvent("onFullResult", mScanResults.clone());
      mScanResults.clear();
    }
  }

  /**
   * Constructs a ChangeListener obj and returns it
   * @return ChangeListener
   */
  public ChangeListener genWifiChangeListener() {
    ChangeListener mWifiChangeListener = MainThread.run(mService, new Callable<ChangeListener>() {
      @Override
      public ChangeListener call() throws Exception {
        return new ChangeListener();
      }
    });
    wifiChangeListenerList.put(mWifiChangeListener.index, mWifiChangeListener);
    return mWifiChangeListener;
  }

  private class ChangeListener implements WifiScanner.WifiChangeListener {
    protected final Bundle mStatus;
    protected final Bundle mResults;
    private final WifiActionListener mWAL;
    public int index;

    public ChangeListener() {
      mStatus = new Bundle();
      mResults = new Bundle();
      WifiChangeListenerCnt += 1;
      index = WifiChangeListenerCnt;
      mWAL = new WifiActionListener("WifiChangeListener", index, mStatus, mResults);
    }

    @Override
    public void onSuccess(Object result) {
      mWAL.onSuccess(result);
    }

    @Override
    public void onFailure(int reason, Object exception) {
      mWAL.onFailure(reason, exception);
    }
    /** indicates that changes were detected in wifi environment
     * @param results indicate the access points that exhibited change
     */
    @Override
    public void onChanging(ScanResult[] results) {           /* changes are found */
      mWAL.reportResult(results, "onChanging");
    }
    /** indicates that no wifi changes are being detected for a while
     * @param results indicate the access points that are bing monitored for change
     */
    @Override
    public void onQuiescence(ScanResult[] results) {         /* changes settled down */
      mWAL.reportResult(results, "onQuiescence");
    }
  }

  public WifiHotspotListener genWifiHotspotListener() {
    WifiHotspotListener mWifiHotspotListener = MainThread.run(mService, new Callable<WifiHotspotListener>() {
      @Override
      public WifiHotspotListener call() throws Exception {
        return new WifiHotspotListener();
      }
    });
    wifiHotspotListenerList.put(mWifiHotspotListener.index, mWifiHotspotListener);
    return mWifiHotspotListener;
  }

  private class WifiHotspotListener implements WifiScanner.HotlistListener {
    protected final Bundle mStatus;
    protected final Bundle mResults;
    private final WifiActionListener mWAL;
    public int index;

    public WifiHotspotListener() {
      mStatus = new Bundle();
      mResults = new Bundle();
      WifiHotspotListenerCnt += 1;
      index = WifiHotspotListenerCnt;
      mWAL = new WifiActionListener("HotspotListener", index, mStatus, mResults);
    }

    @Override
    public void onSuccess(Object result) {
      mWAL.onSuccess(result);
    }

    @Override
    public void onFailure(int reason, Object exception) {
      mWAL.onFailure(reason, exception);
    }

    @Override
    public void onFound(ScanResult[] results) {
      mWAL.reportResult(results, "onHotspotFound");
    }
  }

  /** RPC Method Section */

  /**
   * Starts periodic wifi background scan
   * @param periodInMs
   * @param channel_freqs frequencies of channels to scan
   * @return the id of the scan listener associated with this scan
   */
  @Rpc(description = "Starts a periodic Wifi scan in background.")
  @RpcStartEvent("WifiScan")
  public Integer startWifiBackgroundScan(@RpcParameter(name = "periodInMs") Integer periodInMs,
                                  @RpcParameter(name = "channel_freqs") Integer[] channel_freqs) {
    WifiScanner.ScanSettings ss = new WifiScanner.ScanSettings();
    ss.channels = new WifiScanner.ChannelSpec[channel_freqs.length];
    for(int i=0; i<channel_freqs.length; i++) {
      ss.channels[i] = new WifiScanner.ChannelSpec(channel_freqs[i]);
    }
    ss.periodInMs = periodInMs;
    Log.d("android_scripting periodInMs " + ss.periodInMs);
    for(int i=0; i<ss.channels.length; i++) {
      Log.d("android_scripting " + ss.channels[i].frequency + " " + ss.channels[i].passive + " " + ss.channels[i].dwellTimeMS);
    }
    WifiScanListener mListener = genWifiScanListener();
    mScan.startBackgroundScan(ss, mListener);
    return mListener.index;
  }

  /**
   * Stops a wifi background scan
   * @param listener_index the id of the scan listener whose scan to stop
   */
  @Rpc(description = "Stops an ongoing periodic Wifi scan in background")
  @RpcStopEvent("WifiScan")
  public void stopWifiBackgroundScan(@RpcParameter(name = "listener") Integer listener_index) {
    Log.d("android_scripting stopping background scan");
    WifiScanListener mListener = wifiScannerListenerList.get(listener_index);
    Log.d("android_scripting mListener " + mListener.index + mListener.toString());
    mScan.stopBackgroundScan(mListener);
    wifiScannerListenerList.remove(listener_index);
  }

  @Rpc(description = "Returns a list of indexes of existing listeners")
  public Integer[] showWifiScanListeners() {
    Integer[] result = new Integer[wifiScannerListenerList.size()];
    int j = 0;
    for(int i : wifiScannerListenerList.keySet()) {
      result[j] = wifiScannerListenerList.get(i).index;
      j += 1;
    }
    return result;
  }

  /**
   * Starts tracking wifi changes
   * @return the id of the change listener associated with this track
   */
  @Rpc(description = "Starts tracking wifi changes")
  public Integer startTrackingChange() {
    Log.d("android_scripting starting change track");
    ChangeListener mListener = genWifiChangeListener();
    mScan.startTrackingWifiChange(mListener);
    return mListener.index;
  }

  /**
   * Stops tracking wifi changes
   * @param listener_index the id of the change listener whose track to stop
   */
  @Rpc(description = "Stops tracking wifi changes")
  public void stopTrackingChange(@RpcParameter(name = "listener") Integer listener_index) {
    ChangeListener mListener = wifiChangeListenerList.get(listener_index);
    mScan.stopTrackingWifiChange(mListener);
    wifiChangeListenerList.remove(listener_index);
  }

  /**
   * Starts tracking changes of the wifi networks specified in a hotlist
   * @param hotspotInfos a 'hotlist' specifying which wifi networks to track
   * @param apLostThreshold signal strength below which an AP is considered lost
   * @return the id of the hotlist listener associated with this track
   * @throws Exception
   */
  @Rpc(description = "Starts tracking changes of the APs on hotlist")
  public Integer setHotlist(String[] hotspotInfos, Integer apLostThreshold) throws Exception {
    //Instantiates HotspotInfo objs
    HotspotInfo[] mHotspotInfos = new HotspotInfo[hotspotInfos.length];
    for(int i=0; i<hotspotInfos.length; i++) {
      Log.d("android_scripting " + hotspotInfos[i]);
      String[] tokens = hotspotInfos[i].split(" ");
      if(tokens.length!=3) {
        throw new Exception("Invalid hotspot info: "+hotspotInfos[i]);
      }
      int a = Integer.parseInt(tokens[1]);
      int b = Integer.parseInt(tokens[2]);
      HotspotInfo mHI = new HotspotInfo();
      mHI.bssid = tokens[0];
      mHI.low = a<b? a:b;
      mHI.high = a<b? b:a;
      mHotspotInfos[i] = mHI;
    }
    WifiHotspotListener mWHL = genWifiHotspotListener();
    mScan.setHotlist(mHotspotInfos, apLostThreshold, mWHL);
    return mWHL.index;
  }

  /**
   * Stops tracking the hotlist associated with the input listener
   * @param listener_index the id of the hotspot listener whose track to stop
   */
  @Rpc(description = "Stops tracking changes of the APs on hotlist")
  public void resetHotlist(@RpcParameter(name = "listener") Integer listener_index) {
    WifiHotspotListener mListener = wifiHotspotListenerList.get(listener_index);
    mScan.resetHotlist(mListener);
    wifiHotspotListenerList.remove(listener_index);
  }

  /**
   * Shuts down all activities associated with WifiScanner
   */
  @Rpc(description = "Shuts down all WifiScanner activities")
  public void wifiScannerShutdown() {
    this.shutdown();
  }

  /**
   * Stops all activity
   */
  @Override
  public void shutdown() {
    if(!wifiScannerListenerList.isEmpty()) {
      for(int i : wifiScannerListenerList.keySet()) {
        this.stopWifiBackgroundScan(i);
      }
    }
    if(!wifiChangeListenerList.isEmpty()) {
      for(int i : wifiChangeListenerList.keySet()) {
        this.stopTrackingChange(i);
      }
    }
    if(!wifiHotspotListenerList.isEmpty()) {
      for(int i : wifiHotspotListenerList.keySet()) {
        this.resetHotlist(i);
      }
    }
  }
}
