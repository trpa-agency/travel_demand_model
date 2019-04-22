package com.pb.tahoe.visitor.structures;

import org.apache.log4j.Logger;
import com.pb.tahoe.structures.DecisionMakingUnit;
import com.pb.tahoe.structures.TemporalType;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;

import java.util.Arrays;

/**
 * User: Chris
 * Date: Feb 7, 2007 - 10:32:21 AM
 */
public class TravelParty extends DecisionMakingUnit {

    protected static Logger logger = Logger.getLogger(TravelParty.class);

    /**
     * The sample number of the original party that this instance was constructed.
     */
    private int sampn;

    /**
     * The type of visitor this travel party is.
     */
    private VisitorType visitorType;

    /**
     * The walk segment of the taz the party is staying in. The following codes are used:
     * <ul>
     *   <li>0 - No walk to transit access.</li>
     *   <li>1 - Short walk to transit.</li>
     *   <li>2 - Long walk to transit.</li>
     * </ul>
     */
    private short stayWalkSegment;

    /**
     * Number of people in the travel party
     */
    private short persons;

    /**
     * Number of children in the travel party
     */
    private short children;

    /**
     * Presence of an adult female in the travel party;
     */
    private boolean femaleAdult;

    /**
     * The type of accomodations the party is staying in.
     */
    private StayType stayType;

    /**
     * The chosen pattern ID # from the pattern choice model.
     */
    private int pattern = 0;

    /**
     * The string representation of the chosen pattern.
     */
    private String patternString;

    /**
     * Array of tours associated with this party. The ordering in the array is assumed to be the temporal ordering
     * of the tours (i.e. element 0 is the first tour, element 1 the second,...).
     */
    private VisitorTour[] tours;

    /**
     * The current tour being processed.
     */
//    private VisitorTour tour;

    /**
     * Indicator for gaming tour.
     */
    private boolean gaming;

    /**
     * Indicator for recreation tour.
     */
    private boolean recreation;

    /**
     * Array of hours (the first element is skipped) indicating the availability of this party to make tours
     */
    private boolean[] todAvailability = new boolean[TemporalType.HOURS + 1];

    public TravelParty() {
        Arrays.fill(todAvailability,true);
    }

    /**
     * Indicates whether this is summer or not.
     */
    private boolean summer;

    /**
     * Indicates whether current tour being processed is the first one.
     */
    private boolean first;

    /**
     * Indicates whether current stop being processed is an outbound one.
     */
    private boolean obStop;

    public void setSampn(int sampn) {
        this.sampn = sampn;
    }

    public int getSampn() {
        return sampn;
    }

    public void setVisitorType(VisitorType visitorType) {
        this.visitorType = visitorType;
    }

    public int getVisitorType() {
        return visitorType.getID(); 
    }

    public void setStayWalkSegment(int stayWalkSegment) {
        this.stayWalkSegment = (short) stayWalkSegment;
        setOriginWalkSegment((short) stayWalkSegment);
    }

    public int getStayWalkSegment() {
        return stayWalkSegment;
    }

    public void setPersons(int persons) {
        this.persons = (short) persons;
    }

    public int getPersons() {
        return persons;
    }

    public void setChildren(int children) {
        this.children = (short) children;
    }

    public int getChildren() {
        return children;
    }

    public int getAdults() {
        return persons - children;
    }

    public void setFemaleAdult(boolean femaleAdult) {
         this.femaleAdult = femaleAdult;
    }

    public boolean getFemaleAdult() {
        return femaleAdult;
    }

    public int getFemaleAdultAsInt() {
        return femaleAdult ? 1 : 0;
    }

    public void setStayType(StayType stayType) {
        this.stayType = stayType;
    }

    public StayType getStayType() {
        return stayType;
    }

    public int getStayTypeID() {
        return stayType.getID();
    }

    public int getSummer() {
        return summer ? 1 : 0;
    }

