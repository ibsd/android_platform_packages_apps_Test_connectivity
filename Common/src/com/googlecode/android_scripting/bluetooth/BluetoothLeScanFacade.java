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

package com.googlecode.android_scripting.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanFilter.Builder;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcMinSdk;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

/**
 * BluetoothLe Scan functions.
 */

@RpcMinSdk(5)
public class BluetoothLeScanFacade extends RpcReceiver {

    private final EventFacade mEventFacade;

    private BluetoothAdapter mBluetoothAdapter;
    private static int ScanCallbackCount;
    private static int FilterListCount;
    private static int ScanSettingsCount;
    private final Service mService;
    private final BluetoothLeScanner mScanner;
    private final HashMap<Integer, myScanCallback> mScanCallbackList;
    private final HashMap<Integer, ArrayList<Builder>> mScanFilterList;
    private final HashMap<Integer, android.bluetooth.le.ScanSettings.Builder> mScanSettingsList;

    public BluetoothLeScanFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mBluetoothAdapter = MainThread.run(mService,
                new Callable<BluetoothAdapter>() {
                    @Override
                    public BluetoothAdapter call() throws Exception {
                        return BluetoothAdapter.getDefaultAdapter();
                    }
                });
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mEventFacade = manager.getReceiver(EventFacade.class);
        mScanFilterList = new HashMap<Integer, ArrayList<Builder>>();
        mScanSettingsList = new HashMap<Integer, android.bluetooth.le.ScanSettings.Builder>();
        mScanCallbackList = new HashMap<Integer, myScanCallback>();

    }

    /**
     * Constructs a myScanCallback obj and returns its index
     *
     * @return Integer myScanCallback.index
     */
    @Rpc(description = "Generate a new myScanCallback Object")
    public Integer createScanCallback() {
        ScanCallbackCount += 1;
        int index = ScanCallbackCount;
        myScanCallback mScan = new myScanCallback(index);
        mScanCallbackList.put(mScan.index, mScan);
        return mScan.index;
    }

    /**
     * Constructs a new filter list array and returns its index
     *
     * @return Integer index
     */
    @Rpc(description = "Generate a new Filter list")
    public Integer createFilterList() {
        FilterListCount += 1;
        int index = FilterListCount;
        mScanFilterList.put(index, new ArrayList<Builder>());
        return index;
    }

    /**
     * Constructs a new scan setting and returns its index
     *
     * @return Integer index
     */
    @Rpc(description = "Generate a new scan settings Object")
    public Integer createScanSetting() {
        ScanSettingsCount += 1;
        int index = ScanSettingsCount;
        mScanSettingsList.put(index, new android.bluetooth.le.ScanSettings.Builder());
        return index;
    }

    /**
     * Removes a scan setting
     *
     * @return Integer index
     * @throws Exception
     */
    @Rpc(description = "Removes a scan setting")
    public void deleteScanSetting(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            mScanSettingsList.remove(index);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Removes a scan callback
     *
     * @return Integer index
     * @throws Exception
     */
    @Rpc(description = "Removes a scan callback")
    public void deleteScanCallback(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mScanCallbackList.get(index) != null) {
            mScanner.stopScan(mScanCallbackList.get(index));
            mScanCallbackList.remove(index);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Removes a filter list
     *
     * @return Integer index
     * @throws Exception
     */
    @Rpc(description = "Removes a filter list")
    public void deleteFilterList(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList.remove(index);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Stops a ble scan
     *
     * @param index the id of the myScan whose ScanCallback to stop
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing ble advertisement scan")
    @RpcStopEvent("BleScan")
    public void stopBleScan(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        Log.d("bluetooth_le_scan mScanCallback " + index);
        if (mScanCallbackList.get(index) != null) {
            myScanCallback mScanCallback = mScanCallbackList.get(index);
            mScanner.stopScan(mScanCallback);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Starts a ble scan
     *
     * @param index the id of the myScan whose ScanCallback to start
     * @throws Exception
     */
    @Rpc(description = "Starts a ble advertisement scan")
    @RpcStartEvent("BleScan")
    public void startBleScan(
            @RpcParameter(name = "filterListIndex")
            Integer filterListIndex,
            @RpcParameter(name = "scanSettingsIndex")
            Integer scanSettingsIndex,
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex
            ) throws Exception {
        Log.d("bluetooth_le_scan starting a background scan");
        ArrayList<ScanFilter> mScanFilters = new ArrayList<ScanFilter>();
        ScanSettings mScanSettings = new ScanSettings.Builder().build();
        if (mScanFilterList.get(filterListIndex) != null) {
            ArrayList<Builder> mFilterList = mScanFilterList.get(filterListIndex);
            if (mFilterList.size() == 0) { // then use the default filter
                mScanFilters.add(new ScanFilter.Builder().build());
            } else {
                for (Builder unbuiltScanFilter : mFilterList) {
                    mScanFilters.add(unbuiltScanFilter.build());
                }
            }
        } else {
            throw new Exception("Invalid filterListIndex input:"
                    + Integer.toString(filterListIndex));
        }
        if (mScanSettingsList.get(scanSettingsIndex) != null) {
            mScanSettings = mScanSettingsList.get(scanSettingsIndex).build();
        } else if (!mScanSettingsList.isEmpty()) {
            throw new Exception("Invalid scanSettingsIndex input:"
                    + Integer.toString(scanSettingsIndex));
        }
        if (mScanCallbackList.get(callbackIndex) != null) {
            mScanner.startScan(mScanFilters, mScanSettings, mScanCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid filterListIndex input:"
                    + Integer.toString(filterListIndex));
        }
    }

    /**
     * Get a ble batch Scan results
     *
     * @param flush the results
     * @throws Exception
     */
    @Rpc(description = "Gets the results of the ble ScanCallback")
    public List<ScanResult> getBatchScanResults(
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex,
            @RpcParameter(name = "flush")
            Boolean flush) throws Exception {
        if (mScanCallbackList.get(callbackIndex) != null) {
            return mBluetoothAdapter
                    .getBluetoothLeScanner().getBatchScanResults(
                            mScanCallbackList.get(callbackIndex),
                            flush);
        } else {
            throw new Exception("Invalid callbackIndex input:"
                    + Integer.toString(callbackIndex));
        }
    }

    /**
     * Set scanSettings for ble scan. Note: You have to set all variables at once.
     *
     * @param index the index of the ScanSettings
     * @param callbackType Bluetooth LE scan callback type
     * @param reportDelayNanos Time of delay for reporting the scan result
     * @param scanMode Bluetooth LE scan mode.
     * @param scanResultType Bluetooth LE scan result type
     * @throws Exception
     */
    @Rpc(description = "Set a new Scan Setting for ble scanning")
    public void setScanSettings(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "callbackType")
            Integer callbackType,
            @RpcParameter(name = "reportDelayNanos")
            Integer reportDelayNanos,
            @RpcParameter(name = "scanMode")
            Integer scanMode,
            @RpcParameter(name = "scanResultType")
            Integer scanResultType) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            android.bluetooth.le.ScanSettings.Builder mBuilder = mScanSettingsList.get(index);
            mBuilder.setCallbackType(callbackType);
            mBuilder.setCallbackType(scanMode);
            mBuilder.setCallbackType(scanResultType);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "macAddress" to existing ScanFilter
     *
     * @param index the index of the ScanFilter
     * @param filterIndex Integer the filter to add to
     * @param macAddress the macAddress to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"macAddress\" to existing ScanFilter")
    public void setScanFilterMacAddress(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "macAddress")
            String macAddress
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList.get(index).get(filterIndex).setMacAddress(macAddress);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "manufacturereDataId and/or manufacturerData" to existing ScanFilter
     *
     * @param index the index of the myScan
     * @param filterIndex Integer the filter to add to
     * @param manufacturerDataId the manufacturer data id to filter against
     * @param manufacturerDataMask the manufacturere data mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"manufacturereDataId and/or manufacturerData\" to existing ScanFilter")
    public void setScanFiltermanufacturerData(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "manufacturerDataId")
            Integer manufacturerDataId,
            @RpcParameter(name = "manufacturerData")
            byte[] manufacturerData,
            @RpcParameter(name = "manufacturerDataMask")
            @RpcOptional
            byte[] manufacturerDataMask
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (manufacturerDataMask != null) {
                mScanFilterList.get(index).get(filterIndex).setManufacturerData(manufacturerDataId,
                        manufacturerData, manufacturerDataMask);
            } else {
                mScanFilterList.get(index).get(filterIndex).setManufacturerData(manufacturerDataId,
                        manufacturerData);
            }
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "minRssi and maxRssi" to existing ScanFilter
     *
     * @param index the index of the myScan
     * @param filterIndex Integer the filter to add to
     * @param minRssi the min rssi to filter against
     * @param maxRssi the max rssi to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"minRssi and maxRssi\" to existing ScanFilter")
    public void setScanFilterRssiRange(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "minRssi")
            Integer minRssi,
            @RpcParameter(name = "maxRssi")
            Integer maxRssi
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList.get(index).get(filterIndex).setRssiRange(minRssi, maxRssi);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "serviceData and serviceDataMask" to existing ScanFilter
     *
     * @param index the index of the myScan
     * @param filterIndex Integer the filter to add to
     * @param serviceData the service data to filter against
     * @param serviceDataMask the servie data mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"serviceData and serviceDataMask\" to existing ScanFilter ")
    public void setScanFilterServiceData(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "serviceData")
            byte[] serviceData,
            @RpcParameter(name = "serviceDataMask")
            @RpcOptional
            byte[] serviceDataMask
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (serviceData != null) {
                mScanFilterList.get(index).get(filterIndex)
                        .setServiceData(serviceData, serviceDataMask);
            } else {
                mScanFilterList.get(index).get(filterIndex).setServiceData(serviceData);
            }
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "serviceUuid and/or serviceMask" to existing ScanFilter
     *
     * @param index the index of the myScan
     * @param filterIndex Integer the filter to add to
     * @param serviceUuid the service uuid to filter against
     * @param serviceMask the service mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"serviceUuid and/or serviceMask\" to existing ScanFilter")
    public void setScanFilterServiceUuid(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "serviceUuid")
            String serviceUuid,
            @RpcParameter(name = "serviceMask")
            @RpcOptional
            String serviceMask
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList
                    .get(index)
                    .get(filterIndex)
                    .setServiceUuid(ParcelUuid.fromString(serviceUuid),
                            ParcelUuid.fromString(serviceMask));
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Add filter "name" to existing ScanFilter
     *
     * @param index the index of the myScan
     * @param filterIndex Integer the filter to add to
     * @param name the name to filter against
     * @throws Exception
     */
    @Rpc(description = "Remove a scanFilter from the scanFilterList")
    public void setScanFilterName(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex,
            @RpcParameter(name = "name")
            String name
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList.get(index).get(filterIndex).setName(name);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Remove a scanFilter from the scanFilterList
     *
     * @param index the index of the myScan
     * @param filterIndex Integer of the filter to remove
     * @throws Exception
     */
    @Rpc(description = "Remove a scanFilter from the scanFilterList")
    public void removeScanFilterFromScanFilterList(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex
            ) throws Exception {
        if (mScanFilterList.get(index) != null) {
            mScanFilterList.get(index).remove(filterIndex);
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    private class myScanCallback extends ScanCallback {
        public Integer index;
        String mEventType;
        private final Bundle mResults;

        public myScanCallback(Integer idx) {
            index = idx;
            mEventType = "BleScan";
            mResults = new Bundle();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("bluetooth_le_scan change onScanFailed " + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onScanFailed");
            mResults.putInt("ErrorCode", errorCode);
            mEventFacade.postEvent(mEventType + index + "onScanFailed",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onAdvertisementUpdate(ScanResult result) {
            Log.d("bluetooth_le_scan change onUpdate " + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onAdvertisementUpdate");
            mResults.putParcelable("Result", result);
            mEventFacade.postEvent(
                    mEventType + index + "onAdvertisementUpdate",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onAdvertisementFound(ScanResult result) {
            Log.d("bluetooth_le_scan onAdvertisementFound " + mEventType + " "
                    + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onAdvertisementFound");
            mEventFacade.postEvent(mEventType + index + "onAdvertisementFound",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onAdvertisementLost(ScanResult result) {
            Log.d("bluetooth_le_scan onAdvertisementLost" + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onAdvertisementLost");
            mResults.putParcelable("Result", result);
            mEventFacade.postEvent(mEventType + index + "onAdvertisementLost",
                    mResults.clone());
            mResults.clear();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d("reportResult " + mEventType + " " + index);
            mResults.putLong("Timestamp", System.currentTimeMillis() / 1000);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onBatchScanResults");
            mResults.putParcelableList("Results", results);
            mEventFacade.postEvent(mEventType + index + "onBatchScanResult",
                    mResults.clone());
            mResults.clear();
        }

    }

    @Override
    public void shutdown() {
        if (mScanCallbackList.isEmpty() == false) {
            for (myScanCallback mScanCallback : mScanCallbackList.values()) {
                if (mScanCallback != null) {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(
                            mScanCallback);
                }
            }
        }
        mScanCallbackList.clear();
        mScanFilterList.clear();
        mScanSettingsList.clear();
    }

}
