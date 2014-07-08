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

package com.googlecode.android_scripting.facade.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.AdvertisementData.Builder;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcMinSdk;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;
import com.googlecode.android_scripting.ConvertUtils;

/**
 * BluetoothLe Advertise functions.
 */

@RpcMinSdk(5)
public class BluetoothLeAdvertiseFacade extends RpcReceiver {

    private final EventFacade mEventFacade;
    private BluetoothAdapter mBluetoothAdapter;
    private static int BleAdvertiseCallbackCount;
    private static int ClassicBleAdvertiseCallbackCount;
    private static int BleAdvertiseSettingsCount;
    private static int BleAdvertiseDataCount;
    private final HashMap<Integer, myAdvertiseCallback> mAdvertiseCallbackList;
    private final HashMap<Integer, myClassicAdvertiseCallback> mClassicAdvertiseCallbackList;
    private final BluetoothLeAdvertiser mAdvertise;
    private final Service mService;
    private Builder mAdvertiseDataBuilder;
    private android.bluetooth.le.AdvertiseSettings.Builder mAdvertiseSettingsBuilder;
    private final HashMap<Integer, AdvertisementData> mAdvertiseDataList;
    private final HashMap<Integer, AdvertiseSettings> mAdvertiseSettingsList;

    public BluetoothLeAdvertiseFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mBluetoothAdapter = MainThread.run(mService,
                new Callable<BluetoothAdapter>() {
                    @Override
                    public BluetoothAdapter call() throws Exception {
                        return BluetoothAdapter.getDefaultAdapter();
                    }
                });
        mEventFacade = manager.getReceiver(EventFacade.class);
        mAdvertiseCallbackList = new HashMap<Integer, myAdvertiseCallback>();
        mClassicAdvertiseCallbackList = new HashMap<Integer, myClassicAdvertiseCallback>();
        mAdvertise = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mAdvertiseDataList = new HashMap<Integer, AdvertisementData>();
        mAdvertiseSettingsList = new HashMap<Integer, AdvertiseSettings>();
        mAdvertiseDataBuilder = new Builder();
        mAdvertiseSettingsBuilder = new android.bluetooth.le.AdvertiseSettings.Builder();
    }

    /**
     * Constructs a myAdvertiseCallback obj and returns its index
     *
     * @return myAdvertiseCallback.index
     */
    @Rpc(description = "Generate a new myAdvertisement Object")
    public Integer genBleAdvertiseCallback() {
        BleAdvertiseCallbackCount += 1;
        int index = BleAdvertiseCallbackCount;
        myAdvertiseCallback mCallback = new myAdvertiseCallback(index);
        mAdvertiseCallbackList.put(mCallback.index,
                mCallback);
        return mCallback.index;
    }

    /**
     * Constructs a myClassicAdvertiseCallback obj and returns its index
     *
     * @return myClassicAdvertiseCallback.index
     */
    @Rpc(description = "Generate a new myClassicAdvertisement Object")
    public Integer genClassicBleAdvertiseCallback() {
        ClassicBleAdvertiseCallbackCount += 1;
        int index = ClassicBleAdvertiseCallbackCount;
        myClassicAdvertiseCallback mCallback = new myClassicAdvertiseCallback(index);
        mClassicAdvertiseCallbackList.put(mCallback.index,
                mCallback);
        return mCallback.index;
    }

    /**
     * Constructs a AdvertisementData obj and returns its index
     *
     * @return index
     */
    @Rpc(description = "Constructs a new Builder obj for AdvertiseData and returns its index")
    public Integer buildAdvertisementData() {
        BleAdvertiseDataCount += 1;
        int index = BleAdvertiseDataCount;
        mAdvertiseDataList.put(index,
                mAdvertiseDataBuilder.build());
        mAdvertiseDataBuilder = new Builder();
        return index;
    }

    /**
     * Constructs a Advertise Settings obj and returns its index
     *
     * @return index
     */
    @Rpc(description = "Constructs a new Builder obj for AdvertiseData and returns its index")
    public Integer buildAdvertisementSettings() {
        BleAdvertiseSettingsCount += 1;
        int index = BleAdvertiseSettingsCount;
        mAdvertiseSettingsList.put(index,
                mAdvertiseSettingsBuilder.build());
        mAdvertiseSettingsBuilder = new android.bluetooth.le.AdvertiseSettings.Builder();
        return index;
    }

    /**
     * Stops Advertising and Removes a myAdvertiseCallback obj
     *
     * @throws Exception
     */
    @Rpc(description = "Stops Advertising and Removes a myAdvertiseCallback obj")
    public void removeBleAdvertiseCallback(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mAdvertiseCallbackList.get(index) != null) {
            mAdvertiseCallbackList.remove(index);
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Removes a AdvertiseSettings obj
     *
     * @throws Exception
     */
    @Rpc(description = "Removes a AdvertiseSettings obj")
    public void removeBleAdvertiseSetting(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            mAdvertiseSettingsList.remove(index);
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Removes a AdvertiseData obj
     *
     * @throws Exception
     */
    @Rpc(description = "Removes a AdvertiseData obj")
    public void removeBleAdvertiseData(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            mAdvertiseDataList.remove(index);
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Stops a ble advertisement
     *
     * @param index the id of the advertisement to stop advertising on
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing ble advertisement scan")
    @RpcStopEvent("BleAdvertise")
    public void stopBleAdvertising(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseCallbackList.get(index) != null) {
            Log.d("bluetooth_le mAdvertise " + index);
            mAdvertise.stopAdvertising(mAdvertiseCallbackList
                    .get(index));
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Stops a classic ble advertisement
     *
     * @param index the id of the advertisement to stop advertising on
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing classic ble advertisement scan")
    @RpcStopEvent("BleAdvertise")
    public void stopClassicBleAdvertising(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mClassicAdvertiseCallbackList.get(index) != null) {
            Log.d("bluetooth_le_classic mAdvertise " + index);
            mBluetoothAdapter.stopAdvertising(mClassicAdvertiseCallbackList
                    .get(index));
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Starts ble advertising
     *
     * @param callbackIndex The advertisementCallback index
     * @param dataIndex the advertisementData index
     * @param settingsIndex the advertisementsettings index
     * @throws Exception
     */
    @Rpc(description = "Starts ble advertisement")
    @RpcStartEvent("BleAdvertising")
    public void startBleAdvertising(
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex,
            @RpcParameter(name = "dataIndex")
            Integer dataIndex,
            @RpcParameter(name = "settingsIndex")
            Integer settingsIndex
            ) throws Exception {
        AdvertisementData mData = new AdvertisementData.Builder().build();
        AdvertiseSettings mSettings = new AdvertiseSettings.Builder().build();
        if (mAdvertiseDataList.get(dataIndex) != null) {
            mData = mAdvertiseDataList.get(dataIndex);
        } else {
            throw new Exception("Invalid dataIndex input:" + Integer.toString(dataIndex));
        }
        if (mAdvertiseSettingsList.get(settingsIndex) != null) {
            mSettings = mAdvertiseSettingsList.get(settingsIndex);
        } else {
            throw new Exception("Invalid settingsIndex input:" + Integer.toString(settingsIndex));
        }
        if (mAdvertiseCallbackList.get(callbackIndex) != null) {
            Log.d("bluetooth_le starting a background scan on callback index: "
                    + Integer.toString(callbackIndex));
            mAdvertise
                    .startAdvertising(mSettings, mData, mAdvertiseCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid callbackIndex input" + Integer.toString(callbackIndex));
        }
    }

    /**
     * Starts ble advertising with a scanResponse. ScanResponses are created in the same way
     * AdvertisementData is created since they share the same object type.
     *
     * @param callbackIndex The advertisementCallback index
     * @param dataIndex the advertisementData index
     * @param settingsIndex the advertisementsettings index
     * @param scanResponseIndex the scanResponse index
     * @throws Exception
     */
    @Rpc(description = "Starts ble advertisement")
    @RpcStartEvent("BleAdvertising")
    public void startBleAdvertisingWithScanResponse(
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex,
            @RpcParameter(name = "dataIndex")
            Integer dataIndex,
            @RpcParameter(name = "settingsIndex")
            Integer settingsIndex,
            @RpcParameter(name = "scanResponseIndex")
            Integer scanResponseIndex
            ) throws Exception {
        AdvertisementData mData = new AdvertisementData.Builder().build();
        AdvertiseSettings mSettings = new AdvertiseSettings.Builder().build();
        AdvertisementData mScanResponse = new AdvertisementData.Builder().build();

        if (mAdvertiseDataList.get(dataIndex) != null) {
            mData = mAdvertiseDataList.get(dataIndex);
        } else {
            throw new Exception("Invalid dataIndex input:" + Integer.toString(dataIndex));
        }
        if (mAdvertiseSettingsList.get(settingsIndex) != null) {
            mSettings = mAdvertiseSettingsList.get(settingsIndex);
        } else {
            throw new Exception("Invalid settingsIndex input:" + Integer.toString(settingsIndex));
        }
        if (mAdvertiseDataList.get(scanResponseIndex) != null) {
            mScanResponse = mAdvertiseDataList.get(settingsIndex);
        } else {
            throw new Exception("Invalid scanResponseIndex input:"
                    + Integer.toString(settingsIndex));
        }
        if (mAdvertiseCallbackList.get(callbackIndex) != null) {
            Log.d("bluetooth_le starting a background scan on callback index: "
                    + Integer.toString(callbackIndex));
            mAdvertise
                    .startAdvertising(mSettings, mData, mScanResponse,
                            mAdvertiseCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid callbackIndex input" + Integer.toString(callbackIndex));
        }
    }

    /**
     * Starts Classic ble advertising
     *
     * @param callbackIndex The advertisementCallback index
     * @throws Exception
     */
    @Rpc(description = "Starts ble advertisement")
    @RpcStartEvent("BleAdvertising")
    public void startClassicBleAdvertising(
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex,
            @RpcParameter(name = "dataIndex")
            Integer dataIndex,
            @RpcParameter(name = "settingsIndex")
            Integer settingsIndex
            ) throws Exception {
        if (mClassicAdvertiseCallbackList.get(callbackIndex) != null) {
            Log.d("bluetooth_le starting a background scan on callback index: "
                    + Integer.toString(callbackIndex));
            mBluetoothAdapter.startAdvertising(mClassicAdvertiseCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid callbackIndex input" + Integer.toString(callbackIndex));
        }
    }

    /**
     * Set ble advertisement data include tx power level
     *
     * @param includeTxPowerLevel boolean whether to include the tx power level or not in the
     *            advertisement
     * @throws Exception
     */
    @Rpc(description = "Set ble advertisement data include tx power level")
    public void setAdvertisementDataIncludeTxPowerLevel(
            @RpcParameter(name = "includeTxPowerLevel")
            Boolean includeTxPowerLevel
            ) {
        mAdvertiseDataBuilder.setIncludeTxPowerLevel(includeTxPowerLevel);
    }

    /**
     * Get ble advertisement settings mode
     *
     * @param index the advertise settings object to use
     * @return the mode of the advertise settings object
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement settings mode")
    public int getAdvertisementSettingsMode(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            AdvertiseSettings mSettings = mAdvertiseSettingsList.get(index);
            return mSettings.getMode();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement settings tx power level
     *
     * @param index the advertise settings object to use
     * @return the tx power level of the advertise settings object
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement settings tx power level")
    public int getAdvertisementSettingsTxPowerLevel(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            AdvertiseSettings mSettings = mAdvertiseSettingsList.get(index);
            return mSettings.getTxPowerLevel();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement settings type
     *
     * @param index the advertise settings object to use
     * @return the type of the advertise settings object
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement settings type")
    public int getAdvertisementSettingsType(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            AdvertiseSettings mSettings = mAdvertiseSettingsList.get(index);
            return mSettings.getType();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement data include tx power level
     *
     * @param index the advertise data object to use
     * @return True if include tx power level, false otherwise
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement data include tx power level")
    public Boolean getAdvertisementDataIncludeTxPowerLevel(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            return mData.getIncludeTxPowerLevel();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement data manufacturer id
     *
     * @param index the advertise data object to use
     * @return the advertisement data's manufacturer id.
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement data manufacturer id")
    public Integer getAdvertisementDataManufacturerId(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            return mData.getManufacturerId();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));

        }
    }

    /**
     * Get ble advertisement Manufacturer Specific Data
     *
     * @param index the advertise data object to use
     * @return the advertisement data's manufacturer specific data.
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement Manufacturer Specific Data")
    public String getAdvertisementDataManufacturerSpecificData(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            return ConvertUtils.convertByteArrayToString(mData.getManufacturerSpecificData());
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement Service Data
     *
     * @param index the advertise data object to use
     * @return the advertisement data's service data
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement Service Data")
    public String getAdvertisementDataServiceData(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            return ConvertUtils.convertByteArrayToString(mData.getServiceData());
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement Service Data Uuid
     *
     * @param index the advertise data object to use
     * @return the advertisement data's service data uuid
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement Service Data Uuid")
    public String getAdvertisementDataServiceDataUuid(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            if (mData.getServiceDataUuid() != null) {
                return mData.getServiceDataUuid().toString();
            } else {
                throw new Exception("Service Data Uuid not set for input "
                        + "AdvertisementData: " + index);
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ble advertisement Service Uuids
     *
     * @param index the advertise data object to use
     * @return the advertisement data's Service Uuids
     * @throws Exception
     */
    @Rpc(description = "Get ble advertisement Service Uuids")
    public List<ParcelUuid> getAdvertisementDataServiceUuids(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            AdvertisementData mData = mAdvertiseDataList.get(index);
            return mData.getServiceUuids();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Set ble advertisement data service uuids
     *
     * @param uuidList
     * @throws Exception
     */
    @Rpc(description = "Set ble advertisement data service uuids")
    public void setAdvertisementDataSetServiceUuids(
            @RpcParameter(name = "uuidList")
            String[] uuidList
            ) {
        ArrayList<ParcelUuid> mUuids = new ArrayList<ParcelUuid>();
        for (String uuid : uuidList) {
            mUuids.add(ParcelUuid.fromString(uuid));
        }
        mAdvertiseDataBuilder.setServiceUuids(mUuids);
    }

    /**
     * Set ble advertise data service uuids
     *
     * @param serviceDataUuid
     * @param serviceData
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise data service uuids")
    public void setAdvertisementDataSetServiceData(
            @RpcParameter(name = "serviceDataUuid")
            String serviceDataUuid,
            @RpcParameter(name = "serviceData")
            String serviceData
            ) {
        mAdvertiseDataBuilder.setServiceData(
                ParcelUuid.fromString(serviceDataUuid),
                ConvertUtils.convertStringToByteArray(serviceData));
    }

    /**
     * Set ble advertise data manufacturer id
     *
     * @param manufacturerId the manufacturer id to set
     * @param manufacturerSpecificData the manufacturer specific data to set
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise data manufacturerId")
    public void setAdvertisementDataManufacturerId(
            @RpcParameter(name = "manufacturerId")
            Integer manufacturerId,
            @RpcParameter(name = "manufacturerSpecificData")
            String manufacturerSpecificData
            ) {
        mAdvertiseDataBuilder.setManufacturerData(manufacturerId,
                ConvertUtils.convertStringToByteArray(manufacturerSpecificData));
    }

    /**
     * Set ble advertise settings advertise mode
     *
     * @param advertiseMode
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings advertise mode")
    public void setAdvertisementSettingAdvertiseMode(
            @RpcParameter(name = "advertiseMode")
            Integer advertiseMode
            ) {
        mAdvertiseSettingsBuilder.setAdvertiseMode(advertiseMode);
    }

    /**
     * Set ble advertise settings tx power level
     *
     * @param txPowerLevel the tx power level to set
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings tx power level")
    public void setAdvertisementSettingIncludeTxPowerLevel(
            @RpcParameter(name = "includeTxPowerLevel")
            Integer txPowerLevel
            ) {
        mAdvertiseSettingsBuilder.setTxPowerLevel(txPowerLevel);
    }

    /**
     * Set ble advertise settings the setting type
     *
     * @param type the setting type
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings the setting type")
    public void setAdvertisementSettingType(
            @RpcParameter(name = "type")
            Integer type
            ) {
        mAdvertiseSettingsBuilder.setType(type);
    }

    private class myAdvertiseCallback extends AdvertiseCallback {
        public Integer index;
        private final Bundle mResults;
        String mEventType;

        public myAdvertiseCallback(int idx) {
            index = idx;
            mEventType = "BleAdvertise";
            mResults = new Bundle();
        }

        @Override
        public void onSuccess(AdvertiseSettings settingsInEffect) {
            Log.d("bluetooth_le_advertisement onSuccess " + mEventType + " "
                    + index);
            mResults.putString("Type", "onSuccess");
            mResults.putParcelable("SettingsInEffect", settingsInEffect);
            mEventFacade.postEvent(mEventType + index + "onSuccess", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onFailure(int errorCode) {
            Log.d("bluetooth_le_advertisement onFailure " + mEventType + " "
                    + index);
            mResults.putString("Type", "onFailure");
            mResults.putInt("ErrorCode", errorCode);
            mEventFacade.postEvent(mEventType + index + "onFailure",
                    mResults.clone());
            mResults.clear();
        }
    }

    private class myClassicAdvertiseCallback implements
            android.bluetooth.BluetoothAdapter.AdvertiseCallback {
        public Integer index;
        private final Bundle mResults;
        String mEventType;

        public myClassicAdvertiseCallback(int idx) {
            index = idx;
            mEventType = "BleAdvertise";
            mResults = new Bundle();
        }

        @Override
        public void onAdvertiseStart(int status) {
            Log.d("bluetooth_classic_le_advertisement onAdvertiseStart " + mEventType + " "
                    + index);
            mResults.putString("Type", "onAdvertiseStart");
            mResults.putInt("Status", status);
            mEventFacade.postEvent(mEventType + index + "onAdvertiseStart",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onAdvertiseStop(int status) {
            Log.d("bluetooth_classic_le_advertisement onAdvertiseStop " + mEventType + " "
                    + index);
            mResults.putString("Type", "onAdvertiseStop");
            mResults.putInt("Status", status);
            mEventFacade.postEvent(mEventType + index + "onAdvertiseStop",
                    mResults.clone());
            mResults.clear();
        }

    }

    @Override
    public void shutdown() {
        if (mAdvertiseCallbackList.isEmpty() == false) {
            for (myAdvertiseCallback mAdvertise : mAdvertiseCallbackList
                    .values()) {
                if (mAdvertise != null) {
                    mBluetoothAdapter.getBluetoothLeAdvertiser()
                            .stopAdvertising(mAdvertise);
                }
            }
            mAdvertiseCallbackList.clear();
            mAdvertiseSettingsList.clear();
            mAdvertiseDataList.clear();
        }

        if (mClassicAdvertiseCallbackList.isEmpty() == false) {
            for (myClassicAdvertiseCallback mAdvertise : mClassicAdvertiseCallbackList
                    .values()) {
                if (mAdvertise != null) {
                    mBluetoothAdapter.stopAdvertising(mAdvertise);
                }
            }
            mAdvertiseCallbackList.clear();
        }
    }

}
