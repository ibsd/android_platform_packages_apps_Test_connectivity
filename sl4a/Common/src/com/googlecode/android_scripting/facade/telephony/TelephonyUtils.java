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
            case TelephonyConstants.NETWORK_MODE_WCDMA_PREF:
                return RILConstants.NETWORK_MODE_WCDMA_PREF;
            case TelephonyConstants.NETWORK_MODE_GSM_ONLY:
                return RILConstants.NETWORK_MODE_GSM_ONLY;
            case TelephonyConstants.NETWORK_MODE_WCDMA_ONLY:
                return RILConstants.NETWORK_MODE_WCDMA_ONLY;
            case TelephonyConstants.NETWORK_MODE_GSM_UMTS:
                return RILConstants.NETWORK_MODE_GSM_UMTS;
            case TelephonyConstants.NETWORK_MODE_CDMA:
                return RILConstants.NETWORK_MODE_CDMA;
            case TelephonyConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case TelephonyConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return RILConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case TelephonyConstants.NETWORK_MODE_GLOBAL:
                return RILConstants.NETWORK_MODE_GLOBAL;
            case TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case TelephonyConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_ONLY:
                return RILConstants.NETWORK_MODE_LTE_ONLY;
            case TelephonyConstants.NETWORK_MODE_LTE_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_WCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return RILConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
        }
        Log.d("getNetworkModeIntfromString error. String: " + networkMode);
        return RILConstants.RIL_ERRNO_INVALID_RESPONSE;
    }

    public static String getNetworkModeStringfromInt(int networkMode) {
        switch (networkMode) {
            case RILConstants.NETWORK_MODE_WCDMA_PREF:
                return TelephonyConstants.NETWORK_MODE_WCDMA_PREF;
            case RILConstants.NETWORK_MODE_GSM_ONLY:
                return TelephonyConstants.NETWORK_MODE_GSM_ONLY;
            case RILConstants.NETWORK_MODE_WCDMA_ONLY:
                return TelephonyConstants.NETWORK_MODE_WCDMA_ONLY;
            case RILConstants.NETWORK_MODE_GSM_UMTS:
                return TelephonyConstants.NETWORK_MODE_GSM_UMTS;
            case RILConstants.NETWORK_MODE_CDMA:
                return TelephonyConstants.NETWORK_MODE_CDMA;
            case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return TelephonyConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return TelephonyConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case RILConstants.NETWORK_MODE_GLOBAL:
                return TelephonyConstants.NETWORK_MODE_GLOBAL;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_ONLY:
                return TelephonyConstants.NETWORK_MODE_LTE_ONLY;
            case RILConstants.NETWORK_MODE_LTE_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_WCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case RILConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
        }
        Log.d("getNetworkModeIntfromString error. Int: " + networkMode);
        return TelephonyConstants.NETWORK_MODE_INVALID;
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
