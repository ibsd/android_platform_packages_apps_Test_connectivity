/*
 * Copyright (C) 2014 Google Inc.
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

package com.googlecode.android_scripting.facade.tele;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.provider.Telephony;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyProperties;

import android.content.ContentValues;
import android.os.SystemProperties;

import com.googlecode.android_scripting.facade.AndroidFacade;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .CallStateChangeListener;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .DataConnectionChangeListener;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .DataConnectionStateChangeListener;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .ServiceStateChangeListener;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
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

    private final Service mService;
    private final AndroidFacade mAndroidFacade;
    private final EventFacade mEventFacade;
    private final TelephonyManager mTelephonyManager;

    private CallStateChangeListener mCallStateChangeListener;
    private DataConnectionChangeListener mDataConnectionChangeListener;
    private DataConnectionStateChangeListener mDataConnectionStateChangeListener;
    private ServiceStateChangeListener mServiceStateChangeListener;

    private ITelephony mITelephony;
    private PhoneStateListener mPhoneStateListener;

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
            Telephony.Carriers.MVNO_MATCH_DATA // 21
    };

    public PhoneFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelephonyManager =
                (TelephonyManager) mService.getSystemService(Context.TELEPHONY_SERVICE);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mEventFacade = manager.getReceiver(EventFacade.class);
        MainThread.run(manager.getService(), new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                mCallStateChangeListener = new CallStateChangeListener(mEventFacade);
                mDataConnectionChangeListener = new DataConnectionChangeListener(mEventFacade);
                mDataConnectionStateChangeListener = new DataConnectionStateChangeListener(mEventFacade);
                mServiceStateChangeListener = new ServiceStateChangeListener(mEventFacade);
                return null;
            }
        });
    }

    @Rpc(description = "Starts tracking call state change.")
    public void phoneStartTrackingCallState() {
        mTelephonyManager.listen(mCallStateChangeListener,
                                   CallStateChangeListener.sListeningStates);
    }

    @Rpc(description = "Turn on/off precise listening on fore/background or ringing calls.")
    public void phoneAdjustPreciseCallStateListenLevel(String type, Boolean listen) {
        if (type.equals("Foreground")) {
          mCallStateChangeListener.listenForeground = listen;
        } else if (type.equals("Ringing")) {
            mCallStateChangeListener.listenRinging = listen;
        } else if (type.equals("Background")) {
            mCallStateChangeListener.listenBackground = listen;
        }
    }

    @Rpc(description = "Stops tracking call state change.")
    public void phoneStopTrackingCallStateChange() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Starts tracking power level change.")
    public void phoneStartTrackingPowerLevelChange() {
        mTelephonyManager.listen(mDataConnectionChangeListener,
                                 DataConnectionChangeListener.sListeningStates);
    }

    @Rpc(description = "Stops tracking power level change.")
    public void phoneStopTrackingPowerLevelChange() {
        mTelephonyManager.listen(mDataConnectionChangeListener, PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Starts tracking data connection state change.")
    public void phoneStartTrackingDataConnectionStateChange() {
        mTelephonyManager.listen(mDataConnectionStateChangeListener,
                                 DataConnectionStateChangeListener.sListeningStates);
    }

    @Rpc(description = "Stops tracking data connection state change.")
    public void phoneStopTrackingDataConnectionStateChange() {
        mTelephonyManager.listen(mDataConnectionStateChangeListener, PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Starts tracking service state change.")
    public void phoneStartTrackingServiceStateChange() {
        mTelephonyManager.listen(mServiceStateChangeListener,
                                 ServiceStateChangeListener.sListeningStates);
    }

    @Rpc(description = "Stops tracking service state change.")
    public void phoneStopTrackingServiceStateChange() {
        mTelephonyManager.listen(mServiceStateChangeListener, PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Calls a contact/phone number by URI.")
    public void phoneCall(@RpcParameter(name = "uri")
    final String uriString)
            throws Exception {
        Uri uri = Uri.parse(uriString);
        if (uri.getScheme().equals("content")) {
            String phoneNumberColumn = ContactsContract.PhoneLookup.NUMBER;
            String selectWhere = null;
            if ((FacadeManager.class.cast(mManager)).getSdkLevel() >= 5) {
                Class<?> contactsContract_Data_class =
                        Class.forName("android.provider.ContactsContract$Data");
                Field RAW_CONTACT_ID_field =
                        contactsContract_Data_class.getField("RAW_CONTACT_ID");
                selectWhere = RAW_CONTACT_ID_field.get(null).toString() + "="
                        + uri.getLastPathSegment();
                Field CONTENT_URI_field =
                        contactsContract_Data_class.getField("CONTENT_URI");
                uri = Uri.parse(CONTENT_URI_field.get(null).toString());
                Class<?> ContactsContract_CommonDataKinds_Phone_class =
                        Class.forName("android.provider.ContactsContract$CommonDataKinds$Phone");
                Field NUMBER_field =
                        ContactsContract_CommonDataKinds_Phone_class.getField("NUMBER");
                phoneNumberColumn = NUMBER_field.get(null).toString();
            }
            ContentResolver resolver = mService.getContentResolver();
            Cursor c = resolver.query(uri, new String[] {
                    phoneNumberColumn
            },
                    selectWhere, null, null);
            String number = "";
            if (c.moveToFirst()) {
                number = c.getString(c.getColumnIndexOrThrow(phoneNumberColumn));
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

    @Rpc(description = "Answers an incoming ringing call.")
    public void phoneAnswerCall() throws RemoteException {
        mITelephony.silenceRinger();
        mITelephony.answerRingingCall();
    }

    @Rpc(description = "Dials a phone number.")
    public void phoneDialNumber(@RpcParameter(name = "phone number")
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

    @Rpc(description = "Returns the current RAT in use on the device.")
    public String getNetworkType() {
        // TODO(damonkohler): API level 5 has many more types.
        switch (mTelephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "edge";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "gprs";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "umts";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "hsdpa";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "hsupa";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "cdma";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "evdo_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "evdo_a";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "evdo_b";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xrtt";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "iden";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "lte";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "ehrpd";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "hspap";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "unknown";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the device phone type.")
    public String getPhoneType() {
        switch (mTelephonyManager.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_GSM:
                return "gsm";
            case TelephonyManager.PHONE_TYPE_NONE:
                return "none";
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "cdma";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "sip";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the MCC")
    public String getSimCountryIso() {
        return mTelephonyManager.getSimCountryIso();
    }

    @Rpc(description = "Returns the MCC+MNC")
    public String getSimOperator() {
        return mTelephonyManager.getSimOperator();
    }

    @Rpc(description = "Returns the Service Provider Name (SPN).")
    public String getSimOperatorName() {
        return mTelephonyManager.getSimOperatorName();
    }

    @Rpc(description = "Returns the serial number of the SIM, or Null if unavailable")
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

    @Rpc(description = "Returns the unique subscriber ID (such as IMSI), or null if unavailable")
    public String getSubscriberId() {
        return mTelephonyManager.getSubscriberId();
    }

    @Rpc(description = "Retrieves the alphabetic id associated with the voice mail number.")
    public String getVoiceMailAlphaTag() {
        return mTelephonyManager.getVoiceMailAlphaTag();
    }

    @Rpc(description = "Returns the voice mail number; null if unavailable.")
    public String getVoiceMailNumber() {
        return mTelephonyManager.getVoiceMailNumber();
    }

    @Rpc(description = "Returns true if the device is in a roaming state")
    public Boolean checkNetworkRoaming() {
        return mTelephonyManager.isNetworkRoaming();
    }

    @Rpc(description = "Returns the unique device ID such as MEID or IMEI, null if unavailable")
    public String getDeviceId() {
        return mTelephonyManager.getDeviceId();
    }

    @Rpc(description = "Returns the modem sw version, such as IMEI-SV; null if unavailable")
    public String getDeviceSoftwareVersion() {
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    @Rpc(description = "Returns phone # string \"line 1\", such as MSISDN; null if unavailable")
    public String getLine1Number() {
        return mTelephonyManager.getLine1Number();
    }

    @Rpc(description = "Returns the neighboring cell information of the device.")
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        return mTelephonyManager.getNeighboringCellInfo();
    }

    @Rpc(description = "Returns True if data connection is enabled.")
    public Boolean checkDataConnection() {
        return mTelephonyManager.getDataEnabled();
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
    public void setAPN(@RpcParameter(name = "name") final String name,
                       @RpcParameter(name = "apn") final String apn,
                       @RpcParameter(name = "type") @RpcOptional @RpcDefault("")
                       final String type) {
        Uri uri;
        Cursor cursor;

        String mcc = "";
        String mnc = "";

        String numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
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
            Log.w("Failed to insert new provider into " + Telephony.Carriers.CONTENT_URI);
            return;
        }

        cursor = mService.getContentResolver().query(uri, sProjection, null, null, null);
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
        int result = 0;
        String where = "numeric=\"" + android.os.SystemProperties.get(
                        TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "") + "\"";

        Cursor cursor = mService.getContentResolver().query(
                Telephony.Carriers.CONTENT_URI,
                new String[] {"_id", "name", "apn", "type"}, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            result = cursor.getCount();
        }
        cursor.close();
        return result;
    }

    @Rpc(description = "Returns the currently selected APN name")
    public String getSelectedAPN() {
        String key = null;
        int ID_INDEX = 0;
        final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";

        Cursor cursor = mService.getContentResolver().query(Uri.parse(PREFERRED_APN_URI),
                new String[] {"name"}, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    @Rpc(description = "Sets the preferred Network type")
    public void setPreferredNetwork(Integer networktype) {
        android.provider.Settings.Global.putInt(mService.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                networktype );
        mTelephonyManager.setPreferredNetworkType(networktype);
    }

    @Rpc(description = "Returns the current data connection state")
    public String getDataConnectionState() {
        int state = mTelephonyManager.getDataState();

        switch(state) {
            case TelephonyManager.DATA_DISCONNECTED:
                return "DATA_DISCONNECTED";
            case TelephonyManager.DATA_CONNECTING:
                return "DATA_CONNECTING";
            case TelephonyManager.DATA_CONNECTED:
                return "DATA_CONNECTED";
            case TelephonyManager.DATA_SUSPENDED:
                return "DATA_SUSPENDED";
            default:
                return "DATA_UNKNOWN";
        }
    }

    @Override
    public void shutdown() {
        phoneStopTrackingCallStateChange();
        phoneStopTrackingPowerLevelChange();
        phoneStopTrackingServiceStateChange();
    }
}
