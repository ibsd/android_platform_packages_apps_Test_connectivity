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

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.Log;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

import java.util.List;

/**
 * Store all subclasses of PhoneStateListener here.
 */
public class TelephonyStateListeners {

    public static class CallStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_CALL_STATE |
                                                   PhoneStateListener.LISTEN_PRECISE_CALL_STATE;

        public boolean listenForeground = true;
        public boolean listenRinging = false;
        public boolean listenBackground = false;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public CallStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        public CallStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Bundle mCallStateEvent = new Bundle();
            String subEvent = null;
            String postIncomingNumberStr = null;
            int len = 0;
            if (incomingNumber == null) {
                len = 0;
            } else {
                len = incomingNumber.length();
            }
            if (len > 0) {
                /**
                 * Currently this incomingNumber modification is specific for US numbers.
                 */
                if ((12 == len) && ('+' == incomingNumber.charAt(0))) {
                    postIncomingNumberStr = incomingNumber.substring(1);
                } else if (10 == len) {
                    postIncomingNumberStr = '1' + incomingNumber;
                } else {
                    postIncomingNumberStr = incomingNumber;
                }
                mCallStateEvent.putString("incomingNumber", postIncomingNumberStr);
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    subEvent = TelephonyConstants.TELEPHONY_STATE_IDLE;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    subEvent = TelephonyConstants.TELEPHONY_STATE_OFFHOOK;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    subEvent = TelephonyConstants.TELEPHONY_STATE_RINGING;
                    break;
            }
            mCallStateEvent.putInt("subscriptionId", subscriptionId);
            mCallStateEvent.putString("subEvent", subEvent);
            mEventFacade.postEvent(TelephonyConstants.EventCallStateChanged, mCallStateEvent);
        }

        @Override
        public void onPreciseCallStateChanged(PreciseCallState callState) {
            int foregroundState = callState.getForegroundCallState();
            int ringingState = callState.getRingingCallState();
            int backgroundState = callState.getBackgroundCallState();
            if (listenForeground &&
                foregroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(foregroundState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_FOREGROUND,
                        callState);
            }
            if (listenRinging &&
                ringingState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(ringingState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_RINGING,
                        callState);
            }
            if (listenBackground &&
                backgroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(backgroundState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_BACKGROUND,
                        callState);
            }
        }

        private void processCallState(int newState, String which, PreciseCallState callState) {
            Bundle EventMsg = new Bundle();
            String subEvent = null;
            EventMsg.putString("Type", which);
            if (newState == PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_ACTIVE;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_HOLDING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_HOLDING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DIALING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_DIALING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_ALERTING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_ALERTING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_INCOMING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_WAITING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_WAITING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_DISCONNECTED;
                EventMsg.putInt("Cause", callState.getPreciseDisconnectCause());
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_DISCONNECTING;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_IDLE;
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                subEvent = TelephonyConstants.PRECISE_CALL_STATE_INVALID;
            }
            EventMsg.putInt("subscriptionId", subscriptionId);
            EventMsg.putString("subEvent", subEvent);
            mEventFacade.postEvent(TelephonyConstants.EventPreciseStateChanged, EventMsg);
        }
    }

    public static class DataConnectionRealTimeInfoChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_REAL_TIME_INFO;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public DataConnectionRealTimeInfoChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }

        public DataConnectionRealTimeInfoChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
            Bundle event = new Bundle();
            String subEvent = null;
            event.putString("Type", "modemPowerLvl");
            event.putLong("Time", dcRtInfo.getTime());

            int state = dcRtInfo.getDcPowerState();
            if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_LOW) {
                subEvent = TelephonyConstants.DC_POWER_STATE_LOW;
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH) {
                subEvent = TelephonyConstants.DC_POWER_STATE_HIGH;
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM) {
                subEvent = TelephonyConstants.DC_POWER_STATE_MEDIUM;
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_UNKNOWN) {
                subEvent = TelephonyConstants.DC_POWER_STATE_UNKNOWN;
            }
            event.putInt("subscriptionId", subscriptionId);
            event.putString("subEvent", subEvent);
            mEventFacade.postEvent(TelephonyConstants.EventDataConnectionRealTimeInfoChanged, event);
        }
    }

    public static class DataConnectionStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        private final TelephonyManager mTelephonyManager;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm) {
            super();
            mEventFacade = ef;
            mTelephonyManager = tm;
            subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm, int subId) {
            super(subId);
            mEventFacade = ef;
            mTelephonyManager = tm;
            subscriptionId = subId;
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            Bundle event = new Bundle();
            String subEvent = null;
            event.putString("Type", "DataConnectionState");
            if (state == TelephonyManager.DATA_DISCONNECTED) {
                subEvent = TelephonyConstants.DATA_STATE_DISCONNECTED;
            } else if (state == TelephonyManager.DATA_CONNECTING) {
                subEvent = TelephonyConstants.DATA_STATE_CONNECTING;
            } else if (state == TelephonyManager.DATA_CONNECTED) {
                subEvent = TelephonyConstants.DATA_STATE_CONNECTED;
                event.putString("DataNetworkType", TelephonyUtils.getNetworkTypeString(
                                 mTelephonyManager.getDataNetworkType()));
            } else if (state == TelephonyManager.DATA_SUSPENDED) {
                subEvent = TelephonyConstants.DATA_STATE_SUSPENDED;
            } else if (state == TelephonyManager.DATA_UNKNOWN) {
                subEvent = TelephonyConstants.DATA_STATE_UNKNOWN;
            } else {
                subEvent = "UnknownStateCode";
                event.putInt("UnknownStateCode", state);
            }
            event.putInt("subscriptionId", subscriptionId);
            event.putString("subEvent", subEvent);
            mEventFacade.postEvent(TelephonyConstants.EventDataConnectionStateChanged, event);
        }
    }

    public static class ServiceStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_SERVICE_STATE;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public ServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }

        public ServiceStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Bundle event = new Bundle();
            String subEvent = null;
            String networkRat = null;
            switch(serviceState.getState()) {
                case ServiceState.STATE_EMERGENCY_ONLY:
                    subEvent = TelephonyConstants.SERVICE_STATE_EMERGENCY_ONLY;
                break;
                case ServiceState.STATE_IN_SERVICE:
                    subEvent = TelephonyConstants.SERVICE_STATE_IN_SERVICE;
                break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    subEvent = TelephonyConstants.SERVICE_STATE_OUT_OF_SERVICE;
                    if(serviceState.isEmergencyOnly())
                        subEvent = TelephonyConstants.SERVICE_STATE_EMERGENCY_ONLY;
                break;
                case ServiceState.STATE_POWER_OFF:
                    subEvent = TelephonyConstants.SERVICE_STATE_POWER_OFF;
                break;
            }
            event.putString("VoiceRegState", TelephonyUtils.getNetworkStateString(
                             serviceState.getVoiceRegState()));
            event.putString("VoiceNetworkType", TelephonyUtils.getNetworkTypeString(
                             serviceState.getVoiceNetworkType()));
            event.putString("DataRegState", TelephonyUtils.getNetworkStateString(
                             serviceState.getDataRegState()));
            event.putString("DataNetworkType", TelephonyUtils.getNetworkTypeString(
                             serviceState.getDataNetworkType()));
            event.putString("OperatorName", serviceState.getOperatorAlphaLong());
            event.putString("OperatorId", serviceState.getOperatorNumeric());
            event.putBoolean("isManualNwSelection", serviceState.getIsManualSelection());
            event.putBoolean("Roaming", serviceState.getRoaming());
            event.putBoolean("isEmergencyOnly", serviceState.isEmergencyOnly());
            event.putInt("NetworkId", serviceState.getNetworkId());
            event.putInt("SystemId", serviceState.getSystemId());

            if(subEvent.equals("InService")) {
                switch(serviceState.getVoiceNetworkType()) {
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        networkRat = TelephonyConstants.RAT_LTE;
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        networkRat = TelephonyConstants.RAT_UMTS;
                        break;
                    case TelephonyManager.NETWORK_TYPE_GSM:
                        networkRat = TelephonyConstants.RAT_GSM;
                        break;
                }
                if (networkRat != null) {
                    event.putString("networkRat", networkRat);
                }
            }
            event.putInt("subscriptionId", subscriptionId);
            event.putString("subEvent", subEvent);
            mEventFacade.postEvent(TelephonyConstants.EventServiceStateChanged, event);
        }

    }

    public static class CellInfoChangeListener
            extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public CellInfoChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public CellInfoChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            mEventFacade.postEvent(TelephonyConstants.EventCellInfoChanged, cellInfo);
        }
    }

    public static class VolteServiceStateChangeListener
            extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public VolteServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public VolteServiceStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState volteInfo) {
            Bundle event = new Bundle();

            event.putString("srvccState",
                    TelephonyUtils.getSrvccStateString(volteInfo.getSrvccState()));

            mEventFacade.postEvent(
                    TelephonyConstants.EventVolteServiceStateChanged, event);
        }
    }

    public static class VoiceMailStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public static final int sListeningStates =
                PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR;

        public VoiceMailStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public VoiceMailStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean messageWaitingIndicator) {
            Bundle event = new Bundle();
            event.putBoolean("MessageWaitingIndicator", messageWaitingIndicator);

            mEventFacade.postEvent(
                    TelephonyConstants.EventMessageWaitingIndicatorChanged, event);
        }
    }

}
