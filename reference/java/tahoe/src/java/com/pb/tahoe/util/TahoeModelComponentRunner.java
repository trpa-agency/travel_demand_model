package com.pb.tahoe.util;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.reports.ModelSummary;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerSynthesizer;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerArrayManager;
import com.pb.tahoe.ExternalWorkers.ExternalWorkersOT;
import com.pb.tahoe.visitor.*;
import com.pb.tahoe.visitor.structures.StayType;
import com.pb.tahoe.visitor.structures.VisitorType;
import com.pb.tahoe.auto_ownership.AutoOwnership;
import com.pb.tahoe.synpop.SyntheticPopulation;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.structures.PersonType;
import com.pb.tahoe.dest_time_mode.*;
import com.pb.tahoe.joint_tour.JointToursModel;
import com.pb.tahoe.individual_tour.IndividualNonMandatoryToursModel;
import com.pb.tahoe.stops.MandatoryStops;
import com.pb.tahoe.stops.JointStops;
import com.pb.tahoe.stops.NonMandatoryStops;
import com.pb.tahoe.stops.AtWorkStops;
import com.pb.tahoe.daily_activity_pattern.DAPModel;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Apr 10, 2007 - 9:33:00 AM
 */
public class TahoeModelComponentRunner {
    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(TahoeModelRunner.class);
    static boolean debug = false;

    private static boolean initialized = false;

    private static boolean multiComponentRun = false;

    private static ZonalDataManager zdm;
    private static HouseholdArrayManager ham;
    private static ExternalWorkerArrayManager ewam;
    private static PartyArrayManager pam;
    private static TahoeReports tr;
    private static boolean writeReports;
    private static DTMOutput dtmOutput;
    private static boolean hamSet = false;
    private static boolean ewamSet = false;
    private static boolean pamSet = false;

    private static TahoeModelComponentRunner instance = new TahoeModelComponentRunner();

    private TahoeModelComponentRunner() { }

    private void initializeTahoeModelComponentRunner() {
        zdm = ZonalDataManager.getInstance();
        ham = HouseholdArrayManager.getInstance();
        ewam = ExternalWorkerArrayManager.getInstance();
        pam = PartyArrayManager.getInstance();
        TODDataManager.getInstance();
        tr = new TahoeReports();
        writeReports = ResourceUtil.getBooleanProperty(rb, "write.reports",false);
        dtmOutput = new DTMOutput();
        initialized = true;
    }

    public static TahoeModelComponentRunner getInstance() {
        if (!initialized)
            instance.initializeTahoeModelComponentRunner();
        return instance;
    }

    private enum TahoeModelComponent {
        Synpop(TahoeModelLevel.Resident,false,false,false,false,false,"Runs the resident synthetic population model."),
        AutoOwnership(TahoeModelLevel.Resident,Synpop,false,false,false,false,false,"Runs the resident auto ownership population model."),
        DailyActivityPattern(TahoeModelLevel.Resident,AutoOwnership,false,false,false,false,false,"Runs the resident daily activity model."),
        MandatoryDTM(TahoeModelLevel.Resident,DailyActivityPattern,true,false,true,false,false,"Runs the resident mandatory DTM model."),
        JointTourGeneration(TahoeModelLevel.Resident,MandatoryDTM,true,false,true,false,false,"Runs the resident joint tour generation model."),
        JointDTM(TahoeModelLevel.Resident,JointTourGeneration,true,false,true,false,false,"Runs the resident joint DTM model."),
        NonMandatoryTourGeneration(TahoeModelLevel.Resident,JointDTM,true,false,true,false,false,"Runs the resident non-mandatory tour generation model."),
        NonMandatoryDTM(TahoeModelLevel.Resident,NonMandatoryTourGeneration,true,false,true,false,false,"Runs the resident non-mandatory DTM model."),
        AtWorkDTM(TahoeModelLevel.Resident,NonMandatoryDTM,true,false,true,false,false,"Runs the resident at-work DTM model."),
        MandatoryStops(TahoeModelLevel.Resident,AtWorkDTM,true,false,true,false,false,"Runs the resident mandatory stops model."),
        JointStops(TahoeModelLevel.Resident,MandatoryStops,true,false,true,false,false,"Runs the resident joint stops model."),
        NonMandatoryStops(TahoeModelLevel.Resident,JointStops,true,false,true,false,false,"Runs the resident mandatory stops model."),
        AtWorkStops(TahoeModelLevel.Resident,NonMandatoryStops,true,false,true,false,false,"Runs the resident at-work stops model."),
        ExternalWorkersSynpop(TahoeModelLevel.ExternalWorker,AtWorkStops,false,false,true,false,false,"Runs the external workers synthetic population model."),
        ExternalWorkersOT(TahoeModelLevel.ExternalWorker,ExternalWorkersSynpop,false,false,false,true,false,"Runs the external worker OT model."),
        OvernightVisitorSynpopAndPattern(TahoeModelLevel.Visitor,ExternalWorkersOT,false,true,false,false,false,"Runs the overnight visitor synthetic population and pattern models"),
        DayVisitorSynpopAndPattern(TahoeModelLevel.Visitor,OvernightVisitorSynpopAndPattern,false,true,false,false,true,"Runs the day visitor synthetic population and pattern models"),
        VisitorDTM(TahoeModelLevel.Visitor,DayVisitorSynpopAndPattern,false,true,false,false,true,"Runs the visitor DTM model"),
        VisitorStops(TahoeModelLevel.Visitor,VisitorDTM,false,true,false,false,true,"Runs the visitor stops model"),
        ThruVisitors(TahoeModelLevel.Visitor,VisitorStops,false,true,false,false,false,"Runs the visitor thru visitors model"),
        TripSynthesize(TahoeModelLevel.TripSynthesize,ThruVisitors,false,false,true,true,true,"Runs the trip synthesizer model");



