package com.example.bond_light;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bond_light.databinding.FragmentFirstBinding;

import java.util.Objects;

public class FirstFragment extends Fragment {
    private OnRefreshButtonClickListener listener;
    private FragmentFirstBinding binding;
    private WebView webView;
    private ProgressBar progressBar;
    private boolean hasErrorOccurred = false;
    String deviceIp;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnRefreshButtonClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnRefreshButtonClickListener");
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        webView = binding.webView;
        progressBar = binding.progressBar;
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.refreshButton != null) {
            mainActivity.refreshButton.setOnClickListener(v -> refreshContent());
            mainActivity.refreshButton.setVisibility(View.VISIBLE);
        }
        listener.onRefreshButtonClick();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Config", Context.MODE_PRIVATE);

        if (!sharedPreferences.contains("device_ip")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("device_ip", "192.168.1.8");
            editor.putString("router_ip", "192.168.1.1");
            editor.putString("devices_amount", "30");
            editor.apply();
        }
        deviceIp = sharedPreferences.getString("device_ip", "192.168.1.8");
        webView.setBackgroundColor(0xFF121212);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (!hasErrorOccurred) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                hasErrorOccurred = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!hasErrorOccurred) {
                    if (Objects.equals(view.getTitle(), "Світло")) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                hasErrorOccurred = true;
                progressBar.setVisibility(View.GONE);
                int errorCode = error.getErrorCode();
                String errorDescription = error.getDescription().toString();
                String errorHtml = "<html><body>" +
                        "<h1 style=\"color: white\">Помилка завантаження сторінки " + deviceIp + "</h1>" +
                        "<p style=\"color: white\">Код помилки: " + errorCode + "</p>" +
                        "<p style=\"color: white\">Опис помилки: " + errorDescription + "</p>" +
                        "<h1 style=\"color: white\">Спробуйте перезавантажити сторінку</h1>" +
                        "<h1 style=\"color: white\">Перевірте, чи підключені ви до мережі Wi-Fi</h1>" +
                        "<h1 style=\"color: white\">Переконайтеся, що пристрій підключено до живлення</h1>" +
                        "<h1 style=\"color: white\">Виконайте пошук пристрою в налаштуваннях</h1>" +
                        "</body></html>";

                view.loadData(errorHtml, "text/html", "UTF-8");
            }
        });

        webView.loadUrl("http://" + deviceIp);

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.stopLoading();
        webView.loadData("", "text/html", "UTF-8");
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Config", Context.MODE_PRIVATE);
        deviceIp = sharedPreferences.getString("device_ip", "192.168.1.8");
        webView.loadUrl("http://" + deviceIp);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void refreshContent() {
        webView.stopLoading();
        webView.loadUrl("about:blank");
        webView.loadUrl("http://" + deviceIp);
    }
}
