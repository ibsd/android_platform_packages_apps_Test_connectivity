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

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.net.ConnectivityManager;
import android.provider.Telephony.Sms;

import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes SmsManager functionality.
 *
 */
public class SmsFacade extends RpcReceiver {

    private final AndroidFacade mAndroidFacade;
    private final EventFacade mEventFacade;
    private final SmsManager mSms;
    private final Service mService;
    private final ConnectivityManager mConnect;

    public SmsFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mSms = SmsManager.getDefault();
        mConnect = (ConnectivityManager) mService
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mEventFacade = manager.getReceiver(EventFacade.class);
    }

    @Rpc(description = "Send a text message to a specified number.")
    public void smsSendTextMessage(
            @RpcParameter(name = "phoneNumber")
            String phoneNumber,
            @RpcParameter(name = "message")
            String message) {
        PendingIntent pi = PendingIntent.getActivity(mService, 0, new Intent(mService, Sms.class), 0);
        mSms.sendTextMessage(phoneNumber, null, message, pi, null);
    }

    @Rpc(description = "Retrieves all messages currently stored on ICC.")
    public ArrayList<SmsMessage> smsGetAllMessagesFromIcc() {
        return SmsManager.getAllMessagesFromIcc();
    }

    @Override
    public void shutdown() {
    }
}