        enum TahoeModelLevel {
            Resident("Runs the entire resident model."),
            ExternalWorker("Runs the entire external worker model."),
            Visitor("Runs the entire visitor model."),
            TripSynthesize(null);

            private String description;

            private TahoeModelLevel(String description) {
                this.description = description;
            }

            private String getDescription() {
                return description;
            }
        }

        private Method runMethod;
        private TahoeModelLevel modelLevel;
        TahoeModelComponent prevComponent = null;
        private boolean doPAM;
        private boolean doDOA;
        private boolean needsHAM;
        private boolean needsEWAM;
        private boolean needsPAM;
        private String description;


        private TahoeModelComponent(TahoeModelLevel modelLevel, boolean doDOA, boolean doPAM, boolean needsHAM, boolean needsEWAM, boolean needsPAM, String description)  {
            try {
                runMethod = instance.getClass().getDeclaredMethod("run" + this.toString());
                runMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            this.modelLevel = modelLevel;
            this.doDOA = doDOA;
            this.doPAM = doPAM;
            this.needsHAM = needsHAM;
            this.needsEWAM = needsEWAM;
            this.needsPAM = needsPAM;
            this.description = description;
        }

        private TahoeModelComponent(TahoeModelLevel modelLevel, TahoeModelComponent prevComponent, boolean doDOA, boolean doPAM, boolean needsHAM, boolean needsEWAM, boolean needsPAM, String description) {
            this(modelLevel,doDOA,doPAM,needsHAM,needsEWAM,needsPAM,description);
            this.prevComponent = prevComponent;
        }

        private boolean matchesLevel(TahoeModelLevel tml) {
            return this.modelLevel == tml;
        }

        private void runComponent() throws IllegalAccessException, InvocationTargetException {
            if (!hamSet)
                instance.setHAM(this);
            if (!ewamSet)
                instance.setEWAM(this);
            if (!pamSet)
                instance.setPAM(this);
            runMethod.invoke(instance);
            instance.setLastComponentRun(this);
            if (!multiComponentRun) {
                if (doDOA) {
                    instance.writeHAM(this);
                }
                if (doPAM) {
                    instance.writePAM(this);
                }
                instance.cleanReportsDirectory();
            }
        }

        private String getDescription() {
            return description;
        }
    }

    private void runModelLevelComponents(TahoeModelComponent.TahoeModelLevel tml) throws IllegalAccessException, InvocationTargetException {
        multiComponentRun = true;
        TahoeModelComponent lastComponent = null;
        for (TahoeModelComponent tmc : TahoeModelComponent.values()) {
            if (tmc.matchesLevel(tml)) {
                tmc.runComponent();
                lastComponent = tmc;
            }
        }
        if (lastComponent != null) {
            if (lastComponent.doDOA) {
                instance.writeHAM(lastComponent);
            }
            if (lastComponent.doPAM) {
                instance.writePAM(lastComponent);
            }
            instance.cleanReportsDirectory();
        }
    }


