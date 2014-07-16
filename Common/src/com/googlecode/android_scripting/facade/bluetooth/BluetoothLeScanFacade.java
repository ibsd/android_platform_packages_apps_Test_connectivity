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

import java.math.BigInteger;
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

import com.googlecode.android_scripting.ConvertUtils;
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
    private android.bluetooth.le.ScanSettings.Builder mScanSettingsBuilder;
    private Builder mScanFilterBuilder;
    private final HashMap<Integer, myScanCallback> mScanCallbackList;
    private final HashMap<Integer, ArrayList<ScanFilter>> mScanFilterList;
    private final HashMap<Integer, ScanSettings> mScanSettingsList;

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
        mScanFilterList = new HashMap<Integer, ArrayList<ScanFilter>>();
        mScanSettingsList = new HashMap<Integer, ScanSettings>();
        mScanCallbackList = new HashMap<Integer, myScanCallback>();
        mScanFilterBuilder = new Builder();
        mScanSettingsBuilder = new android.bluetooth.le.ScanSettings.Builder();
    }

    /**
     * Constructs a myScanCallback obj and returns its index
     *
     * @return Integer myScanCallback.index
     */
    @Rpc(description = "Generate a new myScanCallback Object")
    public Integer genScanCallback() {
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
    public Integer genFilterList() {
        FilterListCount += 1;
        int index = FilterListCount;
        mScanFilterList.put(index, new ArrayList<ScanFilter>());
        return index;
    }

    /**
     * Constructs a new filter list array and returns its index
     *
     * @return Integer index
     */
    @Rpc(description = "Generate a new Filter list")
    public Integer buildScanFilter(
            @RpcParameter(name = "filterIndex")
            Integer filterIndex
            ) {
        mScanFilterList.get(filterIndex).add(mScanFilterBuilder.build());
        mScanFilterBuilder = new Builder();
        return mScanFilterList.get(filterIndex).size()-1;
    }

    /**
     * Constructs a new scan setting and returns its index
     *
     * @return Integer index
     */
    @Rpc(description = "Generate a new scan settings Object")
    public Integer buildScanSetting() {
        ScanSettingsCount += 1;
        int index = ScanSettingsCount;
        mScanSettingsList.put(index, mScanSettingsBuilder.build());
        mScanSettingsBuilder = new android.bluetooth.le.ScanSettings.Builder();
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
            throw new Exception("Invalid index input:" + Integer.toString(index));
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
            throw new Exception("Invalid index input:" + Integer.toString(index));
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
            throw new Exception("Invalid index input:" + Integer.toString(index));
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
            throw new Exception("Invalid index input:" + Integer.toString(index));
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
        mScanFilters.add(new ScanFilter.Builder().build());
        ScanSettings mScanSettings = new ScanSettings.Builder().build();
        if (mScanFilterList.get(filterListIndex) != null) {
            mScanFilters = mScanFilterList.get(filterListIndex);
        } else {
            throw new Exception("Invalid filterListIndex input:"
                    + Integer.toString(filterListIndex));
        }
        if (mScanSettingsList.get(scanSettingsIndex) != null) {
            mScanSettings = mScanSettingsList.get(scanSettingsIndex);
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
     * Trigger onBatchScanResults
     *
     * @throws Exception
     */
    @Rpc(description = "Gets the results of the ble ScanCallback")
    public void flushPendingScanResults(
            @RpcParameter(name = "callbackIndex")
            Integer callbackIndex
            ) throws Exception {
        if (mScanCallbackList.get(callbackIndex) != null) {
            mBluetoothAdapter
                    .getBluetoothLeScanner().flushPendingScanResults(
                            mScanCallbackList.get(callbackIndex));
        } else {
            throw new Exception("Invalid callbackIndex input:"
                    + Integer.toString(callbackIndex));
        }
    }

    /**
     * Set scanSettings for ble scan. Note: You have to set all variables at once.
     *
     * @param callbackType Bluetooth LE scan callback type
     * @param reportDelaySeconds Time of delay for reporting the scan result
     * @param scanMode Bluetooth LE scan mode.
     * @param scanResultType Bluetooth LE scan result type
     * @throws Exception
     */
    @Rpc(description = "Set a new Scan Setting for ble scanning")
    public void setScanSettings(
            @RpcParameter(name = "callbackType")
            Integer callbackType,
            @RpcParameter(name = "reportDelaySeconds")
            Integer reportDelaySeconds,
            @RpcParameter(name = "scanMode")
            Integer scanMode,
            @RpcParameter(name = "scanResultType")
            Integer scanResultType) {
        mScanSettingsBuilder.setCallbackType(callbackType);
        mScanSettingsBuilder.setScanMode(scanMode);
        mScanSettingsBuilder.setScanResultType(scanResultType);
        mScanSettingsBuilder.setReportDelaySeconds(reportDelaySeconds);
    }

    /**
     * Get ScanSetting's callback type
     *
     * @param index the ScanSetting object to use
     * @return the ScanSetting's callback type
     * @throws Exception
     */
    @Rpc(description = "Get ScanSetting's callback type")
    public Integer getScanSettingsCallbackType(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            ScanSettings mScanSettings = mScanSettingsList.get(index);
            return mScanSettings.getCallbackType();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanSetting's report delay Seconds
     *
     * @param index the ScanSetting object to use
     * @return the ScanSetting's report delay in seconds
     * @throws Exception
     */
    @Rpc(description = "Get ScanSetting's report delay seconds")
    public Long getScanSettingsReportDelaySeconds(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            ScanSettings mScanSettings = mScanSettingsList.get(index);
            return mScanSettings.getReportDelaySeconds();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanSetting's scan mode
     *
     * @param index the ScanSetting object to use
     * @return the ScanSetting's scan mode
     * @throws Exception
     */
    @Rpc(description = "Get ScanSetting's scan mode")
    public Integer getScanSettingsScanMode(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            ScanSettings mScanSettings = mScanSettingsList.get(index);
            return mScanSettings.getScanMode();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanSetting's scan result type
     *
     * @param index the ScanSetting object to use
     * @return the ScanSetting's scan result type
     * @throws Exception
     */
    @Rpc(description = "Get ScanSetting's scan result type")
    public Integer getScanSettingsScanResultType(
            @RpcParameter(name = "index")
            Integer index) throws Exception {
        if (mScanSettingsList.get(index) != null) {
            ScanSettings mScanSettings = mScanSettingsList.get(index);
            return mScanSettings.getScanResultType();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's Manufacturer Id
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's manufacturer id
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's Manufacturer Id")
    public Integer getScanFilterManufacturerId(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return mScanFilterList.get(index)
                        .get(filterIndex).getManufacturerId();
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's device address
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's device address
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's device address")
    public String getScanFilterDeviceAddress(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return mScanFilterList.get(index).get(filterIndex).getDeviceAddress();
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's device name
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's device name
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's device name")
    public String getScanFilterDeviceName(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return mScanFilterList.get(index).get(filterIndex).getDeviceName();
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's manufacturer data
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's manufacturer data
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's manufacturer data")
    public String getScanFilterManufacturerData(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return ConvertUtils.convertByteArrayToString(mScanFilterList.get(index)
                        .get(filterIndex).getManufacturerData());
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's manufacturer data mask
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's manufacturer data mask
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's manufacturer data mask")
    public String getScanFilterManufacturerDataMask(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return ConvertUtils.convertByteArrayToString(mScanFilterList.get(index)
                        .get(filterIndex).getManufacturerDataMask());
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's max rssi
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's max rssi
     * @throws Exception
     */
    @Rpc(description = "Get ScanSetting's scan result type")
    public Integer getScanFilterMaxRssi(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return mScanFilterList.get(index)
                        .get(filterIndex).getMaxRssi();
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's min rssi
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's mix rssi
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's min rssi")
    public Integer getScanFilterMinRssi(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return mScanFilterList.get(index).get(filterIndex).getMinRssi();
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's service data
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's service data
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's service data")
    public String getScanFilterServiceData(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return ConvertUtils.convertByteArrayToString(mScanFilterList
                        .get(index).get(filterIndex).getServiceData());
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's service data mask
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's service data mask
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's service data mask")
    public String getScanFilterServiceDataMask(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                return ConvertUtils.convertByteArrayToString(mScanFilterList.get(index)
                        .get(filterIndex).getServiceDataMask());
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's service uuid
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's service uuid
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's service uuid")
    public String getScanFilterServiceUuid(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                if (mScanFilterList.get(index).get(filterIndex).getServiceUuid() != null) {
                    return mScanFilterList.get(index).get(filterIndex).getServiceUuid().toString();
                } else {
                    throw new Exception("No Service Uuid set for filter:"
                            + Integer.toString(filterIndex));
                }
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get ScanFilter's service uuid mask
     *
     * @param index the ScanFilter object to use
     * @return the ScanFilter's service uuid mask
     * @throws Exception
     */
    @Rpc(description = "Get ScanFilter's service uuid mask")
    public String getScanFilterServiceUuidMask(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "filterIndex")
            Integer filterIndex)
            throws Exception {
        if (mScanFilterList.get(index) != null) {
            if (mScanFilterList.get(index).get(filterIndex) != null) {
                if (mScanFilterList.get(index).get(filterIndex).getServiceUuidMask() != null) {
                    return mScanFilterList.get(index).get(filterIndex).getServiceUuidMask()
                            .toString();
                } else {
                    throw new Exception("No Service Uuid Mask set for filter:"
                            + Integer.toString(filterIndex));
                }
            } else {
                throw new Exception("Invalid filterIndex input:" + Integer.toString(filterIndex));
            }
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Add filter "macAddress" to existing ScanFilter
     *
     * @param macAddress the macAddress to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"macAddress\" to existing ScanFilter")
    public void setScanFilterDeviceAddress(
            @RpcParameter(name = "macAddress")
            String macAddress
            ) {
            mScanFilterBuilder.setDeviceAddress(macAddress);
    }

    /**
     * Add filter "manufacturereDataId and/or manufacturerData" to existing ScanFilter
     *
     * @param manufacturerDataId the manufacturer data id to filter against
     * @param manufacturerDataMask the manufacturere data mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"manufacturereDataId and/or manufacturerData\" to existing ScanFilter")
    public void setScanFiltermanufacturerData(
            @RpcParameter(name = "manufacturerDataId")
            Integer manufacturerDataId,
            @RpcParameter(name = "manufacturerData")
            Long manufacturerData,
            @RpcParameter(name = "manufacturerDataMask")
            @RpcOptional
            Long manufacturerDataMask
            ){
        if (manufacturerDataMask != null) {
            mScanFilterBuilder.setManufacturerData(manufacturerDataId,
                    BigInteger.valueOf(manufacturerData).toByteArray(),
                    BigInteger.valueOf(manufacturerDataMask).toByteArray());
        } else {
            mScanFilterBuilder.setManufacturerData(manufacturerDataId,
                    BigInteger.valueOf(manufacturerData).toByteArray());
        }
    }

    /**
     * Add filter "minRssi and maxRssi" to existing ScanFilter
     *
     * @param minRssi the min rssi to filter against
     * @param maxRssi the max rssi to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"minRssi and maxRssi\" to existing ScanFilter")
    public void setScanFilterRssiRange(
            @RpcParameter(name = "minRssi")
            Integer minRssi,
            @RpcParameter(name = "maxRssi")
            Integer maxRssi
            ) {
            mScanFilterBuilder.setRssiRange(minRssi, maxRssi);
    }

    /**
     * Add filter "serviceData and serviceDataMask" to existing ScanFilter
     *
     * @param serviceData the service data to filter against
     * @param serviceDataMask the servie data mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"serviceData and serviceDataMask\" to existing ScanFilter ")
    public void setScanFilterServiceData(
            @RpcParameter(name = "serviceData")
            Long serviceData,
            @RpcParameter(name = "serviceDataMask")
            @RpcOptional
            Long serviceDataMask
            ) {
        if (serviceDataMask != null) {
            mScanFilterBuilder
                    .setServiceData(BigInteger.valueOf(serviceData).toByteArray(),
                            BigInteger.valueOf(serviceDataMask).toByteArray());
        } else {
            mScanFilterBuilder.setServiceData(BigInteger.valueOf(serviceData).toByteArray());
        }
    }

    /**
     * Add filter "serviceUuid and/or serviceMask" to existing ScanFilter
     *
     * @param serviceUuid the service uuid to filter against
     * @param serviceMask the service mask to filter against
     * @throws Exception
     */
    @Rpc(description = "Add filter \"serviceUuid and/or serviceMask\" to existing ScanFilter")
    public void setScanFilterServiceUuid(
            @RpcParameter(name = "serviceUuid")
            String serviceUuid,
            @RpcParameter(name = "serviceMask")
            @RpcOptional
            String serviceMask
            ) {
            mScanFilterBuilder
                    .setServiceUuid(ParcelUuid.fromString(serviceUuid),
                            ParcelUuid.fromString(serviceMask));
    }

    /**
     * Add filter "device name" to existing ScanFilter
     *
     * @param name the device name to filter against
     * @throws Exception
     */
    @Rpc(description = "Sets the scan filter's device name")
    public void setScanFilterDeviceName(
            @RpcParameter(name = "name")
            String name
            ) {
            mScanFilterBuilder.setDeviceName(name);
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
            throw new Exception("Invalid index input:" + Integer.toString(index));
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
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("bluetooth_le_scan change onUpdate " + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onScanResult");
            mResults.putParcelable("Result", result);
            mEventFacade.postEvent(mEventType + index + "onScanResults", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d("reportResult " + mEventType + " " + index);
            mResults.putLong("Timestamp", System.currentTimeMillis() / 1000);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onBatchScanResults");
            mResults.putParcelableList("Results", results);
            mEventFacade.postEvent(mEventType + index + "onBatchScanResult", mResults.clone());
            mResults.clear();
        }

    }

    @Override
    public void shutdown() {
        if (mScanCallbackList.isEmpty() == false) {
            for (myScanCallback mScanCallback : mScanCallbackList.values()) {
                if (mScanCallback != null) {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                }
            }
        }
        mScanCallbackList.clear();
        mScanFilterList.clear();
        mScanSettingsList.clear();
    }
}
