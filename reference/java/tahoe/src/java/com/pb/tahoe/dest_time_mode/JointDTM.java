package com.pb.tahoe.dest_time_mode;

import com.pb.common.model.ModelException;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.structures.*;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * JointDTM is a class that run the destination choice, time-of-day choice and mode choice model
 * for joint tours.
 *
 * @author Christi Willison
 * @version 1.0,  May 22, 2006
 */
public class JointDTM extends DTMModel {
    protected static Logger logger = Logger.getLogger(JointDTM.class);
    boolean debug = false;


    public JointDTM (ResourceBundle rb) {
        super(rb, TourType.JOINT_CATEGORY, TourType.getTourTypesForCategory(TourType.JOINT_CATEGORY));
    }

    public void chooseDestTODAndMode(DecisionMakingUnit dmu){
        Household hh = (Household) dmu;
        // get the array of mandatory tours for this household.
        if (hh.getJointTours() == null)
            return;

        int hh_taz_id = hh.getTazID();
        hh.setOrigTaz (hh_taz_id);
        hh.setTourCategory( TourType.JOINT_CATEGORY );


        // get person array for this household.
        int[] jtPersons;


        // loop over all puposes for the mandatory tour category in order
        for (int m=0; m < tourTypes.length; m++) {

            // loop over individual tours of the tour purpose of interest for the hh
            for (int t=0; t < hh.jointTours.length; t++) {

                if (hh.jointTours[t].getJointTourPersons().length < 2) {
                    throw new RuntimeException( "Fewer than 2 persons participating in joint tour for household " + hh.getID() + ", joint tour number " + t);
                }

                if ( hh.jointTours[t].getTourType() != tourTypes[m] )
                    continue;

                hh.jointTours[t].setOrigTaz (hh_taz_id);
                hh.jointTours[t].setOriginShrtWlk (hh.getOriginWalkSegment() );

                if (logger.isDebugEnabled())
                    logger.debug("in DTM joint dc, setting orig short walk="+hh.getOriginWalkSegment());

                jtPersons = hh.jointTours[t].getJointTourPersons();
                hh.setTourID ( t );

                int[] dcResults = chooseDestination(hh, m);

                // set the chosen value in DMU and tour objects
                hh.setChosenDest ( dcResults[0] );
                hh.setChosenWalkSegment( dcResults[1] );
                hh.jointTours[t].setDestTaz (dcResults[0]);
				hh.jointTours[t].setDestShrtWlk (dcResults[1]);

                // check to make sure that the DC subzone selected has subzone proportion > 0.0.
				if ( ZonalDataManager.getWalkPct ( dcResults[1], dcResults[0] ) == 0.0f ) {
                    logger.fatal( TourType.getCategoryLabelForCategory(tourTypeCategory) + " " +
                            TourType.getTourTypeLabelsForCategory(tourTypeCategory)[m] + " tour " + t +
                            " for person " + hh.jointTours[t].getTourPerson() + " in household " + hh.getID());
                    logger.fatal( "selected  Dest TAZ " + dcResults[0] + " and subzone " + dcResults[1]);
                    logger.fatal( "however, the selected subzone has proportion " + (dcResults[1] == 1 ? "short walk" : "long walk") + " equal to 0.0.");
                    System.exit(1);
                }

                int todResult = chooseTimeOfDay(hh, hh.persons, jtPersons, m);

                int start = TODDataManager.getTodStartHour ( todResult );
                int end = TODDataManager.getTodEndHour ( todResult );

                for (int j=start; j <= end; j++) {
					// set hours unavailable for each person in joint tour
					for (int p=0; p < jtPersons.length; p++) {
						hh.persons[ jtPersons[p] ].setHourUnavailable(j);
					}
				}

                //debug output in case we need it.
                if (hh.getID() == 10000 && debug) {
                    String personIDs = "";
                    for(int i=0; i < jtPersons.length; i++){
                        personIDs += hh.persons[jtPersons[i]].getID() + ", ";
                    }
                    logger.debug("PERSONS: " + personIDs);
                    logger.debug("Time of day choice: " + todResult);
                    logger.debug("\tstart: " + start);
                    logger.debug("\tend: " + end);

                    for (int i=0; i < jtPersons.length; i++) {
                        logger.debug("Availability after joint tour time-of-day choice for person" + hh.persons[jtPersons[i]].getID());
                        String avail = "";
                        for(int j=1; j < hh.persons[jtPersons[i]].getAvailable().length; j++){
                            avail += hh.persons[jtPersons[i]].getAvailable()[j] + ",";
                        }
                        logger.debug("Their availability is " + avail);
                        logger.debug("\n\n");
                    }
                }

                // set chosen alternative in tour objects
                hh.setChosenTodAlt( todResult );
                hh.jointTours[t].setTimeOfDayAlt(todResult);

                int chosenShrtWlk = hh.jointTours[t].getDestShrtWlk();
                int modeResult = chooseMode(hh, m, TourType.JOINT_CATEGORY, chosenShrtWlk);
                // set chosen in alternative in tour objects
                hh.jointTours[t].setMode(modeResult);

                if ( modeResult == 3 || modeResult == 4 ) {
                    hh.jointTours[t].setSubmodeOB ( SubModeType.LBS.ordinal() );
                    hh.jointTours[t].setSubmodeIB ( SubModeType.LBS.ordinal() );
                } else {

					hh.jointTours[t].setSubmodeOB ( SubModeType.ZERO.ordinal() );
					hh.jointTours[t].setSubmodeIB ( SubModeType.ZERO.ordinal()  );

				}


            }
        }

    }

