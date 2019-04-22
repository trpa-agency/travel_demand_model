package com.pb.tahoe.dest_time_mode;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.Person;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.structures.DecisionMakingUnit;
import com.pb.tahoe.util.ChoiceModelApplication;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

abstract public class DTMModel {

    protected static Logger baseLogger = Logger.getLogger(DTMModel.class);


    public int everyNth;
    public boolean summer;
    protected int DC_ModelSheet  = 0;
    protected int TOD_ModelSheet  = 0;
    protected int MC_ModelSheet  = 0;
    protected int MCOD_ModelSheet = 0;

    protected int DC_DataSheet  = 0;
    protected int TOD_DataSheet  = 0;
    protected int MC_DataSheet  = 0;
    protected int MCOD_DataSheet = 1;

    protected int[] noTODAvailableIndiv = new int[TourType.TYPES];
    protected int[] noTODAvailableJoint = new int[TourType.TYPES];

    protected ChoiceModelApplication[] dc;
    protected ChoiceModelApplication[] tc;
    protected ChoiceModelApplication[] mc;
    protected UtilityExpressionCalculator[] dcUEC;
    protected UtilityExpressionCalculator[] tcUEC;
    protected UtilityExpressionCalculator[] mcODUEC;
    protected UtilityExpressionCalculator[] mcUEC;
    protected UtilityExpressionCalculator[] smcUEC;

    protected float tcLogsumEaEa = 0.0f;
    protected float tcLogsumEaAm = 0.0f;
    protected float tcLogsumEaMd = 0.0f;
    protected float tcLogsumEaPm = 0.0f;
    protected float tcLogsumEaNt = 0.0f;
    protected float tcLogsumAmAm = 0.0f;
    protected float tcLogsumAmMd = 0.0f;
    protected float tcLogsumAmPm = 0.0f;
    protected float tcLogsumAmNt = 0.0f;
    protected float tcLogsumMdMd = 0.0f;
    protected float tcLogsumMdPm = 0.0f;
    protected float tcLogsumMdNt = 0.0f;
    protected float tcLogsumPmPm = 0.0f;
    protected float tcLogsumPmNt = 0.0f;
    protected float tcLogsumNtNt = 0.0f;

    protected double[] mcUtilities;

    protected boolean[] mcAvailability;
    protected boolean[] tcAvailability;
    protected boolean[] dcAvailability;

    protected int[] mcLogsumAvailability;

    protected int[] dcSample;
    protected int[] tcSample;
    protected int[] mcSample;

    protected short tourTypeCategory;
    protected short[] tourTypes;
    protected long dcLogsumTime = 0;
    protected long dcTime = 0;

    protected long tcLogsumTime = 0;
    protected long tcTime = 0;

    protected long mcLogsumTime = 0;
    protected long mcTime = 0;

    private static HashMap modalODUtilityMap = new HashMap(2097152);
    boolean traceCalculations;  //set in properties file
    ArrayList<String> keysToTrace = new ArrayList<String>();



    public DTMModel ( ResourceBundle rb, short tourTypeCategory, short[] tourTypes ) {

        initDTMModelBase ( rb, tourTypeCategory, tourTypes );

    }

