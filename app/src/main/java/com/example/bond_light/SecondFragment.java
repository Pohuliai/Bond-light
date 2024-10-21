package com.example.bond_light;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bond_light.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment implements DeviceFinder.DeviceSearchCallback {
    private FragmentSecondBinding binding;
    private EditText editTextDeviceIP;
    private EditText editTextRouterIP;
    private EditText editTextDevicesAmount;
    private Button buttonLocalControl;
    private Button buttonSearchDevice;
    private String deviceIp;
    private String routerIp;
    private String devicesAmount;
    private int amount;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.refreshButton != null) {
            mainActivity.refreshButton.setVisibility(View.GONE);
        }

        editTextDeviceIP = view.findViewById(R.id.editTextDeviceIP);
        editTextRouterIP = view.findViewById(R.id.editTextRouterIP);
        editTextDevicesAmount = view.findViewById(R.id.editTextDevicesAmount);
        buttonLocalControl = view.findViewById(R.id.buttonLocalControl);
        buttonSearchDevice = view.findViewById(R.id.buttonSearchDevice);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textView = view.findViewById(R.id.progressBarLabel);
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        loadSettings();

        buttonLocalControl.setOnClickListener(v -> {
            saveSettings();
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_SecondFragment_to_localFragment);
        });

        buttonSearchDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                try {
                    amount = Integer.parseInt(devicesAmount);
                } catch (NumberFormatException e) {
                    amount = 30;
                }
                Context context = getActivity();
                DeviceFinder deviceFinder = new DeviceFinder(context, SecondFragment.this);
                String baseIp = routerIp.substring(0, routerIp.length() - 1);
                deviceFinder.findDeviceInNetwork(baseIp, amount);
                progressBar.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDeviceNotFound() {
        getActivity().runOnUiThread(() -> {
            ProgressBar progressBar = getView().findViewById(R.id.progressBar);
            TextView textView = getView().findViewById(R.id.progressBarLabel);
            progressBar.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            showToast("Пристрій не знайдено");
        });
    }

    @Override
    public void onDeviceFound(String ip) {
        getActivity().runOnUiThread(() -> {
            ProgressBar progressBar = getView().findViewById(R.id.progressBar);
            TextView textView = getView().findViewById(R.id.progressBarLabel);
            progressBar.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            showToast("Пристрій знайдено: " + ip);
        });
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("device_ip", editTextDeviceIP.getText().toString());
        editor.putString("router_ip", editTextRouterIP.getText().toString());
        editor.putString("devices_amount", editTextDevicesAmount.getText().toString());
        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Config", Context.MODE_PRIVATE);
        deviceIp = sharedPreferences.getString("device_ip", "");
        routerIp = sharedPreferences.getString("router_ip", "192.168.1.1");
        devicesAmount = sharedPreferences.getString("devices_amount", "");
        editTextDeviceIP.setText(deviceIp);
        editTextRouterIP.setText(routerIp);
        editTextDevicesAmount.setText(devicesAmount);
    }

    @Override
    public void onDestroyView() {
        saveSettings();
        super.onDestroyView();
        binding = null;
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}

