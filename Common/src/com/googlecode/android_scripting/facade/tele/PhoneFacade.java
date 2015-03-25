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

import android.app.Activity;
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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyProperties;

import android.content.ContentValues;
import android.os.SystemProperties;

import com.googlecode.android_scripting.facade.AndroidFacade;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .CallStateChangeListener;
import com.googlecode.android_scripting.facade.tele.TelephonyStateListeners
                                                   .DataConnectionRealTimeInfoChangeListener;
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
import java.util.HashMap;

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
    private ITelephony mITelephony;
    private final SubscriptionManager mSubscriptionManager;
    private List<SubscriptionInfo> mSubInfos;
    private HashMap<Integer, StateChangeListener> StateChangeListeners =
                             new HashMap<Integer, StateChangeListener>();

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
        mSubscriptionManager = SubscriptionManager.from(mService);
        mSubInfos = mSubscriptionManager.getAllSubscriptionInfoList();
        MainThread.run(manager.getService(), new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // Creating listeners for all subscription IDs
                for (int i = 0; i < mSubInfos.size(); i++) {
                    int subId = mSubInfos.get(i).getSubscriptionId();
                    StateChangeListener mStateListeners =
                                                     new StateChangeListener();
                    mStateListeners.mServiceStateChangeListener =
                        new ServiceStateChangeListener(mEventFacade, subId);
                    mStateListeners.mDataConnectionStateChangeListener =
                        new DataConnectionStateChangeListener(mEventFacade,
                                                      mTelephonyManager, subId);
                    mStateListeners.mCallStateChangeListener =
                        new CallStateChangeListener(mEventFacade, subId);
                    mStateListeners.mDataConnectionRTInfoChangeListener =
                        new DataConnectionRealTimeInfoChangeListener(mEventFacade,
                                                                     subId);

                    StateChangeListeners.put(subId, mStateListeners);
                }
                return null;
            }
        });
    }

    @Rpc(description = "Set preferred network setting " +
                       "for default subscription ID")
    public boolean phoneSetPreferredNetworkType(String mode) {
        return phoneSetPreferredNetworkTypeForSubscription(mode,
                                SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Tethering Entitlement Check")
    public boolean phoneIsTetheringModeAllowed(String mode, Integer timeout) {
        String[] mProvisionApp = mService.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
        /* following check defined in
            packages/apps/Settings/src/com/android/settings/TetherSettings.java
            isProvisioningNeeded
        */
        if ((mProvisionApp == null) || (mProvisionApp.length != 2)){
            Log.d("phoneIsTetheringModeAllowed: no check is present.");
            return true;
        }
        Log.d("phoneIsTetheringModeAllowed mProvisionApp 0 " + mProvisionApp[0]);
        Log.d("phoneIsTetheringModeAllowed mProvisionApp 1 " + mProvisionApp[1]);

        // FIXME: Need to use TetherSettings.xxx to replace the following private definitions.
        /* defined in packages/apps/Settings/src/com/android/settings/TetherSettings.java
        public static final int INVALID             = -1;
        public static final int WIFI_TETHERING      = 0;
        public static final int USB_TETHERING       = 1;
        public static final int BLUETOOTH_TETHERING = 2;
        private static final int PROVISION_REQUEST = 0;
        */
        final int INVALID             = -1;
        final int WIFI_TETHERING      = 0;
        final int USB_TETHERING       = 1;
        final int BLUETOOTH_TETHERING = 2;
        final int PROVISION_REQUEST = 0;

        int mTetherChoice = INVALID;
        if (mode.equals("wifi")){
            mTetherChoice = WIFI_TETHERING;
        } else if (mode.equals("usb")) {
            mTetherChoice = USB_TETHERING;
        } else if (mode.equals("bluetooth")) {
            mTetherChoice = BLUETOOTH_TETHERING;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
        intent.putExtra("TETHER_TYPE", mTetherChoice);
        int result;
        try{
            result = mAndroidFacade.startActivityForResultCodeWithTimeout(
                intent, PROVISION_REQUEST, timeout);
        } catch (Exception e) {
            Log.d("phoneTetherCheck exception" + e.toString());
            return false;
        }

        if (result == Activity.RESULT_OK) {
            return true;
        } else {
            return false;
        }
    }

    @Rpc(description = "Set preferred network setting " +
                       "for specified subscription ID")
    public boolean phoneSetPreferredNetworkTypeForSubscription(String mode,
                               @RpcParameter(name = "subId") Integer subId) {
        int networkType;
        int phoneType = mTelephonyManager.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            switch (mode.toUpperCase()) {
                case "4G":
                case "LTE":
                    networkType = RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
                    break;
                case "3G":
                case "WCDMA":
                    networkType = RILConstants.NETWORK_MODE_WCDMA_PREF;
                    break;
                case "2G":
                case "GSM":
                    networkType = RILConstants.NETWORK_MODE_GSM_ONLY;
                    break;
                case "GLOBAL":
                    networkType =
                            RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
                    break;
                default:
                    return false;
            }
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            switch (mode.toUpperCase()) {
                case "4G":
                case "LTE":
                    networkType = RILConstants.NETWORK_MODE_LTE_CDMA_EVDO;
                    break;
                case "3G":
                case "EVDO":
                    networkType = RILConstants.NETWORK_MODE_CDMA;
                    break;
                case "2G":
                case "1X":
                    networkType = RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
                    break;
                case "GLOBAL":
                    networkType =
                            RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
                    break;
                default:
                    return false;
            }
        } else {
            return false;
        }
        Log.v("SL4A: Setting the preferred network setting of subId: "
                + subId +"to:" + networkType);
        mTelephonyManager.setPreferredNetworkType(networkType);
        // TODO No framework API for setPreferredNetworkType(int subId)
        // Need to find some other way to do this
        return true;
    }

    @Rpc(description = "Get preferred network setting for " +
                       "default subscription ID .Return value is integer.")
    public int phoneGetPreferredNetworkTypeInteger() {
        return phoneGetPreferredNetworkTypeIntegerForSubscription(
                                         SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Get preferred network setting for " +
                       "specified subscription ID .Return value is integer.")
    public int phoneGetPreferredNetworkTypeIntegerForSubscription(
               @RpcParameter(name = "subId") Integer subId) {
        // TODO No framework API for getPreferredNetworkType(int subId)
        // Need to find some other way to do this
        return mTelephonyManager.getPreferredNetworkType();
    }

    @Rpc(description = "Get preferred network setting for " +
                       "default subscription ID.Return value is String.")
    public String phoneGetPreferredNetworkType() {
        return phoneGetPreferredNetworkTypeForSubscription(
                                       SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Get preferred network setting for " +
                       "specified subscription ID.Return value is String.")
    public String phoneGetPreferredNetworkTypeForSubscription(
            @RpcParameter(name = "subId") Integer subId) {
        // TODO No framework API for getPreferredNetworkType(int subId)
        // Need to find some other way to do this
        int mode = mTelephonyManager.getPreferredNetworkType();
        int phoneType = mTelephonyManager.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            switch (mode) {
                case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                    return "LTE";
                case RILConstants.NETWORK_MODE_WCDMA_PREF:
                    return "WCDMA";
                case RILConstants.NETWORK_MODE_GSM_ONLY:
                    return "GSM";
                case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    return "GLOBAL";
                default:
                    Log.d("Unknown mode in phone type GSM: " + mode);
                    return "UNKNOWN";
            }
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            switch (mode) {
                case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                    return "LTE";
                case RILConstants.NETWORK_MODE_CDMA:
                    return "EVDO";
                case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                    return "1X";
                case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    return "GLOBAL";
                default:
                    Log.d("Unknown mode in phone type CDMA: " + mode);
                    return "UNKNOWN";
            }
        } else {
            Log.d("Unknown phone type: " + phoneType);
            return null;
        }
    }

    @Rpc(description = "Starts tracking call state change" +
                       "for default subscription ID.")
    public Boolean phoneStartTrackingCallState() {
        return phoneStartTrackingCallStateForSubscription(
                              SubscriptionManager.getDefaultVoiceSubId());
    }

    @Rpc(description = "Starts tracking call state change" +
                       "for specified subscription ID.")
    public Boolean phoneStartTrackingCallStateForSubscription(
                @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mCallStateChangeListener,
                CallStateChangeListener.sListeningStates);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Turn on/off precise listening on fore/background or" +
                       " ringing calls for default voice subscription ID.")
    public Boolean phoneAdjustPreciseCallStateListenLevel(String type,
                                                          Boolean listen) {
        return phoneAdjustPreciseCallStateListenLevelForSubscription(type, listen,
                                 SubscriptionManager.getDefaultVoiceSubId());
    }

    @Rpc(description = "Turn on/off precise listening on fore/background or" +
                       " ringing calls for specified subscription ID.")
    public Boolean phoneAdjustPreciseCallStateListenLevelForSubscription(String type,
                   Boolean listen,
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            if (type.equals("Foreground")) {
                StateChangeListeners.get(subId).mCallStateChangeListener.listenForeground = listen;
            } else if (type.equals("Ringing")) {
                StateChangeListeners.get(subId).mCallStateChangeListener.listenRinging = listen;
            } else if (type.equals("Background")) {
                StateChangeListeners.get(subId).mCallStateChangeListener.listenBackground = listen;
            }
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Stops tracking call state change " +
            "for default voice subscription ID.")
    public Boolean phoneStopTrackingCallStateChange() {
        return phoneStopTrackingCallStateChangeForSubscription(
                SubscriptionManager.getDefaultVoiceSubId());
    }

    @Rpc(description = "Stops tracking call state change " +
                       "for specified subscription ID.")
    public Boolean phoneStopTrackingCallStateChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mCallStateChangeListener,
                PhoneStateListener.LISTEN_NONE);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Starts tracking data connection real time info change" +
                       "for default subscription ID.")
    public Boolean phoneStartTrackingDataConnectionRTInfoChange() {
        return phoneStartTrackingDataConnectionRTInfoChangeForSubscription(
                                 SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Starts tracking data connection real time info change" +
                       "for specified subscription ID.")
    public Boolean phoneStartTrackingDataConnectionRTInfoChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mDataConnectionRTInfoChangeListener,
                DataConnectionRealTimeInfoChangeListener.sListeningStates);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Stops tracking data connection real time info change" +
                       "for default subscription ID.")
    public Boolean phoneStopTrackingDataConnectionRTInfoChange() {
        return phoneStopTrackingDataConnectionRTInfoChangeForSubscription(
                                 SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Stops tracking data connection real time info change" +
                       "for specified subscription ID.")
    public Boolean phoneStopTrackingDataConnectionRTInfoChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mDataConnectionRTInfoChangeListener,
                PhoneStateListener.LISTEN_NONE);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Starts tracking data connection state change" +
                       "for default subscription ID..")
    public Boolean phoneStartTrackingDataConnectionStateChange() {
        return phoneStartTrackingDataConnectionStateChangeForSubscription(
                                 SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Starts tracking data connection state change" +
                       "for specified subscription ID.")
    public Boolean phoneStartTrackingDataConnectionStateChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mDataConnectionStateChangeListener,
                DataConnectionStateChangeListener.sListeningStates);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Stops tracking data connection state change " +
                       "for default subscription ID..")
    public Boolean phoneStopTrackingDataConnectionStateChange() {
        return phoneStartTrackingDataConnectionStateChangeForSubscription(
                                 SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Stops tracking data connection state change " +
                       "for specified subscription ID..")
    public Boolean phoneStopTrackingDataConnectionStateChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mDataConnectionStateChangeListener,
                PhoneStateListener.LISTEN_NONE);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Starts tracking service state change " +
                       "for default subscription ID.")
    public Boolean phoneStartTrackingServiceStateChange() {
        return phoneStartTrackingServiceStateChangeForSubscription(
                                 SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Starts tracking service state change " +
                       "for specified subscription ID.")
    public Boolean phoneStartTrackingServiceStateChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
         try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mServiceStateChangeListener,
                ServiceStateChangeListener.sListeningStates);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
    }

    @Rpc(description = "Stops tracking service state change " +
                       "for default subscription ID.")
    public Boolean phoneStopTrackingServiceStateChange() {
        return phoneStartTrackingServiceStateChangeForSubscription(
                                 SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Stops tracking service state change " +
                       "for specified subscription ID.")
    public Boolean phoneStopTrackingServiceStateChangeForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        try {
            mTelephonyManager.listen(
                StateChangeListeners.get(subId).mServiceStateChangeListener,
                PhoneStateListener.LISTEN_NONE);
            return true;
        } catch (Exception e) {
            Log.e("Invalid subscription ID");
            return false;
        }
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

    @Rpc(description = "Calls an Emergency number.")
    public void phoneCallEmergencyNumber(
            @RpcParameter(name = "phone number")
            final String number)
            throws Exception {
        String uriString = "tel:" + URLEncoder.encode(number, "ASCII");
        mAndroidFacade.startActivity(Intent.ACTION_CALL_PRIVILEGED, uriString,
                                     null, null, null, null, null);
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

    @Rpc(description = "Returns the numeric name (MCC+MNC) of registered operator." +
                       "for default subscription ID")
    public String getNetworkOperator() {
        return getNetworkOperatorForSubscription(
                        SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the numeric name (MCC+MNC) of registered operator" +
                       "for specified subscription ID.")
    public String getNetworkOperatorForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getNetworkOperatorForSubscription(subId);
    }

    @Rpc(description = "Returns the alphabetic name of current registered operator" +
                       "for specified subscription ID.")
    public String getNetworkOperatorName() {
        return getNetworkOperatorNameForSubscription(
                        SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the alphabetic name of registered operator " +
                       "for specified subscription ID.")
    public String getNetworkOperatorNameForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getNetworkOperatorName(subId);
    }

    private String getNetworkTypeString(int networkType) {

        switch (networkType) {
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
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "hspa";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "hspap";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "gsm";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "td_scdma";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "iwlan";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "unknown";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the current RAT in use on the device.+" +
                       "for default subscription ID")
    public String getNetworkType() {

        Log.d("sl4a:getNetworkType() is deprecated!" +
                "Please use getVoiceNetworkType()" +
                " or getDataNetworkTpe()");

        return getNetworkTypeForSubscription(
                       SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the current RAT in use on the device" +
            " for a given Subscription.")
    public String getNetworkTypeForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {

        Log.d("sl4a:getNetworkTypeForSubscriber() is deprecated!" +
                "Please use getVoiceNetworkType()" +
                " or getDataNetworkTpe()");

        return getNetworkTypeString(mTelephonyManager.getNetworkType(subId));
    }

    @Rpc(description = "Returns the current voice RAT for" +
            " the default voice subscription.")
    public String getVoiceNetworkType() {
        return getVoiceNetworkTypeForSubscription(
                         SubscriptionManager.getDefaultVoiceSubId());
    }

    @Rpc(description = "Returns the current voice RAT for" +
            " the specified voice subscription.")
    public String getVoiceNetworkTypeForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return getNetworkTypeString(mTelephonyManager.getVoiceNetworkType(subId));
    }

    @Rpc(description = "Returns the current data RAT for" +
            " the defaut data subscription")
    public String getDataNetworkType() {
        return getDataNetworkTypeForSubscription(
                         SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Returns the current data RAT for" +
            " the specified data subscription")
    public String getDataNetworkTypeForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return getNetworkTypeString(mTelephonyManager.getDataNetworkType(subId));
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

    @Rpc(description = "Returns the MCC for default subscription ID")
    public String getSimCountryIso() {
         return getSimCountryIsoForSubscription(
                      SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the MCC for specified subscription ID")
    public String getSimCountryIsoForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getSimCountryIso(subId);
    }

    @Rpc(description = "Returns the MCC+MNC for default subscription ID")
    public String getSimOperator() {
        return getSimOperatorForSubscription(
                  SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the MCC+MNC for specified subscription ID")
    public String getSimOperatorForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getSimOperator(subId);
    }

    @Rpc(description = "Returns the Service Provider Name (SPN)" +
                       "for default subscription ID")
    public String getSimOperatorName() {
        return getSimOperatorNameForSubscription(
                  SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the Service Provider Name (SPN)" +
                       " for specified subscription ID.")
    public String getSimOperatorNameForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getSimOperatorNameForSubscription(subId);
    }

    @Rpc(description = "Returns the serial number of the SIM for " +
                       "default subscription ID, or Null if unavailable")
    public String getSimSerialNumber() {
        return getSimSerialNumberForSubscription(
                  SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the serial number of the SIM for " +
                       "specified subscription ID, or Null if unavailable")
    public String getSimSerialNumberForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getSimSerialNumber(subId);
    }

    @Rpc(description = "Returns the state of the SIM card for default slot ID.")
    public String getSimState() {
        return getSimStateForSlotId(
                  mTelephonyManager.getDefaultSim());
    }

    @Rpc(description = "Returns the state of the SIM card for specified slot ID.")
    public String getSimStateForSlotId(
                  @RpcParameter(name = "slotId") Integer slotId) {
        switch (mTelephonyManager.getSimState(slotId)) {
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

    @Rpc(description = "Returns the unique subscriber ID (such as IMSI) " +
                       "for default subscription ID, or null if unavailable")
    public String getSubscriberId() {
        return getSubscriberIdForSubscription(
                   SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the unique subscriber ID (such as IMSI) " +
                       "for specified subscription ID, or null if unavailable")
    public String getSubscriberIdForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getSubscriberId(subId);
    }

    @Rpc(description = "Retrieves the alphabetic id associated with the" +
                       " voice mail number for default subscription ID.")
    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTagForSubscription(
                   SubscriptionManager.getDefaultSubId());
    }


    @Rpc(description = "Retrieves the alphabetic id associated with the " +
                       "voice mail number for specified subscription ID.")
    public String getVoiceMailAlphaTagForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getVoiceMailAlphaTag(subId);
    }

    @Rpc(description = "Returns the voice mail number " +
                       "for default subscription ID; null if unavailable.")
    public String getVoiceMailNumber() {
        return getVoiceMailNumberForSubscription(
                   SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the voice mail number " +
                        "for specified subscription ID; null if unavailable.")
    public String getVoiceMailNumberForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getVoiceMailNumber(subId);
    }

    @Rpc(description = "Returns true if the device is in  roaming state" +
                       "for default subscription ID")
    public Boolean checkNetworkRoaming() {
        return checkNetworkRoamingForSubscription(
                             SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns true if the device is in roaming state " +
                       "for specified subscription ID")
    public Boolean checkNetworkRoamingForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.isNetworkRoaming(subId);
    }

    @Rpc(description = "Returns the unique device ID such as MEID or IMEI " +
                       "for deault sim slot ID, null if unavailable")
    public String getDeviceId() {
        return getDeviceIdForSlotId(mTelephonyManager.getDefaultSim());
    }

    @Rpc(description = "Returns the unique device ID such as MEID or IMEI for" +
                       " specified slot ID, null if unavailable")
    public String getDeviceIdForSlotId(
                  @RpcParameter(name = "slotId")
                  Integer slotId){
        return mTelephonyManager.getDeviceId(slotId);
    }

    @Rpc(description = "Returns the modem sw version, such as IMEI-SV;" +
                       " null if unavailable")
    public String getDeviceSoftwareVersion() {
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    @Rpc(description = "Returns phone # string \"line 1\", such as MSISDN " +
                       "for default subscription ID; null if unavailable")
    public String getLine1Number() {
        return getLine1NumberForSubscription(
                        SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns phone # string \"line 1\", such as MSISDN " +
                       "for specified subscription ID; null if unavailable")
    public String getLine1NumberForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getLine1NumberForSubscriber(subId);
    }

    @Rpc(description = "Returns the neighboring cell information of the device.")
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        return mTelephonyManager.getNeighboringCellInfo();
    }

    @Rpc(description = "Returns True if cellular data is enabled for" +
                       "default data subscription ID.")
    public Boolean isDataEnabled() {
        return isDataEnabledForSubscription(
                   SubscriptionManager.getDefaultDataSubId());
    }

    @Rpc(description = "Returns True if data connection is enabled.")
    public Boolean isDataEnabledForSubscription(
                   @RpcParameter(name = "subId") Integer subId) {
        return mTelephonyManager.getDataEnabled(subId);
    }

    @Rpc(description = "Toggles data connection on /off for" +
                       " default data subscription ID.")
    public void toggleDataConnection(
                @RpcParameter(name = "enabled")
                @RpcOptional Boolean enabled) {
        toggleDataConnectionForSubscription(
                         SubscriptionManager.getDefaultDataSubId(), enabled);
    }

    @Rpc(description = "Toggles data connection on/off for" +
                       " specified subscription ID")
    public void toggleDataConnectionForSubscription(
                @RpcParameter(name = "subId") Integer subId,
                @RpcParameter(name = "enabled")
                @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !isDataEnabledForSubscription(subId);
        }
        mTelephonyManager.setDataEnabled(subId, enabled);
    }

    @Rpc(description = "Sets an APN and make that as preferred APN.")
    public void setAPN(@RpcParameter(name = "name") final String name,
                       @RpcParameter(name = "apn") final String apn,
                       @RpcParameter(name = "type") @RpcOptional @RpcDefault("")
                       final String type,
                       @RpcParameter(name = "subId") @RpcOptional Integer subId) {
        //TODO Need to find out how to set APN for specific subId
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
    public int getNumberOfAPNs(
               @RpcParameter(name = "subId")
               @RpcOptional Integer subId) {
        //TODO Need to find out how to get Number of APNs for specific subId
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
    public String getSelectedAPN(
                  @RpcParameter(name = "subId")
                  @RpcOptional Integer subId) {
        //TODO Need to find out how to get selected APN for specific subId
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

    @Rpc(description = "Enables or Disables Video Calling()")
    public void enableVideoCalling(boolean enable) {
        mTelephonyManager.enableVideoCalling(enable);
    }

    @Rpc(description = "Returns a boolean of isVideoCallingEnabled()")
    public Boolean isVideoCallingEnabled() {
        return mTelephonyManager.isVideoCallingEnabled();
    }

    @Rpc(description = "Returns a boolean of isImsRegistered()")
    public Boolean isImsRegistered() {
        return mTelephonyManager.isImsRegistered();
    }

    @Rpc(description = "Returns a boolean of isVolteEnabled()")
    public Boolean isVolteEnabled() {
        return mTelephonyManager.isVolteEnabled();
    }

    @Rpc(description = "Returns a boolean of isWifiCallingEnabled()")
    public Boolean isWifiCallingEnabled() {
        return mTelephonyManager.isWifiCallingEnabled();
    }

    @Rpc(description = "Returns the service state for default subscription ID")
    public String getServiceState() {
        // TODO
        // No framework API available
        return getServiceStateForSubscription(
                                 SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the service state for specified subscription ID")
    public String getServiceStateForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        // TODO
        // No framework API available
        return null;
    }

    @Rpc(description = "Returns the call state for default subscription ID")
    public String getCallState() {
        return getCallStateForSubscription(
                               SubscriptionManager.getDefaultSubId());
    }

    @Rpc(description = "Returns the call state for specified subscription ID")
    public String getCallStateForSubscription(
                  @RpcParameter(name = "subId") Integer subId) {
        switch (mTelephonyManager.getCallState(subId)) {
            case TelephonyManager.CALL_STATE_IDLE:
                return "idle";
            case TelephonyManager.CALL_STATE_RINGING:
                return "ringing";
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return "offhook";
            default:
                return null;
        }
    }

    private static class StateChangeListener {
        private ServiceStateChangeListener mServiceStateChangeListener;
        private CallStateChangeListener mCallStateChangeListener;
        private DataConnectionStateChangeListener
                           mDataConnectionStateChangeListener;
        private DataConnectionRealTimeInfoChangeListener
                           mDataConnectionRTInfoChangeListener;
    }

    @Override
    public void shutdown() {
        for (int i = 0; i < mSubInfos.size(); i++) {
           int subId = mSubInfos.get(i).getSubscriptionId();
           phoneStopTrackingCallStateChangeForSubscription(subId);
           phoneStopTrackingDataConnectionRTInfoChangeForSubscription(subId);
           phoneStopTrackingServiceStateChangeForSubscription(subId);
           phoneStopTrackingDataConnectionStateChangeForSubscription(subId);
        }
    }
}
