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
import com.android.ims.ImsConfig;
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
                SubscriptionManager.getDefaultVoicePhoneId());
    }

    @Rpc(description = "Return True if Enhanced 4g Lte mode is enabled by platform.")
    public boolean imsIsEnhanced4gLteModeSettingEnabledByPlatform() {
        return ImsManager.isVolteEnabledByPlatform(mContext);
    }

    @Rpc(description = "Return True if Enhanced 4g Lte mode is enabled by user.")
    public boolean imsIsEnhanced4gLteModeSettingEnabledByUser() {
        return ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext);
    }

    @Rpc(description = "Set Enhanced 4G mode.")
    public void imsSetEnhanced4gMode(@RpcParameter(name = "enable") Boolean enable)
            throws ImsException{
        ImsManager.setEnhanced4gLteModeSetting(mContext, enable);
    }

    @Rpc(description = "Set Modem Provisioning for VoLTE")
    public void imsSetVolteProvisioning(
            @RpcParameter(name = "enable") Boolean enable)
            throws ImsException{
        mImsManager.getConfigInterface().setProvisionedValue(
                ImsConfig.ConfigConstants.VLT_SETTING_ENABLED,
                enable? 1 : 0);
    }

    /**************************
     * Begin WFC Calling APIs
     **************************/

    @Rpc(description = "Return True if WiFi Calling is enabled for platform.")
    public boolean imsIsWfcEnabledByPlatform() {
        return ImsManager.isWfcEnabledByPlatform(mContext);
    }

    @Rpc(description = "Set whether or not WFC is enabled during roaming")
    public void imsSetWfcRoamingSetting(
                        @RpcParameter(name = "enable")
            Boolean enable) {
        ImsManager.setWfcRoamingSetting(mContext, enable);

    }

    @Rpc(description = "Return True if WiFi Calling is enabled during roaming.")
    public boolean imsIsWfcRoamingEnabledByUser() {
        return ImsManager.isWfcRoamingEnabledByUser(mContext);
    }

    @Rpc(description = "Set the Wifi Calling Mode of operation")
    public void imsSetWfcMode(
                        @RpcParameter(name = "mode")
            String mode)
            throws IllegalArgumentException {

        int mode_val;

        switch (mode.toUpperCase()) {
            case "WIFI_ONLY":
                mode_val =
                        ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY;
                break;
            case "CELLULAR_PREFERRED":
                mode_val =
                        ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED;
                break;
            case "WIFI_PREFERRED":
                mode_val =
                        ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED;
                break;
            case "DISABLED":
                if (ImsManager.isWfcEnabledByPlatform(mContext) &&
                        ImsManager.isWfcEnabledByUser(mContext) == true) {
                    ImsManager.setWfcSetting(mContext, false);
                }
                return;
            default:
                throw new IllegalArgumentException("Invalid WfcMode");
        }

        ImsManager.setWfcMode(mContext, mode_val);
        if (ImsManager.isWfcEnabledByPlatform(mContext) &&
                ImsManager.isWfcEnabledByUser(mContext) == false) {
            ImsManager.setWfcSetting(mContext, true);
        }

        return;
    }

    @Rpc(description = "Return current WFC Mode if Enabled.")
    public String imsGetWfcMode() {
        if(ImsManager.isWfcEnabledByUser(mContext) == false) {
            return "DISABLED";
        }
       switch(ImsManager.getWfcMode(mContext)) {
           case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
               return "WIFI_PREFERRED";
           case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
               return "CELLULAR_PREFERRED";
           case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
               return "WIFI_ONLY";
           default:
               return "UNKNOWN";
       }
    }

    @Rpc(description = "Return True if WiFi Calling is enabled by user.")
    public boolean imsIsWfcEnabledByUser() {
        return ImsManager.isWfcEnabledByUser(mContext);
    }

    @Rpc(description = "Set whether or not WFC is enabled")
    public void imsSetWfcSetting(
                        @RpcParameter(name = "enable")  Boolean enable) {
        ImsManager.setWfcSetting(mContext,enable);
    }

    /**************************
     * End WFC Calling APIs
     **************************/

    @Override
    public void shutdown() {

    }
}
