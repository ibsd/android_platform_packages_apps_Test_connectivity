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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;

/**
 * Access ConnectivityManager functions.
 */
public class ConnectivityManagerFacade extends RpcReceiver {
    private final Service mService;
    private final ConnectivityManager mCon;

    public ConnectivityManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mCon = (ConnectivityManager) mService.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d("Connectivity state changed.");
            }
        }
    }

    @Rpc(description = "Check whether the active network is connected to the Internet.")
    public Boolean networkIsConnected() {
        NetworkInfo current = mCon.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return false;
        }
        return current.isConnected();
    }

    @Rpc(description = "Return the type of the current network. Null if not connected")
    public String networkGetConnectionType() {
        NetworkInfo current = mCon.getActiveNetworkInfo();
        if (current == null) {
            Log.d("No network is active at the moment.");
            return null;
        }
        int type = current.getType();
        String typrStr = null;
        if (type == ConnectivityManager.TYPE_BLUETOOTH) {
            typrStr = "BLUETOOTH";
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
            typrStr = "ETHERNET";
        } else if (ConnectivityManager.isNetworkTypeMobile(type)) {
            typrStr = "MOBILE";
        } else if (ConnectivityManager.isNetworkTypeWifi(type)) {
            typrStr = "WIFI";
        } else if (type == ConnectivityManager.TYPE_WIMAX) {
            typrStr = "WIMAX";
        }
        return typrStr;
    }

    @Override
    public void shutdown() {
    }
}
