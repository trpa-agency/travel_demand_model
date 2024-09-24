package com.pb.tahoe.structures;

/**
 * SubModeType is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Oct 25, 2006
 */


/**
 * Enumeration of submodes.
 * 
 * Note: Only ZERO and LBS are being used in Tahoe.
 */
public enum SubModeType {
    ZERO ("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    LBS ("local bus service");

    private String description;

    private SubModeType(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

}

