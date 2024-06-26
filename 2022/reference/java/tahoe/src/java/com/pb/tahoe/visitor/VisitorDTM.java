package com.pb.tahoe.visitor;

import com.pb.tahoe.dest_time_mode.DTMModel;
import com.pb.tahoe.structures.DecisionMakingUnit;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.visitor.structures.TravelParty;
import com.pb.tahoe.visitor.structures.VisitorTourType;
import com.pb.tahoe.visitor.structures.VisitorTour;
import com.pb.tahoe.visitor.structures.VisitorMode;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.model.ModelException;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;

/**
 * User: Chris
 * Date: Feb 27, 2007 - 4:19:21 PM
 */
public class VisitorDTM extends DTMModel {
    protected static Logger logger = Logger.getLogger(VisitorDTM.class);

    public VisitorDTM(ResourceBundle rb) {
        super(rb, (short) (TourType.CATEGORIES + 1), VisitorTourType.getTourTypeArray());
    }


    protected String getTourCategoryLabel() {
        return "Visitor";
    }

    protected String getTourTypeLabel(int tourIndex) {
        return VisitorTourType.getTourType(tourIndex).name();
    }

    protected HashMap<String,String> getDTMRbKeyMap(String categoryString) {
        HashMap<String,String> dtmRbKeyMap = new HashMap<String,String>();
        dtmRbKeyMap.put("DC", "visitor.destination.choice.control.file");
        dtmRbKeyMap.put("TOD", "visitor.time.of.day.control.file");
        dtmRbKeyMap.put("MC", "visitor.mode.choice.control.file");
        dtmRbKeyMap.put("OUT",categoryString +"_dtm.choice.output.file");
        return dtmRbKeyMap;
    }

    /**
     * Run the DTM model for all overnight visitor travel parties
     *
     * @param pam
     *        The party array manager containing all of the travel parties to run DTM for.
     */
    public void doWork (PartyArrayManager pam) {
        //loop over all travel parties
        int nParties = 0;
        for (TravelParty party : pam.parties) {
            if (party== null) continue;
            chooseDestTODAndMode(party);
            nParties++;
            if (nParties % 1000 == 0) {
                baseLogger.info("Destination, TOD, and Mode chosen for " + nParties + " travel parties.");
            }
        }
        //tell pam we're done
        pam.modelDone(PartyArrayManager.DC_KEY);
        pam.modelDone(PartyArrayManager.TOD_KEY);
        pam.modelDone(PartyArrayManager.MC_KEY);
        //write out results
        pam.writePartyData("visitor.synpop.dtm.results.file");
    }

    /**
     * Define the UEC model sheets for a particular tour type.
     *
     * @param tourType
     *        The tour type id # to set the sheets for.
     */
    public void defineUECModelSheets (int tourType) {
        DC_ModelSheet  = tourType;
        TOD_ModelSheet  = tourType;
        if (TOD_ModelSheet==4)
            TOD_ModelSheet = 3;
        MC_ModelSheet  = 2;
        MCOD_ModelSheet = 3;
    }

    /**
     * Create the DTM UECs for a particular tour type.
     *
     * @param tourType
     *        The tour type to create the UECs for.
     */
    protected void createDTMUECs(int tourType) {
        // create dest choice UEC
        baseLogger.info ("\tCreating " + getTourTypeLabel(tourTypes[tourType]) + " Destination Choice UECs");
        dcUEC[tourType] = dc[tourType].getUEC(DC_ModelSheet, DC_DataSheet, TravelParty.class);

        // create time-of-day choice UEC
        baseLogger.info("\tCreating " + getTourTypeLabel(tourTypes[tourType]) + " Time-of-Day Choice UECs");
        tcUEC[tourType] = tc[tourType].getUEC(TOD_ModelSheet,  TOD_DataSheet, TravelParty.class);

        // create UEC to calculate OD component of mode choice utilities
        baseLogger.info("\tCreating " + getTourTypeLabel(tourTypes[tourType]) + " Mode Choice OD UECs");
        mcODUEC[tourType] = mc[tourType].getUEC(MCOD_ModelSheet,  MCOD_DataSheet, TravelParty.class);

        // create UEC to calculate non-OD component of mode choice utilities
        baseLogger.info("\tCreating " + getTourTypeLabel(tourTypes[tourType]) + " Mode Choice UECs");
        mcUEC[tourType] = mc[tourType].getUEC(MC_ModelSheet,  MC_DataSheet, TravelParty.class);
    }

