// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.bookmarks;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import org.chromium.base.ApiCompatibilityUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.NativePage;
import org.chromium.chrome.browser.UrlConstants;
import org.chromium.chrome.browser.bookmarks.BookmarkDelegate.BookmarkStateChangeListener;
import org.chromium.chrome.browser.compositor.layouts.content.InvalidationAwareThumbnailProvider;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.content_public.browser.LoadUrlParams;


/**
 * A native page holding a {@link BookmarkManager} on _tablet_.
 */
public class BookmarkPage implements NativePage, BookmarkStateChangeListener,
        InvalidationAwareThumbnailProvider, DrawerLayout.DrawerListener,
        View.OnAttachStateChangeListener {
    private final Activity mActivity;
    private final Tab mTab;
    private final String mTitle;
    private final int mBackgroundColor;
    private final int mThemeColor;
    private BookmarkManager mManager;
    private String mCurrentUrl;
    private boolean mNeedsCapture = true;

    /**
     * Create a new instance of the bookmarks page.
     * @param activity The activity to get context and manage fragments.
     * @param tab The tab to load urls.
     */
    public BookmarkPage(Activity activity, Tab tab) {
        mActivity = activity;
        mTab = tab;
        mTitle = activity.getString(R.string.bookmarks);
        mBackgroundColor = ApiCompatibilityUtils.getColor(activity.getResources(),
                R.color.default_primary_color);
        mThemeColor = ApiCompatibilityUtils.getColor(
                activity.getResources(), R.color.default_primary_color);

        mManager = new BookmarkManager(mActivity, false);
        Resources res = mActivity.getResources();

        MarginLayoutParams layoutParams = new MarginLayoutParams(
                MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0,
                res.getDimensionPixelSize(R.dimen.tab_strip_height)
                + res.getDimensionPixelSize(R.dimen.toolbar_height_no_shadow),
                0, 0);
        mManager.getView().setLayoutParams(layoutParams);
        mManager.setUrlChangeListener(this);
        if (mManager.doesDrawerExist()) mManager.addDrawerListener(this);
        mManager.addSearchViewStateListener(this);
    }

    @Override
    public View getView() {
        return mManager.getView();
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getUrl() {
        return mManager.getCurrentUrl();
    }

    @Override
    public String getHost() {
        return UrlConstants.BOOKMARKS_HOST;
    }

    @Override
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public int getThemeColor() {
        return mThemeColor;
    }

    @Override
    public void updateForUrl(String url) {
        mCurrentUrl = url;
        mManager.updateForUrl(url);
    }

    @Override
    public void destroy() {
        mManager.removeDrawerListener(this);
        mManager.removeSearchViewStateListener(this);
        mManager.destroy();
        mManager = null;
    }

    @Override
    public void onBookmarkUIStateChange(String url) {
        mNeedsCapture = true;
        if (url.equals(mCurrentUrl)) return;
        mTab.loadUrl(new LoadUrlParams(url));
    }

    @Override
    public boolean shouldCaptureThumbnail() {
        return mNeedsCapture;
    }

    @Override
    public void captureThumbnail(Canvas canvas) {
        //BrowserNewTabPage will handle this
    }

    public void onExternalCapture() {
        mNeedsCapture = false;
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        mNeedsCapture = true;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        mNeedsCapture = true;
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mNeedsCapture = true;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {

    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }
}
