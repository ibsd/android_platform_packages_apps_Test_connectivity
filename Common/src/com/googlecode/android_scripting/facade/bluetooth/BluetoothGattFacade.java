
package com.googlecode.android_scripting.facade.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

public class BluetoothGattFacade extends RpcReceiver {
    private final EventFacade mEventFacade;
    private BluetoothAdapter mBluetoothAdapter;
    private final Service mService;
    private final HashMap<Integer, myBluetoothGattCallback> mGattCallbackList;
    private final HashMap<Integer, BluetoothGatt> mBluetoothGattList;
    private final HashMap<Integer, BluetoothGattCharacteristic> mCharacteristicList;
    private final HashMap<Integer, BluetoothGattDescriptor> mDescriptorList;
    private static int GattCallbackCount;
    private static int BluetoothGattCount;
    private static int CharacteristicCount;
    private static int DescriptorCount;

    public BluetoothGattFacade(FacadeManager manager) {
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
        mGattCallbackList = new HashMap<Integer, myBluetoothGattCallback>();
        mCharacteristicList = new HashMap<Integer, BluetoothGattCharacteristic>();
        mBluetoothGattList = new HashMap<Integer, BluetoothGatt>();
        mDescriptorList = new HashMap<Integer, BluetoothGattDescriptor>();
    }

