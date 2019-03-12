/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tahoe.util;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerSynthesizer;
import com.pb.tahoe.ExternalWorkers.ExternalWorkerArrayManager;
import com.pb.tahoe.ExternalWorkers.ExternalWorkersOT;
import com.pb.tahoe.visitor.*;
import com.pb.tahoe.visitor.structures.StayType;
import com.pb.tahoe.visitor.structures.VisitorType;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.HashMap;

/**
 * TahoeModelRunner is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  May 12, 2006
 */
public class TahoeModelRunner {
    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(TahoeModelRunner.class);

    static boolean debug = false;

    public static void main(String[] args) {
        //Create a Tahoe Reports instance (used if writeReports is true)
        TahoeReports tr = new TahoeReports();
        boolean writeReports = ResourceUtil.getBooleanProperty(rb, "write.reports",false);

        //Create a ZonalDataManager that does some file processing, sets
        //walk percents in the zone and calculates size variables that are
        //used later on in the model stream.
        logger.info("Initializing Zonal Data ...");
        ZonalDataManager zdm = ZonalDataManager.getInstance();
        logger.info("Zonal Data has been initialized");

//        /************************************************************************************************/
//        //Resident model
//        //Produces the SynPopH file, that lists all HHs in the region
//        logger.info("Creating Synthetic Population ...");
//        SynPopTest.runSynPop();
//        logger.info("Synthetic Population has been created");
//
//        //Chooses the number of cars each HH owns and appends to the SynPopH file
//        //New file is called SynPopPlusAutoOwnership
//        logger.info("Starting Auto Ownership Module ...");
//        SeededRandom.setSeed(2002);
//        AutoOwnership ao = new AutoOwnership(rb);
//        ao.runAutoOwnership();
//        ao.writeAutoOwnershipResults();
//        logger.info("Auto Ownership Module completed");
//
//        //Runs the Daily Activity Pattern for each person type and appends the pattern to the SynPopH file.
//        //New file is called SynPopAOPlusDAP
//        //This will also write out the SynPopP file.
//        logger.info("Choosing Daily Activity Patterns ...");
//        SeededRandom.setSeed(2002);
//        DAPTest.runPatternModelForAllPersonTypes(rb);
//        logger.info("Daily Activity Patterns chosen");
//
//        //Create a Time-of-Day data manager, that is used for the DTM models
//        TODDataManager.getInstance();
//
//
//        //Read back in the SynPopP and SynPopH file and create a HH array
//        logger.info("Creating Household Array ...");
//        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
//        ham.createBigHHArray();
//        logger.info("Household Array created");
//
//        //Runs the Mandatory Destination, Time-Of-Day and Mode Choice models
//        logger.info( "Running Destination, Time of Day and Mode Choice for Mandatory Tours...");
//        SeededRandom.setSeed(2002);
//        DTMModel mandatoryDTM = new MandatoryDTM(rb);
//        mandatoryDTM.doWork(ham);
//        mandatoryDTM.printTimes(TourType.MANDATORY_CATEGORY);
//
//        //enter shadow price iterations
//        int shadowPriceIteration = 0;
//        ((MandatoryDTM) mandatoryDTM).setShadowPriceIteration(true);
//        while ((zdm.checkShadowPriceNecessity(Float.valueOf(ResourceUtil.getProperty(rb,"shadow.price.epsilon")))) &&
//                (shadowPriceIteration < ResourceUtil.getIntegerProperty(rb, "max.shadow.price.iterations"))) {
//            logger.info("Shadow price iteration # " + (shadowPriceIteration + 1) + " for Mandatory DTM...");
//            SeededRandom.setSeed(2002);
//            ZonalDataManager.clearZonalWorkTrips();
//            mandatoryDTM.doWork(ham);
//            mandatoryDTM.printTimes(TourType.MANDATORY_CATEGORY);
//            shadowPriceIteration++;
//        }
//        File outputFile = new File(rb.getString("mandatory_dtm.choice.output.file"));
//        DTMOutput dtmOutput = new DTMOutput();
//        dtmOutput.writeMandatoryDTMChoiceResults(outputFile, ham.getHouseholds(), mandatoryDTM.everyNth);
//        if(writeReports) {
//            tr.mandReport();
//        }
//        logger.info("Mandatory DTM finished.");
//
//        //Runs the Joint Tour Generation Models
//        logger.info("Running the Joint Tour Generation Model ...");
//        SeededRandom.setSeed(2002);
//        JointToursModel jtm = new JointToursModel(rb ,ham.getHouseholds());
//        jtm.runFrequencyModel();
//        jtm.runCompositionModel();
//        jtm.runParticipationModel();
//        if (writeReports) {
//            tr.jointTourReport();
//        }
//        logger.info("Joint Tour Generation Model finished");
//
//        //Runs the Joint Destination, Time-Of-Day and Mode Choice models
//        logger.info( "Running Destination Choice for Joint Tours...");
//        ZonalDataManager.clearStaticLogsumMatrices();
//        SeededRandom.setSeed(2002);
//        DTMModel jointDTM = new JointDTM(rb);
//        jointDTM.doWork(ham);
//        jointDTM.printTimes(TourType.JOINT_CATEGORY);
//        outputFile = new File(rb.getString("joint_dtm.choice.output.file"));
//        dtmOutput.writeJointDTMChoiceResults(outputFile, ham.getHouseholds(), jointDTM.everyNth);
//        if (writeReports) {
//            tr.jointDTMReport();
//        }
//        logger.info("Joint DTM finished");
//
//        //Runs the Individual Non-mandatory Tour Generation Models
//        logger.info("Running the Individual Non-Mandatory Tour Generation Models...");
//        ZonalDataManager.clearStaticLogsumMatrices();
//        SeededRandom.setSeed(2002);
//        IndividualNonMandatoryToursModel indiv = new IndividualNonMandatoryToursModel(rb ,ham.getHouseholds());
//        indiv.runMaintenanceFrequency();
//        indiv.runMaintenanceAllocation();
//        indiv.runDiscretionaryWorkerFrequency();
//        indiv.runDiscretionaryNonWorkerFrequency();
//        indiv.runDiscretionaryChildFrequency();
//        indiv.runAtWorkFrequency();
//        if (writeReports) {
//            tr.indTourReport();
//        }
//        logger.info("Individual Non-mand Tour Generation Models finished");
//
//
//        //Runs the Non-mandatory Destination, Time-Of-Day and Mode Choice models
//        logger.info( "Running Destination, Time of Day and Mode Choice for Individual Non-mandatory Tours");
//        ZonalDataManager.clearStaticLogsumMatrices();
//        SeededRandom.setSeed(2002);
//        DTMModel nonMandDTM = new NonMandatoryDTM(rb);
//        nonMandDTM.doWork(ham);
//        nonMandDTM.printTimes(TourType.NON_MANDATORY_CATEGORY);
//        outputFile = new File(rb.getString("non-mandatory_dtm.choice.output.file"));
//        dtmOutput.writeIndivNonMandDTMChoiceResults(outputFile, ham.getHouseholds(), nonMandDTM.everyNth);
//        if (writeReports) {
//            tr.indDTMReport();
//        }
//        logger.info("Indiv Non-mandatory DTM is finished");
//
//
//        //Runs the At-work Destination, Time-Of-Day and Mode Choice models
//        logger.info( "Running Destination, Time of Day and Mode Choice for At-Work Tours");
//        ZonalDataManager.clearStaticLogsumMatrices();
//        SeededRandom.setSeed(2002);
//        DTMModel atWorkDTM = new AtWorkDTM(rb);
//        atWorkDTM.doWork(ham);
//        atWorkDTM.printTimes(TourType.AT_WORK_CATEGORY);
//        outputFile = new File(rb.getString("at-work_dtm.choice.output.file"));
//        dtmOutput.writeAtWorkDTMChoiceResults(outputFile, ham.getHouseholds(), atWorkDTM.everyNth);
//        if (writeReports) {
//            tr.indAtWorkReport();
//        }
//        logger.info("At-Work DTM finished");
//        if (ResourceUtil.getBooleanProperty(rb, "write.disk.object.arrays", false)) {
//            String diskArrayFileName = rb.getString("DiskObjectArrayInput.file");
//            ham.writeDiskObjectArray(diskArrayFileName + "_afterAtWorkDTM.doa");
//        }
//
//        logger.info( "Running Stop Frequency, Location and Mode Choice for Mandatory Tours");
//        SeededRandom.setSeed(2002);
//        MandatoryStops manStops = new MandatoryStops(rb, ham);
//        manStops.doSfcSlcWork();
//        manStops.doSmcWork();
//        manStops.printTimes(TourType.MANDATORY_CATEGORY);
//        if (writeReports) {
//            tr.stopsReport("mand");
//        }
//        logger.info("Mandatory Stops Model is finished");
//
//
//        logger.info( "Running Stop Frequency, Location and Mode Choice for Joint Tours");
//        SeededRandom.setSeed(2002);
//        JointStops jointStops = new JointStops(rb, ham);
//        jointStops.doSfcSlcWork();
//        jointStops.doSmcWork();
//        jointStops.printTimes(TourType.JOINT_CATEGORY);
//        if (writeReports) {
//            tr.stopsReport("joint");
//        }
//        logger.info("Joint Stops Model is finished");
//
//
//        logger.info( "Running Stop Frequency, Location and Mode Choice for Individual Non-Mandatory Tours");
//        SeededRandom.setSeed(2002);
//        NonMandatoryStops nonManStops = new NonMandatoryStops(rb, ham);
//        nonManStops.doSfcSlcWork();
//        nonManStops.doSmcWork();
//        nonManStops.printTimes(TourType.NON_MANDATORY_CATEGORY);
//        if (writeReports) {
//            tr.stopsReport("nonmand");
//        }
//        logger.info("NonMandatory Stops Model is finished");
//
//
//        logger.info( "Running Stop Frequency, Location and Mode Choice for AtWork Tours");
//        SeededRandom.setSeed(2002);
//        AtWorkStops atWorkStops = new AtWorkStops(rb, ham);
//        atWorkStops.doSfcSlcWork();
//        atWorkStops.doSmcWork();
//        atWorkStops.printTimes(TourType.AT_WORK_CATEGORY);
//        if (writeReports) {
//            tr.stopsReport("atwork");
//        }
//        logger.info("At Work Stops Model is finished");
//
//        if (ResourceUtil.getBooleanProperty(rb, "write.disk.object.arrays", false)) {
//            String diskArrayFileName = rb.getString("DiskObjectArrayInput.file");
//            ham.writeDiskObjectArray(diskArrayFileName + "_afterAtWorkStops.doa");
//        }
//        /************************************************************************************************/

        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterAtWorkStops.doa");

        /************************************************************************************************/
        //External worker model
        logger.info("Synthesizing external worker population");
        SeededRandom.setSeed(2002);
        ExternalWorkerSynthesizer ews = new ExternalWorkerSynthesizer(ham);
        ExternalWorkerArrayManager ewam = ExternalWorkerArrayManager.getInstance();
        ewam.createExternalWorkerArray(ews.buildExternalWorkersPop());
        ewam.writeExternalWorkerData("external.worker.synpop.file");

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
            SeededRandom.setSeed(2002);
            zdm.clearExternalWorkTours();
            otModel.runExternalWorkerOCModel(ewam);
            externalWorkerShadowPriceIteration++;
        }
        //SeededRandom.setSeed(2002);
        otModel.runExternalWorkerTODModel(ewam);
        ewam.writeExternalWorkerData("external.worker.ot.results.file");
        /************************************************************************************************/