    private void runSynpop() {
        logger.info("Creating Synthetic Population ...");

        //Read in the PUMS hhs from selected PUMAs, put each household into
        //a PUMSHH array and create an array list of pums record numbers that correspond
        //to each hhsize/nWorkers/incomeCategory combination

        logger.info("\tReading PUMS data and sorting HHs by state, puma, size, income, workers");
        SyntheticPopulation synPop =new SyntheticPopulation();
        synPop.sortPUMSData();

        //Read in the seed numbers from a file that will give the
        //the seed numbers of households in each hhsize/nWorkers/incomeCategory combination
        //for every census tract in the study area.  The table will also be sorted into
        //NDimensionalMatrices.
        logger.info("\tReading in CTPP data and saving as seed matrices");
        synPop.create3DSeedMatrices();

        //Figure out the size, worker and income marginals for each TAZ
        // and use the seeds from above to do the table balancing methods that
        // will produce the number of households ecah zone has of the hhsize/nWorkers/incomeCategory combinations
        //Select as many PUMSHHs that match
        //the criteria as needed to populate the zone.
        logger.info("\tBuilding the Synthetic Population");
        synPop.buildPop();

        //Write out all hhs from all TAZs and write out the zonal target numbers.
        logger.info("\tWriting results to disk");
        synPop.writeResults();

        logger.info("Synthetic Population has been created");
    }

    private void runAutoOwnership() {
        logger.info("Starting Auto Ownership Module ...");
        SeededRandom.setSeed(2002);
        AutoOwnership ao = new AutoOwnership(rb);
        ao.runAutoOwnership();
        ao.writeAutoOwnershipResults();
        logger.info("Auto Ownership Module completed");
        deleteFilesFromProperties("synthetic.household.file");
    }

    private void runDailyActivityPattern() {
        logger.info("Choosing Daily Activity Patterns ...");
        SeededRandom.setSeed(2002);

        DataWriter writer = DataWriter.getInstance();
        String[] cmdargs = {PersonType.PRESCHOOLER, PersonType.PREDRIVER, PersonType.DRIVER, PersonType.FULL_TIME,PersonType.PART_TIME, PersonType.NON_WORKER};
        DAPModel model = new DAPModel(rb);

        for (String cmdarg : cmdargs) {
            String fieldInOutput = null;
            if(cmdarg.equals(PersonType.PRESCHOOLER)){
                fieldInOutput = DataWriter.PRESCHOOL_FIELD;
            }else if(cmdarg.equals(PersonType.PREDRIVER)){
                fieldInOutput = DataWriter.SCHOOLPRED_FIELD;
            }else if(cmdarg.equals(PersonType.DRIVER)){
                fieldInOutput = DataWriter.SCHOOLDRIV_FIELD;
            }else if(cmdarg.equals(PersonType.FULL_TIME)){
                fieldInOutput = DataWriter.WORKERS_F_FIELD;
            }else if(cmdarg.equals(PersonType.PART_TIME)){
                fieldInOutput = DataWriter.WORKERS_P_FIELD;
            }else if(cmdarg.equals(PersonType.NON_WORKER)){
                fieldInOutput = DataWriter.NONWORKERS_FIELD;
            }
            model.runDailyActivityPatternChoice(cmdarg, fieldInOutput);
            if(cmdarg.equals(PersonType.PRESCHOOLER)){
                model.writePatternChoicesToFile();
            }

        }
        model.writePatternChoicesToFile();
        writer.writeSynPopPTableDataSet(model.hhTable);

        logger.info("Daily Activity Patterns chosen");
    }

    private void runMandatoryDTM() {
        logger.info( "Running Destination, Time of Day and Mode Choice for Mandatory Tours...");
        SeededRandom.setSeed(2002);
        DTMModel mandatoryDTM = new MandatoryDTM(rb);
        mandatoryDTM.doWork(ham);
        mandatoryDTM.printTimes(TourType.MANDATORY_CATEGORY);

        //enter shadow price iterations
        int shadowPriceIteration = 0;
        ((MandatoryDTM) mandatoryDTM).setShadowPriceIteration(true);
        while ((zdm.checkShadowPriceNecessity(Float.valueOf(ResourceUtil.getProperty(rb,"shadow.price.epsilon")))) &&
                (shadowPriceIteration < ResourceUtil.getIntegerProperty(rb, "max.shadow.price.iterations"))) {
            logger.info("Shadow price iteration # " + (shadowPriceIteration + 1) + " for Mandatory DTM...");
            ZonalDataManager.clearStaticLogsumMatrices();
            ZonalDataManager.clearZonalWorkTrips();
            mandatoryDTM.clearTimes();
            ham.clearTODWindows();
            mandatoryDTM.doWork(ham);
            mandatoryDTM.printTimes(TourType.MANDATORY_CATEGORY);
            shadowPriceIteration++;
        }
        File outputFile = new File(rb.getString("mandatory_dtm.choice.output.file"));
        dtmOutput.writeMandatoryDTMChoiceResults(outputFile, ham.getHouseholds(), mandatoryDTM.everyNth);
        if(writeReports) {
            tr.mandReport();
        }
        DTMModel.clearMCODHashMap();
        logger.info("Mandatory DTM finished.");
    }

