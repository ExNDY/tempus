package com.cappielloantonio.tempo.ui.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.ActivityCrashBinding;
import com.cappielloantonio.tempo.ui.fragment.CrashExportFragment;
import com.cappielloantonio.tempo.ui.fragment.CrashInfoFragment;
import com.cappielloantonio.tempo.ui.fragment.CrashLogsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;

@UnstableApi
public class CrashActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityLogs";
    ActivityCrashBinding bind;
    String stackTraceFromIntent;
    CaocConfig configFromIntent;
    private Toolbar toolbar;
    private BottomNavigationView bottomNav;

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(
                this,
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        );
        DynamicColors.applyToActivityIfAvailable(this);

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        bind = ActivityCrashBinding.inflate(getLayoutInflater());
        View view = bind.getRoot();
        setContentView(view);
        applyEdgeToEdgeInsets();

        stackTraceFromIntent = CustomActivityOnCrash.getStackTraceFromIntent(getIntent());
        configFromIntent = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        init();
        initAppBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind = null;
    }

    private void init() {
        toolbar = findViewById(R.id.crash_toolbar);
        setSupportActionBar(toolbar);

        FrameLayout contentFrame = findViewById(R.id.crash_content_frame);

        // These will be null if the view is not present in the current layout
        bottomNav = findViewById(R.id.crash_bottom_nav);
        drawerLayout = findViewById(R.id.crash_drawer_layout);
        navView = findViewById(R.id.crash_nav_view);

        if (bottomNav != null) {
            setupBottomNav(bottomNav);
        }

        if (drawerLayout != null && navView != null) {
            setupDrawer(drawerLayout, navView);
        }
    }

    private void applyEdgeToEdgeInsets() {
        final int toolbarPaddingLeft = bind.crashToolbar.getPaddingLeft();
        final int toolbarPaddingTop = bind.crashToolbar.getPaddingTop();
        final int toolbarPaddingRight = bind.crashToolbar.getPaddingRight();
        final int toolbarPaddingBottom = bind.crashToolbar.getPaddingBottom();

        final int contentPaddingLeft = bind.crashContentFrame.getPaddingLeft();
        final int contentPaddingTop = bind.crashContentFrame.getPaddingTop();
        final int contentPaddingRight = bind.crashContentFrame.getPaddingRight();
        final int contentPaddingBottom = bind.crashContentFrame.getPaddingBottom();

        final int drawerPaddingLeft = bind.crashDrawerLayout.getPaddingLeft();
        final int drawerPaddingTop = bind.crashDrawerLayout.getPaddingTop();
        final int drawerPaddingRight = bind.crashDrawerLayout.getPaddingRight();
        final int drawerPaddingBottom = bind.crashDrawerLayout.getPaddingBottom();

        final int bottomNavPaddingLeft = bind.crashBottomNav != null ? bind.crashBottomNav.getPaddingLeft() : 0;
        final int bottomNavPaddingTop = bind.crashBottomNav != null ? bind.crashBottomNav.getPaddingTop() : 0;
        final int bottomNavPaddingRight = bind.crashBottomNav != null ? bind.crashBottomNav.getPaddingRight() : 0;
        final int bottomNavPaddingBottom = bind.crashBottomNav != null ? bind.crashBottomNav.getPaddingBottom() : 0;
        final int bottomNavHeight = bind.crashBottomNav != null
                ? bind.crashBottomNav.getLayoutParams().height
                : ViewGroup.LayoutParams.WRAP_CONTENT;

        final int navViewPaddingLeft = bind.crashNavView != null ? bind.crashNavView.getPaddingLeft() : 0;
        final int navViewPaddingTop = bind.crashNavView != null ? bind.crashNavView.getPaddingTop() : 0;
        final int navViewPaddingRight = bind.crashNavView != null ? bind.crashNavView.getPaddingRight() : 0;
        final int navViewPaddingBottom = bind.crashNavView != null ? bind.crashNavView.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(bind.getRoot(), (root, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            bind.crashDrawerLayout.setPadding(
                    drawerPaddingLeft,
                    drawerPaddingTop,
                    drawerPaddingRight,
                    drawerPaddingBottom
            );

            bind.crashToolbar.setPadding(
                    toolbarPaddingLeft,
                    toolbarPaddingTop + systemBars.top,
                    toolbarPaddingRight,
                    toolbarPaddingBottom
            );

            bind.crashContentFrame.setPadding(
                    contentPaddingLeft,
                    contentPaddingTop,
                    contentPaddingRight,
                    contentPaddingBottom
            );

            if (bind.crashBottomNav != null) {
                ViewGroup.LayoutParams layoutParams = bind.crashBottomNav.getLayoutParams();
                if (bottomNavHeight > 0) {
                    layoutParams.height = bottomNavHeight + systemBars.bottom;
                }
                bind.crashBottomNav.setLayoutParams(layoutParams);
                bind.crashBottomNav.setPadding(
                        bottomNavPaddingLeft,
                        bottomNavPaddingTop,
                        bottomNavPaddingRight,
                        bottomNavPaddingBottom + systemBars.bottom
                );
            } else {
                bind.crashContentFrame.setPadding(
                        contentPaddingLeft,
                        contentPaddingTop,
                        contentPaddingRight,
                        contentPaddingBottom + systemBars.bottom
                );
            }

            if (bind.crashNavView != null) {
                bind.crashNavView.setPadding(
                        navViewPaddingLeft,
                        navViewPaddingTop + systemBars.top,
                        navViewPaddingRight,
                        navViewPaddingBottom + systemBars.bottom
                );
            }

            return windowInsets;
        });
        ViewCompat.requestApplyInsets(bind.getRoot());
    }

    private void initAppBar() {

        int orientation = getResources().getConfiguration().orientation;
        boolean isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.toolbar_navigation_drawer_open,
                    R.string.toolbar_navigation_drawer_closed
            );
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
        }
    }

    public String getStackTrace() {
        return stackTraceFromIntent;
    }

    public CaocConfig getConfigFromIntent() {
        return configFromIntent;
    }

    private void setupBottomNav(BottomNavigationView nav) {
        String toolbarTitle = "Crash Landing";
        nav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.crash_nav_info) {
                loadFragment(new CrashInfoFragment(), toolbarTitle);
                return true;
            } else if (itemId == R.id.crash_nav_logs) {
                loadFragment(new CrashLogsFragment(), toolbarTitle);
                return true;
            } else if (itemId == R.id.crash_nav_export) {
                loadFragment(new CrashExportFragment(), toolbarTitle);
                return true;
            }
            return false;
        });

        loadFragment(new CrashInfoFragment(), toolbarTitle);
        nav.getMenu().findItem(R.id.crash_nav_info).setChecked(true);
    }

    private void setupDrawer(DrawerLayout drawer, NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.crash_nav_info) {
                loadFragment(new CrashInfoFragment(), getString(R.string.ca_info_title));
            } else if (itemId == R.id.crash_nav_logs) {
                loadFragment(new CrashLogsFragment(), getString(R.string.ca_logs_title));
            } else if (itemId == R.id.crash_nav_export) {
                loadFragment(new CrashExportFragment(), getString(R.string.ca_export_title));
            }
            drawer.closeDrawers();
            return true;
        });
        loadFragment(new CrashInfoFragment(), getString(R.string.ca_info_title));
        navigationView.setCheckedItem(R.id.crash_nav_info);
    }

    private void loadFragment(Fragment fragment, String toolbarTitle) {
        Objects.requireNonNull(getSupportActionBar())
                .setTitle(toolbarTitle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.crash_content_frame, fragment)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
