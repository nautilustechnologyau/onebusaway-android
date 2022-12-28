package au.mymetro.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.onebusaway.android.R;
import org.onebusaway.android.util.UIUtils;

public class RateItDialogFragment extends DialogFragment {
    private static final int LAUNCHES_UNTIL_PROMPT = 10;
    private static final int DAYS_UNTIL_PROMPT = 3;
    private static final int MILLIS_UNTIL_PROMPT = DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000;
    private static final String PREF_NAME = "APP_RATER";
    private static final String LAST_PROMPT = "LAST_PROMPT";
    private static final String LAUNCHES = "LAUNCHES";
    private static final String DISABLED = "DISABLED";
    private static final boolean debug = false;

    public static void show(Context context, FragmentManager fragmentManager) {
        if (debug) {
            new RateItDialogFragment().show(fragmentManager, null);
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
            new RateItDialogFragment().show(fragmentManager, null);
        } else {
            editor.apply();
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rate_title)
                .setMessage(R.string.rate_message)
                .setPositiveButton(R.string.rate_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() != null) {
                            UIUtils.goToMarket(getActivity(), getContext().getPackageName());
                            getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).apply();
                            dismiss();
                        }
                    }
                })
                .setNeutralButton(R.string.rate_remind_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.rate_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() != null) {
                            getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).apply();
                            dismiss();
                        }
                    }
                }).create();
    }
}