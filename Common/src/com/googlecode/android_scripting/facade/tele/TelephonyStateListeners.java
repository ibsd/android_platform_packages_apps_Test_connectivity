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
            int len = incomingNumber.length();
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
                    mCallStateEvent.putString("State", "IDLE");
                    subEvent = "Idle";
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mCallStateEvent.putString("State", "OFFHOOK");
                    subEvent = "Offhook";
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mCallStateEvent.putString("State", "RINGING");
                    subEvent = "Ringing";
                    break;
            }
            mEventFacade.postEvent("onCallStateChanged"+subEvent, mCallStateEvent.clone());
            mCallStateEvent.clear();
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
                EventMsg.putString("State", "ACTIVE");
                subEvent = "Active";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_HOLDING) {
                EventMsg.putString("State", "HOLDING)");
                subEvent = "Holding";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DIALING) {
                EventMsg.putString("State", "DIALING");
                subEvent = "Dialing";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_ALERTING) {
                EventMsg.putString("State", "ALERTING");
                subEvent = "Alerting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                EventMsg.putString("State", "INCOMING");
                subEvent = "Incoming";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_WAITING) {
                EventMsg.putString("State", "WAITING");
                subEvent = "Waiting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED) {
                EventMsg.putString("State", "DISCONNECTED");
                subEvent = "Disconnected";
                EventMsg.putInt("Cause", callState.getPreciseDisconnectCause());
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING) {
                EventMsg.putString("State", "DISCONNECTING");
                subEvent = "Disconnecting";
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                EventMsg.putString("State", "IDLE");
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
            event.putString("Type", "modemPowerLvl");
            event.putLong("Time", dcRtInfo.getTime());

            int state = dcRtInfo.getDcPowerState();
            if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_LOW) {
                event.putString("PowerLevel", "LOW");
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH) {
                event.putString("PowerLevel", "MEDIUM");
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM) {
                event.putString("PowerLevel", "HIGH");
            } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_UNKNOWN) {
                event.putString("PowerLevel", "UNKNOWN");
            }
            mEventFacade.postEvent("onModemPowerLevelChanged", event);
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
            switch(serviceState.getVoiceRegState()) {
                case ServiceState.STATE_EMERGENCY_ONLY:
                    event.putString("State", "EMERGENCY_ONLY");
                    subEvent = "EmergencyOnly";
                break;
                case ServiceState.STATE_IN_SERVICE:
                    event.putString("State", "IN_SERVICE");
                    subEvent = "InService";
                break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    event.putString("State","OUT_OF_SERVICE");
                    subEvent = "OutOfService";
                break;
                case ServiceState.STATE_POWER_OFF:
                    event.putString("State", "POWER_OFF");
                    subEvent = "PowerOff";
                break;
            }
            event.putString("OperatorName", serviceState.getOperatorAlphaLong());
            event.putString("OperatorId", serviceState.getOperatorNumeric());
            event.putBoolean("ManualNwSelection", serviceState.getIsManualSelection());
            event.putBoolean("Roaming", serviceState.getRoaming());
            event.putBoolean("isEmergencyOnly", serviceState.isEmergencyOnly());

            mEventFacade.postEvent("onServiceStateChanged"+subEvent, event.clone());
            event.clear();
        }
    }

}
