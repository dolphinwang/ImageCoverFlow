# ImageCoverFlow

#### To show Cover Flow effect on Android

ImageCoverFlow is an open source Android library that allows developers to easily create applications with a cover flow effect to show images. This library does not extend Gallery. Feel free to use it all you want in your Android apps provided that you cite this project and include the license in your app.

**Note**: looping mode is currently supported, non-looping mode will be supported later.

![Oops! The screenshot is missing!](https://github.com/dolphinwang/ImageCoverFlow/raw/master/imagecoverflow_screenshot.png)

#### ImageCoverFlow is currently used in some published Android apps:

1. [ICardEnglish](https://play.google.com/store/apps/details?id=com.cn.icardenglish&hl=zh_CN)

---

# How to Use:

#### Step One: Add `CoverFlowView` to your project

1. Via XML:

```xml
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
    imageCoverFlow:visibleImage="5" />
```

2. Programatically (via Java):

```java
CoverFlowView<MyCoverFlowAdapter> mCoverFlowView =
    (CoverFlowView<MyCoverFlowAdapter>) findViewById(R.id.coverflow);

mCoverFlowView.setCoverFlowGravity(CoverFlowGravity.CENTER_VERTICAL);
mCoverFlowView.setCoverFlowLayoutMode(CoverFlowLayoutMode.WRAP_CONTENT);
mCoverFlowView.enableReflection(true);
mCoverFlowView.setReflectionHeight(30);
mCoverFlowView.setReflectionGap(20);
mCoverFlowView.enableReflectionShader(true);
mCoverFlowView.setVisibleImage(5);
```

**TIP**: If you want to support different movement speeds on different screen densities, you can use method `setScreenDensity()`. Otherwise CoverFlow will have a unified movement speed.

---

#### Step Two: Set an adapter, which extends `CoverFlowAdapter`:

```java
MyCoverFlowAdapter adapter = new MyCoverFlowAdapter(this);
mCoverFlowView.setAdapter(adapter);
```

**TIPS**:
* Method `setAdapter()` should be called after all properties of CoverFlow are settled.
* If you want to load image dynamically, you can call method `notifyDataSetChanged()` when bitmaps are loaded.

#### Step Three: if you want to listen for the click event of the top image, you can set a `CoverFlowListener` to it:

```java
mCoverFlowView.setCoverFlowListener(new CoverFlowListener<MyCoverFlowAdapter>() {
    @Override
    public void imageOnTop(CoverFlowView<MyCoverFlowAdapter> view, int position,
            float left, float top, float right,float bottom) {
        // TODO
    }

    @Override
    public void topImageClicked(CoverFlowView<MyCoverFlowAdapter> view, int position) {
        // TODO
    }
});
```

If you want to listen for long click events of the top image, you can set a `TopImageLongClickListener` to it:

```java
mCoverFlowView
    .setTopImageLongClickListener(new CoverFlowView.TopImageLongClickListener() {
        @Override
        public void onLongClick(int position) {
            Log.e(VIEW_LOG_TAG, "top image long clicked ==> " + position);
        }
    });
```

Users can use method `setSelection()` to show a specific position at the top.

---

#### If you want to subclass `CoverFlowView`

1. You can override method `getCustomTransformMatrix()` to make more transformations for images (there is some annotated code which shows how to make image y-axis rotation).
2. You should never override method `onLayout()` to layout any of `CoverFlowView`’s children, because all of image will draw on the canvas directly.

---

#### Developed By:

Roy Wang (dolphinwang@foxmail.com)

If you use this library, please let me know.

---

#### License:

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