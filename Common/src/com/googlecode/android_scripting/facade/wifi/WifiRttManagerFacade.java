
package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.Context;
import android.net.wifi.RttManager;
import android.net.wifi.RttManager.Capabilities;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.RttManager.RttListener;
import android.net.wifi.RttManager.RttParams;
import android.net.wifi.RttManager.RttResult;
import android.os.Bundle;
import android.os.Parcelable;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WifiRttManager functions.
 */
public class WifiRttManagerFacade extends RpcReceiver {
    private final Service mService;
    private final RttManager mRtt;
    private final EventFacade mEventFacade;
    private final Map<Integer, RttListener> mRangingListeners;

    public WifiRttManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mRtt = (RttManager) mService.getSystemService(Context.WIFI_RTT_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
        mRangingListeners = new Hashtable<Integer, RttListener>();
    }

    public static class RangingListener implements RttListener {
        private static final String TAG = "WifiRttRanging";
        private static int sCount = 0;
        private final EventFacade mEventFacade;
        public final int mId;

        public RangingListener(EventFacade eventFacade) {
            sCount += 1;
            mId = sCount;
            mEventFacade = eventFacade;
        }

        private Bundle packRttResult(RttResult result) {
            Bundle rttResult = new Bundle();
            rttResult.putString("BSSID", result.bssid);
            rttResult.putInt("txRate", result.txRate);
            rttResult.putInt("rxRate", result.rxRate);
            rttResult.putInt("distance", result.distance);
            rttResult.putInt("distanceStandardDeviation",
                    result.distanceStandardDeviation);
            rttResult.putInt("distanceSpread", result.distanceSpread);
            rttResult.putInt("burstDuration", result.burstDuration);
            rttResult.putLong("rtt", result.rtt);
            rttResult.putLong("rttStandardDeviation",
                    result.rttStandardDeviation);
            rttResult.putLong("rttSpread", result.rttSpread);
            rttResult.putLong("ts", result.ts);
            rttResult.putInt("rssi", result.rssi);
            rttResult.putInt("rssiSpread", result.rssiSpread);
            rttResult.putInt("retryAfterDuration", result.retryAfterDuration);
            rttResult.putInt("measurementType", result.measurementType);
            rttResult.putInt("status", result.status);
            rttResult.putInt("frameNumberPerBurstPeer",
                    result.frameNumberPerBurstPeer);
            rttResult.putInt("successMeasurementFrameNumber",
                    result.successMeasurementFrameNumber);
            rttResult.putInt("measurementFrameNumber",
                    result.measurementFrameNumber);
            rttResult.putInt("burstNumber", result.burstNumber);
            rttResult.putInt("status", result.status);
            return rttResult;
        }

        @Override
        public void onSuccess(RttResult[] results) {
            Bundle msg = new Bundle();
            Parcelable[] resultBundles = new Parcelable[results.length];
            for (int i = 0; i < results.length; i++) {
                resultBundles[i] = packRttResult(results[i]);
            }
            msg.putParcelableArray("Results", resultBundles);
            mEventFacade
                    .postEvent(RangingListener.TAG + mId + "onSuccess", msg);
        }

        @Override
        public void onFailure(int reason, String description) {
            Bundle msg = new Bundle();
            msg.putInt("Reason", reason);
            msg.putString("Description", description);
            mEventFacade
                    .postEvent(RangingListener.TAG + mId + "onFailure", msg);
        }

        @Override
        public void onAborted() {
            mEventFacade.postEvent(RangingListener.TAG + mId + "onAborted",
                    new Bundle());
        }
    }

    @Rpc(description = "Get wifi Rtt capabilities.")
    public RttCapabilities wifiRttGetCapabilities() {
        return mRtt.getRttCapabilities();
    }

    private RttParams parseRttParam(String rttParam) throws JSONException {
        JSONObject j = new JSONObject(rttParam);
        RttParams result = new RttParams();
        if (j.has("deviceType")) {
            result.deviceType = j.getInt("deviceType");
        }
        if (j.has("requestType")) {
            result.requestType = j.getInt("requestType");
        }
        if (j.has("bssid")) {
            result.bssid = j.getString("bssid");
        }
        if (j.has("frequency")) {
            result.frequency = j.getInt("frequency");
        }
        if (j.has("channelWidth")) {
            result.channelWidth = j.getInt("channelWidth");
        }
        if (j.has("centerFreq0")) {
            result.centerFreq0 = j.getInt("centerFreq0");
        }
        if (j.has("centerFreq1")) {
            result.centerFreq1 = j.getInt("centerFreq1");
        }
        if (j.has("numberBurst")) {
            result.numberBurst = j.getInt("numberBurst");
        }
        if (j.has("interval")) {
            result.interval = j.getInt("interval");
        }
        if (j.has("numSamplesPerBurst")) {
            result.numSamplesPerBurst = j.getInt("numSamplesPerBurst");
        }
        if (j.has("numRetriesPerMeasurementFrame")) {
            result.numRetriesPerMeasurementFrame = j
                    .getInt("numRetriesPerMeasurementFrame");
        }
        if (j.has("numRetriesPerFTMR")) {
            result.numRetriesPerFTMR = j.getInt("numRetriesPerFTMR");
        }
        if (j.has("LCIRequest")) {
            result.LCIRequest = j.getBoolean("LCIRequest");
        }
        if (j.has("LCRRequest")) {
            result.LCRRequest = j.getBoolean("LCRRequest");
        }
        if (j.has("preamble")) {
            result.preamble = j.getInt("preamble");
        }
        if (j.has("bandwidth")) {
            result.bandwidth = j.getInt("bandwidth");
        }
        return result;
    }

    @Rpc(description = "Start ranging.", returns = "Id of the listener associated with the started ranging.")
    public Integer wifiRttStartRanging(
            @RpcParameter(name = "params") String[] params)
            throws JSONException {
        RttParams[] rParams = new RttParams[params.length];
        for (int i = 0; i < params.length; i++) {
            rParams[i] = parseRttParam(params[i]);
        }
        RangingListener listener = new RangingListener(mEventFacade);
        mRangingListeners.put(listener.mId, listener);
        mRtt.startRanging(rParams, listener);
        return listener.mId;
    }

    @Rpc(description = "Stop ranging.")
    public void wifiRttStopRanging(@RpcParameter(name = "index") Integer index) {
        mRtt.stopRanging(mRangingListeners.remove(index));
    }

    @Override
    public void shutdown() {
        ArrayList<Integer> keys = new ArrayList<Integer>(
                mRangingListeners.keySet());
        for (int k : keys) {
            wifiRttStopRanging(k);
        }
    }
}
