package com.dolphinwang.imagecoverflow;

import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * Created by dolphinWang on 2018/3/5.
 */

public class CoverFlowCell {

    public int width;
    public int height;

    public boolean isOnTop;

    public int showingPosition;
    public int index;

    public RectF showingRect;

    public CoverFlowCell() {
        showingRect = new RectF();
    }

    public boolean mapTransform(Matrix transformer) {
        if (height == 0 || width == 0) {
            return false;
        }

        showingRect.top = 0;
        showingRect.left = 0;
        showingRect.right = width;
        showingRect.bottom = height;

        return transformer.mapRect(showingRect);
    }

    public boolean inTouchArea(float x, float y) {
        return showingRect.contains(x, y);
    }
}
