package com.pb.tahoe.structures;

/**
 * @author Freedman
 *
 * An enumeration of tour modes
 */
public final class TourModeType {

    public static final short TYPES=7;
    public static final short SOV=1;
    public static final short HOV=2;
    public static final short WALKTRANSIT=3;
    public static final short DRIVETRANSIT=4;
    public static final short WALK=5;
    public static final short BIKE=6;
    public static final short SCHOOLBUS=7;
        
    public static final short TRANSITMODES=2;
    public static final short WALKMARKETS=3;

    public static final short SHORTWALK=1;
    public static final short LONGWALK=2;
    public static final short CANTWALK=3;

    public static final String[] LABELS = {"sov","hov",
    "walkTransit", "driveTransit","walk", "bike", "schoolBus"};
    
    /**
     * Check if mode is walk or bike.
     * 
     * @param mode
     * @return TRUE if walk or bike, else FALSE
     */
    public static boolean isNonmotor(int mode) {
    	
    	if((mode==WALK)||(mode==BIKE))
    		return true;
    	return false;
    }

    /**
     * Check if mode is SOV or HOV
     * 
     * @param mode
     * @return TRUE if SOV or HOV, else FALSE
     */
    public static boolean isAuto(int mode) {
    	
    	if((mode==SOV)||(mode==HOV))
    		return true;
    	return false;
    }
}
