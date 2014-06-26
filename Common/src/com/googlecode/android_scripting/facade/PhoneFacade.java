/*
 * Copyright (C) 2010 Google Inc.
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

package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.PhonesColumns;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.PreciseCallState;
import android.net.ConnectivityManager;
import android.provider.Telephony;
import android.telephony.DataConnectionRealTimeInfo;

import com.android.internal.telephony.TelephonyProperties;

import android.content.ContentValues;
import android.os.SystemProperties;

import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.rpc.RpcOptional;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Exposes TelephonyManager functionality.
 * 
 * @author Damon Kohler (damonkohler@gmail.com)
 * @author Felix Arends (felix.arends@gmail.com)
 */
public class PhoneFacade extends RpcReceiver {

    private final AndroidFacade mAndroidFacade;
    private final EventFacade mEventFacade;
    private final TelephonyManager mTelephonyManager;
    private final Bundle mPhoneState;
    private final Service mService;
    private PhoneStateListener mPhoneStateListener;
    private final ConnectivityManager mConnect;
    private final Bundle mModemPowerLevel;
    private final Bundle mPreciseCallState;

    private final int POWER_STATE_LOW = 1;
    private final int POWER_STATE_MEDIUM = 2;
    private final int POWER_STATE_HIGH = 3;
    private final int POWER_STATE_UNKNOWN = Integer.MAX_VALUE;

    private static final String[] sProjection = new String[] {
            Telephony.Carriers._ID, // 0
            Telephony.Carriers.NAME, // 1
            Telephony.Carriers.APN, // 2
            Telephony.Carriers.PROXY, // 3
            Telephony.Carriers.PORT, // 4
            Telephony.Carriers.USER, // 5
            Telephony.Carriers.SERVER, // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.ROAMING_PROTOCOL, // 19
            Telephony.Carriers.MVNO_TYPE, // 20
            Telephony.Carriers.MVNO_MATCH_DATA
            // 21
    };

    public PhoneFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelephonyManager = (TelephonyManager) mService
                .getSystemService(Context.TELEPHONY_SERVICE);
        mConnect = (ConnectivityManager) mService
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mEventFacade = manager.getReceiver(EventFacade.class);
        mPhoneState = new Bundle();
        mModemPowerLevel = new Bundle();
        mModemPowerLevel.putLong("time", 0);
        mModemPowerLevel.putInt("power_level", POWER_STATE_UNKNOWN);
        mPreciseCallState = new Bundle();
        mPreciseCallState.putString("CallState", "");

