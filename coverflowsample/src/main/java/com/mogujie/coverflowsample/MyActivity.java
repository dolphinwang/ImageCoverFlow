package com.mogujie.coverflowsample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.dolphinwang.imagecoverflow.CoverFlowView;


public class MyActivity extends Activity {

    protected static final String VIEW_LOG_TAG = "CoverFlowDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = LayoutInflater.from(this).inflate(R.layout.activity_main,
                null, false);
        setContentView(v);

        final CoverFlowView<MyCoverFlowAdapter> mCoverFlowView = (CoverFlowView<MyCoverFlowAdapter>) findViewById(R.id.coverflow);

        final MyCoverFlowAdapter adapter = new MyCoverFlowAdapter(this);
        mCoverFlowView.setAdapter(adapter);
        mCoverFlowView
                .setStateListener(new CoverFlowView.StateListener() {

                    @Override
                    public void imageOnTop(CoverFlowView view, int position, float left, float top, float right, float bottom) {
                        Log.e(VIEW_LOG_TAG, position + " on top!");
                    }

                    @Override
                    public void invalidationCompleted(CoverFlowView view) {

                    }
                });

        mCoverFlowView
                .setImageLongClickListener(new CoverFlowView.ImageLongClickListener() {

                    @Override
                    public void onLongClick(CoverFlowView view, int position) {
                        Log.e(VIEW_LOG_TAG, "image long clicked ==>"
                                + position);
                    }
                });

        mCoverFlowView.setImageClickListener(new CoverFlowView.ImageClickListener() {
            @Override
            public void onClick(CoverFlowView coverFlowView, int position) {
                Log.e(VIEW_LOG_TAG, position + " clicked!");
                coverFlowView.setSelection(position);
            }
        });

        findViewById(R.id.change_bitmap_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                adapter.changeBitmap();
                mCoverFlowView.setSelection(2);
            }
        });
    }
}
