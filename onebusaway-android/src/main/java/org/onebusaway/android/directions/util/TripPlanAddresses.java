package org.onebusaway.android.directions.util;

public class TripPlanAddresses {
    private int id;
    private String planName;
    private CustomAddress fromAddress;
    private CustomAddress toAddress;

    public TripPlanAddresses() {
    }

    public TripPlanAddresses(int id,
                             String planName,
                             String fromAddressLine1,
                             double fromAddressLatitude,
                             double fromAddressLongitude,
                             String toAddressLine1,
                             double toAddressLatitude,
                             double toAddressLongitude) {

        this.id = id;
        this.planName = planName;

        fromAddress = CustomAddress.getEmptyAddress();
        fromAddress.setAddressLine(0, fromAddressLine1);
        fromAddress.setLatitude(fromAddressLatitude);
        fromAddress.setLongitude(fromAddressLongitude);

        toAddress = CustomAddress.getEmptyAddress();
        toAddress.setAddressLine(0, toAddressLine1);
        toAddress.setLatitude(toAddressLatitude);
        toAddress.setLongitude(toAddressLongitude);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public CustomAddress getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(CustomAddress fromAddress) {
        this.fromAddress = fromAddress;
    }

    public CustomAddress getToAddress() {
        return toAddress;
    }

    public void setToAddress(CustomAddress toAddress) {
        this.toAddress = toAddress;
    }

    @Override
    public String toString() {
        return "TripPlanAddresses{" +
                "id=" + id +
                ", planName='" + planName + '\'' +
                ", fromAddress=" + fromAddress +
                ", toAddress=" + toAddress +
                '}';
    }
}