        mPhoneStateListener = MainThread.run(mService,
                new Callable<PhoneStateListener>() {
                    @Override
                    public PhoneStateListener call() throws Exception {
                        return new PhoneStateListener() {
                            @Override
                            public void onCallStateChanged(int state,
                                    String incomingNumber) {
                                mPhoneState.putString("incomingNumber",
                                        incomingNumber);
                                switch (state) {
                                    case TelephonyManager.CALL_STATE_IDLE:
                                        mPhoneState.putString("state", "idle");
                                        break;
                                    case TelephonyManager.CALL_STATE_OFFHOOK:
                                        mPhoneState.putString("state", "offhook");
                                        break;
                                    case TelephonyManager.CALL_STATE_RINGING:
                                        mPhoneState.putString("state", "ringing");
                                        break;
                                }
                                mEventFacade.postEvent("phone",
                                        mPhoneState.clone());
                            }

                            @Override
                            public void onDataConnectionRealTimeInfoChanged(
                                    DataConnectionRealTimeInfo dcRtInfo) {
                                mModemPowerLevel.putString("Type", "modemPowerLvl");
                                mModemPowerLevel.putLong("time", dcRtInfo.getTime());

                                int state = dcRtInfo.getDcPowerState();
                                if (POWER_STATE_LOW == state) {
                                    mModemPowerLevel.putString("power_level", "LOW");
                                } else if (POWER_STATE_MEDIUM == state) {
                                    mModemPowerLevel.putString("power_level", "MEDIUM");
                                } else if (POWER_STATE_HIGH == state) {
                                    mModemPowerLevel.putString("power_level", "HIGH");
                                } else {
                                    mModemPowerLevel.putString("power_level", "UNKNOWN");
                                }

                                mEventFacade.postEvent("modemPowerLvl",
                                        mModemPowerLevel.clone());
                            }

                            @Override
                            public void onPreciseCallStateChanged(
                                    PreciseCallState callState) {
                                int foreGroundCallState = callState.getForegroundCallState();

                                if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                                    mPreciseCallState.putString("CallState", "ACTIVE");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_HOLDING) {
                                    mPreciseCallState.putString("CallState", "HOLDING)");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_DIALING) {
                                    mPreciseCallState.putString("CallState", "DIALING");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_ALERTING) {
                                    mPreciseCallState.putString("CallState", "ALERTING");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                                    mPreciseCallState.putString("CallState", "INCOMING)");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_WAITING) {
                                    mPreciseCallState.putString("CallState", "WAITING");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED) {
                                    mPreciseCallState.putString("CallState", "DISCONNECTED");
                                } else if (foreGroundCallState ==
                                PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING) {
                                    mPreciseCallState.putString("CallState", "DISCONNECTING");
                                } else {
                                    if (callState.getRingingCallState() ==
                                    PreciseCallState.PRECISE_CALL_STATE_INCOMING) {
                                        mPreciseCallState.putString("CallState", "INCOMING");
                                    } else {
                                        mPreciseCallState.putString("CallState", "IDLE");
                                    }
                                }
                                mEventFacade.postEvent("PreciseCallState",
                                        mPreciseCallState.clone());
                            }

                        };
                    }
                });
    }

    @Override
    public void shutdown() {
        stopTrackingPhoneState();
    }

    @Rpc(description = "Starts tracking phone state.")
    @RpcStartEvent("phone")
    public void startTrackingPhoneState() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE |
                        PhoneStateListener.LISTEN_DATA_CONNECTION_REAL_TIME_INFO |
                        PhoneStateListener.LISTEN_PRECISE_CALL_STATE);
    }

    @Rpc(description = "Returns the current phone state and incoming number.",
            returns = "A Map of \"state\" and \"incomingNumber\"")
    public Bundle readPhoneState() {
        return mPhoneState;
    }

    @Rpc(description = "Stops tracking phone state.")
    @RpcStopEvent("phone")
    public void stopTrackingPhoneState() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Calls a contact/phone number by URI.")
    public void phoneCall(@RpcParameter(name = "uri")
    final String uriString)
            throws Exception {
        Uri uri = Uri.parse(uriString);
        if (uri.getScheme().equals("content")) {
            String phoneNumberColumn = PhonesColumns.NUMBER;
            String selectWhere = null;
            if ((FacadeManager.class.cast(mManager)).getSdkLevel() >= 5) {
                Class<?> contactsContract_Data_class = Class
                        .forName("android.provider.ContactsContract$Data");
                Field RAW_CONTACT_ID_field = contactsContract_Data_class
                        .getField("RAW_CONTACT_ID");
                selectWhere = RAW_CONTACT_ID_field.get(null).toString() + "="
                        + uri.getLastPathSegment();
                Field CONTENT_URI_field = contactsContract_Data_class
                        .getField("CONTENT_URI");
                uri = Uri.parse(CONTENT_URI_field.get(null).toString());
                Class<?> ContactsContract_CommonDataKinds_Phone_class = Class
                        .forName("android.provider.ContactsContract$CommonDataKinds$Phone");
                Field NUMBER_field = ContactsContract_CommonDataKinds_Phone_class
                        .getField("NUMBER");
                phoneNumberColumn = NUMBER_field.get(null).toString();
            }
            ContentResolver resolver = mService.getContentResolver();
            Cursor c = resolver.query(uri, new String[] {
                    phoneNumberColumn
            },
                    selectWhere, null, null);
            String number = "";
            if (c.moveToFirst()) {
                number = c
                        .getString(c.getColumnIndexOrThrow(phoneNumberColumn));
            }
            c.close();
            phoneCallNumber(number);
        } else {
            mAndroidFacade.startActivity(Intent.ACTION_CALL, uriString, null,
                    null, null, null, null);
        }
    }

    @Rpc(description = "Calls a phone number.")
    public void phoneCallNumber(
            @RpcParameter(name = "phone number")
            final String number)
            throws Exception {
        phoneCall("tel:" + URLEncoder.encode(number, "ASCII"));
    }

    @Rpc(description = "Dials a contact/phone number by URI.")
    public void phoneDial(@RpcParameter(name = "uri")
    final String uri)
            throws Exception {
        mAndroidFacade.startActivity(Intent.ACTION_DIAL, uri, null, null, null,
                null, null);
    }

    @Rpc(description = "Dials a phone number.")
    public void phoneDialNumber(
            @RpcParameter(name = "phone number")
            final String number)
            throws Exception, UnsupportedEncodingException {
        phoneDial("tel:" + URLEncoder.encode(number, "ASCII"));
    }

    @Rpc(description = "Returns the current cell location.")
    public CellLocation getCellLocation() {
        return mTelephonyManager.getCellLocation();
    }

    @Rpc(description = "Returns the numeric name (MCC+MNC) of current registered operator.")
    public String getNetworkOperator() {
        return mTelephonyManager.getNetworkOperator();
    }

    @Rpc(description = "Returns the alphabetic name of current registered operator.")
    public String getNetworkOperatorName() {
        return mTelephonyManager.getNetworkOperatorName();
    }

    @Rpc(
            description = "Returns a the radio technology (network type) currently in use on the device.")
    public String getNetworkType() {
        // TODO(damonkohler): API level 5 has many more types.
        switch (mTelephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "edge";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "gprs";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "umts";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "unknown";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the device phone type.")
    public String getPhoneType() {
        // TODO(damonkohler): API level 4 includes CDMA.
        switch (mTelephonyManager.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_GSM:
                return "gsm";
            case TelephonyManager.PHONE_TYPE_NONE:
                return "none";
            default:
                return null;
        }
    }

    @Rpc(
            description = "Returns the ISO country code equivalent for the SIM provider's country code.")
    public String getSimCountryIso() {
        return mTelephonyManager.getSimCountryIso();
    }

    @Rpc(
            description = "Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM. 5 or 6 decimal digits.")
    public String getSimOperator() {
        return mTelephonyManager.getSimOperator();
    }

    @Rpc(description = "Returns the Service Provider Name (SPN).")
    public String getSimOperatorName() {
        return mTelephonyManager.getSimOperatorName();
    }

    @Rpc(
            description = "Returns the serial number of the SIM, if applicable. Return null if it is unavailable.")
    public String getSimSerialNumber() {
        return mTelephonyManager.getSimSerialNumber();
    }

    @Rpc(description = "Returns the state of the device SIM card.")
    public String getSimState() {
        switch (mTelephonyManager.getSimState()) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "uknown";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "pin_required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "puk_required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "network_locked";
            case TelephonyManager.SIM_STATE_READY:
                return "ready";
            default:
                return null;
        }
    }

    @Rpc(
            description = "Returns the unique subscriber ID, for example, the IMSI for a GSM phone. Return null if it is unavailable.")
    public String getSubscriberId() {
        return mTelephonyManager.getSubscriberId();
    }

    @Rpc(description = "Retrieves the alphabetic identifier associated with the voice mail number.")
    public String getVoiceMailAlphaTag() {
        return mTelephonyManager.getVoiceMailAlphaTag();
    }

    @Rpc(description = "Returns the voice mail number. Return null if it is unavailable.")
    public String getVoiceMailNumber() {
        return mTelephonyManager.getVoiceMailNumber();
    }

    @Rpc(
            description = "Returns true if the device is considered roaming on the current network, for GSM purposes.")
    public Boolean checkNetworkRoaming() {
        return mTelephonyManager.isNetworkRoaming();
    }

    @Rpc(
            description = "Returns the unique device ID, for example, the IMEI for GSM and the MEID for CDMA phones. Return null if device ID is not available.")
    public String getDeviceId() {
        return mTelephonyManager.getDeviceId();
    }

    @Rpc(
            description = "Returns the software version number for the device, for example, the IMEI/SV for GSM phones. Return null if the software version is not available.")
    public String getDeviceSoftwareVersion() {
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    @Rpc(
            description = "Returns the phone number string for line 1, for example, the MSISDN for a GSM phone. Return null if it is unavailable.")
    public String getLine1Number() {
        return mTelephonyManager.getLine1Number();
    }

    @Rpc(description = "Returns the neighboring cell information of the device.")
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        return mTelephonyManager.getNeighboringCellInfo();
    }

    @Rpc(description = "Checks the data connection state.",
            returns = "True if data conenction is enabled.")
    public Boolean checkDataConnection() {
        return mConnect.getMobileDataEnabled();
    }

    @Rpc(description = "Toggles data connection on or off.")
    public void toggleDataConnection(
            @RpcParameter(name = "enabled")
            @RpcOptional
            Boolean enabled) {
        if (enabled == null) {
            enabled = !checkDataConnection();
        }
        mTelephonyManager.setDataEnabled(enabled);
    }

    @Rpc(description = "Sets an APN and make that as preferred APN.")
    public void setAPN(
            @RpcParameter(name = "name")
            final String name,
            @RpcParameter(name = "apn")
            final String apn,
            @RpcParameter(name = "type")
            @RpcOptional
            @RpcDefault("")
            final String type) {
        Uri uri;
        Cursor cursor;

        String mcc = "";
        String mnc = "";

        String numeric = SystemProperties.get(
                TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        // MCC is first 3 chars and then in 2 - 3 chars of MNC
        if (numeric != null && numeric.length() > 4) {
            // Country code
            mcc = numeric.substring(0, 3);
            // Network code
            mnc = numeric.substring(3);
        }

        uri = mService.getContentResolver().insert(
                Telephony.Carriers.CONTENT_URI, new ContentValues());
        if (uri == null) {
            Log.w("Failed to insert new telephony provider into "
                    + Telephony.Carriers.CONTENT_URI);
            return;
        }

        cursor = mService.getContentResolver().query(uri, sProjection, null,
                null, null);
        cursor.moveToFirst();

        ContentValues values = new ContentValues();

        values.put(Telephony.Carriers.NAME, name);
        values.put(Telephony.Carriers.APN, apn);
        values.put(Telephony.Carriers.PROXY, "");
        values.put(Telephony.Carriers.PORT, "");
        values.put(Telephony.Carriers.MMSPROXY, "");
        values.put(Telephony.Carriers.MMSPORT, "");
        values.put(Telephony.Carriers.USER, "");
        values.put(Telephony.Carriers.SERVER, "");
        values.put(Telephony.Carriers.PASSWORD, "");
        values.put(Telephony.Carriers.MMSC, "");
        values.put(Telephony.Carriers.TYPE, type);
        values.put(Telephony.Carriers.MCC, mcc);
        values.put(Telephony.Carriers.MNC, mnc);
        values.put(Telephony.Carriers.NUMERIC, mcc + mnc);

        int ret = mService.getContentResolver().update(uri, values, null, null);
        Log.d("after update " + ret);
        cursor.close();

        // Make this APN as the preferred
        String where = "name=\"" + name + "\"";

        Cursor c = mService.getContentResolver().query(
                Telephony.Carriers.CONTENT_URI,
                new String[] {
                        "_id", "name", "apn", "type"
                }, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (c != null) {
            c.moveToFirst();
            String key = c.getString(0);
            final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
            ContentResolver resolver = mService.getContentResolver();
            ContentValues prefAPN = new ContentValues();
            prefAPN.put("apn_id", key);
            resolver.update(Uri.parse(PREFERRED_APN_URI), prefAPN, null, null);
        }
        c.close();
    }

    @Rpc(description = "Returns the number of APNs defined")
    public int getNumberOfAPNs() {
        int noOfAPN = 0;
        String where = "numeric=\""
                + android.os.SystemProperties.get(
                        TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "")
                + "\"";

        Cursor cursor = mService.getContentResolver().query(
                Telephony.Carriers.CONTENT_URI,
                new String[] {
                        "_id", "name", "apn", "type"
                }, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            noOfAPN = cursor.getCount();
        }
        cursor.close();
        return noOfAPN;
    }

    @Rpc(description = "Returns the currently selected APN name")
    public String getSelectedAPN() {
        String key = null;
        int ID_INDEX = 0;
        final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";

        Cursor cursor = mService.getContentResolver().query(
                Uri.parse(PREFERRED_APN_URI), new String[] {
                    "name"
                }, null,
                null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }
}