    private void initDTMModelBase ( ResourceBundle rb, short tourTypeCategory, short[] tourTypes ) {

        traceCalculations = ResourceUtil.getBooleanProperty(rb,"traceCalculations",false);
        keysToTrace = ResourceUtil.getList(rb, "keysToTrace");

        everyNth =  ResourceUtil.getIntegerProperty(rb,"everyNth", 1);
        summer =  ResourceUtil.getBooleanProperty(rb,"summer", true); 

        this.tourTypeCategory = tourTypeCategory;
        this.tourTypes = tourTypes;

        baseLogger.info ( getTourCategoryLabel() + " Tours: Running DC, TOD, and MC Models" );

        // create choice model objects and UECs for each purpose
        dc = new ChoiceModelApplication[tourTypes.length];
        tc = new ChoiceModelApplication[tourTypes.length];
        mc = new ChoiceModelApplication[tourTypes.length];

        dcUEC   = new UtilityExpressionCalculator[tourTypes.length];
        tcUEC   = new UtilityExpressionCalculator[tourTypes.length];
        mcUEC   = new UtilityExpressionCalculator[tourTypes.length];
        mcODUEC = new UtilityExpressionCalculator[tourTypes.length];

        for (int i=0; i < tourTypes.length; i++) {
            defineUECModelSheets (tourTypes[i]);
            String categoryString = getTourCategoryLabel().toLowerCase();

            HashMap<String,String> dtmRbKeyMap = getDTMRbKeyMap(categoryString);

            dc[i] =  new ChoiceModelApplication(dtmRbKeyMap.get("DC"), categoryString + dtmRbKeyMap.get("OUT"), rb);
            tc[i] =  new ChoiceModelApplication(dtmRbKeyMap.get("TOD"), categoryString + dtmRbKeyMap.get("OUT"), rb);
            mc[i] =  new ChoiceModelApplication(dtmRbKeyMap.get("MC"), categoryString + dtmRbKeyMap.get("OUT"), rb);

            createDTMUECs(i);

            // create logit model
            dc[i].createLogitModel();
            tc[i].createLogitModel();
            mc[i].createLogitModel();
        }

        int numDcAlternatives = dcUEC[tourTypes.length - 1].getNumberOfAlternatives();
        int numTcAlternatives = tcUEC[tourTypes.length - 1].getNumberOfAlternatives();
        int numMcAlternatives = mcUEC[tourTypes.length - 1].getNumberOfAlternatives();

        mcAvailability = new boolean[numMcAlternatives+1];
        tcAvailability = new boolean[numTcAlternatives+1];
        dcAvailability = new boolean[numDcAlternatives+1];


        dcSample = new int[numDcAlternatives+1];
        tcSample = new int[numTcAlternatives+1];
        mcSample = new int[numMcAlternatives+1];


        //Can't find where these are initialized prior to the destination
        //choice model and I think they need to be.
        Arrays.fill (mcSample, 1);
        Arrays.fill (mcAvailability, true);


        baseLogger.info( "DTM for category " + tourTypeCategory);
    }

    protected String getTourCategoryLabel() {
        return TourType.getCategoryLabelForCategory(tourTypeCategory);
    }

    protected String getTourTypeLabel(int tourIndex) {
        return TourType.getTourTypeLabelsForCategory(tourTypeCategory)[tourIndex];
    }

    protected HashMap<String,String> getDTMRbKeyMap(String categoryString) {
        HashMap<String,String> dtmRbKeyMap = new HashMap<String,String>();
        dtmRbKeyMap.put("DC","destination.choice.control.file");
        dtmRbKeyMap.put("TOD","time.of.day.control.file");
        dtmRbKeyMap.put("MC","mode.choice.control.file");
        dtmRbKeyMap.put("OUT",categoryString +"_dtm.choice.output.file");
        return dtmRbKeyMap;
    }

    protected void createDTMUECs(int tourType) {
        // create dest choice UEC
        baseLogger.info ("\tCreating " + getTourTypeLabel(tourType) + " Destination Choice UECs");
        dcUEC[tourType] = dc[tourType].getUEC(DC_ModelSheet, DC_DataSheet);

        // create time-of-day choice UEC
        baseLogger.info("\tCreating " + getTourTypeLabel(tourType) + " Time-of-Day Choice UECs");
        tcUEC[tourType] = tc[tourType].getUEC(TOD_ModelSheet,  TOD_DataSheet);

        // create UEC to calculate OD component of mode choice utilities
        baseLogger.info("\tCreating " + getTourTypeLabel(tourType) + " Mode Choice OD UECs");
        mcODUEC[tourType] = mc[tourType].getUEC(MCOD_ModelSheet,  MCOD_DataSheet);

        // create UEC to calculate non-OD component of mode choice utilities
        baseLogger.info("\tCreating " + getTourTypeLabel(tourType) + " Mode Choice UECs");
        mcUEC[tourType] = mc[tourType].getUEC(MC_ModelSheet,  MC_DataSheet);
    }


    public void doWork(HouseholdArrayManager hhMgr){
        // get the list of households to be processed
        Household[] hhList = hhMgr.getHouseholds();
        int nHHs = 0;
        for (int i = 1; i < hhList.length; i++) {

            if(i % everyNth == 0){

                Household hh = hhList[i];
                chooseDestTODAndMode(hh);
                nHHs++;
                if(nHHs % 1000 ==0 ) {
                    baseLogger.info("Destination, TOD and Mode chosen for " + nHHs + " hhs");
                }
            }
        }
        hhMgr.sendResults ( hhList );

    }


