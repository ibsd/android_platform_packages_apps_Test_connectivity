package com.googlecode.android_scripting.facade.tele;

import com.googlecode.android_scripting.facade.EventFacade;
import android.os.Bundle;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

/**
 * Store all subclasses of PhoneStateListener here.
 */
public class TelephonyStateListeners {

    private static String getNetworkTypeString(int type) {
        switch(type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPAP";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "GSM";
        }
        return "UNKNOWN";
    }

    private static String getNetworkStateString(int state) {
        switch(state) {
            case ServiceState.STATE_EMERGENCY_ONLY:
                return "EMERGENCY_ONLY";
            case ServiceState.STATE_IN_SERVICE:
                return "IN_SERVICE";
            case ServiceState.STATE_OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case ServiceState.STATE_POWER_OFF:
                return "POWER_OFF";
            default:
                return "UNKNOWN";
        }
   }

    public static class CallStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_CALL_STATE |
                                                   PhoneStateListener.LISTEN_PRECISE_CALL_STATE;

        public boolean listenForeground = true;
        public boolean listenRinging = false;
        public boolean listenBackground = false;

        public CallStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public CallStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
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
            mEventFacade.postEvent("onCallStateChanged"+subEvent, mCallStateEvent);
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
            mEventFacade.postEvent("onPreciseStateChanged"+subEvent, EventMsg);
        }
    }

    public static class DataConnectionChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_REAL_TIME_INFO;

        public DataConnectionChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public DataConnectionChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
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
            mEventFacade.postEvent("onModemPowerLevelChanged"+subEvent, event);
        }
    }

    public static class DataConnectionStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        private final TelephonyManager mTelephonyManager;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm) {
            super();
            mEventFacade = ef;
            mTelephonyManager = tm;
        }

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm, int subId) {
            super(subId);
            mEventFacade = ef;
            mTelephonyManager = tm;
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
                event.putString("DataNetworkType", getNetworkTypeString(
                                 mTelephonyManager.getDataNetworkType()));
            } else if (state == TelephonyManager.DATA_SUSPENDED) {
                subEvent = "Suspended";
            } else if (state == TelephonyManager.DATA_UNKNOWN) {
                subEvent = "Unknown";
            } else {
                subEvent = "UnknownStateCode";
                event.putInt("UnknownStateCode", state);
            }
            mEventFacade.postEvent("onDataConnectionStateChanged"+subEvent, event);
        }
    }

    public static class ServiceStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_SERVICE_STATE;

        public ServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public ServiceStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
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
            event.putString("VoiceRegState", getNetworkStateString(
                             serviceState.getVoiceRegState()));
            event.putString("VoiceNetworkType", getNetworkTypeString(
                             serviceState.getVoiceNetworkType()));
            event.putString("DataRegState", getNetworkStateString(
                             serviceState.getDataRegState()));
            event.putString("DataNetworkType", getNetworkTypeString(
                             serviceState.getDataNetworkType()));
            event.putString("OperatorName", serviceState.getOperatorAlphaLong());
            event.putString("OperatorId", serviceState.getOperatorNumeric());
            event.putBoolean("isManualNwSelection", serviceState.getIsManualSelection());
            event.putBoolean("Roaming", serviceState.getRoaming());
            event.putBoolean("isEmergencyOnly", serviceState.isEmergencyOnly());

            if(subEvent.equals("InService")) {
                switch(serviceState.getVoiceNetworkType()) {
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        subEvent = subEvent + "LTE" ;
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        subEvent = subEvent + "UMTS";
                        break;
                    case TelephonyManager.NETWORK_TYPE_GSM:
                        subEvent = subEvent + "GSM";
                        break;
                }
            }

            mEventFacade.postEvent("onServiceStateChanged"+subEvent, event);
        }
    }

}
