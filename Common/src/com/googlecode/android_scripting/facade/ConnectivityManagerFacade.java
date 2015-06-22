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
import android.net.NetworkInfo;
import android.provider.Settings.SettingNotFoundException;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.tele.TelephonyConstants;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

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

    private final ConnectivityManager mManager;
    private final Service mService;
    private final Context mContext;
    private final ConnectivityReceiver mConnectivityReceiver;
    private final EventFacade mEventFacade;
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
