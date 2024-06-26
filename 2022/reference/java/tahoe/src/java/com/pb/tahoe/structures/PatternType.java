package com.pb.tahoe.structures;

import java.io.Serializable;

/**
 * @author Freedman
 *
 * An enumeration of Pattern Types (day pattern results)
 */
public final class PatternType implements Serializable {

    public static final short TYPES=7;
    public static final short WORK_1 = 1;
    public static final short WORK_2 =2;
    public static final short SCHOOL_1 = 3;
    public static final short SCHOOL_2 = 4;
    public static final short SCHOOL_WORK = 5;
    public static final short NON_MAND = 6;
    public static final short HOME =7;

    public static short getPatternType(String header){

        if(header.contains("work_1")) return WORK_1;
        if(header.contains("work_2")) return WORK_2;
        if(header.contains("school_1")) return SCHOOL_1;
        if(header.contains("school_2")) return SCHOOL_2;
        if(header.contains("school_work")) return SCHOOL_WORK;
        if(header.contains("non_mand")) return NON_MAND;
        if(header.contains("home")) return HOME;
        return -1;
    }

    public static String getDescription(short type) {
        String description = "";
        switch(type){
            case 1:
                description = "work_1";
                break;
            case 2:
                description = "work_2";
                break;
            case 3:
                description = "school_1";
                break;
            case 4:
                description = "school_2";
                break;
            case 5:
                description = "school_work";
                break;
            case 6:
                description = "non_mand";
                break;
            case 7:
                description = "home";
                break;
            default:
                description = "something is weird";
                break;
        }
        return description;

    }

}