    public int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex){
        Household hh = (Household) dmu;
        // get the destination choice sample of alternatives

        Arrays.fill ( dcAvailability, false );
        Arrays.fill(dcSample, 0);

        for (int k=1; k < dcAvailability.length; k++) {
                // set destination choice alternative availability to true if size > 0 for the segment.
                float size = ZonalDataManager.getTotSize (tourTypes[tourTypeIndex], k );
                if ( size > 0.0 ) {
                    dcAvailability[k] = true;
                    dcSample[k]=1;
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
            int s = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(sample[i], 3);
			hh.setChosenDest( d );
			hh.setChosenWalkSegment( s );

            // calculate mode choice logsum based on appropriate od skims
            hh.setTODDefaults ( TourType.JOINT_CATEGORY, "MdMd" );
            ZonalDataManager.setLogsumDcMDMD (sample[i], getMcLogsum(hh, tourTypeIndex, TourType.JOINT_CATEGORY) );
            hh.setTODDefaults ( TourType.JOINT_CATEGORY, "PmNt" );
            ZonalDataManager.setLogsumDcPMNT ( sample[i], getMcLogsum(hh, tourTypeIndex, TourType.JOINT_CATEGORY) );

        }
        dcLogsumTime += (System.currentTimeMillis()-markTime);

        markTime = System.currentTimeMillis();
        // compute destination choice proportions and choose alternative
        dc[tourTypeIndex].updateLogitModel ( hh, dcAvailability, dcSample );
        int chosen = dc[tourTypeIndex].getChoiceResult();
        int chosenDestAlt = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(chosen, 2);
        int chosenShrtWlk = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(chosen, 3);
        dcTime += (System.currentTimeMillis() - markTime);

        return new int[]{chosenDestAlt, chosenShrtWlk};

    }

    public int chooseTimeOfDay(Household hh, Person[] persons, int[] jtPersonIds,  int tourTypeIndex ) {
        // update the time of day choice availabilty based on available time windows
        // tcSample and tcAvailability are 1 based
        Arrays.fill(tcSample, 1);
        Arrays.fill(tcAvailability, true);

        //debug in case we need it
        if(hh.getID() == 10000 && debug) {
            logger.debug("Availability before joint tour time of day choice for persons taking a tour of type " +
                TourType.getTourTypesForCategory(tourTypeCategory)[tourTypeIndex] + " in household " + hh.getID());
            for (int i=0; i < jtPersonIds.length; i++) {
                String avail = "";
                for(int j=1; j < hh.persons[jtPersonIds[i]].getAvailable().length; j++){
                    avail += hh.persons[jtPersonIds[i]].getAvailable()[j] + ",";
                }
                logger.debug("Availability for person" + hh.persons[jtPersonIds[i]].getID()  + ": " + avail);
                logger.debug("\n\n");
            }
        }

        for (int p=0; p < jtPersonIds.length; p++) {
            setTcAvailability ( persons[ jtPersonIds[p] ], tcAvailability, tcSample );
        }

        // count the number of available time-of-day alternatives
        //if even one is found, break and set alternatives 1 and 190 to true.
        int TODAvailable = 0;
        for (int p=1; p <= tcUEC[tourTypeIndex].getNumberOfAlternatives(); p++) {
            if (tcAvailability[p]) {
                TODAvailable++;
                break;
            }
        }
        if (TODAvailable == 0) {
            noTODAvailableIndiv[tourTypeIndex]++;
            tcAvailability[1] = true;
            tcSample[1] = 1;
            tcAvailability[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = true;
            tcSample[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = 1;
        }

        // compute time-of-day choice proportions and choose alternative
        long markTime = System.currentTimeMillis();
        tc[tourTypeIndex].updateLogitModel ( hh, tcAvailability, tcSample );

        int chosenTODAlt;
        try {
            chosenTODAlt = tc[tourTypeIndex].getChoiceResult();
        }
        catch (ModelException e) {
            logger.warn("Invalid TOD for hh " + hh.getID());
            chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
        }
        tcTime += (System.currentTimeMillis()-markTime);
        return chosenTODAlt;



    }

    public void defineUECModelSheets (int tourType) {

        //Joint Worksheets
        final int DC_JointShop_MODEL_SHEET = 4;
        final int DC_JointOthMaint_MODEL_SHEET = 5;
        final int DC_JointDisc_MODEL_SHEET = 6;
        final int DC_JointEat_MODEL_SHEET = 7;

        final int TOD_Joint_MODEL_SHEET = 3;

        final int MC_JointShop_MODEL_SHEET = 6;
        final int MC_JointShop_OD_UTIL_SHEET = 7;
        final int MC_JointOthMaint_MODEL_SHEET = 8;
        final int MC_JointOthMaint_OD_UTIL_SHEET = 9;
        final int MC_JointDisc_MODEL_SHEET = 10;
        final int MC_JointDisc_OD_UTIL_SHEET = 11;
        final int MC_JointEat_MODEL_SHEET = 12;
        final int MC_JointEat_OD_UTIL_SHEET = 13;

        if (tourType == TourType.SHOP) {
            DC_ModelSheet  = DC_JointShop_MODEL_SHEET;
            TOD_ModelSheet  = TOD_Joint_MODEL_SHEET;
            MC_ModelSheet  = MC_JointShop_MODEL_SHEET;
            MCOD_ModelSheet = MC_JointShop_OD_UTIL_SHEET;
        }
        else if(tourType == TourType.OTHER_MAINTENANCE) {
            DC_ModelSheet  = DC_JointOthMaint_MODEL_SHEET;
            TOD_ModelSheet  = TOD_Joint_MODEL_SHEET;
            MC_ModelSheet  = MC_JointOthMaint_MODEL_SHEET;
            MCOD_ModelSheet = MC_JointOthMaint_OD_UTIL_SHEET;
        }
        else if (tourType == TourType.DISCRETIONARY) {
            DC_ModelSheet  = DC_JointDisc_MODEL_SHEET;
            TOD_ModelSheet  = TOD_Joint_MODEL_SHEET;
            MC_ModelSheet  = MC_JointDisc_MODEL_SHEET;
            MCOD_ModelSheet = MC_JointDisc_OD_UTIL_SHEET;
        }
        else if (tourType == TourType.EAT) {
            DC_ModelSheet  = DC_JointEat_MODEL_SHEET;
            TOD_ModelSheet  = TOD_Joint_MODEL_SHEET;
            MC_ModelSheet  = MC_JointEat_MODEL_SHEET;
            MCOD_ModelSheet = MC_JointEat_OD_UTIL_SHEET;
        }
    }



}
