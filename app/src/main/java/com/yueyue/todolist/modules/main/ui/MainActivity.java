package com.yueyue.todolist.modules.main.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yueyue.todolist.R;
import com.yueyue.todolist.base.BaseActivity;
import com.yueyue.todolist.common.Constants;
import com.yueyue.todolist.common.utils.VersionUtil;
import com.yueyue.todolist.component.PreferencesManager;
import com.yueyue.todolist.component.RxBus;
import com.yueyue.todolist.event.MainTabsShowModeEvent;
import com.yueyue.todolist.event.MainTabsUpdateEvent;
import com.yueyue.todolist.modules.about.ui.AboutActivity;
import com.yueyue.todolist.modules.edit.ui.EditNoteActivity;
import com.yueyue.todolist.modules.lock.SetLockActivity;
import com.yueyue.todolist.modules.lock.UnlockActivity;
import com.yueyue.todolist.modules.main.component.WeatherExecutor;
import com.yueyue.todolist.modules.main.db.NoteDbHelper;
import com.yueyue.todolist.modules.main.domain.NoteEntity;
import com.yueyue.todolist.modules.service.AutoUpdateService;
import com.yueyue.todolist.modules.setting.ui.SettingActivity;
import com.yueyue.todolist.modules.weather.ui.WeatherActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    // 显示模式菜单的图标
    public static final int DRAWABLE_MODE_LIST = R.drawable.ic_format_list_bulleted_white_24dp;
    public static final int DRAWABLE_MODE_GRID = R.drawable.ic_border_all_white_24dp;

    public static final int ADD_NOTE_REQUEST_CODE = 0x01;
    public static final int EDIT_NOTE_REQUEST_CODE = 0x02;
    public static final int SET_LOCK_REQUEST_CODE = 0x03;
    public static final int UNLOCK_REQUEST_CODE = 0x04;

    public static final long DRAWER_CLOSE_DELAY = 230L;


    private SparseArray<Fragment> mFragmentSparseArray;


    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.container)
    ViewGroup container;

    @BindView(R.id.fab)
    FloatingActionButton mFab;


    @BindView(R.id.root)
    CoordinatorLayout root;

    //侧滑栏
    @BindView(R.id.nav_view)
    NavigationView mNavView;


    private boolean backPressed;
    private MenuItem currentMenu;
    private List<Disposable> mDisposableList = new ArrayList<>();


    public static void launch(Context context) {
        context.startActivity(new Intent(context, MainActivity.class));
    }

    @Override
    protected int initLayoutId() {
        return R.layout.activity_main;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer();
        initToolbar();
        initNavigationView();
        initFragments(savedInstanceState);
        updateWeather();
        checkVersion();
        AutoUpdateService.launch(MainActivity.this);
    }


    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initToolbar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
            layoutParams.height = BarUtils.getActionBarHeight();
            toolbar.setLayoutParams(layoutParams);
        }

    }


    @TargetApi(Build.VERSION_CODES.M)
    private void initNavigationView() {
        View navHeaderView = mNavView.getHeaderView(0);
        navHeaderView.setOnClickListener(new View.OnClickListener() {
            int index;
            long now;
            long lastTime;

            @Override
            public void onClick(View v) {
                now = new Date().getTime();
                if (now - lastTime < 1000) {
                    if (index < 3) {
                        index++;
                    } else {
                        ToastUtils.showShort(R.string.head_view_hint);
                    }
                } else {
                    updateWeather();
                }
                lastTime = now;
            }
        });
        mNavView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_todo:
                switchMenu(R.id.menu_todo, mFragmentSparseArray);
                setToolbarTitle(getString(R.string.todo));
                RxBus.getDefault().post(new MainTabsUpdateEvent());
                break;
            case R.id.menu_privacy:
                statrLock();
                break;
            case R.id.menu_recycle_bin:
                switchMenu(R.id.menu_recycle_bin, mFragmentSparseArray);
                setToolbarTitle(getString(R.string.recycle_bin));
                RxBus.getDefault().post(new MainTabsUpdateEvent());
                break;
            case R.id.menu_weather:
                WeatherActivity.launch(MainActivity.this);
                break;
            case R.id.menu_setting:
                SettingActivity.launch(MainActivity.this);
                break;
            case R.id.menu_about:
                AboutActivity.launch(MainActivity.this);
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawers();
        return true;
    }

    private void statrLock() {
        String passwordStr = PreferencesManager.getInstance().getLockPassword("");
        if (TextUtils.isEmpty(passwordStr)) {
            SetLockActivity.launch(MainActivity.this);
        } else {
            UnlockActivity.launch(MainActivity.this);
        }
    }

    private void initFragments(Bundle savedInstanceState) {
        if (mFragmentSparseArray == null) {
            mFragmentSparseArray = new SparseArray<>();
            //-主页
            mFragmentSparseArray.put(R.id.menu_todo, MainTabsFragment.newInstance(MainTabsFragment.ITEM_NORMAL));
            mFragmentSparseArray.put(R.id.menu_privacy, MainTabsFragment.newInstance(MainTabsFragment.ITEM_PRIMARY));
            mFragmentSparseArray.put(R.id.menu_recycle_bin, MainTabsFragment.newInstance(MainTabsFragment.ITEM_RECYCLE));
        }
        setMainFragment(R.id.menu_todo, mFragmentSparseArray, savedInstanceState == null);
        setToolbarTitle(getString(R.string.todo));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_activity_main, menu);
        initShowModeMenuIcon(menu.findItem(R.id.menu_note_show_mode));
        return true;
    }

    private void initShowModeMenuIcon(MenuItem item) {
        int mode = PreferencesManager.getInstance().getNoteListShowMode(Constants.STYLE_LINEAR);
        int drawble =
                mode == Constants.STYLE_LINEAR ?
                        R.drawable.ic_border_all_white_24dp :
                        R.drawable.ic_format_list_bulleted_white_24dp;

        item.setIcon(getResources().getDrawable(drawble));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_note_show_mode:
                changeShowModeAndItemIcon(item);
                break;
            default:
                break;
        }
        return true;
    }

    public void changeShowModeAndItemIcon(MenuItem item) {
        int mode = PreferencesManager.getInstance().getNoteListShowMode(Constants.STYLE_LINEAR);
        if (mode == Constants.STYLE_LINEAR) {
            PreferencesManager.getInstance().saveNoteListShowMode(Constants.STYLE_GRID);
            item.setIcon(getResources().getDrawable(DRAWABLE_MODE_LIST));
            RxBus.getDefault().post(new MainTabsShowModeEvent(Constants.STYLE_GRID));
        } else {
            PreferencesManager.getInstance().saveNoteListShowMode(Constants.STYLE_LINEAR);
            item.setIcon(getResources().getDrawable(DRAWABLE_MODE_GRID));
            RxBus.getDefault().post(new MainTabsShowModeEvent(Constants.STYLE_LINEAR));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ADD_NOTE_REQUEST_CODE:
                case EDIT_NOTE_REQUEST_CODE:
                    NoteEntity noteEntity = data.getParcelableExtra(EditNoteActivity.EXTRA_NOTE_DATA);
                    NoteDbHelper.getInstance().addNote(noteEntity);
                    RxBus.getDefault().post(new MainTabsUpdateEvent());
                    break;
                case UNLOCK_REQUEST_CODE:
                case SET_LOCK_REQUEST_CODE:
                    switchMenu(R.id.menu_privacy, mFragmentSparseArray);
                    setToolbarTitle(getString(R.string.privacy));
                    RxBus.getDefault().post(new MainTabsUpdateEvent());
                    break;
                default:
                    break;
            }
        }
    }

    private void checkVersion() {
        boolean autoCheckVersion = PreferencesManager.getInstance().getIsAutoCheckVersion(true);
        if (autoCheckVersion) {
            VersionUtil.checkVersion(this);
        }
    }


    private void updateWeather() {
        //使用RxPermissions（基于RxJava2） - CSDN博客
        //           http://blog.csdn.net/u013553529/article/details/68948971
        RxPermissions permissions = new RxPermissions(this);
        Disposable disposable = permissions.request(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(granted -> {
                    if (granted) {
                        View headerView = mNavView.getHeaderView(0);
                        new WeatherExecutor(MainActivity.this, headerView).execute();
                    } else {
                        ToastUtils.showShort(getString(R.string.no_permission_location));
                    }
                });
        mDisposableList.add(disposable);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        doublePressBackToQuit();
    }


    private void doublePressBackToQuit() {
        if (backPressed) {
            super.onBackPressed();
            return;
        }
        backPressed = true;
        SnackbarUtils.with(mDrawerLayout).setMessage(getString(R.string.leave_app));
        new Handler().postDelayed(() -> backPressed = false, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        for (Disposable disposable : mDisposableList) {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }

}
