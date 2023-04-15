package au.mymetro.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.onebusaway.android.BuildConfig;
import org.onebusaway.android.R;

import java.util.Random;

public class RemoveAdsDialogFragment extends DialogFragment {
    private static final int LAUNCHES_UNTIL_PROMPT = 0;
    private static final int DAYS_UNTIL_PROMPT = 3;
    private static final int MILLIS_UNTIL_PROMPT = DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000;
    private static final String PREF_NAME = "REMOVE_ADS";
    private static final String LAST_PROMPT = "LAST_PROMPT";
    private static final String LAUNCHES = "LAUNCHES";
    private static final String DISABLED = "DISABLED";

    private Context parentContext;

    public static void show(Context context, FragmentManager fragmentManager) {
        if (BuildConfig.DEBUG) {
            Random rand = new Random(System.currentTimeMillis());
            if (rand.nextInt(100) % 10 == 0) {
                showRemoveAdsDialog(context, fragmentManager);
            }

            return;
        }
        boolean shouldShow = false;
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        // if user has disabled it then no further action
        if (sharedPreferences.getBoolean(DISABLED, false)) {
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        long currentTime = System.currentTimeMillis();
        long lastPromptTime = sharedPreferences.getLong(LAST_PROMPT, 0);
        if (lastPromptTime == 0) {
            lastPromptTime = currentTime;
            editor.putLong(LAST_PROMPT, lastPromptTime);
        }

        int launches = sharedPreferences.getInt(LAUNCHES, 0) + 1;
        if (launches > LAUNCHES_UNTIL_PROMPT) {
            if (currentTime > lastPromptTime + MILLIS_UNTIL_PROMPT) {
                shouldShow = true;
            }
        }
        editor.putInt(LAUNCHES, launches);

        if (shouldShow) {
            editor.putInt(LAUNCHES, 0).putLong(LAST_PROMPT, System.currentTimeMillis()).apply();
            showRemoveAdsDialog(context, fragmentManager);
        } else {
            editor.apply();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                .setTitle(R.string.remove_ads_title)
                .setMessage(R.string.remove_ads_caption)
                .setPositiveButton(R.string.remove_ads_dlg_btn_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent removeAds = new Intent(getParentContext(), RemoveAdsActivity.class);
                        startActivity(removeAds);
                        dismiss();
                    }
                })
                .setNeutralButton(R.string.remove_ads_dlg_btn_neutral, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.remove_ads_dlg_btn_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() != null) {
                            getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).apply();
                            dismiss();
                        }
                    }
                }).create();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

    private static void showRemoveAdsDialog(Context context, FragmentManager fragmentManager) {
        RemoveAdsDialogFragment dialog = new RemoveAdsDialogFragment();
        dialog.setParentContext(context);
        dialog.show(fragmentManager, null);
    }

    public Context getParentContext() {
        return parentContext;
    }

    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }
}