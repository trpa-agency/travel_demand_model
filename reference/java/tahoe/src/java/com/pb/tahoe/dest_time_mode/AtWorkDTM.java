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
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */

public class AtWorkDTM extends DTMModel {
    protected static Logger logger = Logger.getLogger(AtWorkDTM.class);
    int defaultStart = 0;           //either your atWork Tour was done in the first hour (so alt corresponds to start and end times = start hour
    int defaultEnd = 0;             //or your atWork Tour was done in the last hour (so alt corresponds to start and end times = end hour



    public AtWorkDTM (ResourceBundle rb) {
        super(rb, TourType.AT_WORK_CATEGORY, TourType.getTourTypesForCategory(TourType.AT_WORK_CATEGORY));
    }

    public void chooseDestTODAndMode(DecisionMakingUnit dmu){
        Household hh = (Household) dmu;
        int m = 0;    //only a single tour-type  (at-work)

        // get the array of mandatory tours for this household.
        if (hh.getMandatoryTours() == null)
            return;

        hh.setTourCategory( TourType.AT_WORK_CATEGORY );

        // get person array for this household.
        Person[] persons = hh.getPersonArray();

        // loop over individual tours of the tour purpose of interest for the hh
        for (int t=0; t < hh.mandatoryTours.length; t++) {

            // get the array of subtours for this work tour
            if (hh.mandatoryTours[t].getSubTours() == null)
                continue;

            int person = hh.mandatoryTours[t].getTourPerson();

            hh.setPersonID ( person );
            hh.setTourID ( t );

            // only hours between start and end of primary work tour are available for subtour
            int todAlt = hh.mandatoryTours[t].getTimeOfDayAlt();
            int start = TODDataManager.getTodStartHour ( todAlt );
            int end = TODDataManager.getTodEndHour ( todAlt );
            for (int p=1; p < tcAvailability.length; p++) {

                int startP = TODDataManager.getTodStartHour ( p );
                int endP = TODDataManager.getTodEndHour ( p );

                if (startP >= start && endP <= end) {
                    tcAvailability[p] = true;
                    tcSample[p] = 1;
                }else {
                        tcAvailability[p] = false;
                        tcSample[p] = 0;
                }

                if ( startP == start && endP == start )
                    defaultStart = p;
                if ( startP == end && endP == end )
                    defaultEnd = p;

             }

        // loop over subtours
            for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

                // the origin for the at-work tour is the destination of the primary work tour
                hh.mandatoryTours[t].subTours[s].setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
                hh.mandatoryTours[t].subTours[s].setOriginShrtWlk ( hh.mandatoryTours[t].getDestShrtWlk() );

                // set the Household object values that will be used as DMU for at-work tour choices
                hh.setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
                hh.setOriginWalkSegment( hh.mandatoryTours[t].getDestShrtWlk() );
                hh.setSubtourID ( s );

                int[] dcResults = chooseDestination(hh, m);

                // set the chosen value in Household and Tour objects
                hh.setChosenDest ( dcResults[0] );
                hh.setChosenWalkSegment( dcResults[1] );
                hh.mandatoryTours[t].subTours[s].setDestTaz(dcResults[0]);
                hh.mandatoryTours[t].subTours[s].setDestShrtWlk(dcResults[1]);

                // check to make sure that the DC subzone selected has subzone proportion > 0.0.
                if ( ZonalDataManager.getWalkPct ( dcResults[1], dcResults[0] ) == 0.0f) {
                    logger.fatal( "At work subtour " + s + " in mandatory tour " + t + " for person " + person + " in household " + hh.getID());
                    logger.fatal( "selected DC  " + dcResults[0] + " and subzone " + dcResults[1]);
                    logger.fatal( "however, the selected subzone has proportion " + (dcResults[0] == 1 ? "short walk" : "long walk") + " equal to 0.0.");
                    System.exit(1);
                }

               int todResult = chooseTimeOfDay(hh, persons[person], m);

                // set chosen in alternative in DMU and tour objects
                hh.mandatoryTours[t].subTours[s].setTimeOfDayAlt (todResult);

                int modeResult = chooseMode(hh, m, TourType.AT_WORK_CATEGORY, dcResults[1]);
                // set chosen in alternative in tour objects
                hh.mandatoryTours[t].subTours[s].setMode(modeResult);

                if ( modeResult == 3 || modeResult == 4 ) {
                    hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubModeType.LBS.ordinal() );
                    hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubModeType.LBS.ordinal() );
                } else {

					hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubModeType.ZERO.ordinal() );
					hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubModeType.ZERO.ordinal()  );

				}


            }
        }

        // reset the Household object data members to their original values
        hh.setOrigTaz ( hh.getTazID() );
        hh.setOriginWalkSegment( hh.getOriginWalkSegment() );
    }

    public int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex){
        Household hh = (Household) dmu;
        // reset the sample and availability arrays for the new household
        Arrays.fill ( dcAvailability, false );
        Arrays.fill(dcSample, 0);


        for (int k=1; k < dcAvailability.length; k++) {
            // set destination choice alternative availability to true if size > 0 for the segment.
            float size = ZonalDataManager.getTotSize (tourTypes[tourTypeIndex], k );
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


        // calculate mode choice logsums for dc alternatives in the sample
        long markTime = System.currentTimeMillis();
        for (int i=1; i < sample.length; i++) {

            int d = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(sample[i], 2);
            int s = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(sample[i], 3);
            hh.setChosenDest( d );
            hh.setChosenWalkSegment( s );

            // calculate mode choice logsum based on appropriate od skims
            hh.setTODDefaults ( TourType.AT_WORK_CATEGORY, "MdMd" );
            ZonalDataManager.setLogsumDcMDMD ( dcSample[i], getMcLogsum(hh, tourTypeIndex, TourType.AT_WORK_CATEGORY) );

        }
        dcLogsumTime += (System.currentTimeMillis()-markTime);

        // compute destination choice proportions and choose alternative
        markTime = System.currentTimeMillis();
        dc[tourTypeIndex].updateLogitModel ( hh, dcAvailability, dcSample );
        int chosen = dc[tourTypeIndex].getChoiceResult();
        int chosenDestAlt = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(chosen, 2);
        int chosenShrtWlk = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(chosen, 3);
        dcTime += (System.currentTimeMillis() - markTime);

        return new int[]{chosenDestAlt, chosenShrtWlk};
    }



    public int chooseTimeOfDay(Household hh, Person person, int tourTypeIndex ) {
        int startP;
        int endP;
        // count the number of tours in which no time-of-day alternative was available
        int noTOD = 0;
        for (int p=1; p <= tcUEC[tourTypeIndex].getNumberOfAlternatives(); p++) {
            if (tcAvailability[p]) {
                noTOD++;
                break;
            }
        }

        if (noTOD == 0) {
            noTODAvailableIndiv[tourTypeIndex]++;
            tcAvailability[defaultStart] = true;
            tcSample[defaultStart] = 1;
            tcAvailability[defaultEnd] = true;
            tcSample[defaultEnd] = 1;
        }

        // compute time-of-day choice proportions and choose alternative
        tc[tourTypeIndex].updateLogitModel ( hh, tcAvailability, tcSample );

        int chosenTODAlt;
        try {
            chosenTODAlt = tc[tourTypeIndex].getChoiceResult();
        }
        catch (ModelException e) {
            chosenTODAlt = SeededRandom.getRandom() < 0.5 ? defaultStart : defaultEnd;
        }

        // set hours unavailable associated with this subtour in case there are more subtours
        //TODO - note I changed this from MORPC, MORPC had todAlt instead.
        int start = TODDataManager.getTodStartHour ( chosenTODAlt );
        int end = TODDataManager.getTodEndHour ( chosenTODAlt );
        for (int p=1; p < tcAvailability.length; p++) {

            startP = TODDataManager.getTodStartHour ( p );
            endP = TODDataManager.getTodEndHour ( p );

            if ( (startP >= start && startP <= end) ||
                (endP >= start && endP <= end) ||
                (startP < start && endP > end) ) {
                    tcAvailability[p] = false;
                    tcSample[p] = 0;
            }

        }
        return chosenTODAlt;
    }



    public void defineUECModelSheets (int tourType) {

        //AtWork Worksheets
        final int DC_AtWork_MODEL_SHEET = 13;

        final int TOD_AtWork_MODEL_SHEET = 6;

        final int MC_AtWork_MODEL_SHEET = 24;
        final int MC_OD_UTIL_SHEET = 25;

        if (tourType == TourType.ATWORK) {
                DC_ModelSheet  = DC_AtWork_MODEL_SHEET;
                TOD_ModelSheet  = TOD_AtWork_MODEL_SHEET;
                MC_ModelSheet  = MC_AtWork_MODEL_SHEET;
                MCOD_ModelSheet = MC_OD_UTIL_SHEET;
            }
    }



}
