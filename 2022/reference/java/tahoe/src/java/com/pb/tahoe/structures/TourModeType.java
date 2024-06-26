package com.pb.tahoe.structures;

/**
 * @author Freedman
 *
 * An enumeration of tour modes
 */
public final class TourModeType {

    public static final short TYPES=6;
    public static final short SOV=1;
    public static final short HOV=2;
    public static final short WALKTRANSIT=3;
    public static final short DRIVETRANSIT=4;
    public static final short NONMOTORIZED=5;
    public static final short SCHOOLBUS=6;

    public static final short TRANSITMODES=2;
    public static final short WALKMARKETS=3;

    public static final short SHORTWALK=1;
    public static final short LONGWALK=2;
    public static final short CANTWALK=3;

    public static final String[] LABELS = {"sov","hov",
    "walkTransit", "driveTransit","nonMotor", "schoolBus"};

}
