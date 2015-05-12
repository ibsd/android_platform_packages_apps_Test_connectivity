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
import android.telecom.VideoProfile;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes TelecomManager functionality.
 */
public class TelecomCallFacade extends RpcReceiver {

    private final Service mService;

    private List<PhoneAccountHandle> mEnabledAccountHandles = null;

    public TelecomCallFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();

        // TODO: Is there a better way to do this?
        InCallServiceImpl.setEventFacade(
                manager.getReceiver(EventFacade.class));
    }

    @Override
    public void shutdown() {
        InCallServiceImpl.setEventFacade(null);
    }

    /**
     * Returns an identifier of the call. When a phone number is available, the number will be
     * returned. Otherwise, the standard object toString result of the Call object. e.g. A
     * conference call does not have a single number associated with it, thus the toString Id will
     * be returned.
     *
     * @param call
     * @return String
     */

    @Rpc(description = "Disconnect call by callId.")
    public void telecomCallDisconnect(
                        @RpcParameter(name = "callId")
            String callId) {
        InCallServiceImpl.callDisconnect(callId);
    }

    @Rpc(description = "Hold call by callId")
    public void telecomCallHold(
                        @RpcParameter(name = "callId")
            String callId) {
        InCallServiceImpl.holdCall(callId);
    }

    @Rpc(description = "Merge call to conference by callId")
    public void telecomCallMergeToConf(
                        @RpcParameter(name = "callId")
            String callId) {
        InCallServiceImpl.mergeCallsInConference(callId);
    }

    @Rpc(description = "Split call from conference by callId.")
    public void telecomCallSplitFromConf(
                        @RpcParameter(name = "callId")
            String callId) {
        InCallServiceImpl.splitCallFromConf(callId);
    }

    @Rpc(description = "Unhold call by callId")
    public void telecomCallUnhold(
                        @RpcParameter(name = "callId")
            String callId) {
        InCallServiceImpl.unholdCall(callId);
    }

    @Rpc(description = "Joins two calls into a conference call. "
            + "Calls are identified by their "
            + "IDs listed by telecomPhoneGetCallIds")
    public void telecomCallJoinCallsInConf(
                        @RpcParameter(name = "callIdOne")
            String callIdOne,
                        @RpcParameter(name = "callIdTwo")
            String callIdTwo) {
        InCallServiceImpl.joinCallsInConf(callIdOne, callIdTwo);
    }

    @Rpc(description = "Obtains the current call audio state of the phone.")
    public AudioState telecomCallGetAudioState() {
        return InCallServiceImpl.serviceGetAudioState();
    }

    @Rpc(description = "Lists the IDs (phone numbers or hex hashes) "
            + "of the current calls.")
    public Set<String> telecomCallGetCallIds() {
        return InCallServiceImpl.getCallIdList();
    }

    @Rpc(description = "Reset the Call List.")
    public void telecomCallClearCallList() {
        InCallServiceImpl.clearCallList();
    }

    @Rpc(description = "Get the state of a call according to call id.")
    public String telecomCallGetCallState(
                        @RpcParameter(name = "callId")
            String callId) {

        return InCallServiceImpl.callGetState(callId);
    }

    @Rpc(description = "Sets the audio route (SPEAKER, BLUETOOTH, etc...).")
    public void telecomCallSetAudioRoute(
                        @RpcParameter(name = "route")
            String route) {

        InCallServiceImpl.serviceSetAudioRoute(route);
    }

    @Rpc(description = "Turns the proximity sensor off. "
            + "If screenOnImmediately is true, "
            + "the screen will be turned on immediately")
    public void telecomCallOverrideProximitySensor(
                        @RpcParameter(name = "screenOn")
            Boolean screenOn) {
        InCallServiceImpl.overrideProximitySensor(screenOn);
    }

    @Rpc(description = "Answer a call of a specified id, with video state")
    public void telecomCallAnswer(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "videoState")
            String videoState) {
        InCallServiceImpl.callAnswer(callId, videoState);
    }

    @Rpc(description = "Answer a call of a specified id, with video state")
    public void telecomCallReject(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "message")
            String message) {
        InCallServiceImpl.callReject(callId, message);
    }

    @Rpc(description = "Start Listening for a VideoCall Event")
    public void telecomCallStartListeningForEvent(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "event")
            String event) {
        InCallServiceImpl.callStartListeningForEvent(callId, event);
    }

    @Rpc(description = "Stop Listening for a Call Event")
    public void telecomCallStopListeningForEvent(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "event")
            String event) {
        InCallServiceImpl.callStopListeningForEvent(callId, event);
    }

    @Rpc(description = "Start Listening for a VideoCall Event")
    public void telecomCallVideoStartListeningForEvent(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "event")
            String event) {
        InCallServiceImpl.videoCallStartListeningForEvent(callId, event);
    }

    @Rpc(description = "Stop Listening for a VideoCall Event")
    public void telecomCallVideoStopListeningForEvent(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "event")
            String event) {
        InCallServiceImpl.videoCallStopListeningForEvent(callId, event);
    }

    @Rpc(description = "Get the Video Call State")
    public String telecomCallVideoGetState(
                        @RpcParameter(name = "call")
            String callId) {
        return InCallServiceImpl.videoCallGetState(callId);
    }

    @Rpc(description = "Get the Video Call Quality")
    public String telecomCallVideoGetQuality(
                        @RpcParameter(name = "call")
            String callId) {
        return InCallServiceImpl.videoCallGetQuality(callId);
    }

    @Rpc(description = "Send a request to modify the video call session parameters")
    public void telecomCallVideoSendSessionModifyRequest(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "videoState")
            String videoState,
                        @RpcParameter(name = "videoQuality")
            String videoQuality) {
        InCallServiceImpl.videoCallSendSessionModifyRequest(callId, videoState, videoQuality);
    }

    @Rpc(description = "Send a response to a modify the video call session request")
    public void telecomCallVideoSendSessionModifyResponse(
                        @RpcParameter(name = "call")
            String callId,
                        @RpcParameter(name = "videoState")
            String videoState,
                        @RpcParameter(name = "videoQuality")
            String videoQuality) {
        InCallServiceImpl.videoCallSendSessionModifyResponse(callId, videoState, videoQuality);
    }
}
