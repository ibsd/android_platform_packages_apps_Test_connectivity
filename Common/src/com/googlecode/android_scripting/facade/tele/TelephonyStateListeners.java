package com.googlecode.android_scripting.facade.tele;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.Log;
import android.os.Bundle;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

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
            subscriptionId = SubscriptionManager.getDefaultVoiceSubId();
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
                    subEvent = "Idle";
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    subEvent = "Offhook";
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    subEvent = "Ringing";
                    break;
            }
            mCallStateEvent.putInt("subscriptionId", subscriptionId);
            // TODO: b/22063774 remove "xxxEvent+subEvent" style event name.
            mEventFacade.postEvent(TelephonyConstants.EventCallStateChanged+subEvent,
                                   mCallStateEvent);
        }

        @Override
        public void onPreciseCallStateChanged(PreciseCallState callState) {
            int foregroundState = callState.getForegroundCallState();
            int ringingState = callState.getRingingCallState();
            int backgroundState = callState.getBackgroundCallState();
            if (listenForeground &&
                foregroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(foregroundState, "Foreground", callState);
            }
            if (listenRinging &&
                ringingState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(ringingState, "Ringing", callState);
            }
            if (listenBackground &&
                backgroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(backgroundState, "Background", callState);
            }
        }

        private void processCallState(int newState, String which, PreciseCallState callState) {
            Bundle EventMsg = new Bundle();
            String subEvent = null;
            EventMsg.putString("Type", which);
            if (newState == PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                subEvent = "Active";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_HOLDING) {
                subEvent = "Holding";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DIALING) {
                subEvent = "Dialing";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_ALERTING) {
                subEvent = "Alerting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                subEvent = "Incoming";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_WAITING) {
                subEvent = "Waiting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED) {
                subEvent = "Disconnected";
                EventMsg.putInt("Cause", callState.getPreciseDisconnectCause());
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING) {
                subEvent = "Disconnecting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                subEvent = "Idle";
            }
            EventMsg.putInt("subscriptionId", subscriptionId);
            // TODO: b/22063774 remove "xxxEvent+subEvent" style event name.
            mEventFacade.postEvent(TelephonyConstants.EventPreciseStateChanged+subEvent,
                                   EventMsg);
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
            subscriptionId = SubscriptionManager.getDefaultDataSubId();
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
                subEvent = "Low";
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH) {
                subEvent = "High";
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM) {
                subEvent = "Medium";
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_UNKNOWN) {
                subEvent = "Unknown";
            }
            event.putInt("subscriptionId", subscriptionId);
            // TODO: b/22063774 remove "xxxEvent+subEvent" style event name.
            mEventFacade.postEvent(TelephonyConstants.EventDataConnectionRealTimeInfoChanged+subEvent,
                                   event);

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
            subscriptionId = SubscriptionManager.getDefaultDataSubId();
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
                subEvent = "Disconnected";
            } else if (state == TelephonyManager.DATA_CONNECTING) {
                subEvent = "Connecting";
            } else if (state == TelephonyManager.DATA_CONNECTED) {
                subEvent = "Connected";
                event.putString("DataNetworkType", TelephonyUtils.getNetworkTypeString(
                                 mTelephonyManager.getDataNetworkType()));
            } else if (state == TelephonyManager.DATA_SUSPENDED) {
                subEvent = "Suspended";
            } else if (state == TelephonyManager.DATA_UNKNOWN) {
                subEvent = "Unknown";
            } else {
                subEvent = "UnknownStateCode";
                event.putInt("UnknownStateCode", state);
            }
            event.putInt("subscriptionId", subscriptionId);
            // TODO: b/22063774 remove "xxxEvent+subEvent" style event name.
            mEventFacade.postEvent(TelephonyConstants.EventDataConnectionStateChanged+subEvent,
                                   event);
        }
    }

    public static class ServiceStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_SERVICE_STATE;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public ServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.getDefaultDataSubId();
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
            switch(serviceState.getState()) {
                case ServiceState.STATE_EMERGENCY_ONLY:
                    subEvent = "EmergencyOnly";
                break;
                case ServiceState.STATE_IN_SERVICE:
                    subEvent = "InService";
                break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    subEvent = "OutOfService";
                    if(serviceState.isEmergencyOnly())
                        subEvent = "EmergencyOnly";
                break;
                case ServiceState.STATE_POWER_OFF:
                    subEvent = "PowerOff";
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
                        subEvent = subEvent + TelephonyConstants.RAT_LTE;
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        subEvent = subEvent + TelephonyConstants.RAT_UMTS;
                        break;
                    case TelephonyManager.NETWORK_TYPE_GSM:
                        subEvent = subEvent + TelephonyConstants.RAT_GSM;
                        break;
                }
            }
            event.putInt("subscriptionId", subscriptionId);
            // TODO: b/22063774 remove "xxxEvent+subEvent" style event name.
            mEventFacade.postEvent(TelephonyConstants.EventServiceStateChanged+subEvent,
                                   event);
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
