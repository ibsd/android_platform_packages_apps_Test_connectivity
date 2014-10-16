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

package com.googlecode.android_scripting.facade.tele;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionManager;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes ImsManager functionality.
 */
public class ImsManagerFacade extends RpcReceiver {

    private final Service mService;
    private final Context mContext;
    private ImsManager mImsManager;

    public ImsManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mContext = mService.getBaseContext();
        mImsManager = ImsManager.getInstance(mContext,
                SubscriptionManager.getDefaultVoiceSubId());
    }

    @Rpc(description = "Return True if Enhanced 4g Lte mode is enabled by platform.")
    public boolean imsIsEnhanced4gLteModeSettingEnabledByPlatform() {
        return ImsManager.isEnhanced4gLteModeSettingEnabledByPlatform(mService);
    }

    @Rpc(description = "Return True if Enhanced 4g Lte mode is enabled by user.")
    public boolean imsIsEnhanced4gLteModeSettingEnabledByUser() {
        return ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mService);
    }

    @Rpc(description = "Set Enhanced 4G mode.")
    public void imsSetAdvanced4gMode(@RpcParameter(name = "enable") Boolean enable)
            throws ImsException{
        android.provider.Settings.Global.putInt(
                  mContext.getContentResolver(),
                  android.provider.Settings.Global.VOLTE_VT_ENABLED, enable ? 1 : 0);

        if (mImsManager != null) {
            Log.v("mImsManager is not null in setting ehanced 4G mode.");
            mImsManager.setAdvanced4GMode(enable);
        }
    }

    @Override
    public void shutdown() {

    }
}
