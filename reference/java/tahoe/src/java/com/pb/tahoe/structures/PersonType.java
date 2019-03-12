package com.pb.tahoe.structures;

import java.io.Serializable;

/**
 * @author Freedman
 *
 * An enumeration of person types.
 */
public final class PersonType implements Serializable {

    public static final short TYPES=6;
    public static final short WORKER_F = 1;
    public static final short WORKER_P = 2;
    public static final short NONWORKER = 3;
    public static final short PRESCHOOL = 4;
    public static final short SCHOOL_PRED =5;
    public static final short SCHOOL_DRIV = 6;

    public static String FULL_TIME = "ft";
    public static String PART_TIME = "pt";
    public static String NON_WORKER = "non";
    public static String PRESCHOOLER =  "presch";
    public static String PREDRIVER =  "predriv";
    public static String DRIVER =  "driver";

    public static String getDescription(short type) {
        String description = "";
        switch(type){
            case 1:
                description = "worker_f";
                break;
            case 2:
                description = "worker_p";
                break;
            case 3:
                description = "non-worker";
                break;
            case 4:
                description = "preschooler";
                break;
            case 5:
                description = "schoolpred";
                break;
            case 6:
                description = "schooldriv";
                break;
           default:
                description = "something is weird";
                break;
        }
        return description;
    }

    public static boolean isAdult(short type){
        boolean adult = false;
        if(type < 4) adult = true;
        return adult;
    }

    public static boolean isPreschooler(short type){
        boolean preschooler = false;
        if(type == 4) preschooler = true;
        return preschooler;
    }

}
