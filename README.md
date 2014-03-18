ImageCoverFlow
==============

### To show coverflow effect on android


ImageCoverFlow is an Open Source Android library that allows developers to easily create applications with coverflow effect to show image.This lib is not extends Gallery. Feel free to use it all you want in your Android apps provided that you cite this project and include the license in your app. (Looping mode has already supported, non-looping mode will be supported later)

![Opps! Screen shot has missed](https://github.com/dolphinwang/ImageCoverFlow/raw/master/imagecoverflow_screenshot.png)


### ImageCoverFlow is currently used in some published Android apps. Here's a list of some of them:

1. [ICardEnglish](https://play.google.com/store/apps/details?id=com.cn.icardenglish&hl=zh_CN)</br >


How to use：
-----------------------------------
### Step one：add it to ur project

1. by xml

		<com.dolphinwang.imagecoverflow.CoverFlowView
		xmlns:imageCoverFlow="http://schemas.android.com/apk/res-auto"
		android:id="@+id/coverflow"
		android:layout_width="match_parent"
		android:layout_height="match_parent"
		android:paddingLeft="20dp"
		android:paddingRight="20dp"
		imageCoverFlow:coverflowGravity="center_vertical"
		imageCoverFlow:coverflowLayoutMode="wrap_content"
		imageCoverFlow:enableReflection="true"
		imageCoverFlow:reflectionGap="10dp"
		imageCoverFlow:reflectionHeight="30%"
		imageCoverFlow:reflectionShaderEnable="true"
		imageCoverFlow:visibleImage="5" >
		</com.dolphinwang.imagecoverflow.CoverFlowView>

2. by java code:

		CoverFlowView<MyCoverFlowAdapter> mCoverFlowView=(CoverFlowView<MyCoverFlowAdapter>)findViewById(R.id.coverflow);
		
		mCoverFlowView.setCoverFlowGravity(CoverFlowGravity.CENTER_VERTICAL);
		mCoverFlowView.setCoverFlowLayoutMode(CoverFlowLayoutMode.WRAP_CONTENT);
		mCoverFlowView.enableReflection(true);
		mCoverFlowView.setReflectionHeight(30);
		mCoverFlowView.setReflectionGap(20);
		mCoverFlowView.enableReflectionShader(true);
		mCoverFlowView.setVisibleImage(5);
	
		tips:If u want to support different move speed on different density of screens,
		     u can call method setScreenDensity to set screen density. Or coverflow 
		     will have unified move speed.


### Step two：then you should give an adapter to it which extends CoverFlowAdapter:

	MyCoverFlowAdapter adapter = new MyCoverFlowAdapter(this);
	mCoverFlowView.setAdapter(adapter);

	tips：method setAdapter should be called after all of properties of coverflow are setted。


### Step three：if u want to listen click event of top image, u can set CoverFlowListener to it.

	mCoverFlowView.setCoverFlowListener(new CoverFlowListener<MyCoverFlowAdapter>() {
	
		@Override
		public void imageOnTop(CoverFlowView<MyCoverFlowAdapter> view,
				int position, float left, float top, float right,float bottom) {}
	
		@Override
		public void topImageClicked(CoverFlowView<MyCoverFlowAdapter> view, int position){}
	});


If u write a class extends CoverFlowView:
-----------------------------------
	1. You can override method getCustomTransformMatrix to make more transform to image.
	   (There are some annotation code in it shows how to make image y-axis rotation).
	2. You should never override method onLayout to layout any of CoverFlowView’s children,
	   because all of image will draw on the canvas directly.


### Developed by: 
Roy Wang (dolphinwang@foxmail.com)

If you use this lib. Please let me know.


### License:

Copyright 2013 Roy Wang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
