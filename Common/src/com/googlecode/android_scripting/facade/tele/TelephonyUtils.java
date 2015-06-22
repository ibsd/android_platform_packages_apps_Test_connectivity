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
                return TelephonyConstants.RAT_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return TelephonyConstants.RAT_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return TelephonyConstants.RAT_UMTS;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return TelephonyConstants.RAT_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return TelephonyConstants.RAT_HSUPA;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return TelephonyConstants.RAT_HSPA;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return TelephonyConstants.RAT_CDMA;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return TelephonyConstants.RAT_1XRTT;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return TelephonyConstants.RAT_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return TelephonyConstants.RAT_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return TelephonyConstants.RAT_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return TelephonyConstants.RAT_EHRPD;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return TelephonyConstants.RAT_LTE;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return TelephonyConstants.RAT_HSPAP;
            case TelephonyManager.NETWORK_TYPE_GSM:
                return TelephonyConstants.RAT_GSM;
            case TelephonyManager. NETWORK_TYPE_TD_SCDMA:
                return TelephonyConstants.RAT_TD_SCDMA;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return TelephonyConstants.RAT_IWLAN;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return TelephonyConstants.RAT_IDEN;
        }
        return TelephonyConstants.RAT_UNKNOWN;
    }

    public static String getNetworkStateString(int state) {
        switch(state) {
            case ServiceState.STATE_EMERGENCY_ONLY:
                return TelephonyConstants.SERVICE_STATE_EMERGENCY_ONLY;
            case ServiceState.STATE_IN_SERVICE:
                return TelephonyConstants.SERVICE_STATE_IN_SERVICE;
            case ServiceState.STATE_OUT_OF_SERVICE:
                return TelephonyConstants.SERVICE_STATE_OUT_OF_SERVICE;
            case ServiceState.STATE_POWER_OFF:
                return TelephonyConstants.SERVICE_STATE_POWER_OFF;
            default:
                return TelephonyConstants.SERVICE_STATE_UNKNOWN;
        }
   }

    public static String getSrvccStateString(int srvccState) {
        switch (srvccState) {
            case VoLteServiceState.HANDOVER_STARTED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_STARTED;
            case VoLteServiceState.HANDOVER_COMPLETED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_COMPLETED;
            case VoLteServiceState.HANDOVER_FAILED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_FAILED;
            case VoLteServiceState.HANDOVER_CANCELED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_CANCELED;
            default:
                Log.e(String.format("getSrvccStateString():"
                        + "unknown state %d", srvccState));
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_UNKNOWN;
        }
    };
}
