package com.example.bond_light;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.Toast;

public class DeviceFinder {

    public interface DeviceSearchCallback {
        void onDeviceNotFound(); // Викликається, коли пристрій не знайдено
        void onDeviceFound(String ip); // Викликається, коли пристрій знайдено
    }

    private Context context;
    private DeviceSearchCallback callback;
    private static final String TAG = "DeviceFinder";
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private volatile boolean deviceFound = false;
    private int remainingRequests;

    public DeviceFinder(Context context, DeviceSearchCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void findDeviceInNetwork(final String baseIp, final int rangeEnd) {
        remainingRequests = rangeEnd - 1;
        for (int i = 2; i <= rangeEnd; i++) {
            final String targetIp = baseIp + i;
            if (deviceFound) {
                break;
            }
            executorService.submit(() -> {
                searchDevice(targetIp);
                onTaskCompleted();
            });
        }
    }

    private void searchDevice(String targetIp) {
        try {
            URL url = new URL("http://" + targetIp + "/question");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(2000);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                if (response.toString().contains("HELLO")) {
                    deviceFound = true;
                    saveDeviceIp(targetIp);
                    if (callback != null) {
                        callback.onDeviceFound(targetIp); // Виводимо колбек для успішного пошуку
                    }
                    navigateToMainActivity();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reaching " + targetIp, e);
        }
    }

    private synchronized void onTaskCompleted() {
        remainingRequests--;
        if (remainingRequests == 0 && !deviceFound) {
            if (callback != null) {
                callback.onDeviceNotFound(); // Виводимо колбек для неуспішного пошуку
            }
        }
    }

    private void saveDeviceIp(String ip) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("device_ip", ip);
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}

