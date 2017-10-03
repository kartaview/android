package com.telenav.osv.ui.list;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.ui.fragment.BTDialogFragment;
import com.telenav.osv.utils.Log;
import java.util.ArrayList;

/**
 * Adapter for holding devices found through scanning.
 * Created by dianat on 3/11/2016.
 */
public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final String TAG = "DeviceAdapter";

  private static final int TYPE_HEADER = 0;

  private static final int TYPE_ITEM = 1;

  private final BTDialogFragment.OnDeviceSelectedListener mListener;

  private Context mContext;

  private ArrayList<BluetoothDevice> mNewDevices;

  private ArrayList<BluetoothDevice> mDevices;

  private int mHighlightedType;

  public DeviceAdapter(Context mContext, BTDialogFragment.OnDeviceSelectedListener listener) {
    super();
    this.mContext = mContext;
    mNewDevices = new ArrayList<>();
    mDevices = new ArrayList<>();
    mListener = listener;
  }

  public void addDevice(BluetoothDevice device) {
    Log.d(TAG, "addDevice: " + device);
    if (device != null) {
      if (device.getBondState() == BluetoothDevice.BOND_BONDED || device.getBondState() == BluetoothDevice.BOND_BONDING) {
        addPairedDevice(device);
      } else {
        addNewDevice(device);
      }
    }
    notifyDataSetChanged();
  }

  public void onDeviceSearchFinished() {
    if (mNewDevices.isEmpty()) {
      addNewDevice(null);
    }
  }

  public void onDeviceListFinished() {
    if (mDevices.isEmpty()) {
      addPairedDevice(null);
    }
  }

  private void addNewDevice(BluetoothDevice device) {
    Log.d(TAG, "addNewDevice: " + device);
    if (device == null) {
      mNewDevices.add(null);
    } else if (!mNewDevices.contains(device) && device.getName() != null) {
      mNewDevices.add(device);
    }
  }

  private void addPairedDevice(BluetoothDevice device) {
    Log.d(TAG, "addPairedDevice: " + device);
    if (device == null) {
      mDevices.add(null);
    } else if (!mDevices.contains(device) && device.getName() != null) {
      mDevices.add(device);
    }
  }

  private BluetoothDevice getDeviceForGlobalIndex(int global) {
    if (global < mDevices.size() + 1 && global > 0) {
      return mDevices.get(global - 1);
    } else if (global >= mDevices.size() + 2) {
      return mNewDevices.get(global - mDevices.size() - 2);
    } else {
      return null;
    }
  }

  public void clear() {
    mDevices.clear();
    mNewDevices.clear();
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
      View view = inflater.inflate(R.layout.item_bt_device_list_header, parent, false);
      return new HeaderViewHolder(view);
    } else if (viewType == TYPE_ITEM) {
      LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
      View view = inflater.inflate(R.layout.item_bt_device_list, parent, false);
      ItemViewHolder viewHolder = new ItemViewHolder(view);
      viewHolder.deviceAddress = view.findViewById(R.id.device_address);
      viewHolder.deviceName = view.findViewById(R.id.device_name);
      return viewHolder;
    }
    return null;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof ItemViewHolder) {
      final ItemViewHolder viewHolder = (ItemViewHolder) holder;
      final BluetoothDevice device = getDeviceForGlobalIndex(position);
      String deviceName = device == null ?
          mContext.getResources().getString((position < mDevices.size() + 1 ? R.string.bt_none_paired : R.string.bt_none_found)) :
          device.getName();
      String deviceAdress = device == null ? "" : device.getAddress();
      if (deviceName != null && deviceName.length() > 0) {
        viewHolder.deviceName.setText(deviceName);
        viewHolder.deviceAddress.setText(deviceAdress);
        if (device != null && device.getType() != mHighlightedType) {
          viewHolder.deviceName.setPaintFlags(viewHolder.deviceName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
      }
      viewHolder.device = device;
      viewHolder.itemView.setOnClickListener(v -> {
        if (mListener != null && device != null) {
          mListener.onDeviceSelected(device);
        }
      });
    } else if (holder instanceof HeaderViewHolder) {
      HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
      if (position == 0) {
        ((TextView) viewHolder.itemView).setText(R.string.paired_devices_label);
      } else {
        ((TextView) viewHolder.itemView).setText(R.string.available_devices_label);
      }
      ((TextView) viewHolder.itemView).setTypeface(null, Typeface.BOLD);
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0 || position == mDevices.size() + 1) {
      return TYPE_HEADER;
    } else {
      return TYPE_ITEM;
    }
  }

  @Override
  public int getItemCount() {
    return mDevices.size() + mNewDevices.size() + 2;
  }

  public void clearNew() {
    mNewDevices.clear();
  }

  public void highlight(int type) {
    mHighlightedType = type;
  }

  static class ItemViewHolder extends RecyclerView.ViewHolder {

    TextView deviceName;

    TextView deviceAddress;

    BluetoothDevice device;

    ItemViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {

    HeaderViewHolder(View itemView) {
      super(itemView);
    }
  }
}
