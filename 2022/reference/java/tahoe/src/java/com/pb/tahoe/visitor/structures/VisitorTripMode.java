package com.pb.tahoe.visitor.structures;

/**
 * User: Chris
 * Date: Mar 5, 2007 - 3:54:26 PM
 */
public enum VisitorTripMode {
    SameAsTour(0),
    FirstLegWalk(1),
    SecondLegWalk(2);

    private int id;

    private VisitorTripMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static VisitorTripMode getMode(int id) {
        switch (id) {
            case 1 : return FirstLegWalk;
            case 2 : return SecondLegWalk;
            default : return SameAsTour;
        }
    }
}
