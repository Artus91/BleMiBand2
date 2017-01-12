package com.example.artus.ble_immediatealert;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements EditNameDialogListener, FirstFragment.FragmentListener {
    private static String TAG = MainActivity.class.toString();
    private static UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    /* Views */
    @ViewById(R.id.label_main)
    TextView mLabel;

    @ViewById(R.id.btn_search)
    Button mSearchBtn;

    @ViewById(R.id.btn_battery_level)
    Button mBatteryLevelBtn;

    @ViewById(R.id.btn_connect)
    Button mConnectBtn;

    @ViewById(R.id.btn_trigger_alert)
    Button mTriggerAlertBtn;

    @ViewById(R.id.vpPager)
    ViewPager vpPager;

    private FragmentPagerAdapter mPagerAdapter;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothDevice mBluetoothDevice;

    List<BluetoothGattService> mServices = new ArrayList<>();
    List<BluetoothGattCharacteristic> mCharacteristics = new ArrayList<>();

    @Click(R.id.btn_search)
    void handleSearch() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            scanLeDevice(true);
        }
    }

    @Click(R.id.btn_connect)
    void handleConnect() {
        if (mBluetoothDevice != null) {
            mGatt = mBluetoothDevice.connectGatt(this, true, new GatListener(mListener));
            List<BluetoothGattService> services = mGatt.getServices();
            for (BluetoothGattService item : services) {
                String description = String.format("uuid %s", item.getUuid());
                Log.d(TAG, String.format("handleConnect, %s", description));
            }
        }
    }

    @Click(R.id.btn_trigger_alert)
    void handleTriggerAlert() {
        BluetoothGattCharacteristic ch = Iterables.find(mCharacteristics, new Predicate<BluetoothGattCharacteristic>() {
            @Override
            public boolean apply(BluetoothGattCharacteristic input) {
                return input.getUuid().equals(ALERT_LEVEL_CHARACTERISTIC);
            }
        });
        if (ch != null) {
            ch.setValue(new byte[]{02});
            mGatt.writeCharacteristic(ch);
        } else {
            showMessage("The immediate service not found");
        }
    }

    @UiThread
    void showMessage(String aText) {
        Toast.makeText(this, aText, Toast.LENGTH_SHORT).show();
    }


    @Click(R.id.btn_battery_level)
    void handleBatteryLevel() {
        //do nothing
    }

    @AfterViews
    public void init() {
        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(mPagerAdapter);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage("Ble not support");
        }

        vpPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
                // Selected page position
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Code goes here
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
                // Code goes here
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, String.format("onCreated %s",
                savedInstanceState == null ? " save is null" : "not null"));
        mHandler = new Handler(Looper.getMainLooper());
    }

    private final GatListener.CharacterisListener mListener = new GatListener.CharacterisListener() {
        public void onStored(BluetoothGatt gatt, BluetoothGattService aCh) {
            mGatt = gatt;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTriggerAlertBtn.setEnabled(true);
                }
            });
        }

        @Override
        public void onAddServices(final List<BluetoothGattService> aServices) {
            mServices.addAll(aServices);
            final List data = Lists.transform(aServices, new Function<BluetoothGattService, String>() {
                @Override
                public String apply(BluetoothGattService input) {
                    return input.getUuid().toString();
                }
            });
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Fragment fr = mPagerAdapter.getItem(1);
                    FirstFragment_ first = (FirstFragment_) fr;
                    first.addData(data);
                    ;
                }
            });

        }

        @Override
        public void onAddCharacteristic(List<BluetoothGattCharacteristic> aCharacteristic) {
            mCharacteristics.addAll(aCharacteristic);

            //stores uuids
            final List data = Lists.transform(aCharacteristic, new Function<BluetoothGattCharacteristic, String>() {
                @Override
                public String apply(BluetoothGattCharacteristic input) {
                    return input.getUuid().toString();
                }
            });
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Fragment fr = mPagerAdapter.getItem(0);
                    FirstFragment_ first = (FirstFragment_) fr;
                    first.addData(data);
                    if (data.contains(ALERT_LEVEL_CHARACTERISTIC.toString())) {
                        mTriggerAlertBtn.setEnabled(true);
                    }
                }
            });

        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mStopedLeCallBack);
                }
            }, 10000);

            mBluetoothAdapter.startLeScan(mStartedLeCallBack);
        } else {
            mBluetoothAdapter.stopLeScan(mStopedLeCallBack);
        }
    }

    @UiThread
    void onDeviceFound(BluetoothDevice aDevice) {
        mLabel.setBackgroundColor(Color.GREEN);
        mLabel.setText(aDevice.getAddress());
        mConnectBtn.setEnabled(true);
        mBluetoothDevice = aDevice;
    }

    private final BluetoothAdapter.LeScanCallback mStopedLeCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice aBluetoothDevice, int i, byte[] bytes) {
            String address = aBluetoothDevice.getName();
            Log.d(TAG, String.format("mStopedLeCallBack, address %s", address));
        }
    };

    private final BluetoothAdapter.LeScanCallback mStartedLeCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice aBluetoothDevice, int i, byte[] bytes) {
            String address = aBluetoothDevice.getName();
            Log.d(TAG, String.format("mStartedLeCallBack, address %s", address));
            if (address.contains("Band")) {
                mBluetoothAdapter.stopLeScan(null);
                onDeviceFound(aBluetoothDevice);
            }
        }
    };

    @Override
    public void onValueChanged(final String aUuid, String aValue) {
        BluetoothGattCharacteristic characteristic = Iterables.find(mCharacteristics, new Predicate<BluetoothGattCharacteristic>() {
            public boolean apply(BluetoothGattCharacteristic uuid) {
                return uuid.getUuid().equals(UUID.fromString(aUuid));
            }
        });
        Integer data = Integer.parseInt(aValue, 16);
        final BigInteger bi = BigInteger.valueOf(data);
        final byte[] bytes = bi.toByteArray();
        characteristic.setValue(bytes);
        mGatt.writeCharacteristic(characteristic);
    }

    @Override
    public void handleItemClick(final UUID aUUID) {
        //find in characteristic
        BluetoothGattCharacteristic ch = Iterables.find(mCharacteristics, new Predicate<BluetoothGattCharacteristic>() {
            @Override
            public boolean apply(BluetoothGattCharacteristic input) {
                return input.getUuid().equals(aUUID);
            }
        }, null);

        if (ch == null) {
            BluetoothGattService service = Iterables.find(mServices, new Predicate<BluetoothGattService>() {
                @Override
                public boolean apply(BluetoothGattService input) {
                    return input.getUuid().equals(aUUID);
                }
            }, null);
            String message =
                    service != null ? "The service are selected" : "Such element not found";
            showMessage(message);
        } else {
            DialogFragment dialog = EditNameDialogFragment.newInstance(ch);
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    public static class MyPagerAdapter extends FragmentPagerAdapter {
        private final List<FirstFragment> NUM_ITEMS;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            NUM_ITEMS = new ArrayList<>(2);
            NUM_ITEMS.add(FirstFragment_.builder().mTitle("Characteristics").build());
            NUM_ITEMS.add(FirstFragment_.builder().mTitle("Services").build());

        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return NUM_ITEMS.get(position);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return NUM_ITEMS.get(position).mTitle;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState " + (outState == null));
        List list = Lists.transform(mCharacteristics, new Function<BluetoothGattCharacteristic, String>() {

            @Override
            public String apply(BluetoothGattCharacteristic input) {
                return input.getUuid().toString();
            }
        });
        outState.putStringArrayList("array", new ArrayList<>(list));

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState " + (savedInstanceState == null));
        if (savedInstanceState != null) {
            Fragment fr = mPagerAdapter.getItem(0);
            FirstFragment_ first = (FirstFragment_) fr;
            showMessage("RestoreInstance" + savedInstanceState.getStringArrayList("array").get(0));

        }

    }
}

