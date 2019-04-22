package com.pb.tahoe.dest_time_mode;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */


import com.pb.common.model.ModelException;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.structures.*;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.ResourceBundle;


public class NonMandatoryDTM extends DTMModel {
    protected static Logger logger = Logger.getLogger(NonMandatoryDTM.class);


    public NonMandatoryDTM (ResourceBundle rb) {
        super(rb, TourType.NON_MANDATORY_CATEGORY, TourType.getTourTypesForCategory(TourType.NON_MANDATORY_CATEGORY));

    }

    public void chooseDestTODAndMode(DecisionMakingUnit dmu){
        Household hh = (Household) dmu;
        // get the array of individual non-mandatory tours for this household.
        if (hh.getIndivTours() == null)
            return;

        int hh_taz_id = hh.getTazID();
        hh.setOrigTaz (hh_taz_id);
        hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );

        // get person array for this household.
        Person[] persons = hh.getPersonArray();

        // loop over all puposes for the individual non-mandatory tour category in order
        for (int m=0; m < tourTypes.length; m++) {

            // loop over individual tours of the tour purpose of interest for the hh
            for (int t=0; t < hh.indivTours.length; t++) {

                if ( hh.indivTours[t].getTourType() != tourTypes[m] )
                    continue;

                int person = hh.indivTours[t].getTourPerson();
                hh.indivTours[t].setOrigTaz (hh_taz_id);
                hh.indivTours[t].setOriginShrtWlk (hh.getOriginWalkSegment() );

                hh.setPersonID ( person );
                hh.setTourID ( t );

                int[] dcResults = chooseDestination(hh, m);

                // set the chosen value in DMU and tour objects
                hh.setChosenDest ( dcResults[0] );
                hh.setChosenWalkSegment( dcResults[1] );
                hh.indivTours[t].setDestTaz (dcResults[0]);
				hh.indivTours[t].setDestShrtWlk (dcResults[1]);



                // check to make sure that the DC subzone selected has subzone proportion > 0.0.
                if ( ZonalDataManager.getWalkPct ( dcResults[1], dcResults[0] ) == 0.0f ) {
                    logger.fatal( TourType.getCategoryLabelForCategory(tourTypeCategory) + " "
                            + TourType.getTourTypeLabelsForCategory(tourTypeCategory)[m] + " tour " + t + " for person " + person + " in household " + hh.getID());
                    logger.fatal( "selected  Dest TAZ " + dcResults[0] + " and subzone " + dcResults[1]);
                    logger.fatal( "however, the selected subzone has proportion " + (dcResults[0] == 1 ? "short walk" : "long walk") + " equal to 0.0.");
                    System.exit(1);
                }

                int todResult = chooseTimeOfDay(hh, persons[person], m);

                // set the hour as unavailable for all hours between start and end for this person
                int start = TODDataManager.getTodStartHour ( todResult );
                int end = TODDataManager.getTodEndHour ( todResult );
                for (int j=start; j <= end; j++) {
                    hh.persons[person].setHourUnavailable(j);
                }

                // set chosen in alternative in tour objects
                hh.setChosenTodAlt( todResult );
                hh.indivTours[t].setTimeOfDayAlt (todResult);

                int chosenShrtWlk = hh.indivTours[t].getDestShrtWlk();
                int modeResult = chooseMode(hh, m,TourType.NON_MANDATORY_CATEGORY, chosenShrtWlk);
                // set chosen in alternative in tour objects
                hh.indivTours[t].setMode(modeResult);

                if ( modeResult == 3 || modeResult == 4 ) {
                    hh.indivTours[t].setSubmodeOB ( SubModeType.LBS.ordinal() );
                    hh.indivTours[t].setSubmodeIB ( SubModeType.LBS.ordinal() );
                } else {

					hh.indivTours[t].setSubmodeOB ( SubModeType.ZERO.ordinal() );
					hh.indivTours[t].setSubmodeIB ( SubModeType.ZERO.ordinal()  );

				}





            }
        }

    }

    public int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex){
        Household hh = (Household) dmu;
         // get the destination choice sample of alternatives
        Arrays.fill ( dcAvailability, false );
        Arrays.fill(dcSample, 0);

        // determine the set of alternatives from which the sample of alternatives will be drawn
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

        long markTime = System.currentTimeMillis();
        for (int i=1; i < sample.length; i++) {

            int d = (int) dcUEC[tourTypeIndex].getAlternativeData().getIndexedValueAt(sample[i], 2);
            hh.setChosenDest( d );

            // calculate mode choice logsum based on appropriate od skims for each individual non-mandatory purpose
            if (TourType.getTourTypesForCategory(TourType.NON_MANDATORY_CATEGORY)[tourTypeIndex] == TourType.ESCORTING) {
                hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
                ZonalDataManager.setLogsumDcMDMD ( dcSample[i], getMcLogsum(hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY) );
            }
            else {
                hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
                ZonalDataManager.setLogsumDcMDMD (dcSample[i], getMcLogsum(hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY) );
                hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmNt" );
                ZonalDataManager.setLogsumDcPMNT ( dcSample[i], getMcLogsum(hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY) );
            }
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



    public int chooseTimeOfDay(Household hh, Person person, int tourTypeIndex ) {
        // update the time of day choice availabilty based on available time windows
        // tcSample and tcAvailability are 1 based
        Arrays.fill(tcSample, 1);
        Arrays.fill(tcAvailability, true);

        setTcAvailability ( person, tcAvailability, tcSample );


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


        long markTime = System.currentTimeMillis();
        // calculate the mode choice logsums for TOD choice based on chosen dest and default time periods
        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaEa" );
        tcLogsumEaEa = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaAm" );
        tcLogsumEaAm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaMd" );
        tcLogsumEaMd = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaPm" );
        tcLogsumEaPm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaNt" );
        tcLogsumEaNt = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmAm" );
        tcLogsumAmAm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmMd" );
        tcLogsumAmMd = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmPm" );
        tcLogsumAmPm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmNt" );
        tcLogsumAmNt = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
        tcLogsumMdMd = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdPm" );
        tcLogsumMdPm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdNt" );
        tcLogsumMdNt = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmPm" );
        tcLogsumPmPm = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmNt" );
        tcLogsumPmNt = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );

        hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "NtNt" );
        tcLogsumNtNt = getMcLogsum ( hh, tourTypeIndex, TourType.NON_MANDATORY_CATEGORY );



        // assign mode choice logsums to time-of-day choice alternatives, given correspondiong time periods
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



    public void defineUECModelSheets (int tourType) {

        //NonMandatory Worksheets
        final int DC_NonMandEscort_MODEL_SHEET = 8;
        final int DC_NonMandShop_MODEL_SHEET = 9;
        final int DC_NonMandOthMaint_MODEL_SHEET = 10;
        final int DC_NonMandDisc_MODEL_SHEET = 11;
        final int DC_NonMandEat_MODEL_SHEET = 12;

        final int TOD_NonMandEscort_MODEL_SHEET = 4;
        final int TOD_NonMandAllButEscort_MODEL_SHEET = 5;

        final int MC_NonMandEscort_MODEL_SHEET = 14;
        final int MC_NonMandEscort_OD_UTIL_SHEET = 15;
        final int MC_NonMandShop_MODEL_SHEET = 16;
        final int MC_NonMandShop_OD_UTIL_SHEET = 17;
        final int MC_NonMandOthMaint_MODEL_SHEET = 18;
        final int MC_NonMandOthMaint_OD_UTIL_SHEET = 19;
        final int MC_NonMandDisc_MODEL_SHEET = 20;
        final int MC_NonMandDisc_OD_UTIL_SHEET = 21;
        final int MC_NonMandEat_MODEL_SHEET = 22;
        final int MC_NonMandEat_OD_UTIL_SHEET = 23;

        if (tourType == TourType.ESCORTING) {
                DC_ModelSheet  = DC_NonMandEscort_MODEL_SHEET;
                TOD_ModelSheet  = TOD_NonMandEscort_MODEL_SHEET;
                MC_ModelSheet  = MC_NonMandEscort_MODEL_SHEET;
                MCOD_ModelSheet = MC_NonMandEscort_OD_UTIL_SHEET;
            }
            else if (tourType == TourType.SHOP) {
                DC_ModelSheet  = DC_NonMandShop_MODEL_SHEET;
                TOD_ModelSheet  = TOD_NonMandAllButEscort_MODEL_SHEET;
                MC_ModelSheet  = MC_NonMandShop_MODEL_SHEET;
                MCOD_ModelSheet = MC_NonMandShop_OD_UTIL_SHEET;
            }
            else if (tourType == TourType.OTHER_MAINTENANCE) {
                DC_ModelSheet  = DC_NonMandOthMaint_MODEL_SHEET;
                TOD_ModelSheet  = TOD_NonMandAllButEscort_MODEL_SHEET;
                MC_ModelSheet  = MC_NonMandOthMaint_MODEL_SHEET;
                MCOD_ModelSheet = MC_NonMandOthMaint_OD_UTIL_SHEET;
            }
            else if (tourType == TourType.DISCRETIONARY) {
                DC_ModelSheet  = DC_NonMandDisc_MODEL_SHEET;
                TOD_ModelSheet  = TOD_NonMandAllButEscort_MODEL_SHEET;
                MC_ModelSheet  = MC_NonMandDisc_MODEL_SHEET;
                MCOD_ModelSheet = MC_NonMandDisc_OD_UTIL_SHEET;
            }
            else if (tourType == TourType.EAT) {
                DC_ModelSheet  = DC_NonMandEat_MODEL_SHEET;
                TOD_ModelSheet  = TOD_NonMandAllButEscort_MODEL_SHEET;
                MC_ModelSheet  = MC_NonMandEat_MODEL_SHEET;
                MCOD_ModelSheet = MC_NonMandEat_OD_UTIL_SHEET;
            }
    }



}
