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

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
//import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.provider.Telephony.Sms.Intents;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;

import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.Log;

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

    private static final String MESSAGE_STATUS_DELIVERED_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_STATUS_DELIVERED";
    private static final String MESSAGE_SENT_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_SENT";
    private static final String MESSAGE_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_RECEIVED";
    private final int MAX_MESSAGE_LENGTH = 160;
    private final int INTERNATIONAL_NUMBER_LENGTH = 12;
    private final int DOMESTIC_NUMBER_LENGTH = 10;

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
    }

    @Rpc(description = "Stops tracking incoming SMS.")
    public void smsStopTrackingIncomingMessage() {
        mService.unregisterReceiver(mSmsIncomingListener);
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
        return SmsManager.getAllMessagesFromIcc();
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
            event.clear();
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
                    event.clear();
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

    @Override
    public void shutdown() {
        smsStopTrackingIncomingMessage();
    }
}
