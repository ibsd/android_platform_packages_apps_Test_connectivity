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
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcMinSdk;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

/**
 * BluetoothLe Advertise functions.
 */

@RpcMinSdk(5)
public class BluetoothLeAdvertiseFacade extends RpcReceiver {

    private final EventFacade mEventFacade;
    private BluetoothAdapter mBluetoothAdapter;
    private static int BleAdvertiseCallbackCount;
    private static int BleAdvertiseSettingsCount;
    private final HashMap<Integer, myAdvertiseCallback> mAdvertiseCallbackList;
    private final BluetoothLeAdvertiser mAdvertise;
    private final Service mService;
    private final HashMap<Integer, Builder> mAdvertiseDataList;
    private final HashMap<Integer, android.bluetooth.le.AdvertiseSettings.Builder> mAdvertiseSettingsList;

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
        mAdvertise = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mAdvertiseDataList = new HashMap<Integer, Builder>();
        mAdvertiseSettingsList = new HashMap<Integer, android.bluetooth.le.AdvertiseSettings.Builder>();
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
     * Constructs a AdvertisementData obj and returns its index
     *
     * @return index
     */
    @Rpc(description = "Constructs a new Builder obj for AdvertiseData and returns its index")
    public Integer genBleAdvertiseData() {
        int index = BleAdvertiseCallbackCount;
        Builder mData = new Builder();
        mAdvertiseDataList.put(index,
                mData);
        return index;
    }

    /**
     * Constructs a AdvertisementSettings obj and returns its index
     *
     * @return index
     */
    @Rpc(description = "Constructs a new android.bluetooth.le.AdvertiseSettings.Builder obj for AdvertiseSettings and returns its index")
    public Integer genBleAdvertiseSettings() {
        BleAdvertiseSettingsCount += 1;
        int index = BleAdvertiseSettingsCount;
        android.bluetooth.le.AdvertiseSettings.Builder mSettings = new android.bluetooth.le.AdvertiseSettings.Builder();
        mAdvertiseSettingsList.put(index,
                mSettings);
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
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
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
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
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
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
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
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Starts ble advertising
     *
     * @param index the myAdvertisement object to start advertising on
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
            mData = mAdvertiseDataList.get(dataIndex).build();
        } else {
            throw new Exception("Invalid dataIndex input:"
                    + Integer.toString(dataIndex));
        }
        if (mAdvertiseSettingsList.get(settingsIndex) != null) {
            mSettings = mAdvertiseSettingsList.get(settingsIndex).build();
        } else {
            throw new Exception("Invalid settingsIndex input:"
                    + Integer.toString(settingsIndex));
        }
        if (mAdvertiseCallbackList.get(callbackIndex) != null) {
            Log.d("bluetooth_le starting a background scan on callback index: "
                    + Integer.toString(callbackIndex));
            mAdvertise
                    .startAdvertising(mSettings, mData, mAdvertiseCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid callbackIndex input:"
                    + Integer.toString(callbackIndex));
        }
    }

    /**
     * Set ble advertisement data include tx power level
     *
     * @param index the advertise data object to start advertising on
     * @param includeTxPowerLevel boolean whether to include the tx power level or not in the
     *            advertisement
     * @throws Exception
     */
    @Rpc(description = "Set ble advertisement data include tx power level")
    public void setAdvertisementDataAdvertisementDataIncludeTxPowerLevel(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "includeTxPowerLevel")
            Boolean includeTxPowerLevel
            ) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            mAdvertiseDataList.get(index).setIncludeTxPowerLevel(includeTxPowerLevel);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertisement data service uuids
     *
     * @param index the advertise data object to start advertising on
     * @param uuidList
     * @throws Exception
     */
    @Rpc(description = "Set ble advertisement data service uuids")
    public void setAdvertisementDataSetServiceUuids(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "uuidList")
            List<String> uuidList
            ) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            ArrayList<ParcelUuid> mUuids = new ArrayList<ParcelUuid>();
            for (String uuid : uuidList) {
                mUuids.add(ParcelUuid.fromString(uuid));
            }
            mAdvertiseDataList.get(index).setServiceUuids(mUuids);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertise data service uuids
     *
     * @param index the advertise data object index
     * @param serviceDataUuid
     * @param serviceData
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise data service uuids")
    public void setAdvertisementDataSetServiceData(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "serviceDataUuid")
            String serviceDataUuid,
            @RpcParameter(name = "serviceData")
            byte[] serviceData
            ) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            mAdvertiseDataList.get(index).setServiceData(
                    ParcelUuid.fromString(serviceDataUuid),
                    serviceData);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertise data manufacturer id
     *
     * @param index the advertise data object index
     * @param manufacturerId the manufacturer id to set
     * @param manufacturerSpecificData the manufacturer specific data to set
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise data manufacturerId")
    public void setAdvertisementDataManufacturerId(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "manufacturerId")
            Integer manufacturerId,
            @RpcParameter(name = "manufacturerSpecificData")
            byte[] manufacturerSpecificData
            ) throws Exception {
        if (mAdvertiseDataList.get(index) != null) {
            mAdvertiseDataList.get(index).setManufacturerData(manufacturerId,
                    manufacturerSpecificData);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertise settings advertise mode
     *
     * @param index the advertise settings object index
     * @param advertiseMode
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings advertise mode")
    public void setAdvertisementSettingAdvertiseMode(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "advertiseMode")
            Integer advertiseMode
            ) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            mAdvertiseSettingsList.get(index).setAdvertiseMode(advertiseMode);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertise settings tx power level
     *
     * @param index the advertise settings object index
     * @param txPowerLevel the tx power level to set
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings tx power level")
    public void setAdvertisementSettingIncludeTxPowerLevel(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "includeTxPowerLevel")
            Integer txPowerLevel
            ) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            mAdvertiseSettingsList.get(index).setTxPowerLevel(
                    txPowerLevel);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Set ble advertise settings the setting type
     *
     * @param index the advertise settings object index
     * @param type the setting type
     * @throws Exception
     */
    @Rpc(description = "Set ble advertise settings the setting type")
    public void setAdvertisementSettingType(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "type")
            Integer type
            ) throws Exception {
        if (mAdvertiseSettingsList.get(index) != null) {
            mAdvertiseSettingsList.get(index).setType(type);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
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
            mResults.putInt("ID", index);
            mResults.putString("Type", "onSuccess");
            mResults.putParcelable("SettingsInEffect", settingsInEffect);
            mEventFacade.postEvent(mEventType + index + "onSuccess",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onFailure(int errorCode) {
            Log.d("bluetooth_le_advertisement onFailure " + mEventType + " "
                    + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onFailure");
            mResults.putInt("ErrorCode", errorCode);
            mEventFacade.postEvent(mEventType + index + "onFailure",
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
    }

}
