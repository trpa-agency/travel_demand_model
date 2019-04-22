package com.pb.tahoe.dest_time_mode;

import com.pb.common.model.ModelException;
import com.pb.common.util.SeededRandom;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.*;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * MandatoryDTM is a class that run the destination choice, time-of-day choice and mode choice model
 * for mandatory tours.
 *
 * @author Christi Willison
 * @version 1.0,  May 22, 2006
 */
public class MandatoryDTM extends DTMModel {
    protected static Logger logger = Logger.getLogger(MandatoryDTM.class);
    private boolean shadowPriceIteration = false;


    public MandatoryDTM (ResourceBundle rb) {
        super(rb, TourType.MANDATORY_CATEGORY, TourType.getTourTypesForCategory(TourType.MANDATORY_CATEGORY));
    }

    public void doWork(HouseholdArrayManager hhMgr){
        // get the list of households to be processed
        Household[] hhList = hhMgr.getHouseholds();
        int nHHs = 0;
        for (int i = 1; i < hhList.length; i++) {

            if(i % everyNth == 0){

                Household hh = hhList[i];
                chooseDestTODAndMode(hh);
                updateTimeWindows (hh);
                nHHs++;
                if(nHHs % 1000 ==0 ) {
                    baseLogger.info("Mandatory - Destination, TOD and Mode chosen for " + nHHs + " hhs");
                }
            }
        }
        hhMgr.sendResults ( hhList );

    }


    public void chooseDestTODAndMode(DecisionMakingUnit dmu){
        Household hh = (Household) dmu;
        // get the array of mandatory tours for this household.
        if ( hh.getMandatoryTours() == null )
            return;

        int hh_taz_id = hh.getTazID();
        hh.setOrigTaz (hh_taz_id);
        hh.setTourCategory( TourType.MANDATORY_CATEGORY );

        // get person array for this household.
        Person[] persons = hh.getPersonArray();

        // loop over all puposes for the mandatory tour category in order
        for (int m=0; m < tourTypes.length; m++) {

            // loop over individual tours of the tour purpose of interest for the hh
            for (int t=0; t < hh.mandatoryTours.length; t++) {

                int tourTypeIndex = m;

                int tourType = hh.mandatoryTours[t].getTourType();

                int person = hh.mandatoryTours[t].getTourPerson();

                //if tour type is school, and pattern is not school_work, and we're in a shadow price iteration, then
                // continue - we've already chosen the dtm for this tour
                if (tourType == TourType.SCHOOL && persons[person].getPatternType() != 5 && shadowPriceIteration) {
                    continue;
                }

                if ( tourType != tourTypes[m] ) {

                    // if we're processing work, and the tour is school, and the patterntype is school_work,
                    // process the tour as a school tour, even though the tourType is work.
                    if ( tourTypes[m] == TourType.WORK && tourType == TourType.SCHOOL
                        && persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
                            tourTypeIndex = 1;
                    } else {
                        continue; // otherwise, it's not the right tour type, so go to the next tour.
                    }
                    
                }  else {

                    // if we're processing school, and the tourType is school, and the patterntype is school_work,
                    // we've already processed the school tour, so skip to the next tour.
                    if ( tourTypes[m] == TourType.SCHOOL && tourType == TourType.SCHOOL
                        && persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
                            continue;
                    }

                }

                hh.mandatoryTours[t].setOrigTaz (hh_taz_id);
                hh.mandatoryTours[t].setOriginShrtWlk (hh.getOriginWalkSegment() );

                if (logger.isDebugEnabled())
                    logger.debug("in DTM mandatory dc, setting orig short walk="+hh.getOriginWalkSegment());

                hh.setPersonID ( person );
                hh.setTourID ( t );

                int[] dcResults = chooseDestination(hh, tourTypeIndex);

                // set the chosen value in Household and Tour objects
                hh.setChosenDest ( dcResults[0] );
                hh.setChosenWalkSegment( dcResults[1] );
                hh.mandatoryTours[t].setDestTaz (dcResults[0]);
                hh.mandatoryTours[t].setDestShrtWlk (dcResults[1]);
                if (tourType == TourType.WORK)
                    ZonalDataManager.addWorkTrip(dcResults[0]);


                // check to make sure that the DC subzone selected has subzone proportion > 0.0.
                if ( ZonalDataManager.getWalkPct ( dcResults[1], dcResults[0] ) == 0.0f ) {
                    logger.fatal( TourType.getCategoryLabelForCategory(tourTypeCategory) + " " + TourType.getTourTypeLabelsForCategory(tourTypeCategory)[tourTypeIndex] + " tour " + t + " for person " + person + " in household " + hh.getID());
                    logger.fatal( "selected  Dest TAZ " + dcResults[0] + " and subzone " + dcResults[1]);
                    logger.fatal( "however, the selected subzone has proportion " + (dcResults[1] == 1 ? "short walk" : "long walk") + " equal to 0.0.");
                    System.exit(1);
                }

                logger.debug("PERSON: " + hh.persons[person].getID());
                int todResult = chooseTimeOfDay(hh, persons[person], tourTypeIndex);
                logger.debug("Time of day choice: " + todResult);

                // set the hour as unavailable for all hours between start and end for this person
                int start = TODDataManager.getTodStartHour ( todResult );
                int end = TODDataManager.getTodEndHour ( todResult );
                logger.debug("\tstart: " + start);
                logger.debug("\tend: " + end);

                for (int j=start; j <= end; j++) {
                    hh.persons[person].setHourUnavailable(j);
                }

                logger.debug("Availability for person after mandatory tour time-of-day choice" + hh.persons[person].getID());
                String avail = "";
                for(int i=1; i < hh.persons[person].getAvailable().length; i++){
                    avail += hh.persons[person].getAvailable()[i] + ",";
                }
                logger.debug("Their availability is " + avail);
                logger.debug("\n\n");



                // set chosen alternative in tour objects
                hh.setChosenTodAlt( todResult );
                hh.mandatoryTours[t].setTimeOfDayAlt(todResult);

                int chosenShrtWlk = hh.mandatoryTours[t].getDestShrtWlk();
                int modeResult = chooseMode(hh, tourTypeIndex,TourType.MANDATORY_CATEGORY, chosenShrtWlk);
                // set chosen in alternative in tour objects
                hh.mandatoryTours[t].setMode(modeResult);

                if ( modeResult == 3 || modeResult == 4 ) {
                    hh.mandatoryTours[t].setSubmodeOB ( SubModeType.LBS.ordinal() );
                    hh.mandatoryTours[t].setSubmodeIB ( SubModeType.LBS.ordinal() );
                } else {

					hh.mandatoryTours[t].setSubmodeOB ( SubModeType.ZERO.ordinal() );
					hh.mandatoryTours[t].setSubmodeIB ( SubModeType.ZERO.ordinal()  );

				}



            }
        }
    }



