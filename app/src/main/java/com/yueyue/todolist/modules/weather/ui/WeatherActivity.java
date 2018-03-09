package com.yueyue.todolist.modules.weather.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.blankj.utilcode.util.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yueyue.todolist.R;
import com.yueyue.todolist.base.BaseActivity;
import com.yueyue.todolist.common.utils.SpUtil;
import com.yueyue.todolist.common.utils.Util;
import com.yueyue.todolist.component.RetrofitSingleton;
import com.yueyue.todolist.modules.address.ui.AddressCheckActivity;
import com.yueyue.todolist.modules.weather.adapter.WeatherAdapter;
import com.yueyue.todolist.modules.weather.domain.Weather;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * author : yueyue on 2018/3/9 20:59
 * desc   :
 */

public class WeatherActivity extends BaseActivity {


    private static final int AddressRequestCode = 666;


    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    private List<Disposable> mDisposableList = new ArrayList<>();
    private Weather mWeather = new Weather();
    private WeatherAdapter mAdapter;


    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;


    public static void launch(Context context) {
        context.startActivity(new Intent(context, WeatherActivity.class));
    }

    @Override
    protected int initLayoutId() {
        return R.layout.activity_weather;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupView();
        initIcon();
        initData();
    }


    private void setupView() {
        mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefresh.setOnRefreshListener(
                () -> mSwipeRefresh.postDelayed(this::load, 1000));

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new WeatherAdapter(mWeather);
        mRecyclerView.setAdapter(mAdapter);

        //RecyclerView的滚动事件OnScrollListener研究 - 简书
        //               https://www.jianshu.com/p/ce347cf991db
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int visiblity = RecyclerView.SCROLL_STATE_IDLE == newState ? View.VISIBLE : View.GONE;
                mFab.setVisibility(visiblity);
            }
        });
    }

    /**
     * 初始化 Icons
     */
    private void initIcon() {
        SpUtil.getInstance().putInt("未知", R.mipmap.none);
        SpUtil.getInstance().putInt("晴", R.mipmap.type_one_sunny);
        SpUtil.getInstance().putInt("阴", R.mipmap.type_one_cloudy);
        SpUtil.getInstance().putInt("多云", R.mipmap.type_one_cloudy);
        SpUtil.getInstance().putInt("少云", R.mipmap.type_one_cloudy);
        SpUtil.getInstance().putInt("晴间多云", R.mipmap.type_one_cloudytosunny);
        SpUtil.getInstance().putInt("小雨", R.mipmap.type_one_light_rain);
        SpUtil.getInstance().putInt("中雨", R.mipmap.type_one_light_rain);
        SpUtil.getInstance().putInt("大雨", R.mipmap.type_one_heavy_rain);
        SpUtil.getInstance().putInt("阵雨", R.mipmap.type_one_thunderstorm);
        SpUtil.getInstance().putInt("雷阵雨", R.mipmap.type_one_thunder_rain);
        SpUtil.getInstance().putInt("霾", R.mipmap.type_one_fog);
        SpUtil.getInstance().putInt("雾", R.mipmap.type_one_fog);
    }



    private void initData() {

        //使用RxPermissions（基于RxJava2） - CSDN博客
        //           http://blog.csdn.net/u013553529/article/details/68948971
        RxPermissions permissions = new RxPermissions(this);
        Disposable disposable = permissions.request(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(granted -> {
                    if (granted) {
                        location();
                    } else {
                        load();
                        ToastUtils.showShort(getString(R.string.need_location_permission_to_offer_accurate_weather));
                    }
                });
        mDisposableList.add(disposable);

    }

    private void load() {
        fetchDataByNetWork()
                .doOnSubscribe(aLong -> changeSwipeRefreshState(true))
                .doOnError(throwable -> {
                    mRecyclerView.setVisibility(View.GONE);
                    SpUtil.getInstance().putCityName("广州");
                    setToolbarTitle("找不到城市啦");
                })
                .doOnNext(weather -> {
                    mRecyclerView.setVisibility(View.VISIBLE);

                    setToolbarTitle(weather.basic.city);
                    mWeather.status = weather.status;
                    mWeather.aqi = weather.aqi;
                    mWeather.basic = weather.basic;
                    mWeather.suggestion = weather.suggestion;
                    mWeather.now = weather.now;
                    mWeather.dailyForecast = weather.dailyForecast;
                    mWeather.hourlyForecast = weather.hourlyForecast;

                    mAdapter.notifyDataSetChanged();

//                    NotificationHelper.showWeatherNotification(getActivity(), weather);
                })
                .doOnComplete(() -> {
                    changeSwipeRefreshState(false);
                    ToastUtils.showShort(getString(R.string.refreshing_complete));
                })
                .subscribe();

    }

    /**
     * 高德定位
     */
    private void location() {
        //初始化定位
        mLocationClient = new AMapLocationClient(this);
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
        mLocationOption.setNeedAddress(true);
        mLocationOption.setOnceLocation(true);
        //设置定位间隔 单位毫秒
        int autoUpdateTime = SpUtil.getInstance().getAutoUpdate();
        mLocationOption.setInterval((autoUpdateTime == 0 ? 100 : autoUpdateTime) * SpUtil.ONE_HOUR);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.setLocationListener(aMapLocation -> {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //定位成功。
                    aMapLocation.getLocationType();
                    SpUtil.getInstance().putCityName(Util.replaceCity(aMapLocation.getCity()));
                    ToastUtils.showShort("定位成功");
                } else {
                    SpUtil.getInstance().putCityName("广州");
                    ToastUtils.showShort("定位失败,已设置回默认的城市:广州");
                }
                ;
                load();
            }
        });
        mLocationClient.startLocation();
    }


    /**
     * 从网络获取
     */
    private Observable<Weather> fetchDataByNetWork() {
        String cityName = SpUtil.getInstance().getCityName();
        return RetrofitSingleton.getInstance()
                .fetchWeather(cityName)
                .compose(this.bindToLifecycle());
    }

    private void changeSwipeRefreshState(boolean state) {
        mSwipeRefresh.setRefreshing(state);
    }

    @OnClick(R.id.fab)
    void selectAddress() {
        Intent intent = new Intent(this, AddressCheckActivity.class);
        startActivityForResult(intent, 666);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case AddressRequestCode:
                    SpUtil.getInstance().putCityName(AddressCheckActivity.parse(data));
                    load();
                    break;
                default:
                    break;
            }
        }
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