    private void runJointTourGeneration() {
        logger.info("Running the Joint Tour Generation Model ...");
        SeededRandom.setSeed(2002);
        JointToursModel jtm = new JointToursModel(rb ,ham.getHouseholds());
        jtm.runFrequencyModel();
        jtm.runCompositionModel();
        jtm.runParticipationModel();
        if (writeReports) {
            tr.jointTourReport();
        }
        logger.info("Joint Tour Generation Model finished");
    }

    private void runJointDTM() {
        logger.info( "Running Destination Choice for Joint Tours...");
        ZonalDataManager.clearStaticLogsumMatrices();
        SeededRandom.setSeed(2002);
        DTMModel jointDTM = new JointDTM(rb);
        jointDTM.doWork(ham);
        jointDTM.printTimes(TourType.JOINT_CATEGORY);
        File outputFile = new File(rb.getString("joint_dtm.choice.output.file"));
        dtmOutput.writeJointDTMChoiceResults(outputFile, ham.getHouseholds(), jointDTM.everyNth);
        if (writeReports) {
            tr.jointDTMReport();
        }
        DTMModel.clearMCODHashMap();
        logger.info("Joint DTM finished");
    }

    private void runNonMandatoryTourGeneration() {
        logger.info("Running the Individual Non-Mandatory Tour Generation Models...");
        ZonalDataManager.clearStaticLogsumMatrices();
        SeededRandom.setSeed(2002);
        IndividualNonMandatoryToursModel indiv = new IndividualNonMandatoryToursModel(rb ,ham.getHouseholds());
        indiv.runMaintenanceFrequency();
        indiv.runMaintenanceAllocation();
        indiv.runDiscretionaryWorkerFrequency();
        indiv.runDiscretionaryNonWorkerFrequency();
        indiv.runDiscretionaryChildFrequency();
        indiv.runAtWorkFrequency();
        if (writeReports) {
            tr.indTourReport();
        }
        logger.info("Individual Non-mand Tour Generation Models finished");
    }

    private void runNonMandatoryDTM() {
        logger.info( "Running Destination, Time of Day and Mode Choice for Individual Non-mandatory Tours");
        ZonalDataManager.clearStaticLogsumMatrices();
        SeededRandom.setSeed(2002);
        DTMModel nonMandDTM = new NonMandatoryDTM(rb);
        nonMandDTM.doWork(ham);
        nonMandDTM.printTimes(TourType.NON_MANDATORY_CATEGORY);
        File outputFile = new File(rb.getString("non-mandatory_dtm.choice.output.file"));
        dtmOutput.writeIndivNonMandDTMChoiceResults(outputFile, ham.getHouseholds(), nonMandDTM.everyNth);
        if (writeReports) {
            tr.indDTMReport();
        }
        DTMModel.clearMCODHashMap();
        logger.info("Indiv Non-mandatory DTM is finished");
    }

    private void runAtWorkDTM() {
        logger.info( "Running Destination, Time of Day and Mode Choice for At-Work Tours");
        ZonalDataManager.clearStaticLogsumMatrices();
        SeededRandom.setSeed(2002);
        DTMModel atWorkDTM = new AtWorkDTM(rb);
        atWorkDTM.doWork(ham);
        atWorkDTM.printTimes(TourType.AT_WORK_CATEGORY);
        File outputFile = new File(rb.getString("at-work_dtm.choice.output.file"));
        dtmOutput.writeAtWorkDTMChoiceResults(outputFile, ham.getHouseholds(), atWorkDTM.everyNth);
        if (writeReports) {
            tr.indAtWorkReport();
        }
        DTMModel.clearMCODHashMap();
        logger.info("At-Work DTM finished");
        deleteFilesFromProperties("auto.ownership.output.file");
    }

