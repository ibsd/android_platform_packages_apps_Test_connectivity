/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.HotspotInfo;
import android.os.Bundle;
import android.os.SystemClock;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * WifiScanner functions.
 *
 */
public class WifiScannerFacade extends RpcReceiver {
  private final Service mService;
  private final EventFacade mEventFacade;
  private final WifiScanner mScan;
  //These counters are just for indexing;
  //they do not represent the total number of listeners
  private static int WifiScanListenerCnt;
  private static int WifiChangeListenerCnt;
  private static int WifiHotspotListenerCnt;
  private final Hashtable<Integer, WifiScanListener> wifiScannerListenerList;
  private final Hashtable<Integer, ChangeListener> wifiChangeListenerList;
  private final Hashtable<Integer, WifiHotspotListener> wifiHotspotListenerList;
  private static Hashtable<Integer, ScanResult[]> wifiScannerResultList;

  public WifiScannerFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mScan = (WifiScanner) mService.getSystemService(Context.WIFI_SCANNING_SERVICE);
    mEventFacade = manager.getReceiver(EventFacade.class);
    wifiScannerListenerList = new Hashtable<Integer, WifiScanListener>();
    wifiChangeListenerList = new Hashtable<Integer, ChangeListener>();
    wifiHotspotListenerList = new Hashtable<Integer, WifiHotspotListener>();
    wifiScannerResultList = new Hashtable<Integer, ScanResult[]>();
  }

  public static List<ScanResult> getWifiScanResult(Integer listener_index, List<ScanResult> scanResults){
    synchronized (wifiScannerResultList) {
      ScanResult[] scanArray = wifiScannerResultList.get(listener_index);
      if (scanArray != null){
        for(ScanResult scanresult :  scanArray)
          scanResults.add(scanresult);
      }
      return scanResults;
    }
  }

  private class WifiActionListener implements WifiScanner.ActionListener {
    private final Bundle mResults;
    public int mIndex;
    protected String mEventType;

    public WifiActionListener(String type, int idx, Bundle resultBundle) {
      this.mIndex = idx;
      this.mEventType = type;
      this.mResults = resultBundle;
    }

    @Override
    public void onSuccess() {
      Log.d("onSuccess " + mEventType + " " + mIndex);
      mResults.putString("Type", "onSuccess");
      mResults.putLong("Realtime", SystemClock.elapsedRealtime());
      mEventFacade.postEvent(mEventType + mIndex, mResults.clone());
      mResults.clear();
    }

    @Override
    public void onFailure(int reason, String description) {
      Log.d("onFailure " + mEventType + " " + mIndex);
      mResults.putString("Type", "onFailure");
      mResults.putInt("Reason", reason);
      mResults.putString("Description", description);
      mEventFacade.postEvent(mEventType + mIndex, mResults.clone());
      mResults.clear();
    }

    public void reportResult(ScanResult[] results, String type) {
      Log.d("reportResult "+ mEventType + " "+ mIndex);
      mResults.putLong("Timestamp", System.currentTimeMillis()/1000);
      mResults.putString("Type", type);
      mResults.putParcelableArray("Results", results);
      mEventFacade.postEvent(mEventType + mIndex, mResults.clone());
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
    wifiScannerListenerList.put(mWifiScannerListener.mIndex, mWifiScannerListener);
    return mWifiScannerListener;
  }

  private class WifiScanListener implements WifiScanner.ScanListener {
    private static final String mEventType =  "WifiScannerScan";
    protected final Bundle mScanResults;
    private final WifiActionListener mWAL;
    public int mIndex;

    public WifiScanListener() {
      mScanResults = new Bundle();
      WifiScanListenerCnt += 1;
      mIndex = WifiScanListenerCnt;
      mWAL = new WifiActionListener(mEventType, mIndex, mScanResults);
    }

    @Override
    public void onSuccess() {
      mWAL.onSuccess();
    }

    @Override
    public void onFailure(int reason, String description) {
      mWAL.onFailure(reason, description);
    }

    @Override
    public void onPeriodChanged(int periodInMs) {
      Log.d("onPeriodChanged " + mEventType + " " + mIndex);
      mScanResults.putString("Type", "onPeriodChanged");
      mScanResults.putInt("NewPeriod", periodInMs);
      mEventFacade.postEvent(mEventType + mIndex, mScanResults.clone());
      mScanResults.clear();
    }

    @Override
    public void onResults(ScanResult[] results) {
      synchronized (wifiScannerResultList) {
        wifiScannerResultList.put(mIndex, results);
      }
      mWAL.reportResult(results, "onScanResults");
    }

    @Override
    public void onFullResult(ScanResult fullScanResult) {
      Log.d("onFullResult WifiScanListener " + mIndex);
      mWAL.reportResult(new ScanResult[]{fullScanResult}, "onFullResult");
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
    wifiChangeListenerList.put(mWifiChangeListener.mIndex, mWifiChangeListener);
    return mWifiChangeListener;
  }

  private class ChangeListener implements WifiScanner.WifiChangeListener {
    private static final String mEventType =  "WifiScannerChange";
    protected final Bundle mResults;
    private final WifiActionListener mWAL;
    public int mIndex;

    public ChangeListener() {
      mResults = new Bundle();
      WifiChangeListenerCnt += 1;
      mIndex = WifiChangeListenerCnt;
      mWAL = new WifiActionListener(mEventType, mIndex, mResults);
    }

    @Override
    public void onSuccess() {
      mWAL.onSuccess();
    }

    @Override
    public void onFailure(int reason, String description) {
      mWAL.onFailure(reason, description);
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
    wifiHotspotListenerList.put(mWifiHotspotListener.mIndex, mWifiHotspotListener);
    return mWifiHotspotListener;
  }

  private class WifiHotspotListener implements WifiScanner.HotspotListener {
    private static final String mEventType =  "WifiScannerHotspot";
    protected final Bundle mResults;
    private final WifiActionListener mWAL;
    public int mIndex;

    public WifiHotspotListener() {
      mResults = new Bundle();
      WifiHotspotListenerCnt += 1;
      mIndex = WifiHotspotListenerCnt;
      mWAL = new WifiActionListener(mEventType, mIndex, mResults);
    }

    @Override
    public void onSuccess() {
      mWAL.onSuccess();
    }

    @Override
    public void onFailure(int reason, String description) {
      mWAL.onFailure(reason, description);
    }

    @Override
    public void onFound(ScanResult[] results) {
      mWAL.reportResult(results, "onHotspotFound");
    }
  }

  /** RPC Method Section */

  /**
   * Starts periodic WifiScanner scan
   * @param periodInMs
   * @param channel_freqs frequencies of channels to scan
   * @return the id of the scan listener associated with this scan
   */
  @Rpc(description = "Starts a periodic WifiScanner scan")
  @RpcStartEvent("WifiScannerScan")
  public Integer startWifiScannerScan(@RpcParameter(name = "periodInMs") Integer periodInMs,
                                  @RpcParameter(name = "channel_freqs") Integer[] channel_freqs) {
    WifiScanner.ScanSettings ss = new WifiScanner.ScanSettings();
    ss.channels = new WifiScanner.ChannelSpec[channel_freqs.length];
    for(int i=0; i<channel_freqs.length; i++) {
      ss.channels[i] = new WifiScanner.ChannelSpec(channel_freqs[i]);
    }
    ss.periodInMs = periodInMs;
    Log.d("startWifiScannerScan periodInMs " + ss.periodInMs);
    for(int i=0; i<ss.channels.length; i++) {
      Log.d("startWifiScannerScan " + ss.channels[i].frequency + " " + ss.channels[i].passive + " " + ss.channels[i].dwellTimeMS);
    }
    WifiScanListener mListener = genWifiScanListener();
    mScan.startBackgroundScan(ss, mListener);
    return mListener.mIndex;
  }

  /**
   * Stops a WifiScanner scan
   * @param listener_mIndex the id of the scan listener whose scan to stop
   */
  @Rpc(description = "Stops an ongoing periodic WifiScanner scan")
  @RpcStopEvent("WifiScannerScan")
  public void stopWifiScannerScan(@RpcParameter(name = "listener") Integer listener_index) {
    WifiScanListener mListener = wifiScannerListenerList.get(listener_index);
    Log.d("stopWifiScannerScan mListener "+ mListener.mIndex );
    mScan.stopBackgroundScan(mListener);
    synchronized (wifiScannerResultList) {
      wifiScannerResultList.remove(listener_index);
    }
    wifiScannerListenerList.remove(listener_index);
  }

  @Rpc(description = "Returns a list of mIndexes of existing listeners")
  public Integer[] showWifiScanListeners() {
    Integer[] result = new Integer[wifiScannerListenerList.size()];
    int j = 0;
    for(int i : wifiScannerListenerList.keySet()) {
      result[j] = wifiScannerListenerList.get(i).mIndex;
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
    Log.d("starting change track");
    ChangeListener mListener = genWifiChangeListener();
    mScan.startTrackingWifiChange(mListener);
    return mListener.mIndex;
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
   * Starts tracking changes of the wifi networks specified in a list of hotspots
   * @param hotspotInfos a list specifying which wifi networks to track
   * @param apLostThreshold signal strength below which an AP is considered lost
   * @return the id of the hotspot listener associated with this track
   * @throws Exception
   */
  @Rpc(description = "Starts tracking changes in the APs specified by the list")
  public Integer startTrackingHotspots(String[] hotspotInfos, Integer apLostThreshold) throws Exception {
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
      mHI.low = a<b ? a:b;
      mHI.high = a<b ? b:a;
      mHotspotInfos[i] = mHI;
    }
    WifiHotspotListener mWHL = genWifiHotspotListener();
    mScan.startTrackingHotspots(mHotspotInfos, apLostThreshold, mWHL);
    return mWHL.mIndex;
  }

  /**
   * Stops tracking the list of APs associated with the input listener
   * @param listener_index the id of the hotspot listener whose track to stop
   */
  @Rpc(description = "Stops tracking changes in the APs on the list")
  public void stopTrackingHotspots(@RpcParameter(name = "listener") Integer listener_index) {
    WifiHotspotListener mListener = wifiHotspotListenerList.get(listener_index);
    mScan.stopTrackingHotspots(mListener);
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
        this.stopWifiScannerScan(i);
      }
    }
    if(!wifiChangeListenerList.isEmpty()) {
      for(int i : wifiChangeListenerList.keySet()) {
        this.stopTrackingChange(i);
      }
    }
    if(!wifiHotspotListenerList.isEmpty()) {
      for(int i : wifiHotspotListenerList.keySet()) {
        this.stopTrackingHotspots(i);
      }
    }
  }
}
