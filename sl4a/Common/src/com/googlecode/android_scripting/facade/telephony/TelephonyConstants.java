/*
 * Copyright (C) 2015 Google Inc.
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

public class TelephonyConstants {
    /**
     * Constant for WiFi Calling WFC mode
     * **/
    public static final String WFC_MODE_WIFI_ONLY = "WIFI_ONLY";
    public static final String WFC_MODE_CELLULAR_PREFERRED = "CELLULAR_PREFERRED";
    public static final String WFC_MODE_WIFI_PREFERRED = "WIFI_PREFERRED";
    public static final String WFC_MODE_DISABLED = "DISABLED";
    public static final String WFC_MODE_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Video Telephony VT state
     * **/
    public static final String VT_STATE_AUDIO_ONLY = "AUDIO_ONLY";
    public static final String VT_STATE_TX_ENABLED = "TX_ENABLED";
    public static final String VT_STATE_RX_ENABLED = "RX_ENABLED";
    public static final String VT_STATE_BIDIRECTIONAL = "BIDIRECTIONAL";
    public static final String VT_STATE_TX_PAUSED = "TX_PAUSED";
    public static final String VT_STATE_RX_PAUSED = "RX_PAUSED";
    public static final String VT_STATE_BIDIRECTIONAL_PAUSED = "BIDIRECTIONAL_PAUSED";
    public static final String VT_STATE_STATE_INVALID = "INVALID";

    /**
     * Constant for Video Telephony Video quality
     * **/
    public static final String VT_VIDEO_QUALITY_DEFAULT = "DEFAULT";
    public static final String VT_VIDEO_QUALITY_UNKNOWN = "UNKNOWN";
    public static final String VT_VIDEO_QUALITY_HIGH = "HIGH";
    public static final String VT_VIDEO_QUALITY_MEDIUM = "MEDIUM";
    public static final String VT_VIDEO_QUALITY_LOW = "LOW";
    public static final String VT_VIDEO_QUALITY_INVALID = "INVALID";

    /**
     * Constant for Call State (for call object)
     * **/
    public static final String CALL_STATE_ACTIVE = "ACTIVE";
    public static final String CALL_STATE_NEW = "NEW";
    public static final String CALL_STATE_DIALING = "DIALING";
    public static final String CALL_STATE_RINGING = "RINGING";
    public static final String CALL_STATE_HOLDING = "HOLDING";
    public static final String CALL_STATE_DISCONNECTED = "DISCONNECTED";
    public static final String CALL_STATE_PRE_DIAL_WAIT = "PRE_DIAL_WAIT";
    public static final String CALL_STATE_CONNECTING = "CONNECTING";
    public static final String CALL_STATE_DISCONNECTING = "DISCONNECTING";
    public static final String CALL_STATE_UNKNOWN = "UNKNOWN";
    public static final String CALL_STATE_INVALID = "INVALID";

    /**
     * Constant for PRECISE Call State (for call object)
     * **/
    public static final String PRECISE_CALL_STATE_ACTIVE = "ACTIVE";
    public static final String PRECISE_CALL_STATE_ALERTING = "ALERTING";
    public static final String PRECISE_CALL_STATE_DIALING = "DIALING";
    public static final String PRECISE_CALL_STATE_INCOMING = "INCOMING";
    public static final String PRECISE_CALL_STATE_HOLDING = "HOLDING";
    public static final String PRECISE_CALL_STATE_DISCONNECTED = "DISCONNECTED";
    public static final String PRECISE_CALL_STATE_WAITING = "WAITING";
    public static final String PRECISE_CALL_STATE_DISCONNECTING = "DISCONNECTING";
    public static final String PRECISE_CALL_STATE_IDLE = "IDLE";
    public static final String PRECISE_CALL_STATE_UNKNOWN = "UNKNOWN";
    public static final String PRECISE_CALL_STATE_INVALID = "INVALID";

    /**
     * Constant for DC POWER STATE
     * **/
    public static final String DC_POWER_STATE_LOW = "LOW";
    public static final String DC_POWER_STATE_HIGH = "HIGH";
    public static final String DC_POWER_STATE_MEDIUM = "MEDIUM";
    public static final String DC_POWER_STATE_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Audio Route
     * **/
    public static final String AUDIO_ROUTE_EARPIECE = "EARPIECE";
    public static final String AUDIO_ROUTE_BLUETOOTH = "BLUETOOTH";
    public static final String AUDIO_ROUTE_SPEAKER = "SPEAKER";
    public static final String AUDIO_ROUTE_WIRED_HEADSET = "WIRED_HEADSET";
    public static final String AUDIO_ROUTE_WIRED_OR_EARPIECE = "WIRED_OR_EARPIECE";

    /**
     * Constant for Call Capability
     * **/
    public static final String CALL_CAPABILITY_HOLD = "HOLD";
    public static final String CALL_CAPABILITY_SUPPORT_HOLD = "SUPPORT_HOLD";
    public static final String CALL_CAPABILITY_MERGE_CONFERENCE = "MERGE_CONFERENCE";
    public static final String CALL_CAPABILITY_SWAP_CONFERENCE = "SWAP_CONFERENCE";
    public static final String CALL_CAPABILITY_UNUSED_1 = "UNUSED_1";
    public static final String CALL_CAPABILITY_RESPOND_VIA_TEXT = "RESPOND_VIA_TEXT";
    public static final String CALL_CAPABILITY_MUTE = "MUTE";
    public static final String CALL_CAPABILITY_MANAGE_CONFERENCE = "MANAGE_CONFERENCE";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_LOCAL_RX = "SUPPORTS_VT_LOCAL_RX";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_LOCAL_TX = "SUPPORTS_VT_LOCAL_TX";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL = "SUPPORTS_VT_LOCAL_BIDIRECTIONAL";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_REMOTE_RX = "SUPPORTS_VT_REMOTE_RX";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_REMOTE_TX = "SUPPORTS_VT_REMOTE_TX";
    public static final String CALL_CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL = "SUPPORTS_VT_REMOTE_BIDIRECTIONAL";
    public static final String CALL_CAPABILITY_SEPARATE_FROM_CONFERENCE = "SEPARATE_FROM_CONFERENCE";
    public static final String CALL_CAPABILITY_DISCONNECT_FROM_CONFERENCE = "DISCONNECT_FROM_CONFERENCE";
    public static final String CALL_CAPABILITY_SPEED_UP_MT_AUDIO = "SPEED_UP_MT_AUDIO";
    public static final String CALL_CAPABILITY_CAN_UPGRADE_TO_VIDEO = "CAN_UPGRADE_TO_VIDEO";
    public static final String CALL_CAPABILITY_CAN_PAUSE_VIDEO = "CAN_PAUSE_VIDEO";
    public static final String CALL_CAPABILITY_UNKOWN = "UNKOWN";

    /**
     * Constant for Call Property
     * **/
    public static final String CALL_PROPERTY_HIGH_DEF_AUDIO = "HIGH_DEF_AUDIO";
    public static final String CALL_PROPERTY_CONFERENCE = "CONFERENCE";
    public static final String CALL_PROPERTY_GENERIC_CONFERENCE = "GENERIC_CONFERENCE";
    public static final String CALL_PROPERTY_WIFI = "WIFI";
    public static final String CALL_PROPERTY_EMERGENCY_CALLBACK_MODE = "EMERGENCY_CALLBACK_MODE";
    public static final String CALL_PROPERTY_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Call Presentation
     * **/
    public static final String CALL_PRESENTATION_ALLOWED = "ALLOWED";
    public static final String CALL_PRESENTATION_RESTRICTED = "RESTRICTED";
    public static final String CALL_PRESENTATION_PAYPHONE = "PAYPHONE";
    public static final String CALL_PRESENTATION_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Network RAT
     * **/
    public static final String RAT_IWLAN = "IWLAN";
    public static final String RAT_LTE = "LTE";
    public static final String RAT_4G = "4G";
    public static final String RAT_3G = "3G";
    public static final String RAT_2G = "2G";
    public static final String RAT_WCDMA = "WCDMA";
    public static final String RAT_UMTS = "UMTS";
    public static final String RAT_1XRTT = "1XRTT";
    public static final String RAT_EDGE = "EDGE";
    public static final String RAT_GPRS = "GPRS";
    public static final String RAT_HSDPA = "HSDPA";
    public static final String RAT_HSUPA = "HSUPA";
    public static final String RAT_CDMA = "CDMA";
    public static final String RAT_EVDO = "EVDO";
    public static final String RAT_EVDO_0 = "EVDO_0";
    public static final String RAT_EVDO_A = "EVDO_A";
    public static final String RAT_EVDO_B = "EVDO_B";
    public static final String RAT_IDEN = "IDEN";
    public static final String RAT_EHRPD = "EHRPD";
    public static final String RAT_HSPA = "HSPA";
    public static final String RAT_HSPAP = "HSPAP";
    public static final String RAT_GSM = "GSM";
    public static final String RAT_TD_SCDMA = "TD_SCDMA";
    public static final String RAT_GLOBAL = "GLOBAL";
    public static final String RAT_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Phone Type
     * **/
    public static final String PHONE_TYPE_GSM = "GSM";
    public static final String PHONE_TYPE_NONE = "NONE";
    public static final String PHONE_TYPE_CDMA = "CDMA";
    public static final String PHONE_TYPE_SIP = "SIP";

    /**
     * Constant for SIM State
     * **/
    public static final String SIM_STATE_READY = "READY";
    public static final String SIM_STATE_UNKNOWN = "UNKNOWN";
    public static final String SIM_STATE_ABSENT = "ABSENT";
    public static final String SIM_STATE_PUK_REQUIRED = "PUK_REQUIRED";
    public static final String SIM_STATE_PIN_REQUIRED = "PIN_REQUIRED";
    public static final String SIM_STATE_NETWORK_LOCKED = "NETWORK_LOCKED";
    public static final String SIM_STATE_NOT_READY = "NOT_READY";
    public static final String SIM_STATE_PERM_DISABLED = "PERM_DISABLED";
    public static final String SIM_STATE_CARD_IO_ERROR = "CARD_IO_ERROR";

    /**
     * Constant for Data Connection State
     * **/
    public static final String DATA_STATE_CONNECTED = "CONNECTED";
    public static final String DATA_STATE_DISCONNECTED = "DISCONNECTED";
    public static final String DATA_STATE_CONNECTING = "CONNECTING";
    public static final String DATA_STATE_SUSPENDED = "SUSPENDED";
    public static final String DATA_STATE_UNKNOWN = "UNKNOWN";

    /**
     * Constant for Telephony Manager Call State
     * **/
    public static final String TELEPHONY_STATE_RINGING = "RINGING";
    public static final String TELEPHONY_STATE_IDLE = "IDLE";
    public static final String TELEPHONY_STATE_OFFHOOK = "OFFHOOK";
    public static final String TELEPHONY_STATE_UNKNOWN = "UNKNOWN";

    /**
     * Constant for TTY Mode
     * **/
    public static final String TTY_MODE_FULL = "FULL";
    public static final String TTY_MODE_HCO = "HCO";
    public static final String TTY_MODE_OFF = "OFF";
    public static final String TTY_MODE_VCO ="VCO";

    /**
     * Constant for Service State
     * **/
    public static final String SERVICE_STATE_EMERGENCY_ONLY = "EMERGENCY_ONLY";
    public static final String SERVICE_STATE_IN_SERVICE = "IN_SERVICE";
    public static final String SERVICE_STATE_OUT_OF_SERVICE = "OUT_OF_SERVICE";
    public static final String SERVICE_STATE_POWER_OFF = "POWER_OFF";
    public static final String SERVICE_STATE_UNKNOWN = "UNKNOWN";

    /**
     * Constant for VoLTE Hand-over Service State
     * **/
    public static final String VOLTE_SERVICE_STATE_HANDOVER_STARTED = "STARTED";
    public static final String VOLTE_SERVICE_STATE_HANDOVER_COMPLETED = "COMPLETED";
    public static final String VOLTE_SERVICE_STATE_HANDOVER_FAILED = "FAILED";
    public static final String VOLTE_SERVICE_STATE_HANDOVER_CANCELED = "CANCELED";
    public static final String VOLTE_SERVICE_STATE_HANDOVER_UNKNOWN = "UNKNOWN";

    /**
     * Constant for precise call state state listen level
     * **/
    public static final String PRECISE_CALL_STATE_LISTEN_LEVEL_FOREGROUND = "FOREGROUND";
    public static final String PRECISE_CALL_STATE_LISTEN_LEVEL_RINGING = "RINGING";
    public static final String PRECISE_CALL_STATE_LISTEN_LEVEL_BACKGROUND = "BACKGROUND";

    /**
     * Constant for Messaging Event Name
     * **/
    public static final String EventSmsDeliverSuccess = "SmsDeliverSuccess";
    public static final String EventSmsDeliverFailure = "SmsDeliverFailure";
    public static final String EventSmsSentSuccess = "SmsSentSuccess";
    public static final String EventSmsSentFailure = "SmsSentFailure";
    public static final String EventSmsReceived = "SmsReceived";
    public static final String EventMmsSentSuccess = "MmsSentSuccess";
    public static final String EventMmsSentFailure = "MmsSentFailure";
    public static final String EventMmsDownloaded = "MmsDownloaded";
    public static final String EventWapPushReceived = "WapPushReceived";
    public static final String EventDataSmsReceived = "DataSmsReceived";
    public static final String EventCmasReceived = "CmasReceived";
    public static final String EventEtwsReceived = "EtwsReceived";

    /**
     * Constant for Telecom Call Event Name
     * **/
    public static final String EventTelecomCallStateChanged = "TelecomCallStateChanged";
    public static final String EventTelecomCallParentChanged = "TelecomCallParentChanged";
    public static final String EventTelecomCallChildrenChanged = "TelecomCallChildrenChanged";
    public static final String EventTelecomCallDetailsChanged = "TelecomCallDetailsChanged";
    public static final String EventTelecomCallCannedTextResponsesLoaded = "TelecomCallCannedTextResponsesLoaded";
    public static final String EventTelecomCallPostDialWait = "TelecomCallPostDialWait";
    public static final String EventTelecomCallVideoCallChanged = "TelecomCallVideoCallChanged";
    public static final String EventTelecomCallDestroyed = "TelecomCallDestroyed";
    public static final String EventTelecomCallConferenceableCallsChanged = "TelecomCallConferenceableCallsChanged";

    /**
     * Constant for Video Call Event Name
     * **/
    public static final String EventTelecomVideoCallSessionModifyRequestReceived = "TelecomVideoCallSessionModifyRequestReceived";
    public static final String EventTelecomVideoCallSessionModifyResponseReceived = "TelecomVideoCallSessionModifyResponseReceived";
    public static final String EventTelecomVideoCallSessionEvent = "TelecomVideoCallSessionEvent";
    public static final String EventTelecomVideoCallPeerDimensionsChanged = "TelecomVideoCallPeerDimensionsChanged";
    public static final String EventTelecomVideoCallVideoQualityChanged = "TelecomVideoCallVideoQualityChanged";
    public static final String EventTelecomVideoCallDataUsageChanged = "TelecomVideoCallDataUsageChanged";
    public static final String EventTelecomVideoCallCameraCapabilities = "TelecomVideoCallCameraCapabilities";

    /**
     * Constant for Video Call Call-Back Event Name
     * **/
    public static final String EventSessionModifyRequestRceived = "SessionModifyRequestRceived";
    public static final String EventSessionModifyResponsetRceived = "SessionModifyResponsetRceived";
    public static final String EventSessionEvent = "SessionEvent";
    public static final String EventPeerDimensionsChanged = "PeerDimensionsChanged";
    public static final String EventVideoQualityChanged = "VideoQualityChanged";
    public static final String EventDataUsageChanged = "DataUsageChanged";
    public static final String EventCameraCapabilitiesChanged = "CameraCapabilitiesChanged";
    public static final String EventInvalid = "Invalid";

    /**
     * Constant for Video Call Session Event Name
     * **/
    public static final String SessionEventRxPause = "SessionEventRxPause";
    public static final String SessionEventRxResume = "SessionEventRxResume";
    public static final String SessionEventTxStart = "SessionEventTxStart";
    public static final String SessionEventTxStop = "SessionEventTxStop";
    public static final String SessionEventCameraFailure = "SessionEventCameraFailure";
    public static final String SessionEventCameraReady = "SessionEventCameraReady";
    public static final String SessionEventUnknown = "SessionEventUnknown";

    /**
     * Constant for Other Event Name
     * **/
    public static final String EventCallStateChanged = "CallStateChanged";
    public static final String EventPreciseStateChanged = "PreciseStateChanged";
    public static final String EventDataConnectionRealTimeInfoChanged = "DataConnectionRealTimeInfoChanged";
    public static final String EventDataConnectionStateChanged = "DataConnectionStateChanged";
    public static final String EventServiceStateChanged = "ServiceStateChanged";
    public static final String EventVolteServiceStateChanged = "VolteServiceStateChanged";
    public static final String EventMessageWaitingIndicatorChanged = "MessageWaitingIndicatorChanged";
    public static final String EventConnectivityChanged = "ConnectivityChanged";

    /**
     * Constant for Packet Keep Alive Call Back
     * **/
    public static final String  PacketKeepaliveCallBack = "PacketKeepliveCallBack";
    public static final String  PacketKeepaliveCallBackStarted = "Started";
    public static final String  PacketKeepaliveCallBackStopped = "Stopped";
    public static final String  PacketKeepaliveCallBackError = "Error";
    public static final String  PacketKeepaliveCallBackInvalid = "Invalid";

    /**
     * Constant for Network Call Back
     * **/
    public static final String  NetworkCallBack = "NetworkCallBack";
    public static final String  NetworkCallBackPreCheck = "PreCheck";
    public static final String  NetworkCallBackAvailable = "Available";
    public static final String  NetworkCallBackLosing = "Losing";
    public static final String  NetworkCallBackLost = "Lost";
    public static final String  NetworkCallBackUnavailable = "Unavailable";
    public static final String  NetworkCallBackCapabilitiesChanged = "CapabilitiesChanged";
    public static final String  NetworkCallBackSuspended = "Suspended";
    public static final String  NetworkCallBackResumed = "Resumed";
    public static final String  NetworkCallBackLinkPropertiesChanged = "LinkPropertiesChanged";
    public static final String  NetworkCallBackInvalid = "Invalid";

    /**
     * Constant for Network Preference
     * **/
    public static final String  NetworkModeWcdmaPref = "NetworkModeWcdmaPref";
    public static final String  NetworkModeGsmOnly = "NetworkModeGsmOnly";
    public static final String  NetworkModeWcdmaOnly = "NetworkModeWcdmaOnly";
    public static final String  NetworkModeGsmUmts = "NetworkModeGsmUmts";
    public static final String  NetworkModeCdma = "NetworkModeCdma";
    public static final String  NetworkModeCdmaNoEvdo = "NetworkModeCdmaNoEvdo";
    public static final String  NetworkModeEvdoNoCdma = "NetworkModeEvdoNoCdma";
    public static final String  NetworkModeGlobal = "NetworkModeGlobal";
    public static final String  NetworkModeLteCdmaEvdo = "NetworkModeLteCdmaEvdo";
    public static final String  NetworkModeLteGsmWcdma = "NetworkModeLteGsmWcdma";
    public static final String  NetworkModeLteCdmaEvdoGsmWcdma = "NetworkModeLteCdmaEvdoGsmWcdma";
    public static final String  NetworkModeLteOnly = "NetworkModeLteOnly";
    public static final String  NetworkModeLteWcdma = "NetworkModeLteWcdma";
    public static final String  NetworkModeTdscdmaOnly = "NetworkModeTdscdmaOnly";
    public static final String  NetworkModeTdscdmaWcdma = "NetworkModeTdscdmaWcdma";
    public static final String  NetworkModeLteTdscdma = "NetworkModeLteTdscdma";
    public static final String  NetworkModeTdsdmaGsm = "NetworkModeTdsdmaGsm";
    public static final String  NetworkModeLteTdscdmaGsm = "NetworkModeLteTdscdmaGsm";
    public static final String  NetworkModeTdscdmaGsmWcdma = "NetworkModeTdscdmaGsmWcdma";
    public static final String  NetworkModeLteTdscdmaWcdma = "NetworkModeLteTdscdmaWcdma";
    public static final String  NetworkModeLteTdscdmaGsmWcdma = "NetworkModeLteTdscdmaGsmWcdma";
    public static final String  NetworkModeTdscdmaCdmaEvdoGsmWcdma = "NetworkModeTdscdmaCdmaEvdoGsmWcdma";
    public static final String  NetworkModeLteTdscdmaCdmaEvdoGsmWcdma = "NetworkModeLteTdscdmaCdmaEvdoGsmWcdma";
    public static final String  NetworkModeInvalid = "Invalid";
}