    private void runMandatoryStops() {
        logger.info( "Running Stop Frequency, Location and Mode Choice for Mandatory Tours");
        SeededRandom.setSeed(2002);
        MandatoryStops manStops = new MandatoryStops(rb, ham);
        manStops.doSfcSlcWork();
        manStops.doSmcWork();
        manStops.printTimes(TourType.MANDATORY_CATEGORY);
        if (writeReports) {
            tr.stopsReport("mand");
        }
        logger.info("Mandatory Stops Model is finished");
    }

    private void runJointStops() {
        logger.info( "Running Stop Frequency, Location and Mode Choice for Joint Tours");
        SeededRandom.setSeed(2002);
        JointStops jointStops = new JointStops(rb, ham);
        jointStops.doSfcSlcWork();
        jointStops.doSmcWork();
        jointStops.printTimes(TourType.JOINT_CATEGORY);
        if (writeReports) {
            tr.stopsReport("joint");
        }
        logger.info("Joint Stops Model is finished");
    }

    private void runNonMandatoryStops() {
        logger.info( "Running Stop Frequency, Location and Mode Choice for Individual Non-Mandatory Tours");
        SeededRandom.setSeed(2002);
        NonMandatoryStops nonManStops = new NonMandatoryStops(rb, ham);
        nonManStops.doSfcSlcWork();
        nonManStops.doSmcWork();
        nonManStops.printTimes(TourType.NON_MANDATORY_CATEGORY);
        if (writeReports) {
            tr.stopsReport("nonmand");
        }
        logger.info("NonMandatory Stops Model is finished");
    }

    private void runAtWorkStops() {
        logger.info( "Running Stop Frequency, Location and Mode Choice for AtWork Tours");
        SeededRandom.setSeed(2002);
        AtWorkStops atWorkStops = new AtWorkStops(rb, ham);
        atWorkStops.doSfcSlcWork();
        atWorkStops.doSmcWork();
        atWorkStops.printTimes(TourType.AT_WORK_CATEGORY);
        if (writeReports) {
            tr.stopsReport("atwork");
        }
        logger.info("At Work Stops Model is finished");
    }

    private void runExternalWorkersSynpop() {
        logger.info("Synthesizing external worker population");
        SeededRandom.setSeed(2002);
        ExternalWorkerSynthesizer ews = new ExternalWorkerSynthesizer(ham);
        ewam.createExternalWorkerArray(ews.buildExternalWorkersPop());
        ewam.writeExternalWorkerData("external.worker.synpop.file");
        ewamSet = true;
    }

    private void runExternalWorkersOT() {
        //run external worker ot model
        //first time around to set things up
        logger.info("Starting external worker ot model");
        SeededRandom.setSeed(2002);
        ExternalWorkersOT otModel = new ExternalWorkersOT(debug);
        otModel.runExternalWorkerOCModel(ewam);

        logger.info("Entering shadow price iterations for external worker ot model...");
        int externalWorkerShadowPriceIteration = 0;
        otModel.setShadowPriceIteration(true);
        int maxIterations = ResourceUtil.getIntegerProperty(rb,"max.shadow.price.iterations");
        while (maxIterations > externalWorkerShadowPriceIteration &&
                zdm.checkExternalWorkerShadowPriceNecessity((float) ResourceUtil.getDoubleProperty(rb,"external.worker.shadow.price.epsilon"))) {
            logger.info("External worker shadow price iteration # " + (externalWorkerShadowPriceIteration + 1));
            //SeededRandom.setSeed(2002);
            zdm.clearExternalWorkTours();
            otModel.runExternalWorkerOCModel(ewam);
            externalWorkerShadowPriceIteration++;
        }
        otModel.runExternalWorkerTODModel(ewam);
        ewam.writeExternalWorkerData("external.worker.ot.results.file");
        //cleanup
        deleteFilesFromProperties("external.worker.synpop.file");
    }

    private void runOvernightVisitorSynpopAndPattern() {
        ZonalDataManager.clearStaticLogsumMatrices();
        //Set the random seed so that population is reproducable
        SeededRandom.setSeed(2002);
        logger.info("Generating overnight visitor units to fill.");
        HashMap<Integer, HashMap<StayType,Integer>> unitsToFill =
                OvernightVisitorUnitsToFill.generateUnitsToFill("overnight.visitor.occupancy.data.file");
        logger.info("Building overnight visitor synthetic population.");
        OvernightVisitorSynpop ovsPop = new OvernightVisitorSynpop(unitsToFill, debug);
        pam.createPartyArray(ovsPop.buildOvernightVisitorSynPop());
        //run pattern model
        SeededRandom.setSeed(2002);
        VisitorPatternModel ovpm = new VisitorPatternModel(VisitorType.OVERNIGHT, debug);
        ovpm.runVisitorPatternModel(pam);
        pamSet = true;
    }

