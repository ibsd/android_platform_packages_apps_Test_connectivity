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

package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.BssidInfo;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanSettings;
import android.os.Bundle;
import android.os.SystemClock;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * WifiScanner functions.
 */
public class WifiScannerFacade extends RpcReceiver {
    private final Service mService;
    private final EventFacade mEventFacade;
    private final WifiScanner mScan;
    // These counters are just for indexing;
    // they do not represent the total number of listeners
    private static int WifiScanListenerCnt;
    private static int WifiChangeListenerCnt;
    private static int WifiBssidListenerCnt;
    private final ConcurrentHashMap<Integer, WifiScanListener> scanListeners;
    private final ConcurrentHashMap<Integer, ChangeListener> trackChangeListeners;
    private final ConcurrentHashMap<Integer, WifiBssidListener> trackBssidListeners;
    private static ConcurrentHashMap<Integer, ScanResult[]> wifiScannerResultList;
    private static ConcurrentHashMap<Integer, ScanData[]> wifiScannerDataList;

    public WifiScannerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mScan = (WifiScanner) mService.getSystemService(Context.WIFI_SCANNING_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
        scanListeners = new ConcurrentHashMap<Integer, WifiScanListener>();
        trackChangeListeners = new ConcurrentHashMap<Integer, ChangeListener>();
        trackBssidListeners = new ConcurrentHashMap<Integer, WifiBssidListener>();
        wifiScannerResultList = new ConcurrentHashMap<Integer, ScanResult[]>();
        wifiScannerDataList = new ConcurrentHashMap<Integer, ScanData[]>();
    }