        /************************************************************************************************/
        //Visitor model
        //create instances of partyArrayManager
        PartyArrayManager pam = PartyArrayManager.getInstance();
        ZonalDataManager.clearStaticLogsumMatrices();
        //Set the random seed so that population is reproducable
        SeededRandom.setSeed(2002);
        logger.info("Generating overnight visitor units to fill.");
        HashMap<Integer,HashMap<StayType,Integer>> unitsToFill =
                OvernightVisitorUnitsToFill.generateUnitsToFill("overnight.visitor.occupancy.data.file");
        logger.info("Building overnight visitor synthetic population.");
        OvernightVisitorSynpop ovsPop = new OvernightVisitorSynpop(unitsToFill, debug);
        pam.createPartyArray(ovsPop.buildOvernightVisitorSynPop());
        //run pattern model
        SeededRandom.setSeed(2002);
        VisitorPatternModel ovpm = new VisitorPatternModel(VisitorType.OVERNIGHT, debug);
        ovpm.runVisitorPatternModel(pam);

        //generate day visitor synthetic population
        SeededRandom.setSeed(2002);
        logger.info("Building day visitor synthetic population.");
        DayVisitorSynpop dvsPop = new DayVisitorSynpop(pam.partyCount,debug);
        dvsPop.generateSynpop();
        //replace party arry with non thru day visitors
        pam.createPartyArray(dvsPop.getNonThruSynpop());
        pam.writePartyData("day.visitor.synpop.results.file");
        //run pattern model
        SeededRandom.setSeed(2002);
        VisitorPatternModel dvpm = new VisitorPatternModel(VisitorType.DAY, debug);
        dvpm.runVisitorPatternModel(pam);