    private void runDayVisitorSynpopAndPattern() {
        SeededRandom.setSeed(2002);
        pam.setAllModelsNotDone();
        logger.info("Building day visitor synthetic population.");
        DayVisitorSynpop dvsPop = new DayVisitorSynpop(pam.partyCount,debug);
        dvsPop.generateSynpop();
        //do thru synpop
        pam.createPartyArray(dvsPop.getThruSynpop());
        pam.writePartyData("thru.visitor.synpop.results.file");
        //replace party arry with non thru day visitors
        pam.createPartyArray(dvsPop.getNonThruSynpop());
        //pam.writePartyData("day.visitor.synpop.results.file");
        //run pattern model
        SeededRandom.setSeed(2002);
        VisitorPatternModel dvpm = new VisitorPatternModel(VisitorType.DAY, debug);
        dvpm.runVisitorPatternModel(pam);

        //rebuild visitor population
        pam.createPartyArray(PartyArrayManager.mergeVisitorPopulations("overnight.pattern.results.file","day.pattern.results.file"));
    }

    private void runVisitorDTM() {
        SeededRandom.setSeed(2002);
        VisitorDTM ovdtm = new VisitorDTM(rb);
        ovdtm.doWork(pam);
        ovdtm.printTimes();
        //cleanup
        //deleteFilesFromProperties("visitor.synpop.dtm.results.file");
        DTMModel.clearMCODHashMap();
    }

    private void runVisitorStops() {
        SeededRandom.setSeed(2002);
        VisitorStopsModel ovsm = new VisitorStopsModel(rb);
        ovsm.doWork(pam);
        ovsm.printTimes();
    }

    private void runThruVisitors() {
        pam.setAllModelsNotDone();
        pam.createPartyArray("thru.visitor.synpop.results.file");
        SeededRandom.setSeed(2002);
        DayVisitorThruDT dvtpdt = new DayVisitorThruDT();
        dvtpdt.doWork(pam);

        //merge all day visitor populations into pam
        pam.createPartyArray(PartyArrayManager.mergeVisitorPopulations("visitor.synpop.stops.results.file","visitor.synpop.full.results.file"));
        //pam.writePartyData("visitor.synpop.full.and.finished.file");
        //reports
        pam.partyArrayToReportsFile("visitor.reports.data.file");
        if (writeReports) {
            tr.visitorReport();
        }
        //cleanup
        //deleteFilesFromProperties("overnight.pattern.results.file","day.pattern.results.file","thru.visitor.synpop.results.file","visitor.synpop.stops.results.file","visitor.synpop.full.results.file");
    }

    private void runTripSynthesize() {
        TripSynthesizer ts = new TripSynthesizer();
        logger.info("Synthesizing resident trips");
        ts.synthesizeResidentTrips(ham);
        logger.info("Synthesizing external worker trips");
        ts.synthesizeExternalWorkerTrips(ewam);
        logger.info("Synthesizing visitor trips");
        ts.synthesizeVisitorTrips(pam);
        ts.writeModeSkimMatrices(ResourceUtil.getProperty(rb,"trip.output.directory"),"Trips_");
        ts.writeTripFile();
        //generate the final model summary - correct place to do this because all of the array managers must be up and running
        ModelSummary.generateModelSummary();
    }

    private void runFullModel() throws IllegalAccessException, InvocationTargetException {
   	
    	for (TahoeModelComponent.TahoeModelLevel tml : TahoeModelComponent.TahoeModelLevel.values()) {
            runModelLevelComponents(tml);
        }
        
    }

    private void setLastComponentRun(TahoeModelComponent tmc) {
        File lastComponentRun = new File(rb.getString("last.component.run.file.path") + tmc.toString() + ".last");
        try {
            lastComponentRun.createNewFile();
            if (tmc.prevComponent != null) {
                File previousComponentRun = new File(rb.getString("last.component.run.file.path") + tmc.prevComponent.toString() + ".last");
                previousComponentRun.delete();
            }
        } catch (IOException e) {
            logger.warn("Failed writing last component ran file!");
        }
    }

