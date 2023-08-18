package au.mymetro.android.ads;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.ads.Ad;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.onebusaway.android.BuildConfig;
import org.onebusaway.android.R;
import org.onebusaway.android.ui.HomeActivity;
import org.onebusaway.android.util.PreferenceUtils;

import java.util.Date;
import java.util.Random;

import au.mymetro.android.ui.RemoveAdsDialogFragment;

public class AdsManager {
    private static final String TAG = "AdsManager";

    private static boolean mAudienceNetworkInterstitialAdsEnabled = true;

    private static boolean mAudienceNetworkBannerAdsEnabled = true;

    private static boolean mAdmobInterstitialAdsEnabled = true;

    private static boolean mAdmobBannerAdsEnabled = true;

    private static String mMainTopAdsFormat = "banner";

    private static boolean mInterstitialAdShowing = false;

    private static long mLastInterstitialAdShowTime = 0L;

    private static int mInterstitialAdShowCount = 0;

    private static boolean mShowAdsInArrivalList = true;

    private static final Random mRandom = new Random();

    private AppCompatActivity mActivity;

    private LinearLayout mBannerAdView;

    private TemplateView mNativeAdView;

    // Ad views
    private InterstitialAd mAdMobInterstitialAd;
    private com.facebook.ads.InterstitialAd mAnInterstitialAd;

    public AdsManager(AppCompatActivity activity) {
        this.mActivity = activity;
    }

    public void loadMainTopAd(LinearLayout bannerAdView, TemplateView nativeAdView) {
        if (mMainTopAdsFormat.equals("banner")) {
            hideAdView(nativeAdView);
            loadBannerAd(bannerAdView);
        } else if (mMainTopAdsFormat.equals("native")) {
            hideAdView(bannerAdView);
            loadNativeAd(nativeAdView);
        }
    }

    public void hideMainTopAd() {
        if (mMainTopAdsFormat.equals("banner")) {
            hideAdView(mBannerAdView);
        } else if (mMainTopAdsFormat.equals("native")) {
            hideAdView(mNativeAdView);
        }
    }

    public void showMainTopAd() {
        if (mMainTopAdsFormat.equals("banner")) {
            showAdView(mBannerAdView);
        } else if (mMainTopAdsFormat.equals("native")) {
            showAdView(mNativeAdView);
        }
    }
    public void loadBannerAd(LinearLayout bannerAdView) {
        if (!BuildConfig.ENABLE_ADMOB || bannerAdView == null) {
            return;
        }

        if (bannerAdView != mBannerAdView && mBannerAdView != null) {
            mBannerAdView.setVisibility(View.GONE);
            return;
        }

        mBannerAdView = bannerAdView;
        mBannerAdView.setVisibility(View.GONE);

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (adsFreeVersion) {
            bannerAdView.setVisibility(View.GONE);
        } else {
            mBannerAdView.setVisibility(View.VISIBLE);
            loadAdmobBannerAds(bannerAdView);
        }
    }

    private void setBannerAdVisibility(LinearLayout mBannerAdView) {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        if (mBannerAdView == null) {
            return;
        }

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (adsFreeVersion) {
            mBannerAdView.setVisibility(View.GONE);
            return;
        }

        mBannerAdView.setVisibility(View.VISIBLE);
    }