    /**
     * Create a BluetoothGatt connection
     *
     * @param index of the callback to start a connection on
     * @param macAddress the mac address of the ble device
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     *            automatically connect as soon as the remote device becomes available (true)
     * @return the index of the BluetoothGatt object
     * @throws Exception
     */
    @Rpc(description = "Create a gatt connection")
    @RpcStartEvent("GattConnect")
    public int connectGatt(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "macAddress")
            String macAddress,
            @RpcParameter(name = "autoConnect")
            Boolean autoConnect
            ) throws Exception {
        if (mGattCallbackList.get(index) != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
            BluetoothGatt mBluetoothGatt = device.connectGatt(mService.getApplicationContext(),
                    autoConnect,
                    mGattCallbackList.get(index));
            BluetoothGattCount += 1;
            mBluetoothGattList.put(BluetoothGattCount, mBluetoothGatt);
            return BluetoothGattCount;
        } else {
            throw new Exception("Invalid index input:"
                    + Integer.toString(index));
        }
    }

    /**
     * Trigger discovering of services on the BluetoothGatt object
     *
     * @param index The BluetoothGatt object index
     * @return true, if the remote service discovery has been started
     * @throws Exception
     */
    @Rpc(description = "Trigger discovering of services on the BluetoothGatt object")
    public boolean bluetoothGattDiscoverServices(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).discoverServices();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Get the services from the BluetoothGatt object
     *
     * @param index The BluetoothGatt object index
     * @return a list of BluetoothGattServices
     * @throws Exception
     */
    @Rpc(description = "Get the services from the BluetoothGatt object")
    public List<BluetoothGattService> bluetoothGattGetServices(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).getServices();
        } else {
            throw new Exception("Invalid index input:" + Integer.toString(index));
        }
    }

    /**
     * Add a characteristic to a bluetooth gatt service
     *
     * @param index the bluetooth gatt service index
     * @param serviceUuid the service Uuid to get
     * @param characteristicIndex the character index to use
     * @throws Exception
     */
    @Rpc(description = "Add a characteristic to a bluetooth gatt service")
    public void bluetoothGattAddCharacteristic(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "serviceUuid")
            String serviceUuid,
            @RpcParameter(name = "characteristicIndex")
            Integer characteristicIndex

            ) throws Exception {
        if (mBluetoothGattList.get(index) != null
                && mBluetoothGattList.get(index).getService(UUID.fromString(serviceUuid)) != null
                && mCharacteristicList.get(characteristicIndex) != null) {
            mBluetoothGattList.get(index).getService(UUID.fromString(serviceUuid))
                    .addCharacteristic(
                            mCharacteristicList.get(characteristicIndex));
        } else {
            if (mBluetoothGattList.get(index) == null) {
                throw new Exception("Invalid index input:"
                        + index);
            } else if (mCharacteristicList.get(characteristicIndex) == null) {
                throw new Exception("Invalid characteristicIndex input:"
                        + characteristicIndex);
            } else {
                throw new Exception("Invalid serviceUuid input:" + serviceUuid);
            }
        }
    }

    /**
     * Abort reliable write of a bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @throws Exception
     */
    @Rpc(description = "Abort reliable write of a bluetooth gatt")
    public void bluetoothGattAbortReliableWrite(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            mBluetoothGattList.get(index).abortReliableWrite();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Begin reliable write of a bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @return
     * @throws Exception
     */
    @Rpc(description = "Begin reliable write of a bluetooth gatt")
    public boolean bluetoothGattBeginReliableWrite(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).beginReliableWrite();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Close a bluetooth gatt
     *
     * @param index the bluetooth gatt index to close
     * @throws Exception
     */
    @Rpc(description = "Close a bluetooth gatt")
    public void bluetoothGattClose(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            mBluetoothGattList.get(index).close();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Configure a bluetooth gatt's MTU
     *
     * @param index the bluetooth gatt index
     * @param mtu the MTU to set
     * @return
     * @throws Exception
     */
    @Rpc(description = "true, if the new MTU value has been requested successfully")
    public boolean bluetoothGattConfigureMTU(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "mtu")
            Integer mtu
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).requestMtu(mtu);
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Disconnect a bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @throws Exception
     */
    @Rpc(description = "Disconnect a bluetooth gatt")
    @RpcStopEvent("GattConnect")
    public void bluetoothGattDisconnect(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            mBluetoothGattList.get(index).disconnect();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Execute reliable write on a bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @return true, if the request to execute the transaction has been sent
     * @throws Exception
     */
    @Rpc(description = "Execute reliable write on a bluetooth gatt")
    public boolean bluetoothGattExecuteReliableWrite(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).executeReliableWrite();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Get a list of Bluetooth Devices connnected to the bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @return List of BluetoothDevice Objects
     * @throws Exception
     */
    @Rpc(description = "Get a list of Bluetooth Devices connnected to the bluetooth gatt")
    public List<BluetoothDevice> bluetoothGattGetConnectedDevices(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).getConnectedDevices();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Get the remote bluetooth device this GATT client targets to
     *
     * @param index the bluetooth gatt index
     * @return the remote bluetooth device this gatt client targets to
     * @throws Exception
     */
    @Rpc(description = "Get the remote bluetooth device this GATT client targets to")
    public BluetoothDevice bluetoothGattGetDevice(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).getDevice();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Get the bluetooth devices matching input connection states
     *
     * @param index the bluetooth gatt index
     * @param states the list of states to match
     * @return The list of BluetoothDevice objects that match the states
     * @throws Exception
     */
    @Rpc(description = "Get the bluetooth devices matching input connection states")
    public List<BluetoothDevice> bluetoothGattGetDevicesMatchingConnectionStates(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "states")
            int[] states
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).getDevicesMatchingConnectionStates(states);
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Get the service from an input UUID
     *
     * @param index the bluetooth gatt index
     * @param uuid the String uuid that matches the service
     * @return BluetoothGattService related to the bluetooth gatt
     * @throws Exception
     */
    @Rpc(description = "Get the service from an input UUID")
    public ArrayList<String> bluetoothGattGetServiceUuidList(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            ArrayList<String> serviceUuidList = new ArrayList<String>();
            for (BluetoothGattService service : mBluetoothGattList.get(index).getServices()) {
                serviceUuidList.add(service.getUuid().toString());
            }
            return serviceUuidList;
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Get the service from an input UUID
     *
     * @param index the bluetooth gatt index
     * @param uuid the String uuid that matches the service
     * @return BluetoothGattService related to the bluetooth gatt
     * @throws Exception
     */
    @Rpc(description = "Get the service from an input UUID")
    public BluetoothGattService bluetoothGattGetService(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "uuid")
            String uuid
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).getService(UUID.fromString(uuid));
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     *
     * @param index the bluetooth gatt index
     * @param characteristicIndex the characteristic index
     * @return true, if the read operation was initiated successfully
     * @throws Exception
     */
    @Rpc(description = "Reads the requested characteristic from the associated remote device.")
    public boolean bluetoothGattReadCharacteristic(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "characteristicIndex")
            Integer characteristicIndex
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            if (mCharacteristicList.get(characteristicIndex) != null) {
                return mBluetoothGattList.get(index).readCharacteristic(
                        mCharacteristicList.get(characteristicIndex));
            } else {
                throw new Exception("Invalid characteristicIndex input:"
                        + characteristicIndex);
            }
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param index the bluetooth gatt index
     * @param characteristicIndex the characteristic index
     * @return true, if the write operation was initiated successfully
     * @throws Exception
     */
    @Rpc(description = "Writes a given characteristic and its values to the associated remote device.")
    public boolean bluetoothGattWriteCharacteristic(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "characteristicIndex")
            Integer characteristicIndex
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            if (mCharacteristicList.get(characteristicIndex) != null) {
                return mBluetoothGattList.get(index).writeCharacteristic(
                        mCharacteristicList.get(characteristicIndex));
            } else {
                throw new Exception("Invalid characteristicIndex input:"
                        + characteristicIndex);
            }
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Reads the value for a given descriptor from the associated remote device
     *
     * @param index the bluetooth gatt index
     * @param descriptorIndex the descriptor index
     * @return true, if the read operation was initiated successfully
     * @throws Exception
     */
    @Rpc(description = "Reads the value for a given descriptor from the associated remote device")
    public boolean bluetoothGattReadDescriptor(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "descriptorIndex")
            Integer descriptorIndex
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            if (mDescriptorList.get(descriptorIndex) != null) {
                return mBluetoothGattList.get(index).readDescriptor(
                        mDescriptorList.get(descriptorIndex));
            } else {
                throw new Exception("Invalid descriptorIndex input:"
                        + descriptorIndex);
            }
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Write the value of a given descriptor to the associated remote device
     *
     * @param index the bluetooth gatt index
     * @param descriptorIndex the descriptor index
     * @return true, if the write operation was initiated successfully
     * @throws Exception
     */
    @Rpc(description = "Write the value of a given descriptor to the associated remote device")
    public boolean bluetoothGattWriteDescriptor(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "descriptorIndex")
            Integer descriptorIndex
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            if (mDescriptorList.get(descriptorIndex) != null) {
                return mBluetoothGattList.get(index).writeDescriptor(
                        mDescriptorList.get(descriptorIndex));
            } else {
                throw new Exception("Invalid descriptorIndex input:" + descriptorIndex);
            }
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Read the RSSI for a connected remote device
     *
     * @param index the bluetooth gatt index
     * @return true, if the RSSI value has been requested successfully
     * @throws Exception
     */
    @Rpc(description = "Read the RSSI for a connected remote device")
    public boolean bluetoothGattReadRSSI(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).readRemoteRssi();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the remote device
     *
     * @param index the bluetooth gatt index
     * @return Clears the internal cache and forces a refresh of the services from the remote
     *         device.
     * @throws Exception
     */
    @Rpc(description = "Clears the internal cache and forces a refresh of the services from the remote device")
    public boolean bluetoothGattRefresh(
            @RpcParameter(name = "index")
            Integer index
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            return mBluetoothGattList.get(index).refresh();
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Request a connection parameter update.
     * @param index the bluetooth gatt index
     * @param connectionParameterUpdateRequest connection priority
     * @return boolean True if successful False otherwise.
     * @throws Exception
     */
    @Rpc(description = "Request a connection parameter update. from the Bluetooth Gatt")
    public boolean bluetoothGattRequestConnectionParameterUpdate(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "connectionParameterUpdateRequest")
            Integer connectionPriority
            ) throws Exception {
        boolean result = false;
        if (mBluetoothGattList.get(index) != null) {
            result = mBluetoothGattList.get(index).requestConnectionPriority(connectionPriority);
        } else {
            throw new Exception("Invalid index input:" + index);
        }
        return result;
    }

    /**
     * Sets the characteristic notification of a bluetooth gatt
     *
     * @param index the bluetooth gatt index
     * @param characteristicIndex the characteristic index
     * @param enable Enable or disable notifications/indications for a given characteristic
     * @return true, if the requested notification status was set successfully
     * @throws Exception
     */
    @Rpc(description = "Sets the characteristic notification of a bluetooth gatt")
    public boolean bluetoothGattSetCharacteristicNotification(
            @RpcParameter(name = "index")
            Integer index,
            @RpcParameter(name = "characteristicIndex")
            Integer characteristicIndex,
            @RpcParameter(name = "enable")
            Boolean enable
            ) throws Exception {
        if (mBluetoothGattList.get(index) != null) {
            if (mCharacteristicList.get(characteristicIndex) != null) {
                return mBluetoothGattList.get(index).setCharacteristicNotification(
                        mCharacteristicList.get(characteristicIndex), enable);
            } else {
                throw new Exception("Invalid characteristicIndex input:"
                        + characteristicIndex);
            }
        } else {
            throw new Exception("Invalid index input:" + index);
        }
    }

    /**
     * Create a new Characteristic object
     *
     * @param characteristicUuid uuid The UUID for this characteristic
     * @param property Properties of this characteristic
     * @param permission permissions Permissions for this characteristic
     * @return
     */
    @Rpc(description = "Create a new Characteristic object")
    public int createBluetoothGattCharacteristic(
            @RpcParameter(name = "characteristicUuid")
            String characteristicUuid,
            @RpcParameter(name = "property")
            Integer property,
            @RpcParameter(name = "permission")
            Integer permission
            ) {
        CharacteristicCount += 1;
        int index = CharacteristicCount;
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(characteristicUuid), property, permission);
        mCharacteristicList.put(index, characteristic);
        return index;
    }

    /**
     * Create a new GattCallback object
     *
     * @return the index of the callback object
     */
    @Rpc(description = "Create a new GattCallback object")
    public Integer createGattCallback() {
        GattCallbackCount += 1;
        int index = GattCallbackCount;
        mGattCallbackList.put(index, new myBluetoothGattCallback(index));
        return index;
    }

    /**
     * Create a new Descriptor object
     *
     * @param descriptorUuid the UUID for this descriptor
     * @param permissions Permissions for this descriptor
     * @return the index of the Descriptor object
     */
    @Rpc(description = "Create a new Descriptor object")
    public int createBluetoothGattDescriptor(
            @RpcParameter(name = "descriptorUuid")
            String descriptorUuid,
            @RpcParameter(name = "permissions")
            Integer permissions
            ) {
        DescriptorCount += 1;
        int index = DescriptorCount;
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                UUID.fromString(descriptorUuid), permissions);
        mDescriptorList.put(index, descriptor);
        return index;
    }

    private class myBluetoothGattCallback extends BluetoothGattCallback {
        private final Bundle mResults;
        private final int index;
        private final String mEventType;

        public myBluetoothGattCallback(int idx) {
            mResults = new Bundle();
            mEventType = "GattConnect";
            index = idx;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            Log.d("gatt_connect change onConnectionStateChange " + mEventType + " " + index);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("State Connected to mac address "
                        + gatt.getDevice().getAddress() + " status " + status);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("State Disconnected from mac address "
                        + gatt.getDevice().getAddress() + " status " + status);
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.d("State Connecting to mac address "
                        + gatt.getDevice().getAddress() + " status " + status);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.d("State Disconnecting from mac address "
                        + gatt.getDevice().getAddress() + " status " + status);
            }
            mResults.putString("Type", "onConnectionStateChange");
            mResults.putInt("Status", status);
            mResults.putInt("State", newState);
            mEventFacade
                    .postEvent(mEventType + index + "onConnectionStateChange", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("gatt_connect change onServicesDiscovered " + mEventType + " " + index);
            mResults.putString("Type", "onServicesDiscovered");
            mResults.putInt("Status", status);
            mEventFacade
                    .postEvent(mEventType + index + "onServicesDiscovered", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            Log.d("gatt_connect change onCharacteristicRead " + mEventType + " " + index);
            mResults.putString("Type", "onCharacteristicRead");
            mResults.putInt("Status", status);
            mResults.putString("CharacteristicUuid", characteristic.getUuid().toString());
            mEventFacade
                    .postEvent(mEventType + index + "onCharacteristicRead", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Log.d("gatt_connect change onCharacteristicWrite " + mEventType + " " + index);
            mResults.putString("Type", "onCharacteristicWrite");
            mResults.putInt("Status", status);
            mResults.putString("CharacteristicUuid", characteristic.getUuid().toString());
            mEventFacade
                    .postEvent(mEventType + index + "onCharacteristicWrite", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            Log.d("gatt_connect change onCharacteristicChanged " + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onCharacteristicChanged");
            mResults.putString("CharacteristicUuid", characteristic.getUuid().toString());
            mEventFacade
                    .postEvent(mEventType + index + "onCharacteristicChanged", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            Log.d("gatt_connect change onServicesDiscovered " + mEventType + " " + index);
            mResults.putString("Type", "onServicesDiscovered");
            mResults.putInt("Status", status);
            mResults.putString("DescriptorUuid", descriptor.getUuid().toString());
            mEventFacade
                    .postEvent(mEventType + index + "onDescriptorRead", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            Log.d("gatt_connect change onDescriptorWrite " + mEventType + " " + index);
            mResults.putInt("ID", index);
            mResults.putString("Type", "onDescriptorWrite");
            mResults.putInt("Status", status);
            mResults.putString("DescriptorUuid", descriptor.getUuid().toString());
            mEventFacade
                    .postEvent(mEventType + index + "onDescriptorWrite", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d("gatt_connect change onReliableWriteCompleted " + mEventType + " "
                    + index);
            mResults.putString("Type", "onReliableWriteCompleted");
            mResults.putInt("Status", status);
            mEventFacade
                    .postEvent(mEventType + index + "onReliableWriteCompleted", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d("gatt_connect change onReadRemoteRssi " + mEventType + " " + index);
            mResults.putString("Type", "onReadRemoteRssi");
            mResults.putInt("Status", status);
            mResults.putInt("Rssi", rssi);
            mEventFacade
                    .postEvent(mEventType + index + "onReadRemoteRssi", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d("gatt_connect change onMtuChanged " + mEventType + " " + index);
            mResults.putString("Type", "onConfigureMTU");
            mResults.putInt("Status", status);
            mResults.putInt("MTU", mtu);
            mEventFacade
                    .postEvent(mEventType + index + "onConfigureMTU", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onConnectionCongested(BluetoothGatt gatt, boolean congested) {
            Log.d("gatt_connect change onConnectionCongested " + mEventType + " " + index);
            mResults.putString("Type", "onConnectionCongested");
            mResults.putBoolean("Congested", congested);
            mEventFacade
                    .postEvent(mEventType + index + "onConnectionCongested", mResults.clone());
            mResults.clear();
        }
    }

    @Override
    public void shutdown() {
        if (!mBluetoothGattList.isEmpty()) {
            if (mBluetoothGattList.values() != null) {
                for (BluetoothGatt mBluetoothGatt : mBluetoothGattList.values()) {
                    mBluetoothGatt.close();
                }
            }
        }
        mGattCallbackList.clear();
    }
}
