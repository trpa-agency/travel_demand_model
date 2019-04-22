package com.pb.tahoe.visitor.structures;

import java.util.HashMap;

/**
 * This class provides the basic structure constants for reading/writing the overnight visitor data.
 *
 * User: Chris
 * Date: Feb 8, 2007 - 9:02:09 AM
 */
public class VisitorDataStructure {

    //These are the constants for identifying the column names of the overnight visitor zonal data file
    public static final String OVZONAL_TAZ_FIELD = "taz";
    public static final String OVZONAL_HOTELMOTEL_FIELD = StayType.HOTELMOTEL.toString().toLowerCase();
    public static final String OVZONAL_RESORT_FIELD = StayType.RESORT.toString().toLowerCase();
    public static final String OVZONAL_CASINO_FIELD = StayType.CASINO.toString().toLowerCase();
    public static final String OVZONAL_CAMPGROUND_FIELD = StayType.CAMPGROUND.toString().toLowerCase();
    public static final String OVZONAL_SEASONALPERCENTAGE_FIELD = "percentHouseSeasonal";
    public static final String OVZONAL_BEACH_FIELD = "beach";

    //These are the constants for identifying the column names of the day visitor zonal data file
    public static final String DVZONAL_TAZ_FIELD = "taz";
    public static final String DVZONAL_NIGHT2DAY_FIELD = "overnight2day";
    public static final String DVZONAL_THRUPERCENT_FIELD = "percentThru";

    //These are the constants for the synpop (and derivative) data table & file.
    public static final String ID_FIELD = "id";
    public static final String SAMPN_FIELD = "sampn";
    public static final String VISITORTYPE_FIELD = "visitorType";
    //This actually maps to the "taz" field in DecisionMakingUnit, but I like this name better
    public static final String STAYTAZ_FIELD = "stayTAZ";
    public static final String WALKSEGMENT_FIELD = "walkSegment";
    public static final String STAYTYPE_FIELD = "stayType";
    public static final String PERSONS_FIELD = "persons";
    public static final String CHILDREN_FIELD = "children";
    public static final String FEMALEADULT_FIELD = "femaleAdult";

    public static final String PATTERN_FIELD = "pattern";
    //The following fields will be repeated for each tour, with a number following the name - e.g. dest1, dest2,....
    public static final String DEST_FIELD = "dest";
    public static final String TOD_FIELD = "tod";
    public static final String MODE_FIELD = "mode";
    public static final String OBDEST_FIELD = "obDest";
    public static final String OBMODE_FIELD = "obMode";
    public static final String IBDEST_FIELD = "ibDest";
    public static final String IBMODE_FIELD = "ibMode";

    //these are used for the reports data set
    public static final String ORIG_REPORTS_FIELD = "origTaz";
    public static final String DEST_REPORTS_FIELD = "destTaz";
    public static final String DEPHR_REPORTS_FIELD = "TOD_StartHr";
    public static final String ARRHR_REPORTS_FIELD = "TOD_EndHr";
    public static final String TOURTYPE_REPORTS_FIELD = "tourType";
    public static final String OBSTART_REPORTS_FIELD = "OB_start_taz";
    public static final String OBSTOP_REPORTS_FIELD = "OB_stop_taz";
    public static final String IBSTART_REPORTS_FIELD = "IB_start_taz";
    public static final String IBSTOP_REPORTS_FIELD = "IB_stop_taz";


    //These are the constants for identifying the columns of the input overnight visitor records data set
    public static final String RECORDS_ID_FIELD = "id";
    public static final String RECORDS_STAYTYPE_FIELD = "stayType";
    public static final String RECORDS_PERSONS_FIELD = "persons";
    public static final String RECORDS_CHILDREN_FIELD = "children";
    public static final String RECORDS_FEMALEADULT_FIELD = "femaleAdult";

    /**
     * The character used to identify "home" in a pattern.
     */
    public static final String homeChar = "H";

    /**
     * The character used to identify a stop in a pattern.
     */
    public static final String stopChar = "T";

    // This set of key to array mappings tells which StayTypes should be sampled from to generate a particular
    // StayType's synthentic population
    public static final HashMap<StayType,StayType[]> synPopSampleStructure = new HashMap<StayType,StayType[]>();
    private static final StayType[] seasonalSampleStructure = {
                                                               StayType.SEASONAL,
                                                               };
    private static final StayType[] hotelmotelSampleStructure = {
                                                               StayType.HOTELMOTEL,
                                                               StayType.CASINO,
                                                               StayType.RESORT,
                                                               };
    private static final StayType[] casinoSampleStructure = {
                                                               StayType.HOTELMOTEL,
                                                               StayType.CASINO,
                                                               StayType.RESORT,
                                                               };
    private static final StayType[] resortSampleStructure = {
                                                               StayType.HOTELMOTEL,
                                                               StayType.CASINO,
                                                               StayType.RESORT,
                                                               };
    private static final StayType[] houseSampleStructure = {
                                                               StayType.HOUSE,
                                                               };
    private static final StayType[] campgroundSampleStructure = {
                                                               StayType.CAMPGROUND,
                                                               };
    private static void defineSynPopSampleStructure() {
        synPopSampleStructure.put(StayType.SEASONAL,seasonalSampleStructure);
        synPopSampleStructure.put(StayType.HOTELMOTEL,hotelmotelSampleStructure);
        synPopSampleStructure.put(StayType.CASINO,casinoSampleStructure);
        synPopSampleStructure.put(StayType.RESORT,resortSampleStructure);
        synPopSampleStructure.put(StayType.HOUSE,houseSampleStructure);
        synPopSampleStructure.put(StayType.CAMPGROUND,campgroundSampleStructure);
    }

    private static VisitorDataStructure instance = new VisitorDataStructure();

    private VisitorDataStructure() {
        defineSynPopSampleStructure();
    }

    public VisitorDataStructure getInstance() {
        return instance;
    }

}
