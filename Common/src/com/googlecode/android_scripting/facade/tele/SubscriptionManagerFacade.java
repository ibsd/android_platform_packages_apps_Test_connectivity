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
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.List;

/**
 * Exposes SubscriptionManager functionality.
 */
public class SubscriptionManagerFacade extends RpcReceiver {

    private final Service mService;
    private final Context mContext;

    public SubscriptionManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mContext = mService.getBaseContext();
    }

    @Rpc(description = "Return the default subscription ID")
    public Long subscriptionGetDefaultSubId() {
        return SubscriptionManager.getDefaultSubId();
    }

    @Rpc(description = "Return the default data subscription ID")
    public Long subscriptionGetDefaultDataSubId() {
        return SubscriptionManager.getDefaultDataSubId();
    }

    @Rpc(description = "Set the default data subscription ID")
    public void subscriptionSetDefaultDataSubId(
            @RpcParameter(name = "subId")
            Long subId) {
        SubscriptionManager.setDefaultDataSubId(subId);
    }

    @Rpc(description = "Return the default voice subscription ID")
    public Long subscriptionGetDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubId();
    }

    @Rpc(description = "Set the default voice subscription ID")
    public void subscriptionSetDefaultVoiceSubId(
            @RpcParameter(name = "subId")
            Long subId) {
        SubscriptionManager.setDefaultVoiceSubId(subId);
    }

    @Rpc(description = "Return a List of all Subscription Info Records")
    public List<SubInfoRecord> subscriptionGetAllSubInfoList() {
        return SubscriptionManager.getAllSubInfoList();
    }

    @Rpc(description = "Return a List of all Active Subscription Info Records")
    public List<SubInfoRecord> subscriptionGetActiveSubInfoList() {
        return SubscriptionManager.getActiveSubInfoList();
    }

    @Rpc(description = "Return the Subscription Info for a Particular Subscription ID")
    public SubInfoRecord subscriptionGetSubInfoForSubscriber(
            @RpcParameter(name = "subId")
            Long subId) {
        return SubscriptionManager.getSubInfoForSubscriber(subId);
    }

    @Rpc(description = "Set Data Roaming Enabled or Disabled for a particular Subscription ID")
    public Integer subscriptionSetDataRoaming(Integer roaming, Long subId) {
        if (roaming != SubscriptionManager.DATA_ROAMING_DISABLE) {
            return SubscriptionManager.setDataRoaming(
                    SubscriptionManager.DATA_ROAMING_ENABLE, subId);
        } else {
            return SubscriptionManager.setDataRoaming(
                    SubscriptionManager.DATA_ROAMING_DISABLE, subId);
        }
    }

    @Override
    public void shutdown() {

    }
}