    protected float getMcLogsum ( DecisionMakingUnit dmu, int tourTypeIndex, short tourCategory ) {
        
        // first calculate the OD related mode choice utility
        setMcODUtility ( dmu, tourTypeIndex, tourCategory );

        // calculate the final mode choice utilities, exponentiate them, and calculate the logsum
        mc[tourTypeIndex].updateLogitModel ( dmu, mcAvailability, mcSample );

        // return the mode choice logsum
        return (float)mc[tourTypeIndex].getLogsum();
    }


    protected void setMcODUtility ( DecisionMakingUnit dmu, int tourTypeIndex, short tourCategory ) {

        double[] ModalUtilities = null;
        String mapKey = defineMapKey(dmu, tourTypeIndex, tourCategory);

        //either get a saved value from the hashmap or calculate it.
        if ( modalODUtilityMap.containsKey( mapKey ) ) {
            ModalUtilities = (double[]) modalODUtilityMap.get( mapKey );
        }
        else {
            IndexValues index = new IndexValues();
            index.setOriginZone( dmu.getOrigTaz() );
            index.setDestZone( dmu.getChosenDest() );
            index.setZoneIndex( dmu.getTazID() );
            index.setHHIndex( dmu.getID() );

            try {
                ModalUtilities = mcODUEC[tourTypeIndex].solve(index, dmu, mcLogsumAvailability);
            }
            catch (java.lang.Exception e) {
                baseLogger.fatal ("runtime exception occurred in DTMModelBase.setMcODUtility() for dmu id=" + dmu.getID(), e );
                baseLogger.fatal("");
                baseLogger.fatal("tourTypeIndex=" + tourTypeIndex);
                baseLogger.fatal("UEC NumberOfAlternatives=" + mcODUEC[tourTypeIndex].getNumberOfAlternatives());
                baseLogger.fatal("UEC MethodInvoker Source Code=");
                baseLogger.fatal(mcODUEC[tourTypeIndex].getMethodInvokerSourceCode());
                baseLogger.fatal("UEC MethodInvoker Variable Table=");
                baseLogger.fatal(mcODUEC[tourTypeIndex].getVariableTable());
                baseLogger.fatal("UEC AlternativeNames=" + mcODUEC[tourTypeIndex].getAlternativeNames());
                String[] altNames = mcODUEC[tourTypeIndex].getAlternativeNames();
                for (int i=0; i < altNames.length; i++)
                    baseLogger.fatal( "[" + i + "]:  " + altNames[i] );
                baseLogger.fatal("");
                dmu.writeContentToLogger(baseLogger);
                baseLogger.fatal("");
                e.printStackTrace();
                System.exit(-1);
            }
            //now that it is calculated, put it in the hashmap
            modalODUtilityMap.put ( mapKey, ModalUtilities );
        }

        ZonalDataManager.setOdUtilModeAlt (ModalUtilities);

        if(traceCalculations){
            if(keysToTrace.contains(mapKey)){
                baseLogger.info("MC OD Utilities for " + mapKey);
                String[] altNames = mcODUEC[tourTypeIndex].getAlternativeNames();
                for (int i=0; i < altNames.length; i++)
                    baseLogger.fatal( altNames[i] + ":  " + ModalUtilities[i] );
                baseLogger.fatal("");
            }
        }

    }

