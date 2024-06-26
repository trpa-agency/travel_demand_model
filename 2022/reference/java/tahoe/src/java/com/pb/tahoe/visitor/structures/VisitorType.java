package com.pb.tahoe.visitor.structures;

/**
 * User: Chris
 * Date: Mar 12, 2007 - 2:40:50 PM
 */
public enum VisitorType {
    OVERNIGHT(1),
    DAY(2),
    THRU(3);

    private int id;

    private VisitorType(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public static VisitorType getVisitorType(int id) {
        switch (id) {
            case 1 : return OVERNIGHT;
            case 3 : return THRU;
            default : return DAY;
        }
    }
}
