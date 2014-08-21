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
            if (incomingNumber.length() > 0) {
              mCallStateEvent.putString("incomingNumber", incomingNumber);
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    mCallStateEvent.putString("State", "IDLE");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mCallStateEvent.putString("State", "OFFHOOK");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mCallStateEvent.putString("State", "RINGING");
                    break;
            }
            mEventFacade.postEvent("onCallStateChanged", mCallStateEvent.clone());
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
            EventMsg.putString("Type", which);
            if (newState == PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                EventMsg.putString("State", "ACTIVE");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_HOLDING) {
                EventMsg.putString("State", "HOLDING)");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DIALING) {
                EventMsg.putString("State", "DIALING");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_ALERTING) {
                EventMsg.putString("State", "ALERTING");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                EventMsg.putString("State", "INCOMING");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_WAITING) {
                EventMsg.putString("State", "WAITING");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED) {
                EventMsg.putString("State", "DISCONNECTED");
                EventMsg.putInt("Cause", callState.getPreciseDisconnectCause());
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING) {
                EventMsg.putString("State", "DISCONNECTING");
            } else if (newState == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                EventMsg.putString("State", "IDLE");
            }
            mEventFacade.postEvent("onPreciseStateChanged", EventMsg);
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
            Bundle event = new Bundle();;
            switch(serviceState.getVoiceRegState()) {
                case ServiceState.STATE_EMERGENCY_ONLY:
                    event.putString("State", "EMERGENCY_ONLY");
                break;
                case ServiceState.STATE_IN_SERVICE:
                    event.putString("State", "IN_SERVICE");
                break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    event.putString("State","OUT_OF_SERVICE");
                break;
                case ServiceState.STATE_POWER_OFF:
                    event.putString("State", "POWER_OFF");
                break;
            }
            event.putString("OperatorName", serviceState.getOperatorAlphaLong());
            event.putString("OperatorId", serviceState.getOperatorNumeric());
            event.putBoolean("ManualNwSelection", serviceState.getIsManualSelection());
            event.putBoolean("Roaming", serviceState.getRoaming());
            event.putBoolean("isEmergencyOnly", serviceState.isEmergencyOnly());

            mEventFacade.postEvent("onServiceStateChanged", event.clone());
            event.clear();
        }
    }

}