    public void setSummer(boolean summer) {
        this.summer = summer;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public int getFirst() {
        return first ? 1 : 0;
    }

    public void setObStop(boolean obStop) {
        this.obStop = obStop;
    }

    public int getObStop() {
        return obStop ? 1 : 0;
    }

    public void setGaming(boolean gaming) {
        this.gaming = gaming;
    }

    public int getGaming() {
        return gaming ? 1 : 0;
    }

    public void setRecreation(boolean recreation) {
        this.recreation = recreation;
    }

    public int getRecreation() {
        return recreation ? 1 : 0;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public String getPatternString() {
        return patternString;
    }

    public void setTours(VisitorTour[] tours) {
        this.tours = tours;
    }

    public VisitorTour[] getTours() {
        return tours;
    }

    public int getNumberOfTours() {
        return tours.length;
    }

//    public void setTour(int tour) {
//        this.tour = tours[tour];
//    }

    public void setTODAvailability(int hour, boolean availability) {
        todAvailability[hour] = availability;
    }

    public boolean getTODAltAvailability(int alternative) {
        boolean available = true;
        for (int h = TODDataManager.getTodStartHour(alternative); h <= TODDataManager.getTodEndHour(alternative); h++) {
            available = available && todAvailability[h];
        }
        return available;
    }

    public int getTodOut() {
        return TODDataManager.getTodStartSkimPeriod(getChosenTodAlt());
    }

    public int getTodIn() {
        return TODDataManager.getTodEndSkimPeriod(getChosenTodAlt());
    }

    public int getTourDuration() {
        return TODDataManager.getTodEndHour(getChosenTodAlt()) - TODDataManager.getTodStartHour(getChosenTodAlt()) + 1;
    }

    public int getSouthShore() {
        return ZonalDataManager.southShore[getTazID()] ? 1 : 0;
    }

    public int getAltZone (int alt) {
        return ZonalDataManager.zoneAlt[alt];
    }
    
    public float getODUtilModeAlt (int alt) {
        return (float)ZonalDataManager.odUtilModeAlt[alt-1];
    }

    /*
     * return the md/md mode choice logsum for the given destination choice alternative
    */
    public float getLogsumMDMDDestAlt (int alt) {
        return ZonalDataManager.logsumDcMDMD[alt];
    }

    /*
     * return the nt/nt mode choice logsum for the given destination choice alternative
    */
    public float getLogsumNTNTDestAlt (int alt) {
        return ZonalDataManager.logsumDcNTNT[alt];
    }

    public float getRecDcSizeAlt (int alt) {
        return ZonalDataManager.getVisitorSize(VisitorTourType.Recreation.getID(), alt);
    }

    public float getGamDcSizeAlt (int alt) {
        return ZonalDataManager.getVisitorSize(VisitorTourType.Gaming.getID(), alt);
    }

    public float getShpDcSizeAlt (int alt) {
        return ZonalDataManager.getVisitorSize(VisitorTourType.Shopping.getID(), alt);
    }

    public float getOthDcSizeAlt (int alt) {
        return ZonalDataManager.getVisitorSize(VisitorTourType.Other.getID(), alt);
    }

    public float getStopSizeAlt (int alt) {
        return ZonalDataManager.getVisitorStopSize(alt);
    }

    /*
     * return the short walk access for the given destination choice alternative
    */
    public float getZonalShortWalkAccessDestAlt (int alt) {
        return ZonalDataManager.zonalShortAccess[alt];
    }

    /*
     * return the urban type for the given destination choice alternative
    */
    public float getUrbTypeDestAlt (int alt) {
        return ZonalDataManager.urbType[alt];
    }

    public float getUrbTypeDest() {
        return ZonalDataManager.urbType[getChosenDest()];
    }

    

    public void setTODDefaults (String TimePeriodCombo, int tourIndex) {
        VisitorTour tour = tours[tourIndex];

        if (TimePeriodCombo.equals("EaEa")) {
            tour.setTimeOfDayAlt ( 2 );
            setChosenTodAlt ( 2 );
        }
        else if (TimePeriodCombo.equals("EaAm")) {
            tour.setTimeOfDayAlt ( 4 );
            setChosenTodAlt ( 4 );
        }
        else if (TimePeriodCombo.equals("EaMd")) {
            tour.setTimeOfDayAlt ( 8 );
            setChosenTodAlt ( 8 );
        }
        else if (TimePeriodCombo.equals("EaPm")) {
            tour.setTimeOfDayAlt ( 12 );
            setChosenTodAlt ( 12 );
        }
        else if (TimePeriodCombo.equals("EaNt")) {
            tour.setTimeOfDayAlt ( 17 );
            setChosenTodAlt ( 17 );
        }
        else if (TimePeriodCombo.equals("AmAm")) {
            tour.setTimeOfDayAlt ( 40 );
            setChosenTodAlt ( 40 );
        }
        else if (TimePeriodCombo.equals("AmMd")) {
            tour.setTimeOfDayAlt ( 60 );
            setChosenTodAlt ( 60 );
        }
        else if (TimePeriodCombo.equals("AmPm")) {
            tour.setTimeOfDayAlt ( 64 );
            setChosenTodAlt ( 64 );
        }
        else if (TimePeriodCombo.equals("AmNt")) {
            tour.setTimeOfDayAlt ( 68 );
            setChosenTodAlt ( 68 );
        }
        else if (TimePeriodCombo.equals("MdMd")) {
            tour.setTimeOfDayAlt ( 104 );
            setChosenTodAlt ( 104 );
        }
        else if (TimePeriodCombo.equals("MdPm")) {
            tour.setTimeOfDayAlt ( 106 );
            setChosenTodAlt ( 106 );
        }
        else if (TimePeriodCombo.equals("MdNt")) {
            tour.setTimeOfDayAlt ( 122 );
            setChosenTodAlt ( 122 );
        }
        else if (TimePeriodCombo.equals("PmPm")) {
            tour.setTimeOfDayAlt ( 158 );
            setChosenTodAlt ( 158 );
        }
        else if (TimePeriodCombo.equals("PmNt")) {
            tour.setTimeOfDayAlt ( 167 );
            setChosenTodAlt ( 167 );
        }
        else if (TimePeriodCombo.equals("NtNt")) {
            tour.setTimeOfDayAlt ( 179 );
            setChosenTodAlt ( 179 );
        }

    }

    public String toString() {
        String outString = "\n******Travel Party Summary******\n";
        outString += "  Travel Party ID: " + getID() + "\n";
        outString += "  Source overnight visitor sample number: " + sampn + "\n";
        outString += "  Visitor Type: " + visitorType.toString().toLowerCase() + "\n";
        outString += "  Stay type: " + stayType + "\n";
        outString += "  Stay TAZ: " + getTazID() + "\n";
        outString += "  Stay walk segment " + stayWalkSegment + "\n";
        outString += "  Persons in party " + persons + ":\n";
        outString += "    " + getAdults() + " adults\n";
        outString += "    " + children + " children\n";
        String fa = "no";
        if (femaleAdult) fa = "yes";
        outString += "    Female adults present: " + fa + "\n";
        outString += "  Chosen pattern: ";
        if (pattern != 0) {
            outString += patternString + "\n";
            for (VisitorTour tour : tours) {
                outString += tour.toString();
            }
        } else {
            outString += "\n";
        }
        outString += "********************************";
        return outString;
    }

    public void writeContentToLogger(Logger logger) {
        logger.info(toString());
    }

}