    /**
     * Select destination, time of day, and mode for a particular travel party (decision making unit).
     *
     * @param dmu
     *        The travel party to run DTM for.
     */
    public void chooseDestTODAndMode(DecisionMakingUnit dmu) {
        TravelParty tp = (TravelParty) dmu;
        //Make sure origin is taz
        tp.setOrigTaz(tp.getTazID());
        VisitorTour[] tours = tp.getTours();


        //loop over all the tours in the party
        for (int t = 0; t < tours.length; t++) {
            //Set the "first tour" variable
            tp.setFirst(t==0);
            //loop over all the tour types
            for (int i = 0; i < tourTypes.length; i++) {
                defineUECModelSheets(tourTypes[i]);
                if (tours[t].getTourType().getID() != tourTypes[i])
                    continue;

                tp.setGaming(tours[t].getTourType() == VisitorTourType.Gaming);
                tp.setRecreation(tours[t].getTourType() == VisitorTourType.Recreation);

                //Choose destination
                int chosenDestAlt = chooseDestination(tp, i, t);
                int chosenTAZ = getDestInfo(chosenDestAlt)[0];
                int chosenWalkSegment = getDestInfo(chosenDestAlt)[1];
                tp.setChosenDest(chosenTAZ);
                tp.setChosenWalkSegment(chosenWalkSegment);
                tours[t].setDestTAZ(chosenTAZ);
                tours[t].setDestWalkSegment(chosenWalkSegment);

                // check to make sure that the DC subzone selected has subzone proportion > 0.0.
                if ( ZonalDataManager.getWalkPct ( chosenWalkSegment, chosenTAZ ) == 0.0f ) {
                    logger.fatal( getTourCategoryLabel() + " " + getTourTypeLabel(i) + " tour " + t + " for travel party " + tp.getID());
                    logger.fatal( "selected  Dest TAZ " + chosenTAZ + " and subzone " + chosenWalkSegment);
                    logger.fatal( "however, the selected subzone has proportion " + (chosenWalkSegment == 1 ? "short walk" : "long walk") + " equal to 0.0.");
                    System.exit(1);
                }

                //Choose time of day
                int chosenTODAlt = chooseTimeOfDay(tp, i, t);
                tours[t].setTimeOfDayAlt(chosenTODAlt);
                tp.setChosenTodAlt(chosenTODAlt);
                //Set anything before this tour to be unavailable
                for (int h = 1; h < TODDataManager.getTodEndHour(chosenTODAlt); h++) {
                    tp.setTODAvailability(h,false);
                }

                //Choose mode choice
                int chosenModeAlt = chooseMode(tp, i);
                tours[t].setMode(VisitorMode.getMode(chosenModeAlt));
                break;
            }
        }

    }