    private AdSize getAdSize() {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);
    }

    private void loadAdmobBannerAds(LinearLayout bannerAdLayoutView) {
        if (!mAdmobBannerAdsEnabled) {
            return;
        }

        if (bannerAdLayoutView == null) {
            return;
        }

        AdRequest bannerAdRequest = new AdRequest.Builder().build();
        AdView admobAdView = new AdView(mActivity);
        if (admobAdView.getParent() != null) {
            ((ViewGroup) admobAdView.getParent()).removeView(admobAdView);
        }
        bannerAdLayoutView.removeAllViews();
        bannerAdLayoutView.addView(admobAdView);
        AdSize adSize = getAdSize();
        admobAdView.setAdSize(adSize);
        admobAdView.setAdUnitId(mActivity.getResources().getString(R.string.admob_banner_unit_id));
        admobAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d(TAG, "onAdLoaded()");
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                RemoveAdsDialogFragment.show(mActivity.getApplicationContext(), mActivity.getSupportFragmentManager());
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, "AdMob onAdFailedToLoad(): " + loadAdError.getMessage());
                loadAudienceNetworkBannerAds(bannerAdLayoutView);
            }
        });
        admobAdView.loadAd(bannerAdRequest);
    }

    private void loadAudienceNetworkBannerAds(LinearLayout bannerAdLayoutView) {
        if (!mAudienceNetworkBannerAdsEnabled) {
            return;
        }

        if (bannerAdLayoutView == null) {
            return;
        }

        String placementId = mActivity.getResources().getString(R.string.an_banner_placement_id);
        if (BuildConfig.DEBUG) {
            placementId = "IMG_16_9_APP_INSTALL#" + placementId;
        }
        com.facebook.ads.AdView anAdView =
                new com.facebook.ads.AdView(mActivity,
                        placementId,
                        com.facebook.ads.AdSize.BANNER_HEIGHT_50);
        bannerAdLayoutView.removeAllViews();
        bannerAdLayoutView.addView(anAdView);

        com.facebook.ads.AdListener adListener = new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                // Ad error callback
                Log.d(TAG, "Audience Network onError: " + adError.getErrorCode() + " - " + adError.getErrorMessage());
                if (BuildConfig.DEBUG) {
                    Toast.makeText(mActivity,
                            "Error: " + adError.getErrorMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
                Log.d(TAG, "Audience Network onAdLoaded: " + ad.toString());
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
                Log.d(TAG, "Audience Network onAdClicked: " + ad.toString());
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
                Log.d(TAG, "Audience Network onLoggingImpression: " + ad.toString());
            }
        };

        anAdView.loadAd(anAdView.buildLoadAdConfig().withAdListener(adListener).build());
    }

    public void hideAdViews() {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        if (mNativeAdView != null) {
            mNativeAdView.setVisibility(View.GONE);
        }

        if (mBannerAdView != null) {
            mBannerAdView.setVisibility(View.GONE);
        }
    }

    public void hideAdView(View mBannerAdView) {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        if (mBannerAdView != null) {
            mBannerAdView.setVisibility(View.GONE);
        }
    }

    public void showAdViews() {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (mBannerAdView != null && !adsFreeVersion) {
            if (mNativeAdView != null) {
                mNativeAdView.setVisibility(View.VISIBLE);
            }

            if (mBannerAdView != null) {
                mBannerAdView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void showAdView(View mBannerAdView) {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (mBannerAdView != null && !adsFreeVersion) {
            mBannerAdView.setVisibility(View.VISIBLE);
        }
    }

    public void updateBannerAdsVisibility(LinearLayout mBannerAdView) {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (mBannerAdView != null) {
            if (adsFreeVersion) {
                mBannerAdView.setVisibility(View.GONE);
            } else {
                mBannerAdView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void loadInterstitialAd(boolean showAlways, String initiator, int markerClickedCount) {
        if (!BuildConfig.ENABLE_ADMOB) {
            return;
        }

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (adsFreeVersion) {
            return;
        }

        if (BuildConfig.DEBUG) {
            Random rand = new Random(System.currentTimeMillis());
            if (rand.nextInt(100) % 10 != 0) {
                return;
            }
        }

        if (!showAlways && !BuildConfig.DEBUG) {
            if ((initiator == null) && (markerClickedCount % 5 != 0)) {
                return;
            }

            float hitPercent = 0.7f; // 30% of the time it will show ad
            Log.d(TAG, "Ad hit percentage: " + hitPercent);
            if (mRandom.nextFloat() > hitPercent) {
                return;
            }

            if (mLastInterstitialAdShowTime > 0) {
                long now = new Date().getTime();
                long duration = now - mLastInterstitialAdShowTime;
                // don't show add too frequently. at least 60 secs gap.
                if (duration < 60000) {
                    return;
                }
            }
        }

        // we will not show ad first time on home screen
        if ("Nearby".equals(initiator) && mInterstitialAdShowCount == 0) {
            mInterstitialAdShowCount++;
            return;
        }

        loadAdMobInterstitialAd();
    }

    private void loadAdMobInterstitialAd() {
        if (!mAdmobInterstitialAdsEnabled) {
            return;
        }
        // an add is showing, don't show again
        if (mInterstitialAdShowing) {
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(mActivity, mActivity.getResources().getString(R.string.admob_interstitial_unit_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mAdMobInterstitialAd = interstitialAd;
                        // Log.i(TAG, "onAdLoaded");
                        Log.d(TAG, "Banner adapter class name: " + interstitialAd.getResponseInfo().getMediationAdapterClassName());
                        mAdMobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                mAdMobInterstitialAd = null;
                                mInterstitialAdShowing = false;
                                RemoveAdsDialogFragment.show(mActivity, mActivity.getSupportFragmentManager());
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "AdMob onAdFailedToShowFullScreenContent(): " + adError.getMessage());
                                mAdMobInterstitialAd = null;
                                mInterstitialAdShowing = false;
                                loadAudienceNetworkInterstitialAds();
                            }
                        });
                        if (!mInterstitialAdShowing) {
                            mInterstitialAdShowing = true;
                            mInterstitialAdShowCount++;
                            mLastInterstitialAdShowTime = new Date().getTime();
                            mAdMobInterstitialAd.show(mActivity);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, "AdMob onAdFailedToLoad: " + loadAdError.toString());
                        mAdMobInterstitialAd = null;
                        mInterstitialAdShowing = false;
                        loadAudienceNetworkInterstitialAds();
                    }
                });

    }

    void loadAudienceNetworkInterstitialAds() {
        if (!mAudienceNetworkInterstitialAdsEnabled) {
            return;
        }

        // an add is showing, don't show again
        if (mInterstitialAdShowing) {
            return;
        }
        String placementId = mActivity.getResources().getString(R.string.an_interstitial_placement_id);
        if (BuildConfig.DEBUG) {
            placementId = "CAROUSEL_IMG_SQUARE_APP_INSTALL#" + placementId;
        }
        mAnInterstitialAd = new com.facebook.ads.InterstitialAd(
                mActivity, placementId);
        // Create listeners for the Interstitial Ad
        InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                // Interstitial ad displayed callback
                mInterstitialAdShowing = true;
                mInterstitialAdShowCount++;
                Log.e(TAG, "Interstitial ad displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                // Interstitial dismissed callback
                Log.e(TAG, "Audience Network Interstitial ad dismissed.");
                mAnInterstitialAd = null;
                mInterstitialAdShowing = false;
            }

            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                // Ad error callback
                Log.e(TAG, "Audience Network Interstitial ad failed to load: " + adError.getErrorCode() + " - " + adError.getErrorMessage());
                if (BuildConfig.DEBUG) {
                    Toast.makeText(mActivity,
                            "Error: " + adError.getErrorMessage(),
                            Toast.LENGTH_LONG).show();
                }
                mAnInterstitialAd = null;
                mInterstitialAdShowing = false;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Interstitial ad is loaded and ready to be displayed
                Log.d(TAG, "Audience Network Interstitial ad is loaded and ready to be displayed!");
                // Show the ad
                mInterstitialAdShowing = true;
                mAnInterstitialAd.show();
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
                Log.d(TAG, "Audience Network Interstitial ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
                Log.d(TAG, "Audience Network Interstitial ad impression logged!");
            }
        };

        // For auto play video ads, it's recommended to load the ad
        // at least 30 seconds before it is shown
        mAnInterstitialAd.loadAd(
                mAnInterstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());
    }

    public void loadNativeAd(TemplateView nativeAdView) {
        if (!BuildConfig.ENABLE_ADMOB || nativeAdView == null) {
            return;
        }

        if (nativeAdView != mNativeAdView && mNativeAdView != null) {
            return;
        }

        mNativeAdView = nativeAdView;
        mNativeAdView.setVisibility(View.GONE);

        boolean adsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (adsFreeVersion) {
            nativeAdView.setVisibility(View.GONE);
        } else {
            VideoOptions videoOptions = new VideoOptions.Builder()
                    .setStartMuted(false)
                    .build();

            NativeAdOptions adOptions = new NativeAdOptions.Builder()
                    .setVideoOptions(videoOptions)
                    .setRequestMultipleImages(true)
                    .build();
            AdLoader adLoader = new AdLoader.Builder(mActivity, mActivity.getResources().getString(R.string.admob_native_unit_id))
                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                        @Override
                        public void onNativeAdLoaded(NativeAd nativeAd) {
                            nativeAdView.setNativeAd(nativeAd);
                            nativeAdView.setVisibility(View.VISIBLE);
                        }
                    })
                    .withNativeAdOptions(adOptions)
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());

        }
    }

    public void destroy() {
        if (mAnInterstitialAd != null) {
            mAnInterstitialAd.destroy();
        }
    }

    public static boolean isAudienceNetworkInterstitialAdsEnabled() {
        return mAudienceNetworkInterstitialAdsEnabled;
    }

    public static void setAudienceNetworkInterstitialAdsEnabled(boolean value) {
        AdsManager.mAudienceNetworkInterstitialAdsEnabled = value;
    }

    public static boolean isAudienceNetworkBannerAdsEnabled() {
        return mAudienceNetworkBannerAdsEnabled;
    }

    public static void setAudienceNetworkBannerAdsEnabled(boolean value) {
        AdsManager.mAudienceNetworkBannerAdsEnabled = value;
    }

    public static boolean isAdmobInterstitialAdsEnabled() {
        return mAdmobInterstitialAdsEnabled;
    }

    public static void setAdmobInterstitialAdsEnabled(boolean value) {
        AdsManager.mAdmobInterstitialAdsEnabled = value;
    }

    public static boolean isAdmobBannerAdsEnabled() {
        return mAdmobBannerAdsEnabled;
    }

    public static void setAdmobBannerAdsEnabled(boolean value) {
        AdsManager.mAdmobBannerAdsEnabled = value;
    }

    public static String getMainTopAdsFormat() {
        return mMainTopAdsFormat;
    }

    public static void setMainTopAdsFormat(String format) {
        AdsManager.mMainTopAdsFormat = format;
    }

    public static boolean isShowAdsInArrivalList() {
        return mShowAdsInArrivalList;
    }

    public static void setShowAdsInArrivalList(boolean mShowAdsInArrivalList) {
        AdsManager.mShowAdsInArrivalList = mShowAdsInArrivalList;
    }
}
