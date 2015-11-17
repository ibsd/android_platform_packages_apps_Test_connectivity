package com.googlecode.android_scripting.facade.tele;
import com.android.internal.telephony.RILConstants;
import com.googlecode.android_scripting.Log;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

/**
 * Telephony utility functions
 */
public class TelephonyUtils {

    public static int getNetworkModeIntfromString(String networkMode) {
        switch (networkMode) {
            case TelephonyConstants.NetworkModeWcdmaPref:
                return RILConstants.NETWORK_MODE_WCDMA_PREF;
            case TelephonyConstants.NetworkModeGsmOnly:
                return RILConstants.NETWORK_MODE_GSM_ONLY;
            case TelephonyConstants.NetworkModeWcdmaOnly:
                return RILConstants.NETWORK_MODE_WCDMA_ONLY;
            case TelephonyConstants.NetworkModeGsmUmts:
                return RILConstants.NETWORK_MODE_GSM_UMTS;
            case TelephonyConstants.NetworkModeCdma:
                return RILConstants.NETWORK_MODE_CDMA;
            case TelephonyConstants.NetworkModeCdmaNoEvdo:
                return RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case TelephonyConstants.NetworkModeEvdoNoCdma:
                return RILConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case TelephonyConstants.NetworkModeGlobal:
                return RILConstants.NETWORK_MODE_GLOBAL;
            case TelephonyConstants.NetworkModeLteCdmaEvdo:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case TelephonyConstants.NetworkModeLteGsmWcdma:
                return RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case TelephonyConstants.NetworkModeLteCdmaEvdoGsmWcdma:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NetworkModeLteOnly:
                return RILConstants.NETWORK_MODE_LTE_ONLY;
            case TelephonyConstants.NetworkModeLteWcdma:
                return RILConstants.NETWORK_MODE_LTE_WCDMA;
            case TelephonyConstants.NetworkModeTdscdmaOnly:
                return RILConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case TelephonyConstants.NetworkModeTdscdmaWcdma:
                return RILConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case TelephonyConstants.NetworkModeLteTdscdma:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA;
            case TelephonyConstants.NetworkModeTdsdmaGsm:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM;
            case TelephonyConstants.NetworkModeLteTdscdmaGsm:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case TelephonyConstants.NetworkModeTdscdmaGsmWcdma:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NetworkModeLteTdscdmaWcdma:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case TelephonyConstants.NetworkModeLteTdscdmaGsmWcdma:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NetworkModeTdscdmaCdmaEvdoGsmWcdma:
                return RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NetworkModeLteTdscdmaCdmaEvdoGsmWcdma:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
        }
        Log.d("getNetworkModeIntfromString error. String: " + networkMode);
        return RILConstants.RIL_ERRNO_INVALID_RESPONSE;
    }

    public static String getNetworkModeStringfromInt(int networkMode) {
        switch (networkMode) {
            case RILConstants.NETWORK_MODE_WCDMA_PREF:
                return TelephonyConstants.NetworkModeWcdmaPref;
            case RILConstants.NETWORK_MODE_GSM_ONLY:
                return TelephonyConstants.NetworkModeGsmOnly;
            case RILConstants.NETWORK_MODE_WCDMA_ONLY:
                return TelephonyConstants.NetworkModeWcdmaOnly;
            case RILConstants.NETWORK_MODE_GSM_UMTS:
                return TelephonyConstants.NetworkModeGsmUmts;
            case RILConstants.NETWORK_MODE_CDMA:
                return TelephonyConstants.NetworkModeCdma;
            case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return TelephonyConstants.NetworkModeCdmaNoEvdo;
            case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return TelephonyConstants.NetworkModeEvdoNoCdma;
            case RILConstants.NETWORK_MODE_GLOBAL:
                return TelephonyConstants.NetworkModeGlobal;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return TelephonyConstants.NetworkModeLteCdmaEvdo;
            case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return TelephonyConstants.NetworkModeLteGsmWcdma;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NetworkModeLteCdmaEvdoGsmWcdma;
            case RILConstants.NETWORK_MODE_LTE_ONLY:
                return TelephonyConstants.NetworkModeLteOnly;
            case RILConstants.NETWORK_MODE_LTE_WCDMA:
                return TelephonyConstants.NetworkModeLteWcdma;
            case RILConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return TelephonyConstants.NetworkModeTdscdmaOnly;
            case RILConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return TelephonyConstants.NetworkModeTdscdmaWcdma;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA:
                return TelephonyConstants.NetworkModeLteTdscdma;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM:
                return TelephonyConstants.NetworkModeTdsdmaGsm;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return TelephonyConstants.NetworkModeLteTdscdmaGsm;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NetworkModeTdscdmaGsmWcdma;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return TelephonyConstants.NetworkModeLteTdscdmaWcdma;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NetworkModeLteTdscdmaGsmWcdma;
            case RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NetworkModeTdscdmaCdmaEvdoGsmWcdma;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NetworkModeLteTdscdmaCdmaEvdoGsmWcdma;
        }
        Log.d("getNetworkModeIntfromString error. Int: " + networkMode);
        return TelephonyConstants.NetworkModeInvalid;
    }

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