    public static List<ScanResult> getWifiScanResult(Integer listener_index) {
        ScanResult[] sr = wifiScannerResultList.get(listener_index);
        return Arrays.asList(sr);
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
            mEventFacade.postEvent(mEventType + mIndex + "onSuccess", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onFailure(int reason, String description) {
            Log.d("onFailure " + mEventType + " " + mIndex);
            mResults.putString("Type", "onFailure");
            mResults.putInt("Reason", reason);
            mResults.putString("Description", description);
            mEventFacade.postEvent(mEventType + mIndex + "onFailure", mResults.clone());
            mResults.clear();
        }

        public void reportResult(ScanResult[] results, String type) {
            Log.d("reportResult " + mEventType + " " + mIndex);
            mResults.putLong("Timestamp", System.currentTimeMillis() / 1000);
            mResults.putString("Type", type);
            mResults.putParcelableArray("Results", results);
            mEventFacade.postEvent(mEventType + mIndex + type, mResults.clone());
            mResults.clear();
        }
    }

    /**
     * Constructs a wifiScanListener obj and returns it
     *
     * @return WifiScanListener
     */
    private WifiScanListener genWifiScanListener() {
        WifiScanListener mWifiScannerListener = MainThread.run(mService,
                new Callable<WifiScanListener>() {
                    @Override
                    public WifiScanListener call() throws Exception {
                        return new WifiScanListener();
                    }
                });
        scanListeners.put(mWifiScannerListener.mIndex, mWifiScannerListener);
        return mWifiScannerListener;
    }

    private class WifiScanListener implements WifiScanner.ScanListener {
        private static final String mEventType = "WifiScannerScan";
        protected final Bundle mScanResults;
        protected final Bundle mScanData;
        private final WifiActionListener mWAL;
        public int mIndex;

        public WifiScanListener() {
            mScanResults = new Bundle();
            mScanData = new Bundle();
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
            scanListeners.remove(mIndex);
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
        public void onFullResult(ScanResult fullScanResult) {
            Log.d("onFullResult WifiScanListener " + mIndex);
            mWAL.reportResult(new ScanResult[]{fullScanResult}, "onFullResult");
        }

        @Override
        public void onResults(ScanData[] results) {
            Log.d("onResult WifiScanListener " + mIndex);
            wifiScannerDataList.put(mIndex, results);
            mScanData.putLong("Timestamp", System.currentTimeMillis()/1000);
            mScanData.putString("Type", "onResults");
            mScanData.putParcelableArray("Results", results);
            mEventFacade.postEvent(mEventType + mIndex + "onResults", mScanData.clone());
            mScanData.clear();
        }
    }

    /**
     * Constructs a ChangeListener obj and returns it
     *
     * @return ChangeListener
     */
    private ChangeListener genWifiChangeListener() {
        ChangeListener mWifiChangeListener = MainThread.run(mService,
                new Callable<ChangeListener>() {
                    @Override
                    public ChangeListener call() throws Exception {
                        return new ChangeListener();
                    }
                });
        trackChangeListeners.put(mWifiChangeListener.mIndex, mWifiChangeListener);
        return mWifiChangeListener;
    }

    private class ChangeListener implements WifiScanner.WifiChangeListener {
        private static final String mEventType = "WifiScannerChange";
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
            trackChangeListeners.remove(mIndex);
            mWAL.onFailure(reason, description);
        }

        /**
         * indicates that changes were detected in wifi environment
         *
         * @param results indicate the access points that exhibited change
         */
        @Override
        public void onChanging(ScanResult[] results) { /* changes are found */
            mWAL.reportResult(results, "onChanging");
        }

        /**
         * indicates that no wifi changes are being detected for a while
         *
         * @param results indicate the access points that are bing monitored for change
         */
        @Override
        public void onQuiescence(ScanResult[] results) { /* changes settled down */
            mWAL.reportResult(results, "onQuiescence");
        }
    }

    private WifiBssidListener genWifiBssidListener() {
        WifiBssidListener mWifiBssidListener = MainThread.run(mService,
                new Callable<WifiBssidListener>() {
                    @Override
                    public WifiBssidListener call() throws Exception {
                        return new WifiBssidListener();
                    }
                });
        trackBssidListeners.put(mWifiBssidListener.mIndex, mWifiBssidListener);
        return mWifiBssidListener;
    }

    private class WifiBssidListener implements WifiScanner.BssidListener {
        private static final String mEventType = "WifiScannerBssid";
        protected final Bundle mResults;
        private final WifiActionListener mWAL;
        public int mIndex;

        public WifiBssidListener() {
            mResults = new Bundle();
            WifiBssidListenerCnt += 1;
            mIndex = WifiBssidListenerCnt;
            mWAL = new WifiActionListener(mEventType, mIndex, mResults);
        }

        @Override
        public void onSuccess() {
            mWAL.onSuccess();
        }

        @Override
        public void onFailure(int reason, String description) {
            trackBssidListeners.remove(mIndex);
            mWAL.onFailure(reason, description);
        }

        @Override
        public void onFound(ScanResult[] results) {
            mWAL.reportResult(results, "onFound");
        }

        @Override
        public void onLost(ScanResult[] results) {
            mWAL.reportResult(results, "onLost");
        }
    }

    private ScanSettings parseScanSettings(String scanSettings) throws JSONException {
        JSONObject j = new JSONObject(scanSettings);
        ScanSettings result = new ScanSettings();
        if (j.has("band")) {
            result.band = j.optInt("band");
        }
        if (j.has("channels")) {
            JSONArray chs = j.getJSONArray("channels");
            ChannelSpec[] channels = new ChannelSpec[chs.length()];
            for (int i = 0; i < channels.length; i++) {
                channels[i] = new ChannelSpec(chs.getInt(i));
            }
            result.channels = channels;
        }
        if (j.has("maxScansToCache")) {
            result.maxScansToCache = j.getInt("maxScansToCache");
        }
        /* periodInMs and reportEvents are required */
        result.periodInMs = j.getInt("periodInMs");
        result.reportEvents = j.getInt("reportEvents");
        if (j.has("numBssidsPerScan")) {
            result.numBssidsPerScan = j.getInt("numBssidsPerScan");
        }
        return result;
    }

    private BssidInfo parseBssidInfo(String info) throws JSONException {
        JSONObject bi = new JSONObject(info);
        BssidInfo bssidInfo = new BssidInfo();
        bssidInfo.bssid = bi.getString("bssid");
        bssidInfo.high = bi.getInt("high");
        bssidInfo.low = bi.getInt("low");
        bssidInfo.frequencyHint = bi.getInt("frequencyHint");
        return bssidInfo;
    }

    /**
     * Starts periodic WifiScanner scan
     * @param scanSettings
     * @return the id of the scan listener associated with this scan
     * @throws JSONException
     */
    @Rpc(description = "Starts a WifiScanner Background scan")
    public Integer wifiScannerStartBackgroundScan(@RpcParameter(name = "scanSettings") String scanSettings)
            throws JSONException {
        ScanSettings ss = parseScanSettings(scanSettings);
        Log.d("startWifiScannerScan with " + ss.channels);
        WifiScanListener mListener = genWifiScanListener();
        mScan.startBackgroundScan(ss, mListener);
        return mListener.mIndex;
    }


    /**
     * Stops a WifiScanner scan
     * @param listener_mIndex the id of the scan listener whose scan to stop
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing  WifiScanner Background scan")
    public void wifiScannerStopBackgroundScan(@RpcParameter(name = "listener") Integer listener_index)
            throws Exception {
        if(!scanListeners.containsKey(listener_index)) {
          throw new Exception("Background scan session " + listener_index + " does not exist");
        }
        WifiScanListener mListener = scanListeners.get(listener_index);
        Log.d("stopWifiScannerScan mListener "+ mListener.mIndex );
        mScan.stopBackgroundScan(mListener);
        wifiScannerResultList.remove(listener_index);
        scanListeners.remove(listener_index);
    }

    /**
     * Starts periodic WifiScanner scan
     * @param scanSettings
     * @return the id of the scan listener associated with this scan
     * @throws JSONException
     */
    @Rpc(description = "Starts a WifiScanner single scan")
    public Integer wifiScannerStartScan(@RpcParameter(name = "scanSettings") String scanSettings)
            throws JSONException {
        ScanSettings ss = parseScanSettings(scanSettings);
        Log.d("startWifiScannerScan with " + ss.channels);
        WifiScanListener mListener = genWifiScanListener();
        mScan.startScan(ss, mListener);
        return mListener.mIndex;
    }


    /**
     * Stops a WifiScanner scan
     * @param listener_mIndex the id of the scan listener whose scan to stop
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing  WifiScanner single scan")
    public void wifiScannerStopScan(@RpcParameter(name = "listener") Integer listener_index)
            throws Exception {
        if(!scanListeners.containsKey(listener_index)) {
          throw new Exception("Background scan session " + listener_index + " does not exist");
        }
        WifiScanListener mListener = scanListeners.get(listener_index);
        Log.d("stopWifiScannerScan mListener "+ mListener.mIndex );
        mScan.stopScan(mListener);
        wifiScannerResultList.remove(listener_index);
        scanListeners.remove(listener_index);
    }

    /** RPC Methods */
    @Rpc(description = "Returns the channels covered by the specified band number.")
    public List<Integer> wifiScannerGetAvailableChannels(@RpcParameter(name = "band") Integer band) {
        return mScan.getAvailableChannels(band);
    }

    /**
     * Starts tracking wifi changes
     *
     * @return the id of the change listener associated with this track
     * @throws Exception
     */
    @Rpc(description = "Starts tracking wifi changes")
    public Integer wifiScannerStartTrackingChange() throws Exception {
        ChangeListener listener = genWifiChangeListener();
        mScan.startTrackingWifiChange(listener);
        return listener.mIndex;
    }

    /**
     * Stops tracking wifi changes
     *
     * @param listener_index the id of the change listener whose track to stop
     * @throws Exception
     */
    @Rpc(description = "Stops tracking wifi changes")
    public void wifiScannerStopTrackingChange(
            @RpcParameter(name = "listener") Integer listener_index
            ) throws Exception {
        if (!trackChangeListeners.containsKey(listener_index)) {
            throw new Exception("Wifi change tracking session " + listener_index
                    + " does not exist");
        }
        ChangeListener mListener = trackChangeListeners.get(listener_index);
        mScan.stopTrackingWifiChange(mListener);
        trackChangeListeners.remove(listener_index);
    }

    /**
     * Starts tracking changes of the specified bssids.
     *
     * @param bssidInfos An array of json strings, each representing a BssidInfo object.
     * @param apLostThreshold
     * @return The index of the listener used to start the tracking.
     * @throws JSONException
     */
    @Rpc(description = "Starts tracking changes of the specified bssids.")
    public Integer wifiScannerStartTrackingBssids(
            @RpcParameter(name = "bssidInfos") String[] bssidInfos,
            @RpcParameter(name = "apLostThreshold") Integer apLostThreshold
            ) throws JSONException {
        BssidInfo[] infos = new BssidInfo[bssidInfos.length];
        for (int i = 0; i < bssidInfos.length; i++) {
            infos[i] = parseBssidInfo(bssidInfos[i]);
        }
        WifiBssidListener listener = genWifiBssidListener();
        mScan.startTrackingBssids(infos, apLostThreshold, listener);
        return listener.mIndex;
    }

    /**
     * Stops tracking the list of APs associated with the input listener
     *
     * @param listener_index the id of the bssid listener whose track to stop
     * @throws Exception
     */
    @Rpc(description = "Stops tracking changes in the APs on the list")
    public void wifiScannerStopTrackingBssids(
            @RpcParameter(name = "listener") Integer listener_index
            ) throws Exception {
        if (!trackBssidListeners.containsKey(listener_index)) {
            throw new Exception("Bssid tracking session " + listener_index + " does not exist");
        }
        WifiBssidListener mListener = trackBssidListeners.get(listener_index);
        mScan.stopTrackingBssids(mListener);
        trackBssidListeners.remove(listener_index);
    }

    @Rpc(description = "Returns a list of mIndexes of existing listeners")
    public Set<Integer> wifiGetCurrentScanIndexes() {
        return scanListeners.keySet();
    }

    /**
     * Starts tracking wifi changes
     *
     * @return the id of the change listener associated with this track
     * @throws Exception
     */
    @Rpc(description = "Starts tracking wifi changes")
    public Integer startTrackingChange(
            @RpcParameter(name = "bssidInfos") String[] bssidInfos,
            @RpcParameter(name = "rssiSS") Integer rssiSS,
            @RpcParameter(name = "lostApSS") Integer lostApSS,
            @RpcParameter(name = "unchangedSS") Integer unchangedSS,
            @RpcParameter(name = "minApsBreachingThreshold") Integer minApsBreachingThreshold,
            @RpcParameter(name = "periodInMs") Integer periodInMs) throws Exception {
        Log.d("starting change track");
        BssidInfo[] mBssidInfos = new BssidInfo[bssidInfos.length];
        for (int i = 0; i < bssidInfos.length; i++) {
            Log.d("android_scripting " + bssidInfos[i]);
            String[] tokens = bssidInfos[i].split(" ");
            if (tokens.length != 3) {
                throw new Exception("Invalid bssid info: " + bssidInfos[i]);

            }
            int rssiHI = Integer.parseInt(tokens[1]);
            BssidInfo mBI = new BssidInfo();
            mBI.bssid = tokens[0];
            mBI.low = rssiHI - unchangedSS;
            mBI.high = rssiHI + unchangedSS;
            mBI.frequencyHint = Integer.parseInt(tokens[2]);
            mBssidInfos[i] = mBI;
        }
        ChangeListener mListener = genWifiChangeListener();
        mScan.configureWifiChange(rssiSS, lostApSS, unchangedSS, minApsBreachingThreshold,
                periodInMs, mBssidInfos);
        mScan.startTrackingWifiChange(mListener);
        return mListener.mIndex;
    }

    /**
     * Starts tracking changes of the wifi networks specified in a list of bssid
     *
     * @param bssidInfos a list specifying which wifi networks to track
     * @param apLostThreshold signal strength below which an AP is considered lost
     * @return the id of the bssid listener associated with this track
     * @throws Exception
     */
    @Rpc(description = "Starts tracking changes in the APs specified by the list")
    public Integer startTrackingBssid(String[] bssidInfos, Integer apLostThreshold)
            throws Exception {
        // Instantiates BssidInfo objs
        BssidInfo[] mBssidInfos = new BssidInfo[bssidInfos.length];
        for (int i = 0; i < bssidInfos.length; i++) {
            Log.d("android_scripting " + bssidInfos[i]);
            String[] tokens = bssidInfos[i].split(" ");
            if (tokens.length != 3) {
                throw new Exception("Invalid bssid info: " + bssidInfos[i]);

            }
            int a = Integer.parseInt(tokens[1]);
            int b = Integer.parseInt(tokens[2]);
            BssidInfo mBI = new BssidInfo();
            mBI.bssid = tokens[0];
            mBI.low = a < b ? a : b;
            mBI.high = a < b ? b : a;
            mBssidInfos[i] = mBI;
        }
        WifiBssidListener mWHL = genWifiBssidListener();
        mScan.startTrackingBssids(mBssidInfos, apLostThreshold, mWHL);
        return mWHL.mIndex;
    }

    /**
     * Shuts down all activities associated with WifiScanner
     */
    @Rpc(description = "Shuts down all WifiScanner activities and remove listeners.")
    public void wifiScannerShutdown() {
        this.shutdown();
    }

    /**
     * Stops all activity
     */
    @Override
    public void shutdown() {
        try {
            if (!scanListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, WifiScanListener>> iter = scanListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, WifiScanListener> entry = iter.next();
                    this.wifiScannerStopScan(entry.getKey());
                }
            }
            if (!trackChangeListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, ChangeListener>> iter = trackChangeListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, ChangeListener> entry = iter.next();
                    this.wifiScannerStopScan(entry.getKey());
                }
            }
            if (!trackBssidListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, WifiBssidListener>> iter = trackBssidListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, WifiBssidListener> entry = iter.next();
                    this.wifiScannerStopScan(entry.getKey());
                }
            }
        } catch (Exception e) {
            Log.e("Shutdown failed: " + e.toString());
        }
    }
}
