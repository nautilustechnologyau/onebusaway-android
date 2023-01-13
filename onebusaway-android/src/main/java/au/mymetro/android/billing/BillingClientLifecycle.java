package au.mymetro.android.billing;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.mymetro.android.ui.SingleLiveEvent;

public class BillingClientLifecycle implements DefaultLifecycleObserver, PurchasesUpdatedListener,
        BillingClientStateListener, ProductDetailsResponseListener, PurchasesResponseListener {

    private static final String TAG = "BillingClientLifecycle";
    public static final String ADS_FREE_PRODUCT_ID = "au.mymetro_adfree.android";

    private static final List<String> LIST_OF_PRODUCT_IDS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(ADS_FREE_PRODUCT_ID);
            }});

    /**
     * The purchase event is observable. Only one observer will be notified.
     */
    public SingleLiveEvent<List<Purchase>> purchaseUpdateEvent = new SingleLiveEvent<>();

    /**
     * The purchase event is observable. Only one observer will be notified.
     */
    public SingleLiveEvent<BillingResult> newPurchaseEvent = new SingleLiveEvent<>();

    /**
     * Purchases are observable. This list will be updated when the Billing Library
     * detects new or existing purchases. All observers will be notified.
     */
    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>();

    /**
     * ProductDetails for all known product IDs.
     */
    public MutableLiveData<Map<String, ProductDetails>> productsWithProductDetails = new MutableLiveData<>();

    private static volatile BillingClientLifecycle INSTANCE;

    private Application mApp;
    private BillingClient billingClient;
    private boolean mLoading = false;

    private BillingClientLifecycle(Application app) {
        mApp = app;
    }

    public static BillingClientLifecycle getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (BillingClientLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingClientLifecycle(app);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        if (this.mLoading) {
            return;
        }
        this.mLoading = true;
        Log.d(TAG, "ON_CREATE");
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        billingClient = BillingClient.newBuilder(mApp)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();
        if (!billingClient.isReady()) {
            Log.d(TAG, "BillingClient: Start connection...");
            billingClient.startConnection(this);
        }
    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        this.mLoading = false;
        Log.d(TAG, "ON_DESTROY");
        if (billingClient.isReady()) {
            Log.d(TAG, "BillingClient can only be used once -- closing connection");
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected");
        retryBillingServiceConnectionWithExponentialBackoff();
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
            queryProductDetails();
            queryPurchases();
        } else {
            retryBillingServiceConnectionWithExponentialBackoff();
        }
    }

    /**
     * Receives the result from {@link #queryProductDetails()}}.
     * <p>
     * Store the SkuDetails and post them in the {@link #productsWithProductDetails}. This allows other
     * parts of the app to use the {@link ProductDetails} to show SKU information and make purchases.
     */
    @Override
    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
        if (billingResult == null) {
            Log.wtf(TAG, "onProductDetailsResponse: null BillingResult");
            return;
        }

        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                Log.i(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
                final int expectedProductDetailsCount = LIST_OF_PRODUCT_IDS.size();
                if (productDetailsList == null) {
                    productsWithProductDetails.postValue(Collections.<String, ProductDetails>emptyMap());
                    Log.e(TAG, "onProductDetailsResponse: " +
                            "Expected " + expectedProductDetailsCount + ", " +
                            "Found null ProductDetails. " +
                            "Check to see if the Product IDs you requested are correctly published " +
                            "in the Google Play Console.");
                } else {
                    Map<String, ProductDetails> newProductDetailList = new HashMap<String, ProductDetails>();
                    for (ProductDetails skuDetails : productDetailsList) {
                        newProductDetailList.put(skuDetails.getProductId(), skuDetails);
                    }
                    productsWithProductDetails.postValue(newProductDetailList);
                    int productDetailsCount = newProductDetailList.size();
                    if (productDetailsCount == expectedProductDetailsCount) {
                        Log.i(TAG, "onProductDetailsResponse: Found " + productDetailsCount + " ProductDetails");
                    } else {
                        Log.e(TAG, "onProductDetailsResponse: " +
                                "Expected " + expectedProductDetailsCount + ", " +
                                "Found " + productDetailsCount + " ProductDetails. " +
                                "Check to see if the product IDs you requested are correctly published " +
                                "in the Google Play Console.");
                    }
                }
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Log.e(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            // These response codes are not expected.
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }

    /**
     * Query Google Play Billing for existing purchases.
     * <p>
     * New purchases will be provided to the PurchasesUpdatedListener.
     * You still need to check the Google Play Billing API to know when purchase tokens are removed.
     */
    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),this
        );
    }

    /**
     * Callback from the billing library when queryPurchasesAsync is called.
     */
    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        processPurchases(list);
    }

    /**
     * Called by the Billing Library when new purchases are detected.
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult == null) {
            Log.wtf(TAG, "onPurchasesUpdated: null BillingResult");
            return;
        }
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, String.format("onPurchasesUpdated: %s %s",responseCode, debugMessage));
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: null purchase list");
                }
                processPurchases(purchases);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }

        newPurchaseEvent.postValue(billingResult);
    }

    /**
     * Send purchase SingleLiveEvent and update purchases LiveData.
     * <p>
     * The SingleLiveEvent will trigger network call to verify the subscriptions on the sever.
     * The LiveData will allow Google Play settings UI to update based on the latest purchase data.
     */
    private void processPurchases(List<Purchase> purchasesList) {
        if (purchasesList != null) {
            Log.d(TAG, "processPurchases: " + purchasesList.size() + " purchase(s)");
        } else {
            Log.d(TAG, "processPurchases: with no purchases");
        }

        if (isUnchangedPurchaseList(purchasesList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed");
            return;
        }

        purchaseUpdateEvent.postValue(purchasesList);
        purchases.postValue(purchasesList);

        if (purchasesList != null) {
            logAcknowledgementStatus(purchasesList);
        }
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.isAcknowledged()) {
                ack_yes++;
            } else {
                ack_no++;
            }
        }
        Log.d(TAG, "logAcknowledgementStatus: acknowledged=" + ack_yes +
                " unacknowledged=" + ack_no);
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    public void acknowledgePurchases(List<Purchase> purchasesList) {
        if (purchasesList == null) {
            return;
        }

        int ack = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase.getPurchaseToken());
                    ack++;
                } else {
                    Log.d(TAG, "Purchase already acknowledged");
                }
            }
        }

        if (ack > 0) {
            queryProductDetails();
        }
    }

    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }

    /**
     * In order to make purchases, you need the {@link ProductDetails} for the item or subscription.
     * This is an asynchronous call that will receive a result in {@link #onProductDetailsResponse}.
     */
    public void queryProductDetails() {
        Log.d(TAG, "queryProductDetails");
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String productId : LIST_OF_PRODUCT_IDS) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build());
        }

        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.unmodifiableList(productList))
                .build();
        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                this
        );
    }

    public int launchBillingFlow(Activity activity, String productId) {
        ProductDetails productDetails = productsWithProductDetails.getValue().get(productId);
        BillingFlowParams.ProductDetailsParams.Builder builder = BillingFlowParams.ProductDetailsParams.newBuilder();
        builder = builder.setProductDetails(productDetails);

        // TODO: process offers
        // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
        // for a list of offers that are available to the user
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        builder = builder.setOfferToken(offers.get(0).getOfferToken());

        ImmutableList productDetailsParamsList =
                ImmutableList.of(builder.build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        return launchBillingFlow(activity, billingFlowParams);
    }

    /**
     * Launching the billing flow.
     * <p>
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    public int launchBillingFlow(Activity activity, BillingFlowParams params) {
        if (!billingClient.isReady()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * TODO(134506821): Acknowledge purchases on the server.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    public void acknowledgePurchase(String purchaseToken) {
        Log.d(TAG, "acknowledgePurchase");
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
            }
        });
    }

    private void retryBillingServiceConnectionWithExponentialBackoff() {

    }

    public boolean hasPurchasedAdsFreeSubscription() {
        return hasPurchased(ADS_FREE_PRODUCT_ID);
    }

    public boolean hasPurchased(String productId) {
        Purchase purchase = getPurchaseDetails(productId);
        return purchase != null;
    }

    public Purchase getAdsFreePurchaseDetails() {
        return getPurchaseDetails(ADS_FREE_PRODUCT_ID);
    }

    private Purchase getPurchaseDetails(String productId) {
        List<Purchase> purchases = this.purchases.getValue();
        if (purchases == null || purchases.isEmpty()) {
            return null;
        }

        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                for (String purchasedProductId : purchase.getProducts()) {
                    if (productId.equals(purchasedProductId)) {
                        return purchase;
                    }
                }
            }
        }

        return null;
    }

    public ProductDetails getAdsFreeProductDetails() {
        return getProductDetails(ADS_FREE_PRODUCT_ID);
    }

    private ProductDetails getProductDetails(String productId) {
        Map<String, ProductDetails> productList = this.productsWithProductDetails.getValue();
        if (productList != null && productList.containsKey(productId)) {
            return productList.get(productId);
        }

        return null;
    }
}
