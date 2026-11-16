package com.virtu_stock.Projection;


public interface SubscriptionProjection {

    String getIpoId();

    Double getQib();

    Double getNonInstitutional();

    Double getRetailer();

    Double getTotal();
}