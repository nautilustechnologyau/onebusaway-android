package au.mymetro.android.ads;

import static com.facebook.ads.AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CALLBACK_MODE;
import static com.facebook.ads.AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CRASH_DEBUG_MODE;

import android.content.Context;
import android.util.Log;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BuildConfig;

public class AudienceNetworkInitializeHelper implements AudienceNetworkAds.InitListener {

    /**
     * It's recommended to call this method from Application.onCreate().
     * Otherwise you can call it from all Activity.onCreate()
     * methods for Activities that contain ads.
     *
     * @param context Application or Activity.
     */
    public static void initialize(Context context) {
        if (!AudienceNetworkAds.isInitialized(context)) {
            if (BuildConfig.DEBUG) {
                AdSettings.turnOnSDKDebugger(context);
                AdSettings.setIntegrationErrorMode(INTEGRATION_ERROR_CALLBACK_MODE);
                AdSettings.addTestDevice("d2de8850-e804-439d-96cc-1755a201484d");
            }

            AudienceNetworkAds
                    .buildInitSettings(context)
                    .withInitListener(new AudienceNetworkInitializeHelper())
                    .initialize();

        }
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult result) {
        Log.d(AudienceNetworkAds.TAG, result.getMessage());
    }
}
