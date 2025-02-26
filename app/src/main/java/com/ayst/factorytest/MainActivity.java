package com.ayst.factorytest;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ayst.factorytest.adapter.TestItemAdapter;
import com.ayst.factorytest.base.BaseActivity;
import com.ayst.factorytest.data.TestItemManager;
import com.ayst.factorytest.data.TestResultExport;
import com.ayst.factorytest.model.ResultEvent;
import com.ayst.factorytest.model.TestItem;
import com.ayst.factorytest.utils.SPUtils;
import com.blankj.utilcode.util.AppUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.google.gson.Gson;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.actionbar.TitleBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    private static final int INVALID_ITEM = -1;

    @BindView(R.id.title_bar)
    TitleBar mTitleBar;
    @BindView(R.id.rv_items)
    RecyclerView mItemsRv;
    @BindView(R.id.btn_start)
    Button mStartBtn;
    @BindView(R.id.tv_version)
    TextView mVersionTv;
    @BindView(R.id.layout_qrcode)
    RelativeLayout mQRCodeLayout;
    @BindView(R.id.iv_qrcode)
    ImageView mQRCodeIv;
    @BindView(R.id.btn_close_qrcode)
    ImageButton mCloseQRCodeBtn;
    @BindView(R.id.btn_show_qrcode)
    Button mShowQRCodeBtn;
    @BindView(R.id.btn_clean_result)
    Button mCleanResultBtn;

    private int mNextItem = INVALID_ITEM;
    private Gson mGson = new Gson();
    private ArrayList<TestItem> mTestItems;
    private TestItemAdapter mTestItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        EventBus.getDefault().register(this);

        mTitleBar.setTitle(getString(R.string.app_name));
        mTitleBar.setLeftText("");
        mTitleBar.setLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.exitApp();
            }
        });

        mVersionTv.setText(AppUtils.getAppVersionName());

        int spanCount = 4;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            spanCount = 2;
        }
        WidgetUtils.initGridRecyclerView(mItemsRv, spanCount, 1, getResources().getColor(R.color.gray));
        mTestItems = TestItemManager.getInstance(getApplicationContext()).loadTestItems();
        mTestItemAdapter = new TestItemAdapter();
        mTestItemAdapter.setList(mTestItems);
        mItemsRv.setAdapter(mTestItemAdapter);
        mTestItemAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                if (mNextItem != INVALID_ITEM) {
                    mNextItem = INVALID_ITEM;
                }
                Intent intent = new Intent(MainActivity.this,
                        ((TestItem) adapter.getData().get(position)).getActivity());
                intent.putExtra("item", (TestItem) adapter.getData().get(position));
                startActivity(intent);
            }
        });

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNextItem = 0;
                next();
            }
        });

        mCloseQRCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQRCodeLayout.setVisibility(View.GONE);
            }
        });

        mShowQRCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TestResultExport export = new TestResultExport(MainActivity.this, mTestItems);
                Bitmap qrcode = export.exportQRCode();
                mQRCodeLayout.setVisibility(View.VISIBLE);
                if (qrcode != null) {
                    mQRCodeIv.setImageBitmap(qrcode);
                }
            }
        });

        mCleanResultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SPUtils.get(MainActivity.this).clear();
                mTestItems = TestItemManager.getInstance(getApplicationContext()).loadTestItems();
                mTestItemAdapter.setList(mTestItems);
                mTestItemAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);

        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResultEvent(ResultEvent event) {
        TestItem item = event.getTestItem();
        Log.i(TAG, "onResultEvent, " + item.toString());
        SPUtils.get(this).saveData(item.getKey(), mGson.toJson(item));
        for (int i = 0; i < mTestItems.size(); i++) {
            if (TextUtils.equals(item.getName(), mTestItems.get(i).getName())) {
                mTestItems.get(i).setState(item.getState());
                mTestItems.get(i).setResult(item.getResult());
                mTestItemAdapter.notifyDataSetChanged();
            }
        }
        if (mNextItem != INVALID_ITEM) {
            next();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AppUtils.exitApp();
    }

    private void next() {
        if (mNextItem < mTestItems.size()) {
            TestItem item = mTestItems.get(mNextItem);

            Log.i(TAG, "next, test: " + item.getName());

            Intent intent = new Intent(MainActivity.this, item.getActivity());
            intent.putExtra("item", item);
            startActivity(intent);

            mNextItem++;
        } else {
            mNextItem = INVALID_ITEM;
            TestResultExport export = new TestResultExport(this, mTestItems);
            export.exportFile();
            Bitmap qrcode = export.exportQRCode();
            mQRCodeLayout.setVisibility(View.VISIBLE);
            if (qrcode != null) {
                mQRCodeIv.setImageBitmap(qrcode);
            }
        }
    }
}