package com.dualearbuds.connect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private static final String PREFS = "DualEarbuds";
    private static final String KEY_MAC1 = "mac1";
    private static final String KEY_NAME1 = "name1";
    private static final String KEY_MAC2 = "mac2";
    private static final String KEY_NAME2 = "name2";

    private BluetoothAdapter btAdapter;
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvDevice1, tvDevice2, tvStatus1, tvStatus2;
    private TextView tvLog, tvDualStatus;
    private LinearLayout scanResultsContainer, scanResultsList;

    private int currentScanTarget = 0; // 1 or 2
    private List<BluetoothDevice> scannedDevices = new ArrayList<>();

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !scannedDevices.contains(device)) {
                    scannedDevices.add(device);
                    addScanResult(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                log("Scan complete. Found " + scannedDevices.size() + " devices.");
                if (scannedDevices.isEmpty()) {
                    log("No new devices found. Showing paired devices instead.");
                    showPairedDevices();
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                if (state == BluetoothDevice.BOND_BONDED && device != null) {
                    log("✓ Paired: " + getDeviceName(device));
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    log("✓ Connected: " + getDeviceName(device));
                    updateConnectionStatus();
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    log("Disconnected: " + getDeviceName(device));
                    updateConnectionStatus();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        tvDevice1 = findViewById(R.id.tv_device1);
        tvDevice2 = findViewById(R.id.tv_device2);
        tvStatus1 = findViewById(R.id.tv_status1);
        tvStatus2 = findViewById(R.id.tv_status2);
        tvLog = findViewById(R.id.tv_log);
        tvDualStatus = findViewById(R.id.tv_dual_status);
        scanResultsContainer = findViewById(R.id.scan_results_container);
        scanResultsList = findViewById(R.id.scan_results_list);

        registerReceivers();
        requestPermissions();
        loadSavedDevices();
        checkDualAudioStatus();
        updateConnectionStatus();
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiver, filter);
    }

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            perms = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_REQUEST);
        }
    }

    private void loadSavedDevices() {
        String mac1 = prefs.getString(KEY_MAC1, null);
        String name1 = prefs.getString(KEY_NAME1, "No device saved");
        String mac2 = prefs.getString(KEY_MAC2, null);
        String name2 = prefs.getString(KEY_NAME2, "No device saved");

        tvDevice1.setText(mac1 != null ? name1 + "\n" + mac1 : "No device saved");
        tvDevice2.setText(mac2 != null ? name2 + "\n" + mac2 : "No device saved");
    }

    private void checkDualAudioStatus() {
        try {
            int val = Settings.Global.getInt(getContentResolver(), "bluetooth_a2dp_sink_allow_multiple", 0);
            if (val == 1) {
                tvDualStatus.setText("✅ Dual Audio ENABLED");
                tvDualStatus.setTextColor(0xFF4CAF50);
            } else {
                tvDualStatus.setText("❌ Dual Audio DISABLED");
                tvDualStatus.setTextColor(0xFFFF5252);
            }
        } catch (Exception e) {
            tvDualStatus.setText("⚡ Status unknown — tap Enable");
            tvDualStatus.setTextColor(0xFFFFB300);
        }
    }

    public void enableDualAudio(View v) {
        // Try to enable programmatically (requires WRITE_SECURE_SETTINGS — granted via ADB or system)
        try {
            Settings.Global.putInt(getContentResolver(), "bluetooth_a2dp_sink_allow_multiple", 1);
            Settings.Global.putInt(getContentResolver(), "bluetooth_audio_sharing_enabled", 1);
            checkDualAudioStatus();
            log("✅ Dual Audio enabled!");
            toast("Dual Audio enabled!");
        } catch (SecurityException se) {
            // If no permission, guide user to Developer Options
            log("Auto-enable failed — opening Developer Options. Look for 'Disable Bluetooth A2DP hardware offload' or 'Enable dual audio'.");
            new AlertDialog.Builder(this)
                    .setTitle("Enable Dual Audio Manually")
                    .setMessage(
                            "Steps to enable Dual Audio on Moto G62 5G:\n\n" +
                            "1. Go to Settings → About Phone\n" +
                            "2. Tap 'Build Number' 7 times to unlock Developer Options\n" +
                            "3. Go to Settings → Developer Options\n" +
                            "4. Enable 'Disable Bluetooth A2DP hardware offload'\n" +
                            "   OR look for 'Dual Audio' toggle\n" +
                            "5. Restart Bluetooth and connect both earbuds\n\n" +
                            "Opening Developer Options now...")
                    .setPositiveButton("Open Dev Options", (d, w) -> openDevOptions(null))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    public void openBtSettings(View v) {
        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        log("Opened Bluetooth settings. Pair your earbuds there first.");
    }

    public void openDevOptions(View v) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
        log("Opened Developer Options. Look for 'Dual Audio' or 'Disable A2DP hardware offload'.");
    }

    // ── SCAN ──────────────────────────────────────────────────────
    public void scanForDevice1(View v) { startScan(1); }
    public void scanForDevice2(View v) { startScan(2); }

    private void startScan(int target) {
        if (btAdapter == null) { toast("Bluetooth not available"); return; }
        if (!btAdapter.isEnabled()) { toast("Please turn on Bluetooth first"); return; }

        currentScanTarget = target;
        scannedDevices.clear();
        scanResultsList.removeAllViews();
        scanResultsContainer.setVisibility(View.VISIBLE);
        log("Scanning for Bluetooth devices (slot " + target + ")...\nMake sure your earbud is in pairing mode!");

        // First show already-paired devices
        showPairedDevices();

        // Then start discovery for new ones
        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                btAdapter.startDiscovery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                btAdapter.startDiscovery();
            } else {
                requestPermissions();
            }
        }
    }

    private void showPairedDevices() {
        if (btAdapter == null) return;
        try {
            Set<BluetoothDevice> paired = btAdapter.getBondedDevices();
            if (paired != null) {
                for (BluetoothDevice d : paired) {
                    if (!scannedDevices.contains(d)) {
                        scannedDevices.add(d);
                        addScanResult(d);
                    }
                }
                if (!paired.isEmpty()) {
                    log("Showing " + paired.size() + " paired device(s). Scanning for new ones...");
                }
            }
        } catch (SecurityException e) {
            log("Permission needed to list devices.");
        }
    }

    private void addScanResult(BluetoothDevice device) {
        runOnUiThread(() -> {
            String name = getDeviceName(device);
            String mac = device.getAddress();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 14, 12, 14);
            row.setBackgroundColor(0xFFF8F8FF);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 6);
            row.setLayoutParams(rowParams);

            TextView tvInfo = new TextView(this);
            tvInfo.setText("🎧 " + name + "\n" + mac);
            tvInfo.setTextSize(12);
            tvInfo.setTextColor(0xFF333355);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button btnSel = new Button(this);
            btnSel.setText("Save to slot " + currentScanTarget);
            btnSel.setTextSize(11);
            btnSel.setTextColor(0xFFFFFFFF);
            btnSel.setBackgroundColor(0xFF3949AB);
            btnSel.setPadding(16, 10, 16, 10);
            btnSel.setOnClickListener(vv -> saveDevice(currentScanTarget, device));

            row.addView(tvInfo);
            row.addView(btnSel);
            scanResultsList.addView(row);
        });
    }

    private void saveDevice(int slot, BluetoothDevice device) {
        String name = getDeviceName(device);
        String mac = device.getAddress();
        SharedPreferences.Editor ed = prefs.edit();
        if (slot == 1) {
            ed.putString(KEY_MAC1, mac);
            ed.putString(KEY_NAME1, name);
            tvDevice1.setText(name + "\n" + mac);
            tvStatus1.setText("Saved ✓");
            tvStatus1.setTextColor(0xFF4CAF50);
        } else {
            ed.putString(KEY_MAC2, mac);
            ed.putString(KEY_NAME2, name);
            tvDevice2.setText(name + "\n" + mac);
            tvStatus2.setText("Saved ✓");
            tvStatus2.setTextColor(0xFF4CAF50);
        }
        ed.apply();
        scanResultsContainer.setVisibility(View.GONE);
        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        log("✅ Saved " + name + " to slot " + slot);
        toast("Saved: " + name);
    }

    // ── CONNECT ───────────────────────────────────────────────────
    public void connectDevice1(View v) { connectSaved(1); }
    public void connectDevice2(View v) { connectSaved(2); }

    public void connectBoth(View v) {
        String mac1 = prefs.getString(KEY_MAC1, null);
        String mac2 = prefs.getString(KEY_MAC2, null);
        if (mac1 == null && mac2 == null) {
            toast("Save at least one device first!");
            return;
        }
        log("Connecting both earbuds...");
        if (mac1 != null) {
            connectByMac(mac1, prefs.getString(KEY_NAME1, "Earbud 1"));
        }
        handler.postDelayed(() -> {
            if (mac2 != null) {
                connectByMac(mac2, prefs.getString(KEY_NAME2, "Earbud 2"));
            }
        }, 2500); // slight delay so both connections don't race
    }

    private void connectSaved(int slot) {
        String mac = prefs.getString(slot == 1 ? KEY_MAC1 : KEY_MAC2, null);
        String name = prefs.getString(slot == 1 ? KEY_NAME1 : KEY_NAME2, "Earbud " + slot);
        if (mac == null) {
            toast("No device saved for slot " + slot + ". Tap Scan first.");
            return;
        }
        connectByMac(mac, name);
    }

    private void connectByMac(String mac, String name) {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            toast("Bluetooth is off");
            return;
        }
        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(mac);
            log("Connecting to " + name + "...");

            // Connect via A2DP profile using reflection
            connectA2dp(device);

        } catch (Exception e) {
            log("Error connecting to " + name + ": " + e.getMessage());
            log("→ Try connecting manually in Bluetooth Settings");
        }
    }

    private void connectA2dp(BluetoothDevice device) {
        btAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                try {
                    Method connect = proxy.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
                    connect.setAccessible(true);
                    connect.invoke(proxy, device);
                    log("→ Connect signal sent to " + getDeviceName(device));
                    handler.postDelayed(() -> {
                        btAdapter.closeProfileProxy(profile, proxy);
                        updateConnectionStatus();
                    }, 3000);
                } catch (Exception e) {
                    log("A2DP connect error: " + e.getMessage());
                    log("→ Please connect manually from Bluetooth settings");
                    btAdapter.closeProfileProxy(profile, proxy);
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {}
        }, BluetoothProfile.A2DP);
    }

    // ── STATUS UPDATE ─────────────────────────────────────────────
    private void updateConnectionStatus() {
        String mac1 = prefs.getString(KEY_MAC1, null);
        String mac2 = prefs.getString(KEY_MAC2, null);

        if (btAdapter == null) return;

        btAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                List<BluetoothDevice> connected = proxy.getConnectedDevices();
                runOnUiThread(() -> {
                    if (mac1 != null) {
                        boolean conn1 = isConnected(connected, mac1);
                        tvStatus1.setText(conn1 ? "🟢 Connected" : "⚫ Not connected");
                        tvStatus1.setTextColor(conn1 ? 0xFF4CAF50 : 0xFF888888);
                    }
                    if (mac2 != null) {
                        boolean conn2 = isConnected(connected, mac2);
                        tvStatus2.setText(conn2 ? "🟢 Connected" : "⚫ Not connected");
                        tvStatus2.setTextColor(conn2 ? 0xFF4CAF50 : 0xFF888888);
                    }
                });
                btAdapter.closeProfileProxy(profile, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {}
        }, BluetoothProfile.A2DP);

        checkDualAudioStatus();
    }

    private boolean isConnected(List<BluetoothDevice> devices, String mac) {
        for (BluetoothDevice d : devices) {
            if (d.getAddress().equalsIgnoreCase(mac)) return true;
        }
        return false;
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private String getDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return (name != null && !name.isEmpty()) ? name : device.getAddress();
        } catch (SecurityException e) {
            return device.getAddress();
        }
    }

    private void log(String msg) {
        runOnUiThread(() -> {
            String current = tvLog.getText().toString();
            String newText = msg + "\n──────────────\n" + current;
            if (newText.length() > 1200) newText = newText.substring(0, 1200);
            tvLog.setText(newText);
        });
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST) {
            boolean allGranted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            if (allGranted) {
                log("✅ Permissions granted");
                loadSavedDevices();
                updateConnectionStatus();
            } else {
                log("⚠️ Some permissions denied — Bluetooth features may not work fully");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
        checkDualAudioStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(btReceiver);
        } catch (Exception ignored) {}
        if (btAdapter != null && btAdapter.isDiscovering()) {
            try { btAdapter.cancelDiscovery(); } catch (Exception ignored) {}
        }
    }
}
