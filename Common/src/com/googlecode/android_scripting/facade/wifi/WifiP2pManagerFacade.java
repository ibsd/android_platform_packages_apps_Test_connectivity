
package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WifiP2pManager functions.
 */
public class WifiP2pManagerFacade extends RpcReceiver {

    class WifiP2pPeerListListener implements WifiP2pManager.PeerListListener {
        private final EventFacade mEventFacade;

        public WifiP2pPeerListListener(EventFacade eventFacade) {
            mEventFacade = eventFacade;
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList newPeers) {
            Collection<WifiP2pDevice> devices = newPeers.getDeviceList();
            Log.d(devices.toString());
            if (devices.size() > 0) {
                mP2pPeers.clear();
                mP2pPeers.addAll(devices);
                Bundle msg = new Bundle();
                msg.putParcelableList("Peers", mP2pPeers);
                mEventFacade.postEvent(mEventType + "OnPeersAvailable", msg);
            }
        }
    }

    class WifiP2pActionListener implements WifiP2pManager.ActionListener {
        private final EventFacade mEventFacade;
        private final String mEventType;
        private final String TAG;

        public WifiP2pActionListener(EventFacade eventFacade, String tag) {
            mEventType = "WifiP2pManager";
            mEventFacade = eventFacade;
            TAG = tag;
        }

        @Override
        public void onFailure(int reason) {
            Log.d("WifiActionListener  " + mEventType);
            Bundle msg = new Bundle();
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                msg.putString("reason", "P2P_UNSUPPORTED");
            } else if (reason == WifiP2pManager.ERROR) {
                msg.putString("reason", "ERROR");
            } else if (reason == WifiP2pManager.BUSY) {
                msg.putString("reason", "BUSY");
            } else if (reason == WifiP2pManager.NO_SERVICE_REQUESTS) {
                msg.putString("reason", "NO_SERVICE_REQUESTS");
            } else {
                msg.putInt("reason", reason);
            }
            mEventFacade.postEvent(mEventType + TAG + "OnFailure", msg);
        }

        @Override
        public void onSuccess() {
            mEventFacade.postEvent(mEventType + TAG + "OnSuccess", null);
        }
    }

    class WifiP2pStateChangedReceiver extends BroadcastReceiver {
        private final EventFacade mEventFacade;
        private final Bundle mResults;

        WifiP2pStateChangedReceiver(EventFacade eventFacade) {
            mEventFacade = eventFacade;
            mResults = new Bundle();
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                Log.d("Wifi P2p State Changed.");
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
                if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                    Log.d("Disabled");
                    isP2pEnabled = false;
                } else if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("Enabled");
                    isP2pEnabled = true;
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                Log.d("Wifi P2p Peers Changed. Requesting peers.");
                WifiP2pDeviceList peers = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                Log.d(peers.toString());
                wifiP2pRequestPeers();
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                Log.d("Wifi P2p Connection Changed.");
                WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                NetworkInfo networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                if (networkInfo.isConnected()) {
                    Log.d("Wifi P2p Connected.");
                    mResults.putParcelable("P2pInfo", p2pInfo);
                    mResults.putParcelable("Group", group);
                    mEventFacade.postEvent(mEventType + "Connected", mResults);
                    mResults.clear();
                } else {
                    mEventFacade.postEvent(mEventType + "Disconnected", null);
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
                Log.d("Wifi P2p This Device Changed.");
                WifiP2pDevice device = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mResults.putParcelable("Device", device);
                mEventFacade.postEvent(mEventType + "ThisDeviceChanged", mResults);
                mResults.clear();
            } else if (action.equals(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)) {
                Log.d("Wifi P2p Discovery Changed.");
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    Log.d("discovery started.");
                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    Log.d("discovery stoped.");
                }
            }
        }
    }

    private final static String mEventType = "WifiP2pManager";
    private WifiP2pManager.Channel mChannel;
    private final EventFacade mEventFacade;

    private final WifiP2pManager mP2p;
    private final WifiP2pStateChangedReceiver mP2pStateChangedReceiver;

    private final Service mService;

    private final IntentFilter mStateChangeFilter;

    private boolean isP2pEnabled;
    private List<WifiP2pDevice> mP2pPeers = new ArrayList<WifiP2pDevice>();

    public WifiP2pManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mP2p = (WifiP2pManager) mService.getSystemService(Context.WIFI_P2P_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);

        mStateChangeFilter = new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mStateChangeFilter.setPriority(999);

        mP2pStateChangedReceiver = new WifiP2pStateChangedReceiver(mEventFacade);
    }

    @Override
    public void shutdown() {
        mService.unregisterReceiver(mP2pStateChangedReceiver);
    }

    private boolean wifiP2pDeviceMatches(WifiP2pDevice d, String deviceId) {
        return d.deviceName.equals(deviceId) || d.deviceAddress.equals(deviceId);
    }

    @Rpc(description = "Initialize wifi p2p. Must be called before any other p2p functions.")
    public void wifiP2pInitialize() {
        mService.registerReceiver(mP2pStateChangedReceiver, mStateChangeFilter);
        mChannel = mP2p.initialize(mService, mService.getMainLooper(), null);
    }

    @Rpc(description = "Returns true if wifi p2p is enabled, false otherwise.")
    public Boolean wifiP2pIsEnabled() {
        return isP2pEnabled;
    }

    @Rpc(description = "Connects to a discovered wifi p2p device.")
    public void wifiP2pConnect(@RpcParameter(name = "deviceId") String deviceId) {
        for (WifiP2pDevice d : mP2pPeers) {
            if (wifiP2pDeviceMatches(d, deviceId)) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = d.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                mP2p.connect(mChannel, config,
                        new WifiP2pActionListener(mEventFacade, "Connect"));
            }
        }
    }

    @Rpc(description = "Disconnects wifi p2p.")
    public void wifiP2pDisconnect() {
        mP2p.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("removeGroup onSuccess.");
                mEventFacade.postEvent(mEventType + "Disconnected", null);
            }

            @Override
            public void onFailure(int reason) {
                Log.d("removeGroup failed for reason: " + reason);
            }
        });
    }

    @Rpc(description = "Start peers discovery for wifi p2p.")
    public void wifiP2pDiscoverPeers() {
        mP2p.discoverPeers(mChannel, new WifiP2pActionListener(mEventFacade, "DiscoverPeers"));
    }

    @Rpc(description = "Request peers that are discovered for wifi p2p.")
    public void wifiP2pRequestPeers() {
        mP2p.requestPeers(mChannel, new WifiP2pPeerListListener(mEventFacade));
    }

}
