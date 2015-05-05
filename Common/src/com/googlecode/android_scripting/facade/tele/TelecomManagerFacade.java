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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.telecom.AudioState;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes TelecomManager functionality.
 */
public class TelecomManagerFacade extends RpcReceiver {

    private final Service mService;

    private final TelecomManager mTelecomManager;

    private List<PhoneAccountHandle> mEnabledAccountHandles = null;

    public TelecomManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelecomManager = new TelecomManager(mService);
    }

    @Override
    public void shutdown() {
    }

    @Rpc(description = "If there's a ringing call, accept on behalf of the user.")
    public void telecomAcceptRingingCall() {
        mTelecomManager.acceptRingingCall();
    }

    @Rpc(description = "Removes the missed-call notification if one is present.")
    public void telecomCancelMissedCallsNotification() {
        mTelecomManager.cancelMissedCallsNotification();
    }

    @Rpc(description = "Remove all Accounts that belong to the calling package from the system.")
    public void telecomClearAccounts() {
        mTelecomManager.clearAccounts();
    }

    @Rpc(description = "End an ongoing call.")
    public Boolean telecomEndCall() {
        return mTelecomManager.endCall();
    }

    @Rpc(description = "Get a list of all PhoneAccounts.")
    public List<PhoneAccount> telecomGetAllPhoneAccounts() {
        return mTelecomManager.getAllPhoneAccounts();
    }

    @Rpc(description = "Get the current call state.")
    public String telecomGetCallState() {
        int state = mTelecomManager.getCallState();
        String stateStr = null;
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            stateStr = "RINGING";
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            stateStr = "IDLE";
        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            stateStr = "OFFHOOK";
        }
        return stateStr;
    }

    @Rpc(description = "Get the current tty mode.")
    public String telecomGetCurrentTtyMode() {
        int mode = mTelecomManager.getCurrentTtyMode();
        String modeStr = null;
        if (mode == TelecomManager.TTY_MODE_FULL) {
            modeStr = "FULL";
        } else if (mode == TelecomManager.TTY_MODE_HCO) {
            modeStr = "HCO";
        } else if (mode == TelecomManager.TTY_MODE_OFF) {
            modeStr = "OFF";
        } else if (mode == TelecomManager.TTY_MODE_VCO) {
            modeStr = "VCO";
        }
        return modeStr;
    }

    @Rpc(description = "Get the list of PhoneAccountHandles with calling capability.")
    public List<PhoneAccountHandle> telecomGetEnabledPhoneAccounts() {
        mEnabledAccountHandles = mTelecomManager.getCallCapablePhoneAccounts();
        return mEnabledAccountHandles;
    }

    @Rpc(description = "Set the user-chosen default PhoneAccount for making outgoing phone calls.")
    public void telecomSetUserSelectedOutgoingPhoneAccount(
                        @RpcParameter(name = "phoneAccountHandleId")
            String phoneAccountHandleId) throws Exception {

        List<PhoneAccountHandle> accountHandles = mTelecomManager
                .getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : accountHandles) {
            if (handle.getId().equals(phoneAccountHandleId)) {
                mTelecomManager.setUserSelectedOutgoingPhoneAccount(handle);
                Log.d(String.format("Set default Outgoing Phone Account(%s)",
                        phoneAccountHandleId));
                return;
            }
        }
        Log.d(String.format(
                "Failed to find a matching phoneAccountHandleId(%s).",
                phoneAccountHandleId));
        throw new Exception(String.format(
                "Failed to find a matching phoneAccountHandleId(%s).",
                phoneAccountHandleId));
    }

    @Rpc(description = "Get the user-chosen default PhoneAccount for making outgoing phone calls.")
    public PhoneAccountHandle telecomGetUserSelectedOutgoingPhoneAccount() {
        return mTelecomManager.getUserSelectedOutgoingPhoneAccount();
    }

    @Rpc(description = "Returns whether there is an ongoing phone call.")
    public Boolean telecomIsInCall() {
        return mTelecomManager.isInCall();
    }

    @Rpc(description = "Returns whether there is a ringing incoming call.")
    public Boolean telecomIsRinging() {
        return mTelecomManager.isRinging();
    }

    @Rpc(description = "Silences the rigner if there's a ringing call.")
    public void telecomSilenceRinger() {
        mTelecomManager.silenceRinger();
    }

    @Rpc(description = "Swap two calls")
    public void telecomSwapCalls() {
        // TODO: Swap the foreground and back ground calls
    }

    @Rpc(description = "Toggles call waiting feature on or off for default voice subscription id.")
    public void toggleCallWaiting(
            @RpcParameter(name = "enabled")
            @RpcOptional
            Boolean enabled) {
        toggleCallWaitingForSubscription(
                SubscriptionManager.getDefaultVoiceSubId(), enabled);
    }

    @Rpc(description = "Toggles call waiting feature on or off for specified subscription id.")
    public void toggleCallWaitingForSubscription(
            @RpcParameter(name = "subId")
            @RpcOptional
            Integer subId,
            @RpcParameter(name = "enabled")
            @RpcOptional
            Boolean enabled) {
        // TODO: Enable or Disable the call waiting feature
    }

    @Rpc(description = "Sends an MMI string to Telecom for processing")
    public void telecomHandleMmi(
                        @RpcParameter(name = "dialString")
            String dialString) {
        mTelecomManager.handleMmi(dialString);
    }
}
