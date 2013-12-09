package com.dolphinwang.imagecoverflow;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Bitmap;

public abstract class CoverFlowAdapter {
	private final DataSetObservable mDataSetObservable = new DataSetObservable();

	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.registerObserver(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.unregisterObserver(observer);
	}

	public void notifyDataSetChanged() {
		mDataSetObservable.notifyChanged();
	}

	public void notifyDataSetInvalidated() {
		mDataSetObservable.notifyInvalidated();
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public abstract int getCount();

	public abstract Bitmap getImage(int position);
}
