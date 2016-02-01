/*
 * Copyright (C) 2016 Google Inc.
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

package com.googlecode.android_scripting.facade.telephony;

import org.json.JSONException;
import org.json.JSONObject;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import com.googlecode.android_scripting.jsonrpc.JsonSerializable;
import com.googlecode.android_scripting.facade.telephony.TelephonyConstants;
import com.googlecode.android_scripting.facade.telephony.TelephonyUtils;

public class TelephonyEvents {

    public static class CallStateEvent implements JsonSerializable {
        private String mSubEvent;
        private String mIncomingNumber;
        private int mSubscriptionId;

        CallStateEvent(int state, String incomingNumber, int subscriptionId) {
            mSubEvent = null;
            mIncomingNumber = TelephonyUtils.formatIncomingNumber(
                    incomingNumber);
            mSubEvent = TelephonyUtils.getTelephonyCallStateString(
                    state);
            mSubscriptionId = subscriptionId;
        }

        public String getSubEvent() {
            return mSubEvent;
        }

        public String getIncomingNumber() {
            return mIncomingNumber;
        }

        public int getSubscriptionId() {
            return mSubscriptionId;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject callState = new JSONObject();

            callState.put(
                    TelephonyConstants.CallStateContainer.SUBSCRIPTION_ID,
                    mSubscriptionId);
            callState.put(
                    TelephonyConstants.CallStateContainer.INCOMING_NUMBER,
                    mIncomingNumber);
            callState.put(TelephonyConstants.CallStateContainer.SUB_EVENT,
                    mSubEvent);

            return callState;
        }
    }

    public static class PreciseCallStateEvent implements JsonSerializable {
        private PreciseCallState mPreciseCallState;
        private String mSubEvent;
        private String mType;
        private int mCause;
        private int mSubscriptionId;

        PreciseCallStateEvent(int newState, String type,
                PreciseCallState preciseCallState, int subscriptionId) {
            mSubEvent = TelephonyUtils.getPreciseCallStateString(
                    newState);
            mPreciseCallState = preciseCallState;
            mType = type;
            mSubscriptionId = subscriptionId;
            mCause = preciseCallState.getPreciseDisconnectCause();
        }

        public String getSubEvent() {
            return mSubEvent;
        }

        public String getType() {
            return mType;
        }

        public int getSubscriptionId() {
            return mSubscriptionId;
        }

        public PreciseCallState getPreciseCallState() {
            return mPreciseCallState;
        }

        public int getCause() {
            return mCause;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject preciseCallState = new JSONObject();

            preciseCallState.put(
                    TelephonyConstants.PreciseCallStateContainer.SUBSCRIPTION_ID,
                    mSubscriptionId);
            preciseCallState.put(
                    TelephonyConstants.PreciseCallStateContainer.TYPE, mType);
            preciseCallState.put(
                    TelephonyConstants.PreciseCallStateContainer.SUB_EVENT, mSubEvent);
            preciseCallState.put(
                    TelephonyConstants.PreciseCallStateContainer.CAUSE, mCause);

            return preciseCallState;
        }
    }

    public static class DataConnectionRealTimeInfoEvent implements JsonSerializable {
        private DataConnectionRealTimeInfo mDataConnectionRealTimeInfo;
        private String mSubEvent;
        private int mSubscriptionId;
        private long mTime;

        DataConnectionRealTimeInfoEvent(
                DataConnectionRealTimeInfo dataConnectionRealTimeInfo,
                int subscriptionId) {
            mTime = dataConnectionRealTimeInfo.getTime();
            mSubscriptionId = subscriptionId;
            mSubEvent = TelephonyUtils.getDcPowerStateString(
                    dataConnectionRealTimeInfo.getDcPowerState());
            mDataConnectionRealTimeInfo = dataConnectionRealTimeInfo;
        }

        public String getSubEvent() {
            return mSubEvent;
        }

        public int getSubscriptionId() {
            return mSubscriptionId;
        }

        public long getTime() {
            return mTime;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject dataConnectionRealTimeInfo = new JSONObject();

            dataConnectionRealTimeInfo.put(
                    TelephonyConstants.DataConnectionRealTimeInfoContainer.SUBSCRIPTION_ID,
                    mSubscriptionId);
            dataConnectionRealTimeInfo.put(
                    TelephonyConstants.DataConnectionRealTimeInfoContainer.TIME,
                    mTime);
            dataConnectionRealTimeInfo.put(
                    TelephonyConstants.DataConnectionRealTimeInfoContainer.SUB_EVENT,
                    mSubEvent);

            return dataConnectionRealTimeInfo;
        }
    }

    public static class DataConnectionStateEvent implements JsonSerializable {
        private String mSubEvent;
        private int mSubscriptionId;
        private int mState;
        private String mDataNetworkType;

        DataConnectionStateEvent(int state, String dataNetworkType,
                int subscriptionId) {
            mSubscriptionId = subscriptionId;
            mSubEvent = TelephonyUtils.getDataConnectionStateString(
                    state);
            mDataNetworkType = dataNetworkType;
            mState = state;
        }

        public String getSubEvent() {
            return mSubEvent;
        }

        public int getSubscriptionId() {
            return mSubscriptionId;
        }

        public int getState() {
            return mState;
        }

        public String getDataNetworkType() {
            return mDataNetworkType;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject dataConnectionState = new JSONObject();

            dataConnectionState.put(
                    TelephonyConstants.DataConnectionStateContainer.SUBSCRIPTION_ID,
                    mSubscriptionId);
            dataConnectionState.put(
                    TelephonyConstants.DataConnectionStateContainer.SUB_EVENT,
                    mSubEvent);
            dataConnectionState.put(
                    TelephonyConstants.DataConnectionStateContainer.DATA_NETWORK_TYPE,
                    mDataNetworkType);
            dataConnectionState.put(
                    TelephonyConstants.DataConnectionStateContainer.STATE_CODE,
                    mState);

            return dataConnectionState;
        }
    }

    public static class ServiceStateEvent implements JsonSerializable {
        private String mSubEvent;
        private int mSubscriptionId;
        private ServiceState mServiceState;

        ServiceStateEvent(ServiceState serviceState, int subscriptionId) {
            mServiceState = serviceState;
            mSubscriptionId = subscriptionId;
            mSubEvent = TelephonyUtils.getNetworkStateString(
                    serviceState.getState());
            if (mSubEvent.equals(
                    TelephonyConstants.SERVICE_STATE_OUT_OF_SERVICE) &&
                    serviceState.isEmergencyOnly()) {
                mSubEvent = TelephonyConstants.SERVICE_STATE_EMERGENCY_ONLY;
            }
        }

        public String getSubEvent() {
            return mSubEvent;
        }

        public int getSubscriptionId() {
            return mSubscriptionId;
        }

        public ServiceState getServiceState() {
            return mServiceState;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject serviceState = new JSONObject();

            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.SUBSCRIPTION_ID,
                    mSubscriptionId);
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.VOICE_REG_STATE,
                    TelephonyUtils.getNetworkStateString(
                            mServiceState.getVoiceRegState()));
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.VOICE_NETWORK_TYPE,
                    TelephonyUtils.getNetworkTypeString(
                            mServiceState.getVoiceNetworkType()));
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.DATA_REG_STATE,
                    TelephonyUtils.getNetworkStateString(
                            mServiceState.getDataRegState()));
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.DATA_NETWORK_TYPE,
                    TelephonyUtils.getNetworkTypeString(
                            mServiceState.getDataNetworkType()));
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.OPERATOR_NAME,
                    mServiceState.getOperatorAlphaLong());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.OPERATOR_ID,
                    mServiceState.getOperatorNumeric());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.IS_MANUAL_NW_SELECTION,
                    mServiceState.getIsManualSelection());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.ROAMING,
                    mServiceState.getRoaming());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.IS_EMERGENCY_ONLY,
                    mServiceState.isEmergencyOnly());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.NETWORK_ID,
                    mServiceState.getNetworkId());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.SYSTEM_ID,
                    mServiceState.getSystemId());
            serviceState.put(
                    TelephonyConstants.ServiceStateContainer.SUB_EVENT, mSubEvent);

            return serviceState;
        }
    }

    public static class MessageWaitingIndicatorEvent implements JsonSerializable {
        private boolean mMessageWaitingIndicator;

        MessageWaitingIndicatorEvent(boolean messageWaitingIndicator) {
            mMessageWaitingIndicator = messageWaitingIndicator;
        }

        public boolean getMessageWaitingIndicator() {
            return mMessageWaitingIndicator;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject messageWaitingIndicator = new JSONObject();

            messageWaitingIndicator.put(
                    TelephonyConstants.MessageWaitingIndicatorContainer.IS_MESSAGE_WAITING,
                    mMessageWaitingIndicator);

            return messageWaitingIndicator;
        }
    }

}
