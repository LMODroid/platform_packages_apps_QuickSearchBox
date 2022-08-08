/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Activity that looks like a dialog window.
 */
public abstract class DialogActivity extends Activity {

    protected TextView mTitleView;
    protected FrameLayout mContentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_activity);
        mTitleView = (TextView) findViewById(R.id.alertTitle);
        mContentFrame = (FrameLayout) findViewById(R.id.content);
    }

    public void setHeading(int titleRes) {
        mTitleView.setText(titleRes);
    }

    public void setHeading(CharSequence title) {
        mTitleView.setText(title);
    }

    public void setDialogContent(int layoutRes) {
        mContentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutRes, mContentFrame);
    }

    public void setDialogContent(View content) {
        mContentFrame.removeAllViews();
        mContentFrame.addView(content);
    }

    public View getDialogContent() {
        if (mContentFrame.getChildCount() > 0) {
            return mContentFrame.getChildAt(0);
        } else {
            return null;
        }
    }

}