    /**
     * Method required by abstract parent class. Should not be used, use
     * {@link #chooseDestination(TravelParty, int, int)} instead.
     *
     * @param dmu
     *        Decision making unit.
     *
     * @param tourTypeIndex
     *        Tour type index.
     *
     * @return an empty integer array.
     */
    public int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex) {
        return new int[0];
    }

    /**
     * Choose a destination for a travel party's tour.
     *
     * @param tp
     *        The {@code TravelParty}.
     *
     * @param tourTypeIndex
     *        The index of the tour type of this tour (from {@link com.pb.tahoe.dest_time_mode.DTMModel#tourTypes}).
     *
     * @param tourIndex
     *        The tour's index (ordinal value).
     *
     * @return the chosen destination alternative.
     */
    public int chooseDestination(TravelParty tp, int tourTypeIndex, int tourIndex) {
         // get the destination choice sample of alternatives
        Arrays.fill ( dcAvailability, false );
        Arrays.fill(dcSample, 0);

        // determine the set of alternatives from which the sample of alternatives will be drawn
        for (int k=1; k < dcAvailability.length; k++) {
                // set destination choice alternative availability to true if size > 0 for the segment.
                float size = ZonalDataManager.getVisitorSize(tourTypes[tourTypeIndex], k );
                if ( size > 0.0 ) {
                    dcAvailability[k] = true;
                    dcSample[k] = 1;
                }
        }

        //all alternatives are in our sample in tahoe
        int[] sample = new int[dcSample.length];
        for(int i=1; i< sample.length; i++){
            sample[i] = i;
        }

        long markTime = System.currentTimeMillis();
        for (int i=1; i < sample.length; i++) {

            int d = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(sample[i], 2);
            tp.setChosenDest(d);

            if (VisitorTourType.getTourType(tourTypes[tourTypeIndex]) == VisitorTourType.Gaming) {
                tp.setTODDefaults ("NtNt", tourIndex);
                ZonalDataManager.setLogsumDcNTNT ( dcSample[i], getMcLogsum(tp, tourTypeIndex, (short) (TourType.TYPES + 1)));
            } else {
                tp.setTODDefaults ("MdMd", tourIndex);
                ZonalDataManager.setLogsumDcMDMD ( dcSample[i], getMcLogsum(tp, tourTypeIndex, (short) (TourType.TYPES + 1)));
            }
        }
        dcLogsumTime += (System.currentTimeMillis()-markTime);
        
        markTime = System.currentTimeMillis();
        // compute destination choice proportions and choose alternative
        dc[tourTypeIndex].updateLogitModel ( tp, dcAvailability, dcSample );
        int chosen = dc[tourTypeIndex].getChoiceResult();
        dcTime += (System.currentTimeMillis() - markTime);

        return chosen;
    }

    /**
     * Choose a time of day alternative for a travel party's tour.
     *
     *@param tp
     *        The {@code TravelParty}.
     *
     * @param tourTypeIndex
     *        The index of the tour type of this tour (from {@link com.pb.tahoe.dest_time_mode.DTMModel#tourTypes}).
     *
     * @param tourIndex
     *        The tour's index (ordinal value).
     *
     * @return the chosen TOD alternative.
     */
    public int chooseTimeOfDay(TravelParty tp, int tourTypeIndex, int tourIndex) {
        // update the time of day choice availabilty based on available time windows
        // tcSample and tcAvailability are 1 based
        for (int i = 1; i < tcAvailability.length; i++) {
            boolean available = tp.getTODAltAvailability(i);
            tcAvailability[i] = available;
            tcSample[i] = (available ? 1 : 0);
        }

        // count the number of tours in which no time-of-day alternative was available
        boolean noTOD = false;
        for (boolean avail : tcAvailability) {
            noTOD = avail;
            if (noTOD) break;
        }

        //Set default TOD if none available
        if (!noTOD) {
            baseLogger.warn("No TOD choice available for party " + tp.getID() + ", tour " + tourIndex + ". Default will be chosen.");
            noTODAvailableIndiv[tourTypeIndex]++;
            tcAvailability[1] = true;
            tcSample[1] = 1;
            tcAvailability[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = true;
            tcSample[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = 1;
        }

        // compute time-of-day choice proportions and choose alternative
        long markTime = System.currentTimeMillis();
        tc[tourTypeIndex].updateLogitModel ( tp, tcAvailability, tcSample );

        int chosenTODAlt;
        try {
            chosenTODAlt = tc[tourTypeIndex].getChoiceResult();
        } catch (ModelException e) {
            baseLogger.warn("Model exception in TOD for party " + tp.getID() + ", tour " + tourIndex + ".  " +
                    "Will choose random TOD period. Here is the exception:\n" + e);
            chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
        }
        tcTime += (System.currentTimeMillis()-markTime);
        return chosenTODAlt;
    }

    /**
     * Chosse a mode alternative for this travel party's tour.
     *
     *@param tp
     *        The {@code TravelParty}.
     *
     * @param tourTypeIndex
     *        The index of the tour type of this tour (from {@link com.pb.tahoe.dest_time_mode.DTMModel#tourTypes}).
     *
     * @return the chosen mode alternative.
     */
    public int chooseMode(TravelParty tp, int tourTypeIndex) {
        // compute mode choice proportions and choose alternative
        long markTime = System.currentTimeMillis();
        Arrays.fill(mcSample, 1);
        Arrays.fill (mcAvailability, true);
        setMcODUtility ( tp, tourTypeIndex, (short) (TourType.TYPES + 1));
        // set transit modes to unavailable if a no walk access subzone was selected in DC.

        if ( tp.getChosenWalkSegment() == 0 ) {
            mcSample[3] = 0;
            mcAvailability[3] = false;
            mcSample[4] = 0;
            mcAvailability[4] = false;
        }

        //this is the original by Jim
        mc[tourTypeIndex].updateLogitModel ( tp, mcAvailability, mcSample );
        mcTime += (System.currentTimeMillis()-markTime);

        return mc[tourTypeIndex].getChoiceResult();
    }

    /**
     * Define the unique hash map key for mode choice od logsums.
     *
     * @param dmu
     *        The travel party (decision making unit) to get logsums for.
     *
     * @param tourTypeIndex
     *        The tour type index.
     *
     * @param tourCategory
     *        The tour category index.
     *
     * @return the hash map key for this travel party.
     */
    public String defineMapKey(DecisionMakingUnit dmu, int tourTypeIndex, short tourCategory){
        TravelParty tp = (TravelParty) dmu;

        int todAlt = tp.getChosenTodAlt();
        int startPeriod = TODDataManager.getTodStartPeriod( todAlt );
        int startSkimPeriod = TODDataManager.getTodSkimPeriod ( startPeriod );
        int endPeriod = TODDataManager.getTodEndPeriod( todAlt );
        int endSkimPeriod = TODDataManager.getTodSkimPeriod ( endPeriod );

        return "TravelParty_"
                    + Integer.toString(tourTypeIndex) + "_"
                    + Integer.toString(startSkimPeriod) + "_"
                    + Integer.toString(endSkimPeriod) + "_"
                    + Integer.toString(tp.getOrigTaz()) + "_"
                    + Integer.toString(tp.getOriginWalkSegment()) + "_"
                    + Integer.toString(tp.getChosenDest() )  + "_"
                    + Integer.toString(tp.getChosenWalkSegment());
    }


    //**********Below are static methods used for finding dc alternative info***************
    static ResourceBundle propertyMap = ResourceUtil.getResourceBundle("tahoe");

    private static final String dcAlternativeFile = "dc.alternative.set.file";

    /**
     * This is a mapping from the destination unique id and its [TAZ,Walk Segment].
     */
    private static HashMap<Integer,int[]> destinationMap;

    /**
     * This is the reverse of the {@link #destinationMap}, only the {@code int[]} array is replaced with a {@code String}.
     */
    private static HashMap<String,Integer> reverseDestinationMap;

    //build the destination mapping
    private static void buildDestinationMap() {
        destinationMap = new HashMap<Integer,int[]>();
        reverseDestinationMap = new HashMap<String,Integer>();
        TableDataSet dcAlts;
        String dcAltFile = propertyMap.getString(dcAlternativeFile);

        try {
            CSVFileReader reader = new CSVFileReader();
            dcAlts = reader.readFile(new File(dcAltFile));
        } catch (IOException e) {
            throw new RuntimeException("Error reading DC alternative file " + dcAltFile);
        }

        if (dcAlts != null) {
            for (int row = 1; row <= dcAlts.getRowCount(); row++) {
                int altNumber = (int) dcAlts.getValueAt(row,"a");
                destinationMap.put(altNumber,
                        new int[] {(int) dcAlts.getValueAt(row,"dtaz"),(int) dcAlts.getValueAt(row,"shortWalk")});
                reverseDestinationMap.put("" + destinationMap.get(altNumber)[0] + "_" + destinationMap.get(altNumber)[1],altNumber);
            }
        }
    }

    //Get the destination information from a destination alternative
    public static int[] getDestInfo(int destAlt) {
        if (destinationMap == null) buildDestinationMap();
        if(destinationMap.containsKey(destAlt)) {
            return destinationMap.get(destAlt);
        } else {
            logger.warn("Destination alternative " + destAlt + " not found in destination mapping!");
            return new int[0];
        }
    }

    //Get destination alternative number from taz and walk segment
    public static int getDestAlt(int destTAZ, int walkSegment) {
        if (destinationMap == null) buildDestinationMap();
        String destinationKey = "" + destTAZ + "_" + walkSegment;
        if(reverseDestinationMap.containsKey(destinationKey)) {
            return reverseDestinationMap.get(destinationKey);
        } else {
            logger.warn("Destination taz " + destTAZ + " + walk segment " + walkSegment + " not found in destination mapping!");
            return 0;
        }
    }

    public void printTimes() {
        baseLogger.info ( "DTM Model Component Times for " + getTourCategoryLabel() + " tours:");

        baseLogger.info ( "total seconds processing dtm dc logsums = " + (float)dcLogsumTime/1000);
        baseLogger.info ( "total seconds processing dtm dest choice = " + (float)dcTime/1000);
        baseLogger.info ( "total seconds processing dtm tc logsums = " + (float)tcLogsumTime/1000);
        baseLogger.info ( "total seconds processing dtm tod choice = " + (float)tcTime/1000);
        baseLogger.info ( "total seconds processing dtm mode choice = " + (float)mcTime/1000);
        baseLogger.info ( "");
    }
}
