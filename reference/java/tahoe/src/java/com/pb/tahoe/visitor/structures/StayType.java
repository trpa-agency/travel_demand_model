package com.pb.tahoe.visitor.structures;

/**
 * User: Chris
 * Date: Feb 7, 2007 - 10:38:46 AM
 */
public enum StayType {
    SEASONAL(1),
    HOTELMOTEL(2),
    CASINO(3),
    RESORT(4),
    HOUSE(5),
    CAMPGROUND(6);

    private int stayTypeID;

    private StayType(int stayTypeID) {
        this.stayTypeID = stayTypeID;
    }

    public int getID() {
        return stayTypeID;
    }

    public static StayType getStayType(int stayTypeID) {
        switch(stayTypeID) {
            case 2 : return HOTELMOTEL;
            case 3 : return CASINO;
            case 4 : return RESORT;
            case 5 : return HOUSE;
            case 6 : return CAMPGROUND;
            default : return SEASONAL;
        }
    }

    public static boolean isIDValid(int stayTypeID) {
        return ((stayTypeID > 0) && (stayTypeID <= StayType.values().length));
    }
}
