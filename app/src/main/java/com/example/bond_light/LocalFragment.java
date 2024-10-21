package com.example.bond_light;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.example.bond_light.databinding.FragmentLocalBinding;

import java.util.Objects;


public class LocalFragment extends Fragment {
    private OnRefreshButtonClickListener listener;
    private FragmentLocalBinding binding;
    private WebView webView;
    private ProgressBar progressBar;

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
        binding = FragmentLocalBinding.inflate(inflater, container, false);
        webView = binding.webViewLocal;
        progressBar = binding.progressBarLocal;
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.refreshButton != null) {
            mainActivity.refreshButton.setOnClickListener(v -> refreshContentLocal());
            mainActivity.refreshButton.setVisibility(View.VISIBLE);
        }
        listener.onRefreshButtonClick();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(0xFF121212);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                new android.os.Handler().postDelayed(() -> {
                    if (!Objects.equals(view.getTitle(), "Світло")) {
                        String errorHtml = "<html><head><title>Світло</title></head><body>" +
                                "<h1 style=\"color: white\">Помилка завантаження сторінки 192.168.4.1</h1>" +
                                "<h1 style=\"color: white\">Спробуйте перезавантажити сторінку</h1>" +
                                "<h1 style=\"color: white\">Перевірте, чи підключені ви до мережі Wi-Fi ESP_HOME</h1>" +
                                "<h1 style=\"color: white\">Переконайтеся, що пристрій підключено до живлення</h1>" +
                                "</body></html>";
                        view.stopLoading();
                        view.loadData(errorHtml, "text/html", "UTF-8");
                    }
                    super.onPageFinished(view, url);
                }, 300);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!Objects.equals(view.getTitle(), "Світло")) {
                    progressBar.setVisibility(View.GONE);
                    int errorCode = error.getErrorCode();
                    String errorDescription = error.getDescription().toString();
                    String errorHtml = "<html><head><title>Світло</title></head><body>" +
                            "<h1 style=\"color: white\">Помилка завантаження сторінки 192.168.4.1</h1>" +
                            "<p style=\"color: white\">Код помилки: " + errorCode + "</p>" +
                            "<p style=\"color: white\">Опис помилки: " + errorDescription + "</p>" +
                            "<h1 style=\"color: white\">Спробуйте перезавантажити сторінку</h1>" +
                            "<h1 style=\"color: white\">Перевірте, чи підключені ви до мережі Wi-Fi ESP_HOME</h1>" +
                            "<h1 style=\"color: white\">Переконайтеся, що пристрій підключено до живлення</h1>" +
                            "</body></html>";
                    view.stopLoading();
                    view.loadData(errorHtml, "text/html", "UTF-8");
                }
            }
        });

        webView.loadUrl("http://192.168.4.1");

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
        webView.loadUrl("http://192.168.4.1");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public void refreshContentLocal() {
        webView.stopLoading();
        webView.loadData("", "text/html", "UTF-8");
        webView.loadUrl("http://192.168.4.1");
    }
}