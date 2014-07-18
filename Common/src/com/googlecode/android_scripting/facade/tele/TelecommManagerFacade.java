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

import java.util.List;

import android.app.Service;
import android.telecomm.PhoneAccount;
import android.telecomm.TelecommManager;

import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;

/**
 * Exposes TelecommManager functionality.
 *
 */
public class TelecommManagerFacade extends RpcReceiver {
    private final TelecommManager mTelecommManager;
    private final Service mService;

    public TelecommManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelecommManager = new TelecommManager(mService);
    }

    @Rpc(description = "If there's a ringing call, accept on behalf of the user.")
    public List<PhoneAccount> telecommGetEnabledPhoneAccounts() {
        return mTelecommManager.getEnabledPhoneAccounts();
    }

    @Rpc(description = "If there's a ringing call, accept on behalf of the user.")
    public void telecommAcceptRingingCall() {
        mTelecommManager.acceptRingingCall();
    }

    @Rpc(description = "End an ongoing call.")
    public Boolean telecommEndCall() {
        return mTelecommManager.endCall();
    }

    @Rpc(description = "Returns whether there is a ringing incoming call.")
    public Boolean telecommIsRinging() {
        return mTelecommManager.isRinging();
    }

    @Rpc(description = "Returns whether there is an ongoing phone call.")
    public Boolean telecommIsInAPhoneCall() {
        return mTelecommManager.isInAPhoneCall();
    }

    @Rpc(description = "Silences the rigner if there's a ringing call.")
    public void telecommSilenceRinger() {
        mTelecommManager.silenceRinger();
    }

    @Override
    public void shutdown() {
    }

}
