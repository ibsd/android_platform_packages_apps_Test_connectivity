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

package com.googlecode.android_scripting.facade;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsCbMessage;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbCmasInfo;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.ArrayList;

//import android.telephony.ServiceState;

/**
 * Exposes SmsManager functionality.
 *
 */
public class SmsFacade extends RpcReceiver {

    private final EventFacade mEventFacade;
    private final SmsManager mSms;
    private final Service mService;
    private IntentFilter mSmsReceived;
    private BroadcastReceiver mSmsSendListener;
    private BroadcastReceiver mSmsIncomingListener;
    private int mNumExpectedSentEvents;
    private int mNumReceivedSentEvents;
    private int mNumExpectedDeliveredEvents;
    private int mNumReceivedDeliveredEvents;
    private Intent mSendIntent;
    private Intent mDeliveredIntent;
    private boolean mListeningIncomingSms;
    private IntentFilter mEmergencyCBMessage;
    private BroadcastReceiver mGsmEmergencyCBMessageListener;
    private BroadcastReceiver mCdmaEmergencyCBMessageListener;
    private boolean mGsmEmergencyCBListenerRegistered;
    private boolean mCdmaEmergencyCBListenerRegistered;


    private static final String MESSAGE_STATUS_DELIVERED_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_STATUS_DELIVERED";
    private static final String MESSAGE_SENT_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_SENT";
    private static final String MESSAGE_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_RECEIVED";
    private static final String EMERGENCY_CB_MESSAGE_RECEIVED_ACTION=
            "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";
    private final int MAX_MESSAGE_LENGTH = 160;
    private final int INTERNATIONAL_NUMBER_LENGTH = 12;
    private final int DOMESTIC_NUMBER_LENGTH = 10;

    private final int[] mGsmCbMessageIdList = {
        SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING,
        SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING,
        SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING,
        SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE ,
        SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_CHILD_ABDUCTION_EMERGENCY,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_REQUIRED_MONTHLY_TEST,
        SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXERCISE
    };

    private final int[] mCdmaCbMessageIdList = {
            SmsEnvelope.SERVICE_CATEGORY_CMAS_PRESIDENTIAL_LEVEL_ALERT,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT ,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY ,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE
        };

    public SmsFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mSms = SmsManager.getDefault();
        mEventFacade = manager.getReceiver(EventFacade.class);
        mSmsSendListener = new SmsSendListener();
        mSmsIncomingListener = new SmsIncomingListener();
        mNumExpectedSentEvents = 0;
        mNumReceivedSentEvents = 0;
        mNumExpectedDeliveredEvents = 0;
        mNumReceivedDeliveredEvents = 0;
        mListeningIncomingSms=false;
        mGsmEmergencyCBMessageListener = new SmsEmergencyCBMessageListener();
        mCdmaEmergencyCBMessageListener = new SmsEmergencyCBMessageListener();
        mGsmEmergencyCBListenerRegistered = false;
        mCdmaEmergencyCBListenerRegistered = false;

