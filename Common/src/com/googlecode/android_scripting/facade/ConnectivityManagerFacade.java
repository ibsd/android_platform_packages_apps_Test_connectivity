/*
 * Copyright (C) 2010 Google Inc.
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.ConnectivityManager.PacketKeepaliveCallback;
import android.net.ConnectivityManager.PacketKeepalive;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.provider.Settings.SettingNotFoundException;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.tele.TelephonyConstants;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Access ConnectivityManager functions.
 */
public class ConnectivityManagerFacade extends RpcReceiver {

    public static int AIRPLANE_MODE_OFF = 0;
    public static int AIRPLANE_MODE_ON = 1;

    class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.e("ConnectivityReceiver received non-connectivity action!");
                return;
            }

            Bundle b = intent.getExtras();

            if (b == null) {
                Log.e("ConnectivityReceiver failed to receive extras!");
                return;
            }

            int netType =
                    b.getInt(ConnectivityManager.EXTRA_NETWORK_TYPE,
                            ConnectivityManager.TYPE_NONE);

            if(netType == ConnectivityManager.TYPE_NONE) {
                Log.i("ConnectivityReceiver received change to TYPE_NONE.");
                return;
            }

            /*
             * Technically there is a race condition here, but
             * retrieving the NetworkInfo from the bundle is deprecated.
             * See ConnectivityManager.EXTRA_NETWORK_INFO
            */
            for (NetworkInfo info : mManager.getAllNetworkInfo()) {
                if (info.getType() == netType) {
                    mEventFacade.postEvent(TelephonyConstants.EventConnectivityChanged, info);
                }
            }
        }
    }
    class PacketKeepaliveReceiver extends PacketKeepaliveCallback {
        @Override
        public void onStarted() {
            Log.d("PacketKeepaliveCallback on start!");
        }
        @Override
        public void onStopped() {
            Log.d("PacketKeepaliveCallback on stop!");
        }
        @Override
        public void onError(int error) {
            Log.d("PacketKeepaliveCallback on error! - code:" + error);
        }
    }
    class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onPreCheck(Network network) {
            Log.d("NetworkCallback onPreCheck");
        }
        @Override
        public void onAvailable(Network network) {
            Log.d("NetworkCallback onAvailable");
        }
        @Override
        public void onLosing(Network network, int maxMsToLive) {
            Log.d("NetworkCallback onLosing");
        }
        @Override
        public void onLost(Network network) {
            Log.d("NetworkCallback onLost");
        }
        @Override
        public void onUnavailable() {
            Log.d("NetworkCallback onUnavailable");
        }
        @Override
        public void onCapabilitiesChanged(Network network,
                NetworkCapabilities networkCapabilities) {
            Log.d("NetworkCallback onCapabilitiesChanged. RSSI:" +
                networkCapabilities.getSignalStrength());
        }
        @Override
        public void onNetworkSuspended(Network network) {
            Log.d("NetworkCallback onNetworkSuspended");
        }
        @Override
        public void onLinkPropertiesChanged(Network network,
                LinkProperties linkProperties) {
            Log.d("NetworkCallback onLinkPropertiesChanged");
        }
        @Override
        public void onNetworkResumed(Network network) {
            Log.d("NetworkCallback onNetworkResumed");
        }
    }
    private final ConnectivityManager mManager;
    private final Service mService;
    private final Context mContext;
    private final ConnectivityReceiver mConnectivityReceiver;
    private final EventFacade mEventFacade;
    private PacketKeepalive mPacketKeepalive;
    private NetworkCallback mNetworkCallback;
    private static HashMap<String, PacketKeepalive> mPacketKeepaliveMap =
            new HashMap<String, PacketKeepalive>();
    private static HashMap<String, NetworkCallback> mNetworkCallbackMap =
            new HashMap<String, NetworkCallback>();
    private boolean mTrackingConnectivityStateChange;

    public ConnectivityManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mContext = mService.getBaseContext();
        mManager = (ConnectivityManager) mService.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
        mConnectivityReceiver = new ConnectivityReceiver();
        mTrackingConnectivityStateChange = false;
    }

    @Rpc(description = "Listen for connectivity changes")
    public void startTrackingConnectivityStateChange() {
        if( !mTrackingConnectivityStateChange) {
            mTrackingConnectivityStateChange = true;
            mContext.registerReceiver(mConnectivityReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Rpc(description = "start natt keep alive")
    public String startNattKeepalive(Integer intervalSeconds, String srcAddrString,
            Integer srcPort, String dstAddrString) throws UnknownHostException {
        try{
            Network mNetwork = mManager.getActiveNetwork();
            InetAddress srcAddr = InetAddress.getByName(srcAddrString);
            InetAddress dstAddr = InetAddress.getByName(dstAddrString);
            Log.d("startNattKeepalive srcAddr:" + srcAddr.getHostAddress());
            Log.d("startNattKeepalive dstAddr:" + dstAddr.getHostAddress());
            Log.d("startNattKeepalive srcPort:" + srcPort);
            Log.d("startNattKeepalive intervalSeconds:" + intervalSeconds);
            mPacketKeepalive = mManager.startNattKeepalive(mNetwork, (int)intervalSeconds,
                    new PacketKeepaliveReceiver(), srcAddr, (int)srcPort, dstAddr);
            if (mPacketKeepalive != null){
                String key = mPacketKeepalive.toString();
                mPacketKeepaliveMap.put(key, mPacketKeepalive);
                return key;
            } else {
                Log.e("startNattKeepalive fail, startNattKeepalive return null");
                return null;
            }
        } catch(UnknownHostException e) {
            Log.e("startNattKeepalive UnknownHostException");
            return null;
        }
    }

    @Rpc(description = "stop natt keep alive")
    public Boolean stopNattKeepalive(String key) {
        mPacketKeepalive = mPacketKeepaliveMap.get(key);
        if(mPacketKeepalive != null){
            mPacketKeepaliveMap.remove(key);
            mPacketKeepalive.stop();
            return true;
        } else {
            return false;
        }
    }

    @Rpc(description = "Set Rssi Threshold Monitor")
    public String setRssiThresholdMonitor(Integer rssi) {
        Log.d("SL4A:setRssiThresholdMonitor rssi = " + rssi);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.setSignalStrength((int)rssi);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        NetworkRequest networkRequest = builder.build();
        mNetworkCallback = new NetworkCallback();
        mManager.registerNetworkCallback(networkRequest, mNetworkCallback);
        String key = mNetworkCallback.toString();
        mNetworkCallbackMap.put(key, mNetworkCallback);
        return key;
    }

    @Rpc(description = "Stop Rssi Threshold Monitor")
    public Boolean stopRssiThresholdMonitor(String key) {
        Log.d("SL4A:stopRssiThresholdMonitor key = " + key);
        mNetworkCallback = mNetworkCallbackMap.get(key);
        if (mNetworkCallback != null){
            mNetworkCallbackMap.remove(key);
            mManager.unregisterNetworkCallback(mNetworkCallback);
            return true;
        } else {
            return false;
        }
    }

    @Rpc(description = "Stop listening for connectivity changes")
    public void stopTrackingConnectivityStateChange() {
        if(mTrackingConnectivityStateChange) {
            mTrackingConnectivityStateChange = false;
            mContext.unregisterReceiver(mConnectivityReceiver);
        }
    }

    @Rpc(description = "Get the extra information about the network state provided by lower network layers.")
    public String networkGetActiveConnectionExtraInfo() {
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return null;
        }
        return current.getExtraInfo();
    }

    @Rpc(description = "Return the subtype name of the current network, null if not connected")
    public String networkGetActiveConnectionSubtypeName() {
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return null;
        }
        return current.getSubtypeName();
    }

    @Rpc(description = "Return a human-readable name describe the type of the network, e.g. WIFI")
    public String networkGetActiveConnectionTypeName() {
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return null;
        }
        return current.getTypeName();
    }

    @Rpc(description = "Get connection status information about all network types supported by the device.")
    public NetworkInfo[] networkGetAllInfo() {
        return mManager.getAllNetworkInfo();
    }

    @Rpc(description = "Check whether the active network is connected to the Internet.")
    public Boolean networkIsConnected() {
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return false;
        }
        return current.isConnected();
    }

    @Rpc(description = "Checks the airplane mode setting.",
            returns = "True if airplane mode is enabled.")
    public Boolean checkAirplaneMode() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON) == AIRPLANE_MODE_ON;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    @Rpc(description = "Toggles airplane mode on and off.",
            returns = "True if airplane mode is enabled.")
    public void toggleAirplaneMode(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !checkAirplaneMode();
        }
        mManager.setAirplaneMode(enabled);
    }

    @Override
    public void shutdown() {
        stopTrackingConnectivityStateChange();
    }
}