    private void modelOutOfOrderError() {
        String[] lastFile = getFileNames(rb.getString("last.component.run.file.path"),"last");
        String lastComponent = "None";
        if (lastFile.length != 0)
            lastComponent = lastFile[0].replaceAll("[.]last","");
        String nextComponent = TahoeModelComponent.Synpop.toString();
        if (!lastComponent.equals("None")) {
            TahoeModelComponent[] tmc = TahoeModelComponent.values();
            for (int i = 0; i < tmc.length; i++) {
                if (tmc[i].toString().equals(lastComponent)) {
                    if (i == tmc.length)
                        nextComponent = "None";
                    else
                        nextComponent = tmc[i+1].toString();
                    break;
                }
            }
        }
        logger.fatal("\n\tCannot run model out of order!\n\t\tLast component run: " + lastComponent + "\n\t\tNext component to run: " + nextComponent);
        System.exit(0);
    }

    private void setHAM(TahoeModelComponent tmc) {
        if (tmc.needsHAM) {
            logger.info("Creating Household Array ...");
            if (tmc == TahoeModelComponent.MandatoryDTM) {
                ham.createBigHHArray();
            } else if (tmc == TahoeModelComponent.TripSynthesize) {
                ham.createBigHHArrayFromDiskObject("_after" + TahoeModelComponent.AtWorkStops.toString() + ".doa");
            } else {
                ham.createBigHHArrayFromDiskObject("_after" + tmc.prevComponent.toString() + ".doa");
//                //allow a model component to be re-run
//                if (ham.bigHHArray == null)
//                    ham.createBigHHArrayFromDiskObject("_after" + tmc.toString() + ".doa");
            }
            if (ham.bigHHArray == null) {
                modelOutOfOrderError();
            }
            logger.info("Household Array created");
            hamSet = true;
        }
    }

    private void writeHAM(TahoeModelComponent tmc) {
        if (!tmc.doDOA) return;
        String diskArrayFileName = rb.getString("DiskObjectArrayInput.file");
        //Delete all doa objects
        logger.info("Writing Household Array to file...");
        //deleteFiles(diskArrayFileName.substring(0,diskArrayFileName.lastIndexOf("/") + 1),"doa");
        ham.writeDiskObjectArray(diskArrayFileName + "_after" + tmc.toString() + ".doa");
        logger.info("...finished.");
    }

    private void setEWAM(TahoeModelComponent tmc) {
        if (tmc.needsEWAM) {
            logger.info("Creating External Worker Array ...");
            if (tmc == TahoeModelComponent.ExternalWorkersOT)
                ewam.createExternalWorkerArray("external.worker.synpop.file");
            else
                ewam.createExternalWorkerArray("external.worker.ot.results.file");
            if (ewam.workers == null) {
                modelOutOfOrderError();
            }
            logger.info("Extermal Worker Array created");
            ewamSet = true;
        }
    }

    private void setPAM(TahoeModelComponent tmc) {
        if (tmc.needsPAM) {
            logger.info("Creating Visitor Party Array ...");
            String pamFileName = rb.getString("latest.party.array.manager.file");
            File partyArrayData = new File(pamFileName + "_after" + tmc.prevComponent.toString() + ".pam");
            File tempPartyArrayData = new File(pamFileName);
            if (partyArrayData.exists()) {
                partyArrayData.renameTo(tempPartyArrayData);
                pam.createPartyArray("latest.party.array.manager.file");
                tempPartyArrayData.renameTo(partyArrayData);
            }
            if (pam.parties == null) {
                modelOutOfOrderError();
            }
            logger.info("Visitor Party Array created");
            pamSet = true;
        }
    }

    private void writePAM(TahoeModelComponent tmc) {
        String pamFileName = rb.getString("latest.party.array.manager.file");
        //Delete all pam objects
        deleteFiles(pamFileName.substring(0,pamFileName.lastIndexOf("/") + 1),"pam");
        pam.writePartyData("latest.party.array.manager.file");
        File partyArrayData = new File(pamFileName);
        partyArrayData.renameTo(new File(pamFileName + "_after" + tmc.toString() + ".pam"));
    }

    private void deleteFiles(String directory, String extension) {
        File dir = new File(directory);
        String[] fileList = dir.list(new FileFilter(extension));
        if (fileList.length == 0) return;
        for (String file : fileList) {
            File doaFile = new File(directory + file);
            doaFile.delete();
        }
    }

    private String[] getFileNames(String directory, String extension) {
        File dir = new File(directory);
        return dir.list(new FileFilter(extension));
    }

    private class FileFilter implements FilenameFilter {
        private String fileType;
        private FileFilter(String fileType) {
            this.fileType = fileType;
        }
        public boolean accept(File dir, String name) {
            return name.endsWith("." + fileType);
        }
    }

