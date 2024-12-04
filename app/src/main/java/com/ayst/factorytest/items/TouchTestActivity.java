package com.ayst.factorytest.items;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.ayst.factorytest.R;
import com.ayst.factorytest.base.ChildTestActivity;
import com.ayst.factorytest.model.TestItem;
import com.ayst.factorytest.view.TouchTestView;

import java.util.HashMap;

import butterknife.BindView;

public class TouchTestActivity extends ChildTestActivity implements TouchTestView.CallBack {

    @BindView(R.id.touch_test)
    TouchTestView mTouchTestView;

    private HashMap<Integer, Boolean> mClicked = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getContentLayout() {
        return 0;
    }

    @Override
    public int getFullscreenLayout() {
        return R.layout.content_touch_test;
    }

    @Override
    public void initViews() {
        super.initViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        mTitleBar.setVisibility(View.INVISIBLE);
        mContainerLayout.setVisibility(View.INVISIBLE);
        mSuccessBtn.setVisibility(View.GONE);
        mFailureBtn.setVisibility(View.GONE);

        mTouchTestView.setCallBack(this);
    }

    @Override
    public void onTestCompleted() {
        finish(TestItem.STATE_SUCCESS);
    }
}