   // public String defineMapKey(Household hh, int tourTypeIndex, short tourCategory){
     public String defineMapKey(DecisionMakingUnit dmu, int tourTypeIndex, short tourCategory){
        String mapKey = "";
        Household hh = (Household) dmu;

        int todAlt = hh.getChosenTodAlt();
        int startPeriod = TODDataManager.getTodStartPeriod( todAlt );
        int startSkimPeriod = TODDataManager.getTodSkimPeriod ( startPeriod );
        int endPeriod = TODDataManager.getTodEndPeriod( todAlt );
        int endSkimPeriod = TODDataManager.getTodSkimPeriod ( endPeriod );

        if(tourCategory == TourType.MANDATORY_CATEGORY){
            mapKey = Integer.toString(tourTypeIndex) + "_"
                        + Integer.toString(startSkimPeriod) + "_"
                        + Integer.toString(endSkimPeriod) + "_"
                        + Integer.toString(hh.getOrigTaz()) + "_"
                        + Integer.toString(hh.getOriginWalkSegment()) + "_"
                        + Integer.toString(hh.getChosenDest() )  + "_"
                        + Integer.toString(hh.getChosenWalkSegment());
        } else if(tourCategory == TourType.JOINT_CATEGORY){
            mapKey = Integer.toString((int)hh.getPartySize()) + "_"
                        + Integer.toString(startSkimPeriod) + "_"
                        + Integer.toString(endSkimPeriod) + "_"
                        + Integer.toString(hh.getOrigTaz()) + "_"
                        + Integer.toString(hh.getOriginWalkSegment()) + "_"
                        + Integer.toString(hh.getChosenDest() )  + "_"
                        + Integer.toString(hh.getChosenWalkSegment());
        } else if(tourCategory == TourType.NON_MANDATORY_CATEGORY){
            mapKey = Integer.toString(tourTypeIndex) + "_"
                        + Integer.toString(startSkimPeriod) + "_"
                        + Integer.toString(endSkimPeriod) + "_"
                        + Integer.toString(hh.getOrigTaz()) + "_"
                        + Integer.toString(hh.getOriginWalkSegment()) + "_"
                        + Integer.toString(hh.getChosenDest() );
        }  else if(tourCategory == TourType.AT_WORK_CATEGORY){
            mapKey = Integer.toString(startSkimPeriod) + "_"
                        + Integer.toString(endSkimPeriod) + "_"
                        + Integer.toString(hh.getOrigTaz()) + "_"
                        + Integer.toString(hh.getOriginWalkSegment()) + "_"
                        + Integer.toString(hh.getChosenDest() )  + "_"
                        + Integer.toString(hh.getChosenWalkSegment());
        }
        return mapKey;
    }

    protected void setTcAvailability (Person person, boolean[] tcAvailability, int[] tcSample) {

        int start;
        int end;

        boolean[] personAvailability = person.getAvailable();

        for (int p=1; p < tcAvailability.length; p++) {

            start = TODDataManager.getTodStartHour (p);
            end = TODDataManager.getTodEndHour (p);

            // if any hour between the start and end of tod combination p is unavailable,
            // the combination is unavailable.
            for (int j=start; j <= end; j++) {
                if (!personAvailability[j]) {
                    tcAvailability[p] = false;
                    tcSample[p] = 0;
                    break;
                }
            }
        }
    }

    

    public int chooseMode(Household hh, int tourTypeIndex, short tourCategory, int chosenShrtWlk){

        // compute mode choice proportions and choose alternative
        long markTime = System.currentTimeMillis();
        Arrays.fill(mcSample, 1);
        Arrays.fill (mcAvailability, true);
        setMcODUtility ( hh, tourTypeIndex, tourCategory );
        // set transit modes to unavailable if a no walk access subzone was selected in DC.

        if ( chosenShrtWlk == 0 ) {
            mcSample[3] = 0;
            mcAvailability[3] = false;
            mcSample[4] = 0;
            mcAvailability[4] = false;
        }

        //this is the original by Jim
        mc[tourTypeIndex].updateLogitModel ( hh, mcAvailability, mcSample );
        mcTime += (System.currentTimeMillis()-markTime);

        return mc[tourTypeIndex].getChoiceResult();
    }

    public void clearTimes() {
        dcLogsumTime = 0l;
        dcTime = 0l;
        tcLogsumTime = 0l;
        tcTime = 0l;
        mcTime = 0l;
    }

    public void printTimes ( short tourTypeCategory ) {

        for (int i=1; i < 5; i++) {

            if (tourTypeCategory == i) {

                baseLogger.info ( "DTM Model Component Times for " + getTourCategoryLabel() + " tours:");

                baseLogger.info ( "total seconds processing dtm dc logsums = " + (float)dcLogsumTime/1000);
                baseLogger.info ( "total seconds processing dtm dest choice = " + (float)dcTime/1000);
                baseLogger.info ( "total seconds processing dtm tc logsums = " + (float)tcLogsumTime/1000);
                baseLogger.info ( "total seconds processing dtm tod choice = " + (float)tcTime/1000);
                baseLogger.info ( "total seconds processing dtm mode choice = " + (float)mcTime/1000);
                baseLogger.info ( "");

            }
        }
    }

    public static void clearMCODHashMap() {
        modalODUtilityMap = new HashMap(2097152);
    }


    public abstract void defineUECModelSheets (int tourType);

    public abstract void chooseDestTODAndMode(DecisionMakingUnit dmu);

    public abstract int[] chooseDestination(DecisionMakingUnit dmu, int tourTypeIndex);

    //an extended class must also implement its own version of chooseTimeOfDay()


}