    private void deleteFilesFromProperties(String ... properties) {
        for (String property : properties) {
            File file = new File(rb.getString(property));
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void cleanReportsDirectory() {
        Set<String> temporaryReports = new HashSet<String>();
        temporaryReports.add("mcOtaz");
        temporaryReports.add("mcDtaz");
        temporaryReports.add("dcDist");
        temporaryReports.add("dcCounty");
        temporaryReports.add("dcExt");
        temporaryReports.add("todDep");
        temporaryReports.add("todArr");
        temporaryReports.add("todDur");
        temporaryReports.add("jtComp");
        temporaryReports.add("jtPart");
        temporaryReports.add("jtFreq");
        temporaryReports.add("indMaintFreq");
        temporaryReports.add("indMaintPart");
        temporaryReports.add("indAWFreq");
        temporaryReports.add("indDiscWFreq");
        temporaryReports.add("indDiscNFreq");
        temporaryReports.add("indDiscCFreq");
        temporaryReports.add("mandStopFreq");
        temporaryReports.add("mandOutStopLoc");
        temporaryReports.add("mandInStopLoc");
        temporaryReports.add("mandOutStopDist");
        temporaryReports.add("mandInStopDist");
        temporaryReports.add("jointStopFreq");
        temporaryReports.add("jointOutStopLoc");
        temporaryReports.add("jointInStopLoc");
        temporaryReports.add("jointOutStopDist");
        temporaryReports.add("jointInStopDist");
        temporaryReports.add("nonmandStopFreq");
        temporaryReports.add("nonmandOutStopLoc");
        temporaryReports.add("nonmandInStopLoc");
        temporaryReports.add("nonmandOutStopDist");
        temporaryReports.add("nonmandInStopDist");
        temporaryReports.add("atworkStopFreq");
        temporaryReports.add("atworkOutStopDist");
        temporaryReports.add("atworkInStopDist");
        deleteFilesFromProperties(temporaryReports.toArray(new String[temporaryReports.size()]));
    }

    private static void logException(Exception e) {
        StringBuffer log = new StringBuffer(e.toString());
        for (StackTraceElement i : e.getStackTrace()) {
            log.append("\n\tat ").append(i);
        }
        logger.error(log.toString());
    }

    private void printUsage() {
        System.out.println("Tahoe Model Component Runner - Runs a Tahoe activity based model component(s).");
        System.out.println("Usage: java TahoeModelComponentRunner.java {component}");
        System.out.println("Available component options:");
        for (TahoeModelComponent tmc : TahoeModelComponent.values()) {
            if (tmc.getDescription() != null)
                System.out.println("  " + tmc.toString() + " - " + tmc.getDescription());
        }
        for (TahoeModelComponent.TahoeModelLevel tml : TahoeModelComponent.TahoeModelLevel.values()) {
            if (tml.getDescription() != null)
                System.out.println("  " + tml.toString() + " - " + tml.getDescription());
        }
        System.out.println("  TahoeModel - Run the entire Tahoe model.");
    }

    public static void main(String[] args) {
        //only one argument
        if (args.length != 1) {
            logger.fatal("There must be one and only one command line argument!\n");
            instance.printUsage();
            return;
        }

        //find argument match
        TahoeModelComponent tmc = null;
        TahoeModelComponent.TahoeModelLevel tml = null;
        for (TahoeModelComponent tahoeMC : TahoeModelComponent.values()) {
            if (tahoeMC.toString().equals(args[0])) {
                tmc = tahoeMC;
                break;
            }
        }
        if (tmc == null) {
            for (TahoeModelComponent.TahoeModelLevel tahoeML : TahoeModelComponent.TahoeModelLevel.values()) {
                if (tahoeML.toString().equals(args[0])) {
                    tml = tahoeML;
                    break;
                }
            }
        }

        //only allowed argument
        if (tmc == null && tml == null) {
            if (!args[0].equals("TahoeModel")) {
                logger.fatal("Invalid target: " + args[0] + "\nSee available target list below.\n");
                instance.printUsage();
                return;
            }
        }

        //run component(s)
        instance.initializeTahoeModelComponentRunner();
        try {
            if (tmc != null) {
                tmc.runComponent();
            } else if (tml != null) {
                instance.runModelLevelComponents(tml);
            } else {
                instance.runFullModel();
            }
        } catch (IllegalAccessException e) {
            logException(e);
        } catch (InvocationTargetException e) {
            logException(e);
            logException((Exception) e.getTargetException());
        } catch (Exception e) {
            logger.warn("Unexpected/Unkown exception thrown!");
            logException(e);
        }

    }
}
