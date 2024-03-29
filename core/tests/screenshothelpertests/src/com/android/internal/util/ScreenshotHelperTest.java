/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.util;

import static android.view.WindowManager.TAKE_SCREENSHOT_FULLSCREEN;
import static android.view.WindowManager.TAKE_SCREENSHOT_SELECTED_REGION;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class ScreenshotHelperTest {
    private Context mContext;
    private ScreenshotHelper mScreenshotHelper;
    private Handler mHandler;


    @Before
    public void setUp() {
        // `ScreenshotHelper.notifyScreenshotError()` calls `Context.sendBroadcastAsUser()` and
        // `Context.bindServiceAsUser`.
        //
        // This raises a `SecurityException` if the device is locked. Calling either `Context`
        // method results in a broadcast of `android.intent.action. USER_PRESENT`. Only the system
        // process is allowed to broadcast that `Intent`.
        mContext = Mockito.spy(Context.class);
        Mockito.doNothing().when(mContext).sendBroadcastAsUser(any(), any());
        Mockito.doReturn(true).when(mContext).bindServiceAsUser(any(), any(), anyInt(), any());

        mHandler = new Handler(Looper.getMainLooper());
        mScreenshotHelper = new ScreenshotHelper(mContext);
    }

    @Test
    public void testFullscreenScreenshot() {
        mScreenshotHelper.takeScreenshot(TAKE_SCREENSHOT_FULLSCREEN, false, false, mHandler, null);
    }

    @Test
    public void testSelectedRegionScreenshot() {
        mScreenshotHelper.takeScreenshot(TAKE_SCREENSHOT_SELECTED_REGION, false, false, mHandler,
                null);
    }

    @Test
    public void testScreenshotTimesOut() {
        long timeoutMs = 10;

        CountDownLatch lock = new CountDownLatch(1);
        mScreenshotHelper.takeScreenshot(TAKE_SCREENSHOT_FULLSCREEN, false, false, timeoutMs,
                mHandler,
                worked -> {
                    assertFalse(worked);
                    lock.countDown();
                });

        try {
            // Add tolerance for delay to prevent flakes.
            long awaitDurationMs = timeoutMs + 100;
            if (!lock.await(awaitDurationMs, TimeUnit.MILLISECONDS)) {
                fail("lock never freed");
            }
        } catch (InterruptedException e) {
            fail("lock interrupted");
        }
    }
}
