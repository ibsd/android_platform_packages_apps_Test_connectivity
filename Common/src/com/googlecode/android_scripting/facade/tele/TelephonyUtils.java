package com.googlecode.android_scripting.facade.tele;

import com.googlecode.android_scripting.Log;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

/**
 * Telephony utility functions
 */
public class TelephonyUtils {

    public static String getNetworkTypeString(int type) {
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
            case TelephonyManager. NETWORK_TYPE_TD_SCDMA:
                return "TD-SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "WLAN";
        }
        return "UNKNOWN";
    }

    public static String getNetworkStateString(int state) {
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

    public static String getSrvccStateString(int srvccState) {
        switch (srvccState) {
            case VoLteServiceState.HANDOVER_STARTED:
                return "HANDOVER_STARTED";
            case VoLteServiceState.HANDOVER_COMPLETED:
                return "HANDOVER_COMPLETED";
            case VoLteServiceState.HANDOVER_FAILED:
                return "HANDOVER_FAILED";
            case VoLteServiceState.HANDOVER_CANCELED:
                return "HANDOVER_CANCELED";
            default:
                Log.e(String.format("getSrvccStateString():"
                        + "unknown state %d", srvccState));
                return "UNKNOWN";
        }
    };
}