    public int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex) {
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


            // calculate mode choice logsum based on appropriate od skims for each mandatory purpose
            if (TourType.getTourTypesForCategory(tourTypeCategory)[tourTypeIndex] == TourType.WORK ){
                hh.setTODDefaults(TourType.MANDATORY_CATEGORY, "AmPm");
                if (!shadowPriceIteration) {
                    ZonalDataManager.setLogsumDcAMPM ( sample[i], getMcLogsum(hh, tourTypeIndex,TourType.MANDATORY_CATEGORY) );
                }
            } else if (TourType.getTourTypesForCategory(tourTypeCategory)[tourTypeIndex] == TourType.SCHOOL) {
                hh.setTODDefaults(TourType.MANDATORY_CATEGORY, "AmMd");
                ZonalDataManager.setLogsumDcAMMD(sample[i], getMcLogsum(hh, tourTypeIndex,TourType.MANDATORY_CATEGORY));
                hh.setTODDefaults(TourType.MANDATORY_CATEGORY, "MdMd");
                ZonalDataManager.setLogsumDcMDMD(sample[i], getMcLogsum(hh, tourTypeIndex,TourType.MANDATORY_CATEGORY));
            }

        }
        if (TourType.getTourTypesForCategory(tourTypeCategory)[tourTypeIndex] == TourType.WORK ){
            if ((!shadowPriceIteration)) {
                hh.mandatoryTours[hh.getTourID()].setWorkMCLogsum(new float[ZonalDataManager.logsumDcAMPM.length]);
                System.arraycopy(ZonalDataManager.logsumDcAMPM,0,hh.mandatoryTours[hh.getTourID()].getWorkMCLogsum(),0,ZonalDataManager.logsumDcAMPM.length);
            } else {
                ZonalDataManager.logsumDcAMPM = hh.mandatoryTours[hh.getTourID()].getWorkMCLogsum();
            }
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





    public int chooseTimeOfDay(Household hh, Person person, int tourTypeIndex){

        // update the time of day choice availabilty based on available time windows
        // tcSample and tcAvailability are 1 based
        Arrays.fill(tcSample, 1);
        Arrays.fill(tcAvailability, true);

        logger.debug("Availability before mandatory tour time of day choice for person taking a tour of type " +
                TourType.getTourTypesForCategory(tourTypeCategory)[tourTypeIndex] + " in household " + hh.getID());
        setTcAvailability (person, tcAvailability, tcSample);


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
            tcAvailability[1] = true;
            tcSample[1] = 1;
            tcAvailability[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = true;
            tcSample[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = 1;
        }


        // calculate the mode choice logsums for TOD choice based on chosen dest and default time periods
        //TODO - these defaults are the ones we need to double-check
        long markTime = System.currentTimeMillis();
        if (TourType.getTourTypesForCategory(TourType.MANDATORY_CATEGORY)[tourTypeIndex] != TourType.SCHOOL) {

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaEa" );
            tcLogsumEaEa = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaAm" );
            tcLogsumEaAm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaMd" );
            tcLogsumEaMd = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY);

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaPm" );
            tcLogsumEaPm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaNt" );
            tcLogsumEaNt = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmAm" );
            tcLogsumAmAm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmMd" );
            tcLogsumAmMd = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmPm" );
            tcLogsumAmPm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmNt" );
            tcLogsumAmNt = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdMd" );
            tcLogsumMdMd = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdPm" );
            tcLogsumMdPm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdNt" );
            tcLogsumMdNt = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "PmPm" );
            tcLogsumPmPm = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "PmNt" );
            tcLogsumPmNt = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

            hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "NtNt" );
            tcLogsumNtNt = getMcLogsum ( hh, tourTypeIndex,TourType.MANDATORY_CATEGORY );

        }


        // assign mode choice logsums to time-of-day choice alternatives, given correspondiong time periods
        if (TourType.getTourTypesForCategory(TourType.MANDATORY_CATEGORY)[tourTypeIndex] != TourType.SCHOOL) {
            TODDataManager.logsumTcEAEA[1]   = tcLogsumEaEa;
            TODDataManager.logsumTcEAEA[2]   = tcLogsumEaEa;
            TODDataManager.logsumTcEAAM[3]   = tcLogsumEaAm;
            TODDataManager.logsumTcEAAM[4]   = tcLogsumEaAm;
            TODDataManager.logsumTcEAAM[5]   = tcLogsumEaAm;
            TODDataManager.logsumTcEAMD[6]   = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[7]   = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[8]   = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[9]   = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[10]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[11]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAPM[12]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[13]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[14]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[15]  = tcLogsumEaPm;
            TODDataManager.logsumTcEANT[16]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[17]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[18]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[19]  = tcLogsumEaNt;
            TODDataManager.logsumTcEAEA[20]  = tcLogsumEaEa;
            TODDataManager.logsumTcEAAM[21]  = tcLogsumEaAm;
            TODDataManager.logsumTcEAAM[22]  = tcLogsumEaAm;
            TODDataManager.logsumTcEAAM[23]  = tcLogsumEaAm;
            TODDataManager.logsumTcEAMD[24]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[25]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[26]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[27]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[28]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAMD[29]  = tcLogsumEaMd;
            TODDataManager.logsumTcEAPM[30]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[31]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[32]  = tcLogsumEaPm;
            TODDataManager.logsumTcEAPM[33]  = tcLogsumEaPm;
            TODDataManager.logsumTcEANT[34]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[35]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[36]  = tcLogsumEaNt;
            TODDataManager.logsumTcEANT[37]  = tcLogsumEaNt;
            TODDataManager.logsumTcAMAM[38]  = tcLogsumEaAm;
            TODDataManager.logsumTcAMAM[39]  = tcLogsumEaAm;
            TODDataManager.logsumTcAMAM[40]  = tcLogsumEaAm;
            TODDataManager.logsumTcAMMD[41]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[42]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[43]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[44]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[45]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[46]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMPM[47]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[48]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[49]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[50]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMNT[51]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[52]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[53]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[54]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMAM[55]  = tcLogsumAmAm;
            TODDataManager.logsumTcAMAM[56]  = tcLogsumAmAm;
            TODDataManager.logsumTcAMMD[57]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[58]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[59]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[60]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[61]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[62]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMPM[63]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[64]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[65]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[66]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMNT[67]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[68]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[69]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[70]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMAM[71]  = tcLogsumAmAm;
            TODDataManager.logsumTcAMMD[72]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[73]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[74]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[75]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[76]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMMD[77]  = tcLogsumAmMd;
            TODDataManager.logsumTcAMPM[78]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[79]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[80]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMPM[81]  = tcLogsumAmPm;
            TODDataManager.logsumTcAMNT[82]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[83]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[84]  = tcLogsumAmNt;
            TODDataManager.logsumTcAMNT[85]  = tcLogsumAmNt;
            TODDataManager.logsumTcMDMD[86]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[87]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[88]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[89]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[90]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[91]  = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[92]  = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[93]  = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[94]  = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[95]  = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[96]  = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[97]  = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[98]  = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[99]  = tcLogsumMdNt;
            TODDataManager.logsumTcMDMD[100] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[101] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[102] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[103] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[104] = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[105] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[106] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[107] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[108] = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[109] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[110] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[111] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[112] = tcLogsumMdNt;
            TODDataManager.logsumTcMDMD[113] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[114] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[115] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[116] = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[117] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[118] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[119] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[120] = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[121] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[122] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[123] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[124] = tcLogsumMdNt;
            TODDataManager.logsumTcMDMD[125] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[126] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[127] = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[128] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[129] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[130] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[131] = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[132] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[133] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[134] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[135] = tcLogsumMdNt;
            TODDataManager.logsumTcMDMD[136] = tcLogsumMdMd;
            TODDataManager.logsumTcMDMD[137] = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[138] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[139] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[140] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[141] = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[142] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[143] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[144] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[145] = tcLogsumMdNt;
            TODDataManager.logsumTcMDMD[146] = tcLogsumMdMd;
            TODDataManager.logsumTcMDPM[147] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[148] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[149] = tcLogsumMdPm;
            TODDataManager.logsumTcMDPM[150] = tcLogsumMdPm;
            TODDataManager.logsumTcMDNT[151] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[152] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[153] = tcLogsumMdNt;
            TODDataManager.logsumTcMDNT[154] = tcLogsumMdNt;
            TODDataManager.logsumTcPMPM[155] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[156] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[157] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[158] = tcLogsumPmPm;
            TODDataManager.logsumTcPMNT[159] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[160] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[161] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[162] = tcLogsumPmNt;
            TODDataManager.logsumTcPMPM[163] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[164] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[165] = tcLogsumPmPm;
            TODDataManager.logsumTcPMNT[166] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[167] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[168] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[169] = tcLogsumPmNt;
            TODDataManager.logsumTcPMPM[170] = tcLogsumPmPm;
            TODDataManager.logsumTcPMPM[171] = tcLogsumPmPm;
            TODDataManager.logsumTcPMNT[172] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[173] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[174] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[175] = tcLogsumPmNt;
            TODDataManager.logsumTcPMPM[176] = tcLogsumPmPm;
            TODDataManager.logsumTcPMNT[177] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[178] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[179] = tcLogsumPmNt;
            TODDataManager.logsumTcPMNT[180] = tcLogsumPmNt;
            TODDataManager.logsumTcNTNT[181] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[182] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[183] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[184] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[185] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[186] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[187] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[188] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[189] = tcLogsumNtNt;
            TODDataManager.logsumTcNTNT[190] = tcLogsumNtNt;
        }
        tcLogsumTime += (System.currentTimeMillis()-markTime);

        // compute time-of-day choice proportions and choose alternative
        markTime = System.currentTimeMillis();
        tc[tourTypeIndex].updateLogitModel ( hh, tcAvailability, tcSample );

        int chosenTODAlt;
        try {
            chosenTODAlt = tc[tourTypeIndex].getChoiceResult();
        }
        catch (ModelException e) {
            chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
        }
        tcTime += (System.currentTimeMillis()-markTime);

        return chosenTODAlt;
    }

    public void updateTimeWindows ( Household hh ) {

        boolean pAdult;
        boolean qAdult;


        // get person array for this household.
        Person[] persons = hh.getPersonArray();


        int maxAdultOverlapsHH = 0;
        int maxChildOverlapsHH = 0;
        int maxMixedOverlapsHH = 0;
        int maxAdultWindowHH = 0;
        int maxChildWindowHH = 0;

        int maxAdultOverlapsP = 0;
        int maxChildOverlapsP = 0;

        // loop over persons in the household and count available time windows
        for (int p=1; p < persons.length; p++) {

            // determine if person p is an adult
            if (persons[p].getPersonType() == PersonType.WORKER_F ||
                persons[p].getPersonType() == PersonType.WORKER_P ||
                persons[p].getPersonType() == PersonType.NONWORKER)
                    pAdult = true;
            else
                    pAdult = false;

            boolean[] pAvail = persons[p].getAvailable();


            // loop over time windows between 7 & 22 (hours in which to count avaiable hours)
            // and count instances where the hour is available for person p.
            int window = 0;
            for (int w=7; w <= 22; w++) {
                if (pAvail[w])
                    window++;
            }

            if (pAdult && window > maxAdultWindowHH)
                maxAdultWindowHH = window;
            else if (!pAdult && window > maxChildWindowHH)
                maxChildWindowHH = window;



            // loop over persons greater than p and compute available time window overlaps.
            // Don't need q,p if we've already done p,q,
            // so we only need triangular part of matrix above diagonal.
            for (int q=p+1; q < persons.length; q++) {

                // determine if person q is an adult
                if (persons[q].getPersonType() == PersonType.WORKER_F ||
                    persons[q].getPersonType() == PersonType.WORKER_P ||
                   persons[q].getPersonType() == PersonType.NONWORKER)
                        qAdult = true;
                else
                        qAdult = false;

                boolean[] qAvail = persons[q].getAvailable();


                // loop over time windows between 7 & 22 (hours in which to start a joint tour)
                // and count instances where the hour is available for both persons
                int overlaps = 0;
                for (int w=7; w <= 22; w++) {
                    if (pAvail[w] && qAvail[w])
                        overlaps++;
                }

                // determine max time window overlap between pairs of adults,
                // pairs of children, and pairs of 1 adult 1 child
                if (pAdult && qAdult) {
                    if (overlaps > maxAdultOverlapsHH)
                        maxAdultOverlapsHH = overlaps;
                    if (overlaps > maxAdultOverlapsP)
                        maxAdultOverlapsP = overlaps;
                }
                else if (!pAdult && !qAdult) {
                    if (overlaps > maxChildOverlapsHH)
                        maxChildOverlapsHH = overlaps;
                    if (overlaps > maxChildOverlapsP)
                        maxChildOverlapsP = overlaps;
                }
                else {
                    if (overlaps > maxMixedOverlapsHH)
                        maxMixedOverlapsHH = overlaps;
                }

            } // end of person q

            // set person attributes
            persons[p].setAvailableWindow( window );
            persons[p].setMaxAdultOverlaps( maxAdultOverlapsP );
            persons[p].setMaxChildOverlaps( maxChildOverlapsP );

        } // end of person p

        // set household attributes
        hh.setMaxAdultOverlaps( maxAdultOverlapsHH );
        hh.setMaxChildOverlaps( maxChildOverlapsHH );
        hh.setMaxMixedOverlaps( maxMixedOverlapsHH );
        hh.setMaxAdultWindow (maxAdultWindowHH);
        hh.setMaxChildWindow (maxChildWindowHH);

    }

    public void defineUECModelSheets (int tourType) {

        //Mandatory Worksheets
        final int DC_MandWork_MODEL_SHEET;
        if (this.summer) {
            DC_MandWork_MODEL_SHEET = 1;
        } else {
            DC_MandWork_MODEL_SHEET = 2;
        }
        final int DC_MandSchool_MODEL_SHEET = 3;

        final int TOD_MandWork_MODEL_SHEET = 1;
        final int TOD_MandSchool_MODEL_SHEET = 2;

        final int MC_MandWork_MODEL_SHEET = 2;
        final int MC_OD_UTIL_MandWork_SHEET = 3;
        final int MC_MandSchool_MODEL_SHEET = 4;
        final int MC_OD_UTIL_MandSchool_SHEET = 5;

        if (tourType == TourType.WORK) {
                DC_ModelSheet  = DC_MandWork_MODEL_SHEET;
                TOD_ModelSheet  = TOD_MandWork_MODEL_SHEET;
                MC_ModelSheet  = MC_MandWork_MODEL_SHEET;
                MCOD_ModelSheet = MC_OD_UTIL_MandWork_SHEET;
            }
            else if (tourType == TourType.SCHOOL) {
                DC_ModelSheet  = DC_MandSchool_MODEL_SHEET;
                TOD_ModelSheet  = TOD_MandSchool_MODEL_SHEET;
                MC_ModelSheet  = MC_MandSchool_MODEL_SHEET;
                MCOD_ModelSheet = MC_OD_UTIL_MandSchool_SHEET;
            }
    }

    public void setShadowPriceIteration (boolean shadowPriceIteration) {
        this.shadowPriceIteration = shadowPriceIteration;
    }

}

