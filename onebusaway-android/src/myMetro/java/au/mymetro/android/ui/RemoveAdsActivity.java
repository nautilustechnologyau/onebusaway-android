package au.mymetro.android.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.onebusaway.android.R;
import org.onebusaway.android.io.ObaAnalytics;
import org.onebusaway.android.ui.HomeActivity;
import org.onebusaway.android.ui.NavHelp;
import org.onebusaway.android.util.PreferenceUtils;
import org.onebusaway.android.util.UIUtils;

import au.mymetro.android.billing.BillingClientLifecycle;

public class RemoveAdsActivity extends AppCompatActivity {

    public static final String TAG = "RemoveAdsActivity";

    private FirebaseAnalytics mFirebaseAnalytics;

    private BillingClientLifecycle mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remove_ads);
        UIUtils.setupActionBar(this);
        setTitle(getResources().getString(R.string.remove_ads_title));
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mBillingClient = BillingClientLifecycle.getInstance(getApplication());

        setupPurchaseStatusView();
        setupProductDetailsView();

        mBillingClient.newPurchaseEvent.observe(this, new Observer<BillingResult>() {
            @Override
            public void onChanged(BillingResult billingResult) {
                Log.d(TAG, billingResult.toString());
                setupPurchaseStatusView();
                setupProductDetailsView();
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    NavHelp.goHome(RemoveAdsActivity.this, false);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Ensure the software and hardware back buttons have the same behavior
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupProductDetailsView() {
        FrameLayout layout = findViewById(R.id.productDetailsLayout);
        TextView productDetailsTitleTextView = findViewById(R.id.productDetailsTitle);
        TextView productDetailsDescTextView = findViewById(R.id.productDetailsDesc);
        TextView productDetailsPriceTextView = findViewById(R.id.productDetailsPrice);

        ProductDetails productDetails = mBillingClient.getAdsFreeProductDetails();
        if (productDetails == null) {
            layout.setVisibility(View.GONE);
        } else {
            productDetailsTitleTextView.setText(productDetails.getTitle());
            productDetailsDescTextView.setText(productDetails.getDescription());

            ProductDetails.PricingPhase pricing = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0);
            String price = pricing.getPriceCurrencyCode() + " " + pricing.getFormattedPrice() + " / " + pricing.getBillingPeriod();
            productDetailsPriceTextView.setText(price);
            layout.setVisibility(View.VISIBLE);
        }
    }

    private void setupPurchaseStatusView() {
        TextView purchaseStatusView = findViewById(R.id.purchaseStatus);
        purchaseStatusView.setVisibility(View.GONE);
        Button purchaseButton = findViewById(R.id.purchaseBtn);
        purchaseButton.setEnabled(true);

        boolean isAdsFreeVersion = PreferenceUtils.getBoolean(HomeActivity.ADS_FREE_VERSION, false);
        if (isAdsFreeVersion) {
            purchaseStatusView.setVisibility(View.VISIBLE);
            purchaseButton.setEnabled(false);
        }
    }

    public void onBuyAdsFreeSubscriptionBtnClick(View view) {
        mBillingClient.launchBillingFlow(this, BillingClientLifecycle.ADS_FREE_PRODUCT_ID);
        ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                getString(R.string.analytics_label_button_press_buy_ads_free_subscription),
                null);
    }
}