        //rebuild visitor population
        pam.createPartyArray(PartyArrayManager.mergeVisitorPopulations("overnight.pattern.results.file","day.pattern.results.file"));

        //Run dtm model
        SeededRandom.setSeed(2002);
        VisitorDTM ovdtm = new VisitorDTM(rb);
        ovdtm.doWork(pam);
        ovdtm.printTimes();

        //Run stops model
        //pam.createPartyArray("overnight.synpop.dtm.results.file");
        SeededRandom.setSeed(2002);
        VisitorStopsModel ovsm = new VisitorStopsModel(rb);
        ovsm.doWork(pam);
        ovsm.printTimes();

        //replace pam with day visitor thru trip data
        pam.setAllModelsNotDone();
        pam.createPartyArray(dvsPop.getThruSynpop());
        pam.writePartyData("visitor.synpop.results.file");
        SeededRandom.setSeed(2002);
        DayVisitorThruDT dvtpdt = new DayVisitorThruDT();
        dvtpdt.doWork(pam);

        //merge all day visitor populations into pam
        pam.createPartyArray(PartyArrayManager.mergeVisitorPopulations("visitor.synpop.stops.results.file","visitor.synpop.full.results.file"));
        pam.writePartyData("visitor.synpop.full.and.finished.file");
        pam.partyArrayToReportsFile("visitor.reports.data.file");
        if (writeReports) {  
            tr.visitorReport();
        }
        /************************************************************************************************/
//        PartyArrayManager pam = PartyArrayManager.getInstance();
//        pam.createPartyArray("visitor.synpop.full.and.finished.file");
//        pam.partyArrayToReportsFile("visitor.reports.data.file");

        /************************************************************************************************/
        //Synthesize trip matrices
//        TripSynthesize ts = new TripSynthesize();
//        logger.info("Synthesizing resident trips");
//        ts.synthesizeResidentTrips(ham);
//        logger.info("Synthesizing external worker trips");
//        ts.synthesizeExternalWorkerTrips(ewam);
//        logger.info("Synthesizing visitor trips");
//        ts.synthesizeVisitorTrips(pam);
//        ts.writeModeSkimMatrices(ResourceUtil.getProperty(rb,"trip.output.directory"),"Trips_");
        /************************************************************************************************/
    }
}