        mSendIntent = new Intent(MESSAGE_SENT_ACTION);
        mDeliveredIntent = new Intent(MESSAGE_STATUS_DELIVERED_ACTION);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MESSAGE_SENT_ACTION);
        filter.addAction(MESSAGE_STATUS_DELIVERED_ACTION);
        mService.registerReceiver(mSmsSendListener, filter);
    }

    @Rpc(description = "Starts tracking incoming SMS.")
    public void smsStartTrackingIncomingMessage() {
        mSmsReceived = new IntentFilter(MESSAGE_RECEIVED_ACTION);
        mService.registerReceiver(mSmsIncomingListener, mSmsReceived);
        mListeningIncomingSms = true;
    }

    @Rpc(description = "Stops tracking incoming SMS.")
    public void smsStopTrackingIncomingMessage() {
        mListeningIncomingSms = false;
        try {
        mService.unregisterReceiver(mSmsIncomingListener);
        } catch( Exception e ) {
          Log.e( "Tried to unregister nonexistent SMS Listener!");
        }
    }

    @Rpc(description = "Send a text message to a specified number.")
    public void smsSendTextMessage(
            @RpcParameter(name = "phoneNumber")
            String phoneNumber,
            @RpcParameter(name = "message")
            String message,
            @RpcParameter(name = "deliveryReportRequired")
            Boolean deliveryReportRequired) {

        if(message.length() > MAX_MESSAGE_LENGTH) {
            ArrayList<String> messagesParts = mSms.divideMessage(message);
            mNumExpectedSentEvents = mNumExpectedDeliveredEvents = messagesParts.size();
            ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<PendingIntent>();
            for (int i = 0; i < messagesParts.size(); i++) {
                sentIntents.add(PendingIntent.getBroadcast(mService, 0, mSendIntent, 0));
                deliveredIntents.add(PendingIntent.getBroadcast(mService, 0, mDeliveredIntent, 0));
            }
            mSms.sendMultipartTextMessage(phoneNumber, null, messagesParts, sentIntents, deliveredIntents);
        } else {
            mNumExpectedSentEvents = mNumExpectedDeliveredEvents = 1;
            PendingIntent sentIntent = PendingIntent.getBroadcast(mService, 0, mSendIntent, 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(mService, 0, mDeliveredIntent, 0);
            mSms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveredIntent);
        }
    }

    @Rpc(description = "Retrieves all messages currently stored on ICC.")
    public ArrayList<SmsMessage> smsGetAllMessagesFromIcc() {
        return SmsManager.getDefault().getAllMessagesFromIcc();
    }

    @Rpc(description = "Starts tracking GSM Emergency CB Messages.")
    public void smsStartTrackingGsmEmergencyCBMessage() {
        if(!mGsmEmergencyCBListenerRegistered) {
            for (int messageId : mGsmCbMessageIdList) {
                mSms.enableCellBroadcast(
                     messageId,
                     SmsManager.CELL_BROADCAST_RAN_TYPE_GSM);
             }

             mEmergencyCBMessage = new IntentFilter(EMERGENCY_CB_MESSAGE_RECEIVED_ACTION);
             mService.registerReceiver(mGsmEmergencyCBMessageListener,
                                       mEmergencyCBMessage);
             mGsmEmergencyCBListenerRegistered = true;
        }
    }

    @Rpc(description = "Stop tracking GSM Emergency CB Messages")
    public void smsStopTrackingGsmEmergencyCBMessage() {
        if(mGsmEmergencyCBListenerRegistered) {
            mService.unregisterReceiver(mGsmEmergencyCBMessageListener);
            mGsmEmergencyCBListenerRegistered = false;
            for (int messageId : mGsmCbMessageIdList) {
                mSms.disableCellBroadcast(
                     messageId,
                     SmsManager.CELL_BROADCAST_RAN_TYPE_GSM);
            }
        }
    }

    @Rpc(description = "Starts tracking CDMA Emergency CB Messages")
    public void smsStartTrackingCdmaEmergencyCBMessage() {
        if(!mCdmaEmergencyCBListenerRegistered) {
            for (int messageId : mCdmaCbMessageIdList) {
                mSms.enableCellBroadcast(
                     messageId,
                     SmsManager.CELL_BROADCAST_RAN_TYPE_CDMA);
            }
            mEmergencyCBMessage = new IntentFilter(EMERGENCY_CB_MESSAGE_RECEIVED_ACTION);
            mService.registerReceiver(mCdmaEmergencyCBMessageListener,
                                      mEmergencyCBMessage);
            mCdmaEmergencyCBListenerRegistered = true;
        }
    }

    @Rpc(description = "Stop tracking CDMA Emergency CB Message.")
    public void smsStopTrackingCdmaEmergencyCBMessage() {
        if(mCdmaEmergencyCBListenerRegistered) {
            mService.unregisterReceiver(mCdmaEmergencyCBMessageListener);
            mCdmaEmergencyCBListenerRegistered = false;
            for (int messageId : mCdmaCbMessageIdList) {
                mSms.disableCellBroadcast(
                     messageId,
                     SmsManager.CELL_BROADCAST_RAN_TYPE_CDMA);
            }
        }
    }

    private class SmsSendListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle event = new Bundle();
            event.putString("Type", "SmsDeliverStatus");
            String action = intent.getAction();
            int resultCode = getResultCode();
            if (MESSAGE_STATUS_DELIVERED_ACTION.equals(action)) {
                if (resultCode == Activity.RESULT_OK) {
                    mNumReceivedDeliveredEvents++;
                    if(mNumReceivedDeliveredEvents == mNumExpectedDeliveredEvents ) {
                        Log.d("SMS Message delivered successfully");
                        mEventFacade.postEvent("onSmsDeliverSuccess", event);
                        mNumReceivedDeliveredEvents = 0;
                    }
                } else {
                    Log.e("SMS Message delivery failed");
                    // TODO . Need to find the reason for failure from pdu
                    mEventFacade.postEvent("onSmsDeliverFailure", event);
                }
            } else if (MESSAGE_SENT_ACTION.equals(action)) {
                if (resultCode == Activity.RESULT_OK) {
                    mNumReceivedSentEvents++;
                    if(mNumReceivedSentEvents == mNumExpectedSentEvents ) {
                        event.putString("Type", "SmsSentSuccess");
                        Log.d("SMS Message sent successfully");
                       mEventFacade.postEvent("onSmsSentSuccess", event);
                       mNumReceivedSentEvents = 0;
                    }
                } else {
                    Log.e("SMS Message send failed");
                    event.putString("Type", "SmsSentFailure");
                    switch(resultCode) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            event.putString("Reason", "GenericFailure");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF :
                            event.putString("Reason", "RadioOff");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            event.putString("Reason", "NullPdu");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE :
                            event.putString("Reason", "NoService");
                            break;
                        case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED  :
                            event.putString("Reason", "LimitExceeded");
                            break;
                        case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE :
                            event.putString("Reason", "FdnCheckFailure");
                            break;
                        default:
                            event.putString("Reason", "Unknown");
                            break;
                    }
                    mEventFacade.postEvent("onSmsSentFailure", event);
                }
            }
        }
    }

    private class SmsIncomingListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MESSAGE_RECEIVED_ACTION.equals(action)) {
                Log.d("New SMS Received");
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Bundle event = new Bundle();
                    event.putString("Type", "NewSmsReceived");
                    SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                    StringBuilder smsMsg = new StringBuilder();

                    SmsMessage sms = msgs[0];
                    String sender = sms.getOriginatingAddress();
                    event.putString("Sender", formatPhoneNumber(sender));

                    for (int i = 0; i < msgs.length; i++) {
                        sms = msgs[i];
                        smsMsg.append(sms.getMessageBody());
                    }
                    event.putString("Text", smsMsg.toString());
                    mEventFacade.postEvent("onSmsReceived", event);
                }
            }
        }
    }

    String formatPhoneNumber(String phoneNumber) {
        String senderNumberStr = null;
        int len = phoneNumber.length();
        if (len > 0) {
            /**
             * Currently this incomingNumber modification is specific for US numbers.
             */
            if ((INTERNATIONAL_NUMBER_LENGTH == len) && ('+' == phoneNumber.charAt(0))) {
                senderNumberStr = phoneNumber.substring(1);
            } else if (DOMESTIC_NUMBER_LENGTH == len) {
                senderNumberStr = '1' + phoneNumber;
            } else {
                senderNumberStr = phoneNumber;
            }
        }
        return senderNumberStr;
    }

    private class SmsEmergencyCBMessageListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (EMERGENCY_CB_MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Bundle event = new Bundle();
                    String eventName = null;
                    SmsCbMessage message = (SmsCbMessage) extras.get("message");
                    if(message != null) {
                        if(message.isEmergencyMessage()) {
                            event.putString("geographicalScope", getGeographicalScope(
                                             message.getGeographicalScope()));
                            event.putInt("serialNumber", message.getSerialNumber());
                            event.putString("location", message.getLocation().toString());
                            event.putInt("serviceCategory", message.getServiceCategory());
                            event.putString("language", message.getLanguageCode());
                            event.putString("message", message.getMessageBody());
                            event.putString("priority", getPriority(message.getMessagePriority()));
                            if (message.isCmasMessage()) {
                                // CMAS message
                                eventName = "onCMASReceived";
                                event.putString("cmasMessageClass", getCMASMessageClass(
                                                 message.getCmasWarningInfo().getMessageClass()));
                                event.putString("cmasCategory", getCMASCategory(
                                                 message.getCmasWarningInfo().getCategory()));
                                event.putString("cmasResponseType", getCMASResponseType(
                                                 message.getCmasWarningInfo().getResponseType()));
                                event.putString("cmasSeverity", getCMASSeverity(
                                                 message.getCmasWarningInfo().getSeverity()));
                                event.putString("cmasUrgency", getCMASUrgency(
                                                 message.getCmasWarningInfo().getUrgency()));
                                event.putString("cmasCertainty", getCMASCertainty(
                                                message.getCmasWarningInfo().getCertainty()));
                            } else if (message.isEtwsMessage()) {
                                // ETWS message
                                eventName = "onETWSReceived";
                                event.putString("etwsWarningType",getETWSWarningType(
                                                 message.getEtwsWarningInfo().getWarningType()));
                                event.putBoolean("etwsIsEmergencyUserAlert",
                                                  message.getEtwsWarningInfo().isEmergencyUserAlert());
                                event.putBoolean("etwsActivatePopup",
                                                  message.getEtwsWarningInfo().isPopupAlert());
                            } else {
                                Log.d("Received message is not CMAS or ETWS");
                            }
                            if(eventName != null)
                                mEventFacade.postEvent(eventName, event);
                        }
                    }
                } else {
                    Log.d("Received  Emergency CB without extras");
                }
            }
        }
    }

    private static String getETWSWarningType(int type) {
        switch(type) {
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE:
                return"EARTHQUAKE";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TSUNAMI:
                return "TSUNAMI";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI:
                return "EARTHQUAKE_AND_TSUNAMI";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TEST_MESSAGE:
                return "TEST_MESSAGE";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_OTHER_EMERGENCY:
               return "OTHER_EMERGENCY";
       }
       return "UNKNOWN";
    }

    private static String getCMASMessageClass(int messageclass) {
        switch(messageclass) {
            case SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT:
                return "PRESIDENTIAL_LEVEL_ALERT";
            case SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT:
                return "EXTREME_THREAT";
            case SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT:
                return "SEVERE_THREAT";
            case SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY :
                return "CHILD_ABDUCTION_EMERGENCY";
            case SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST:
                return "REQUIRED_MONTHLY_TEST";
            case SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE :
                return "CMAS_EXERCISE";
       }
       return "UNKNOWN";
    }

    private static String getCMASCategory(int category) {
        switch(category) {
            case SmsCbCmasInfo.CMAS_CATEGORY_GEO:
                return "GEOPHYSICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_MET:
                return "METEOROLOGICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_SAFETY:
                return "SAFETY";
            case SmsCbCmasInfo.CMAS_CATEGORY_SECURITY:
                return "SECURITY";
            case SmsCbCmasInfo.CMAS_CATEGORY_RESCUE:
                return "RESCUE";
            case SmsCbCmasInfo.CMAS_CATEGORY_FIRE:
                return "FIRE";
            case SmsCbCmasInfo.CMAS_CATEGORY_HEALTH:
                return "HEALTH";
            case SmsCbCmasInfo.CMAS_CATEGORY_ENV:
                return "ENVIRONMENTAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_TRANSPORT:
                return "TRANSPORTATION";
            case SmsCbCmasInfo.CMAS_CATEGORY_INFRA:
                return "INFRASTRUCTURE";
            case SmsCbCmasInfo.CMAS_CATEGORY_CBRNE:
                return "CHEMICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_OTHER:
                return "OTHER";
       }
       return "UNKNOWN";
    }

    private static String getCMASResponseType(int type) {
        switch(type) {
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_SHELTER:
                return "SHELTER";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EVACUATE:
                return "EVACUATE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_PREPARE:
                return "PREPARE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EXECUTE:
                return "EXECUTE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_MONITOR:
                return "MONITOR";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_AVOID:
                return "AVOID";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_ASSESS:
                return "ASSESS";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_NONE:
                return "NONE";
       }
       return "UNKNOWN";
    }

    private static String getCMASSeverity(int severity) {
        switch(severity) {
            case SmsCbCmasInfo.CMAS_SEVERITY_EXTREME:
                return "EXTREME";
            case SmsCbCmasInfo.CMAS_SEVERITY_SEVERE:
                return "SEVERE";
       }
       return "UNKNOWN";
    }

    private static String getCMASUrgency(int urgency) {
        switch(urgency) {
            case SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE:
                return "IMMEDIATE";
            case SmsCbCmasInfo.CMAS_URGENCY_EXPECTED:
                return "EXPECTED";
       }
       return "UNKNOWN";
    }

    private static String getCMASCertainty(int certainty) {
        switch(certainty) {
            case SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED:
                return "IMMEDIATE";
            case SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY:
                return "LIKELY";
       }
       return "UNKNOWN";
    }

    private static String getGeographicalScope(int scope) {
        switch(scope) {
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE:
                return "CELL_WIDE_IMMEDIATE";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_PLMN_WIDE:
                return "PLMN_WIDE ";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_LA_WIDE :
                return "LA_WIDE";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE:
                return "CELL_WIDE";
       }
       return "UNKNOWN";
    }

    private static String getPriority(int priority) {
        switch(priority) {
            case SmsCbMessage.MESSAGE_PRIORITY_NORMAL:
                return "NORMAL";
            case SmsCbMessage.MESSAGE_PRIORITY_INTERACTIVE:
                return "INTERACTIVE";
            case SmsCbMessage.MESSAGE_PRIORITY_URGENT:
                return "URGENT";
            case SmsCbMessage.MESSAGE_PRIORITY_EMERGENCY:
                return "EMERGENCY";
       }
       return "UNKNOWN";
    }

    @Override
    public void shutdown() {
      mService.unregisterReceiver(mSmsSendListener);
      if(mListeningIncomingSms) {
          smsStopTrackingIncomingMessage();
      }
      smsStopTrackingGsmEmergencyCBMessage();
      smsStopTrackingCdmaEmergencyCBMessage();
    }
}
