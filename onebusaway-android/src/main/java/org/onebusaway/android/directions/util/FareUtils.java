package org.onebusaway.android.directions.util;

import android.text.TextUtils;

import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Money;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;

public class FareUtils {
    private static String FARE_NA = "";

    public static String getFare(Fare fare) {
        if (fare == null) {
            return FARE_NA;
        }

        if (fare.getRegular() != null) {
            return getFare(fare.getRegular());
        } else {
            // check the items in the array
            ArrayList<Fare> fareList = fare.getFare();
            if (fareList == null || fareList.isEmpty()) {
                return FARE_NA;
            }

            // calculate total fare
            int cents = 0;
            Money regularFare = null;
            for (Fare fareItem : fareList) {
                if (fareItem.getRegular() != null) {
                    regularFare = fareItem.getRegular();
                    cents += regularFare.getCents();
                }
            }

            if (regularFare == null) {
                return FARE_NA;
            }

            regularFare.setCents(cents);
            return getFare(regularFare);
        }
    }

    private static String getFare(Money currency) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        Currency cur = currency.getCurrency().getCurrency();
        if (cur == null) {
            return FARE_NA;
        } else {
            nf.setCurrency(cur);
            return nf.format((double)currency.getCents() / Math.pow(10.0D, (double)currency.getCurrency().getDefaultFractionDigits()));
        }
    }
}
