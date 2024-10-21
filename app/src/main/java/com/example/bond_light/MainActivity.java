package com.example.bond_light;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.bond_light.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements OnRefreshButtonClickListener{

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private WebView webView;
    public ImageButton refreshButton;

    String deviceIp;
    //String routerIp = sharedPreferences.getString("router_ip", "192.168.1.1");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            invalidateOptionsMenu();
        });

        webView = findViewById(R.id.webView);
        SharedPreferences sharedPreferences = getSharedPreferences("Config", Context.MODE_PRIVATE);

        if (!sharedPreferences.contains("device_ip")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("device_ip", "192.168.1.8");
            editor.putString("router_ip", "192.168.1.1");
            editor.putString("devices_amount", "30");
            editor.apply();
        }
        deviceIp = sharedPreferences.getString("device_ip", "192.168.1.8");

        refreshButton = findViewById(R.id.btn_refresh);
        refreshButton.setOnClickListener(v -> {
            webView.stopLoading();
            webView.loadData("", "text/html", "UTF-8");
            webView.loadUrl("http://" + deviceIp);
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_FirstFragment_to_SecondFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public void onRefreshButtonClick() {
        Fragment activeFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (activeFragment instanceof FirstFragment) {
            ((FirstFragment) activeFragment).refreshContent();
        } else if (activeFragment instanceof LocalFragment) {
            ((LocalFragment) activeFragment).refreshContentLocal();
        }
    }
}

