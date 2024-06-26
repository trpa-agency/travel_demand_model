package com.pb.tahoe.structures;

/**
 * @author Freedman
 *
 * An enumeration of tour types.
 */
public final class TourType {

    public static final short TYPES=8;
    public static final short WORK = 1;
    public static final short SCHOOL = 2;
    public static final short ESCORTING = 3;
    public static final short SHOP = 4;
    public static final short OTHER_MAINTENANCE = 5;
    public static final short DISCRETIONARY=6;
    public static final short EAT = 7;
    public static final short ATWORK = 8;

    public static final short CATEGORIES = 4;
    public static final short MANDATORY_CATEGORY = 1;
    public static final short JOINT_CATEGORY = 2;
    public static final short NON_MANDATORY_CATEGORY = 3;
    public static final short AT_WORK_CATEGORY = 4;



   public static short[] getTourTypesForCategory(short category){
        switch(category){
            case MANDATORY_CATEGORY:
                return new short[]{WORK, SCHOOL};
            case JOINT_CATEGORY:
                return new short[]{SHOP, OTHER_MAINTENANCE, DISCRETIONARY, EAT};
            case NON_MANDATORY_CATEGORY:
                return new short[]{ESCORTING, SHOP, OTHER_MAINTENANCE, DISCRETIONARY, EAT};
            case AT_WORK_CATEGORY:
                return new short[]{ATWORK};
            default:
                return null;
        }
    }

    public static String[] getTourTypeLabelsForCategory(short category){
        switch(category){
            case MANDATORY_CATEGORY:
                return new String[]{ "WORK", "SCHOOL" };
            case JOINT_CATEGORY:
                return new String[]{ "SHOP", "OTHER_MAINT", "DISCR", "EAT" };
            case NON_MANDATORY_CATEGORY:
                return new String[]{ "ESCORTING", "SHOP", "OTHER_MAINT", "DISCR", "EAT" };
            case AT_WORK_CATEGORY:
                return new String[]{"AT_WORK"};
            default:
                return null;
        }
    }

    public static String getCategoryLabelForCategory(short category){
        switch(category){
            case MANDATORY_CATEGORY:
                return "MANDATORY";
            case JOINT_CATEGORY:
                return "JOINT";
            case NON_MANDATORY_CATEGORY:
                return "NON-MANDATORY";
            case AT_WORK_CATEGORY:
                return "AT-WORK";
            default:
                return null;
        }
    }

    public static String getTypeLabel(short type){
        switch(type){
            case WORK:
                return "work";
            case SCHOOL:
                return "school";
            case ESCORTING:
                return "escort";
            case SHOP:
                return "shop";
            case OTHER_MAINTENANCE:
                return "oth_main";
            case DISCRETIONARY:
                return "disc";
            case EAT:
                return "eat";
            case ATWORK:
                return "atwork";
            default:
                return null;

        }
    }
}
