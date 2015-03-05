
package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.util.Base64;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WifiManager functions.
 */
// TODO: make methods handle various wifi states properly
// e.g. wifi connection result will be null when flight mode is on
public class WifiManagerFacade extends RpcReceiver {
    private final static String mEventType = "WifiManager";
    private final Service mService;
    private final WifiManager mWifi;
    private final EventFacade mEventFacade;

    private final IntentFilter mScanFilter;
    private final IntentFilter mStateChangeFilter;
    private final IntentFilter mTetherFilter;
    private final WifiScanReceiver mScanResultsAvailableReceiver;
    private final WifiStateChangeReceiver mStateChangeReceiver;
    private boolean mTrackingWifiStateChange;

    private final BroadcastReceiver mTetherStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                Log.d("Wifi AP state changed.");
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE,
                        WifiManager.WIFI_AP_STATE_FAILED);
                if (state == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mEventFacade.postEvent("WifiManagerApEnabled", null);
                } else if (state == WifiManager.WIFI_AP_STATE_DISABLED) {
                    mEventFacade.postEvent("WifiManagerApDisabled", null);
                }
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                Log.d("Tether state changed.");
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                Bundle msg = new Bundle();
                msg.putStringArrayList("AVAILABLE_TETHER", available);
                msg.putStringArrayList("ACTIVE_TETHER", active);
                msg.putStringArrayList("ERRORED_TETHER", errored);
                mEventFacade.postEvent("TetherStateChanged", msg);
            }
        }
    };

    private WifiLock mLock = null;
    private boolean mIsConnected = false;

    public WifiManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mWifi = (WifiManager) mService.getSystemService(Context.WIFI_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);

        mScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mStateChangeFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mStateChangeFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);

        mTetherFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mTetherFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);

        mScanResultsAvailableReceiver = new WifiScanReceiver(mEventFacade);
        mStateChangeReceiver = new WifiStateChangeReceiver();
        mTrackingWifiStateChange = false;
    }

    private void makeLock(int wifiMode) {
        if (mLock == null) {
            mLock = mWifi.createWifiLock(wifiMode, "sl4a");
            mLock.acquire();
        }
    }

    /**
     * Handle Broadcast receiver for Scan Result
     *
     * @parm eventFacade Object of EventFacade
     */
    class WifiScanReceiver extends BroadcastReceiver {
        private final EventFacade mEventFacade;
        private final Bundle mResults;

        WifiScanReceiver(EventFacade eventFacade) {
            mEventFacade = eventFacade;
            mResults = new Bundle();
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d("Wifi connection scan finished, results available.");
                mResults.putLong("Timestamp", System.currentTimeMillis() / 1000);
                mEventFacade.postEvent(mEventType + "ScanResultsAvailable", mResults);
                mResults.clear();
                mService.unregisterReceiver(mScanResultsAvailableReceiver);
            }
        }
    }

    class WifiActionListener implements WifiManager.ActionListener {
        private final EventFacade mEventFacade;
        private final String TAG;

        public WifiActionListener(EventFacade eventFacade, String tag) {
            mEventFacade = eventFacade;
            this.TAG = tag;
        }

        @Override
        public void onSuccess() {
            mEventFacade.postEvent(mEventType + TAG + "OnSuccess", null);
        }

        @Override
        public void onFailure(int reason) {
            Log.d("WifiActionListener  " + mEventType);
            Bundle msg = new Bundle();
            msg.putInt("reason", reason);
            mEventFacade.postEvent(mEventType + TAG + "OnFailure", msg);
        }
    }

    public class WifiStateChangeReceiver extends BroadcastReceiver {
        String mCachedWifiInfo = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.d("Wifi network state changed.");
                NetworkInfo nInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                WifiInfo wInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                Log.d("NetworkInfo " + nInfo);
                Log.d("WifiInfo " + wInfo);
                // If network info is of type wifi, send wifi events.
                if (nInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (wInfo != null && nInfo.getDetailedState().equals(DetailedState.CONNECTED)) {
                        Bundle msg = new Bundle();
                        String ssid = wInfo.getSSID();
                        if (ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"') {
                            msg.putString("SSID", ssid.substring(1, ssid.length() - 1));
                        } else {
                            msg.putString("SSID", ssid);
                        }
                        String bssid = wInfo.getBSSID();
                        msg.putString("bssid", bssid);
                        if (bssid != null && !mCachedWifiInfo.equals(wInfo.toString())) {
                            Log.d("WifiNetworkConnected");
                            mEventFacade.postEvent("WifiNetworkConnected", msg);
                        }
                        mCachedWifiInfo = wInfo.toString();
                    } else {
                        if (nInfo.getDetailedState().equals(DetailedState.DISCONNECTED)) {
                            if (!mCachedWifiInfo.equals("")) {
                                mCachedWifiInfo = "";
                                mEventFacade.postEvent("WifiNetworkDisconnected", null);
                            }
                        }
                    }
                }
            } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                Log.d("Supplicant connection state changed.");
                mIsConnected = intent
                        .getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
                Bundle msg = new Bundle();
                msg.putBoolean("Connected", mIsConnected);
                mEventFacade.postEvent("SupplicantConnectionChanged", msg);
            }
        }
    }

    public class WifiWpsCallback extends WifiManager.WpsCallback {
        private static final String tag = "WifiWps";

        @Override
        public void onStarted(String pin) {
            Bundle msg = new Bundle();
            msg.putString("pin", pin);
            mEventFacade.postEvent(tag + "OnStarted", msg);
        }

        @Override
        public void onSucceeded() {
            Log.d("Wps op succeeded");
            mEventFacade.postEvent(tag + "OnSucceeded", null);
        }

        @Override
        public void onFailed(int reason) {
            Bundle msg = new Bundle();
            msg.putInt("reason", reason);
            mEventFacade.postEvent(tag + "OnFailed", msg);
        }
    }

    private void applyingkeyMgmt(WifiConfiguration config, ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            config.allowedKeyManagement.set(KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
        } else if (result.capabilities.contains("PSK")) {
            config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
        } else if (result.capabilities.contains("EAP")) {
            // this is probably wrong, as we don't have a way to enter the enterprise config
            config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
            config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
    }

    private WifiConfiguration genEnterpriseConfig(String configStr) throws JSONException,
            GeneralSecurityException {
        if (configStr == null) {
            return null;
        }
        JSONObject j = new JSONObject(configStr);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
        config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
        if (j.has("SSID")) {
            config.SSID = j.getString("SSID");
        }
        WifiEnterpriseConfig eConfig = new WifiEnterpriseConfig();
        if (j.has(WifiEnterpriseConfig.EAP_KEY)) {
            int eap = j.getInt(WifiEnterpriseConfig.EAP_KEY);
            eConfig.setEapMethod(eap);
        }
        if (j.has(WifiEnterpriseConfig.PHASE2_KEY)) {
            int p2Method = j.getInt(WifiEnterpriseConfig.PHASE2_KEY);
            eConfig.setPhase2Method(p2Method);
        }
        if (j.has(WifiEnterpriseConfig.CA_CERT_KEY)) {
            String certStr = j.getString(WifiEnterpriseConfig.CA_CERT_KEY);
            Log.v("CA Cert String is " + certStr);
            eConfig.setCaCertificate(strToX509Cert(certStr));
        }
        if (j.has(WifiEnterpriseConfig.CLIENT_CERT_KEY)
                && j.has(WifiEnterpriseConfig.PRIVATE_KEY_ID_KEY)) {
            String certStr = j.getString(WifiEnterpriseConfig.CLIENT_CERT_KEY);
            String keyStr = j.getString(WifiEnterpriseConfig.PRIVATE_KEY_ID_KEY);
            Log.v("Client Cert String is " + certStr);
            Log.v("Client Key String is " + keyStr);
            X509Certificate cert = strToX509Cert(certStr);
            PrivateKey privKey = strToPrivateKey(keyStr);
            Log.v("Cert is " + cert);
            Log.v("Private Key is " + privKey);
            eConfig.setClientKeyEntry(privKey, cert);
        }
        if (j.has(WifiEnterpriseConfig.IDENTITY_KEY)) {
            String identity = j.getString(WifiEnterpriseConfig.IDENTITY_KEY);
            Log.v("Setting identity to " + identity);
            eConfig.setIdentity(identity);
        }
        if (j.has(WifiEnterpriseConfig.PASSWORD_KEY)) {
            String pwd = j.getString(WifiEnterpriseConfig.PASSWORD_KEY);
            Log.v("Setting password to " + pwd);
            eConfig.setPassword(pwd);
        }
        if (j.has(WifiEnterpriseConfig.ALTSUBJECT_MATCH_KEY)) {
            String altSub = j.getString(WifiEnterpriseConfig.ALTSUBJECT_MATCH_KEY);
            Log.v("Setting Alt Subject to " + altSub);
            eConfig.setAltSubjectMatch(altSub);
        }
        config.enterpriseConfig = eConfig;
        return config;
    }

    private WifiConfiguration genWifiConfig(String SSID, String pwd) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + SSID + "\"";
        if (pwd == null) {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfig.preSharedKey = "\"" + pwd + "\"";
        }
        return wifiConfig;
    }

    private boolean matchScanResult(ScanResult result, String id) {
        if (result.BSSID.equals(id) || result.SSID.equals(id)) {
            return true;
        }
        return false;
    }

    private WifiConfiguration parseWifiApConfig(String configStr) throws JSONException {
        if (configStr == null) {
            return null;
        }
        JSONObject j = new JSONObject(configStr);
        WifiConfiguration config = new WifiConfiguration();
        if (j.has("SSID")) {
            config.SSID = j.getString("SSID");
        } else {
            config.SSID = "AndroidAP";
        }
        if (j.has("PASSWORD")) {
            config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
            config.preSharedKey = j.getString("PASSWORD");
        } else {
            config.allowedKeyManagement.set(KeyMgmt.NONE);
        }
        return config;
    }

    private WpsInfo parseWpsInfo(String infoStr) throws JSONException {
        if (infoStr == null) {
            return null;
        }
        JSONObject j = new JSONObject(infoStr);
        WpsInfo info = new WpsInfo();
        if (j.has("setup")) {
            info.setup = j.getInt("setup");
        }
        if (j.has("BSSID")) {
            info.BSSID = j.getString("BSSID");
        }
        if (j.has("pin")) {
            info.pin = j.getString("pin");
        }
        return info;
    }

    private byte[] base64StrToBytes(String input) {
        return Base64.decode(input, Base64.DEFAULT);
    }

    private X509Certificate strToX509Cert(String certStr) throws CertificateException {
        byte[] certBytes = base64StrToBytes(certStr);
        InputStream certStream = new ByteArrayInputStream(certBytes);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return (X509Certificate) cf.generateCertificate(certStream);
    }

    private PrivateKey strToPrivateKey(String key) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte[] keyBytes = base64StrToBytes(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PrivateKey priv = fact.generatePrivate(keySpec);
        return priv;
    }

    private PublicKey strToPublicKey(String key) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte[] keyBytes = base64StrToBytes(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PublicKey pub = fact.generatePublic(keySpec);
        return pub;
    }

    private WifiConfiguration wifiConfigurationFromScanResult(ScanResult result) {
        if (result == null)
            return null;
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + result.SSID + "\"";
        applyingkeyMgmt(config, result);
        config.BSSID = result.BSSID;
        config.scanResultCache = new HashMap<String, ScanResult>();
        if (config.scanResultCache == null)
            return null;
        config.scanResultCache.put(result.BSSID, result);
        return config;
    }

    @Rpc(description = "Test for base64 string transfer.")
    public void wifiTest(String base64Str) throws JSONException, UnsupportedEncodingException {
        Log.d(new String(Base64.decode(base64Str, Base64.DEFAULT), "UTF-8"));
    }

    @Rpc(description = "Add a network.")
    public Integer wifiAddNetwork(@RpcParameter(name = "wifiId") String wifiId) {
        ScanResult target = null;
        for (ScanResult r : mWifi.getScanResults()) {
            if (matchScanResult(r, wifiId)) {
                target = r;
                break;
            }
        }
        return mWifi.addNetwork(wifiConfigurationFromScanResult(target));
    }

    @Rpc(description = "Cancel Wi-fi Protected Setup.")
    public void wifiCancelWps() throws JSONException {
        WifiWpsCallback listener = new WifiWpsCallback();
        mWifi.cancelWps(listener);
    }

    @Rpc(description = "Checks Wifi state.", returns = "True if Wifi is enabled.")
    public Boolean wifiCheckState() {
        return mWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }

    /**
     * Connects to a WPA protected wifi network
     *
     * @param wifiSSID SSID of the wifi network
     * @param wifiPassword password for the wifi network
     * @return true on success
     * @throws ConnectException
     */
    @Rpc(description = "Connects a wifi network by ssid",
            returns = "True if the operation succeeded.")
    public Boolean wifiConnect(
            @RpcParameter(name = "SSID") String SSID,
            @RpcParameter(name = "Password") @RpcOptional @RpcDefault(value = "") String Password)
            throws ConnectException {
        WifiConfiguration wifiConfig = genWifiConfig(SSID, Password);
        mWifi.addNetwork(wifiConfig);
        Boolean status = false;
        List<WifiConfiguration> configuredNetworks = mWifi.getConfiguredNetworks();
        if (configuredNetworks == null) {
            Log.d("No configured network available.");
            return false;
        }
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.SSID != null && conf.SSID.equals("\"" + SSID + "\"")) {
                mWifi.disconnect();
                mWifi.enableNetwork(conf.networkId, true);
                status = mWifi.reconnect();
                break;
            }
        }
        return status;
    }

    @Rpc(description = "Disconnects from the currently active access point.",
            returns = "True if the operation succeeded.")
    public Boolean wifiDisconnect() {
        return mWifi.disconnect();
    }

    @Rpc(description = "Enable a configured network. Initiate a connection if disableOthers is true",
            returns = "True if the operation succeeded.")
    public Boolean wifiEnableNetwork(@RpcParameter(name = "netId") Integer netId,
            @RpcParameter(name = "disableOthers") Boolean disableOthers) {
        return mWifi.enableNetwork(netId, disableOthers);
    }

    @Rpc(description = "Connect to a wifi network that uses Enterprise authentication methods.")
    public void wifiEnterpriseConnect(@RpcParameter(name = "configStr") String configStr)
            throws JSONException, GeneralSecurityException {
        // Create Certificate
        WifiActionListener listener = new WifiActionListener(mEventFacade, "EnterpriseConnect");
        WifiConfiguration config = this.genEnterpriseConfig(configStr);
        mWifi.connect(config, listener);
    }

    /**
     * Forget a wifi network with priority
     *
     * @param networkID Id of wifi network
     */
    @Rpc(description = "Forget a wifi network with priority")
    public void wifiForgetNetwork(@RpcParameter(name = "wifiSSID") Integer newtorkId) {
        WifiActionListener listener = new WifiActionListener(mEventFacade, "ForgetNetwork");
        mWifi.forget(newtorkId, listener);
    }

    @Rpc(description = "Return a list of all the configured wifi networks.")
    public List<WifiConfiguration> wifiGetConfiguredNetworks() {
        return mWifi.getConfiguredNetworks();
    }

    @Rpc(description = "Returns information about the currently active access point.")
    public WifiInfo wifiGetConnectionInfo() {
        return mWifi.getConnectionInfo();
    }

    @Rpc(description = "Returns wifi activity and energy usage info.")
    public WifiActivityEnergyInfo getControllerActivityEnergyInfo() {
        return mWifi.getControllerActivityEnergyInfo(0);
    }

    @Rpc(description = "Returns the list of access points found during the most recent Wifi scan.")
    public List<ScanResult> wifiGetScanResults() {
        return mWifi.getScanResults();
    }

    @Rpc(description = "Gets the Wi-Fi AP Configuration.")
    public WifiConfiguration wifiGetApConfiguration() {
        return mWifi.getWifiApConfiguration();
    }

    @Rpc(description = "Return whether Wi-Fi AP is enabled or disabled.")
    public Boolean wifiIsApEnabled() {
        return mWifi.isWifiApEnabled();
    }

    @Rpc(description = "Check if this adapter supports advanced power/performance counters.")
    public Boolean wifiIsEnhancedPowerReportingSupported() {
        return mWifi.isEnhancedPowerReportingSupported();
    }

    @Rpc(description = "Check if wifi scanner is supported on this device.")
    public Boolean wifiIsScannerSupported() {
        return mWifi.isWifiScannerSupported();
    }

    @Rpc(description = "Acquires a full Wifi lock.")
    public void wifiLockAcquireFull() {
        makeLock(WifiManager.WIFI_MODE_FULL);
    }

    @Rpc(description = "Acquires a scan only Wifi lock.")
    public void wifiLockAcquireScanOnly() {
        makeLock(WifiManager.WIFI_MODE_SCAN_ONLY);
    }

    @Rpc(description = "Releases a previously acquired Wifi lock.")
    public void wifiLockRelease() {
        if (mLock != null) {
            mLock.release();
            mLock = null;
        }
    }

    /**
     * Connects to a wifi network with priority
     *
     * @param wifiSSID SSID of the wifi network
     * @param wifiPassword password for the wifi network
     */
    @Rpc(description = "Connects a wifi network as priority by pasing ssid")
    public void wifiPriorityConnect(@RpcParameter(name = "SSID") String SSID,
            @RpcParameter(name = "Password") @RpcOptional @RpcDefault(value = "") String Password) {
        WifiConfiguration wifiConfig = genWifiConfig(SSID, Password);
        WifiActionListener listener = new WifiActionListener(mEventFacade, "PriorityConnect");
        mWifi.connect(wifiConfig, listener);
    }

    @Rpc(description = "Reassociates with the currently active access point.",
            returns = "True if the operation succeeded.")
    public Boolean wifiReassociate() {
        return mWifi.reassociate();
    }

    @Rpc(description = "Reconnects to the currently active access point.",
            returns = "True if the operation succeeded.")
    public Boolean wifiReconnect() {
        return mWifi.reconnect();
    }

    @Rpc(description = "Remove a configured network.",
            returns = "True if the operation succeeded.")
    public Boolean wifiRemoveNetwork(@RpcParameter(name = "netId") Integer netId) {
        return mWifi.removeNetwork(netId);
    }

    @Rpc(description = "Start/stop wifi tethering.")
    public Boolean wifiSetApEnabled(
            @RpcParameter(name = "configStr") @RpcOptional @RpcDefault(value = "{}") String configStr,
            @RpcParameter(name = "enable") Boolean enable
            ) throws JSONException {
        int wifiState = mWifi.getWifiState();
        if (enable) {
            if ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
                mWifi.setWifiEnabled(false);
            }
            WifiConfiguration config = parseWifiApConfig(configStr);
            return mWifi.setWifiApEnabled(config, enable);
        } else {
            return mWifi.setWifiApEnabled(null, enable);
        }
    }

    @Rpc(description = "Starts a scan for Wifi access points.",
            returns = "True if the scan was initiated successfully.")
    public Boolean wifiStartScan() {
        mService.registerReceiver(mScanResultsAvailableReceiver, mScanFilter);
        return mWifi.startScan();
    }

    @Rpc(description = "Start Wi-fi Protected Setup.")
    public void wifiStartWps(
            @RpcParameter(name = "config",
                    description = "A json string with fields \"setup\", \"BSSID\", and \"pin\"") String config)
            throws JSONException {
        WpsInfo info = parseWpsInfo(config);
        WifiWpsCallback listener = new WifiWpsCallback();
        Log.d("Starting wps with: " + info);
        mWifi.startWps(info, listener);
    }

    @Rpc(description = "Start listening for wifi state change related broadcasts.")
    public void wifiStartTrackingStateChange() {
        mService.registerReceiver(mStateChangeReceiver, mStateChangeFilter);
        mService.registerReceiver(mTetherStateReceiver, mTetherFilter);
        mTrackingWifiStateChange = true;
    }

    @Rpc(description = "Stop listening for wifi state change related broadcasts.")
    public void wifiStopTrackingStateChange() {
        if (mTrackingWifiStateChange == true) {
            mService.unregisterReceiver(mTetherStateReceiver);
            mService.unregisterReceiver(mStateChangeReceiver);
            mTrackingWifiStateChange = false;
        }
    }

    @Rpc(description = "Toggle Wifi on and off.", returns = "True if Wifi is enabled.")
    public Boolean wifiToggleState(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !wifiCheckState();
        }
        mWifi.setWifiEnabled(enabled);
        return enabled;
    }

    @Rpc(description = "Toggle Wifi scan always available on and off.",
            returns = "True if Wifi scan is always available.")
    public Boolean wifiToggleScanAlwaysAvailable(
            @RpcParameter(name = "enabled") @RpcOptional Boolean enabled)
                    throws SettingNotFoundException {
        ContentResolver cr = mService.getContentResolver();
        int isSet = 0;
        if (enabled == null) {
            isSet = Global.getInt(cr, Global.WIFI_SCAN_ALWAYS_AVAILABLE);
            isSet ^= 1;
        } else if (enabled == true) {
            isSet = 1;
        }
        Global.putInt(cr, Global.WIFI_SCAN_ALWAYS_AVAILABLE, isSet);
        if (isSet == 1) {
            return true;
        }
        return false;
    }

    @Override
    public void shutdown() {
        wifiLockRelease();
        if (mTrackingWifiStateChange == true) {
            wifiStopTrackingStateChange();
        }
    }
}
