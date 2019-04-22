package com.pb.tahoe.ExternalWorkers;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.util.TripSynthesizer;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Mar 7, 2007 - 11:51:21 AM
 */
public class ExternalWorkerModelRunner {

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(ExternalWorkerModelRunner.class);

    static boolean debug = false;

    public static void runExternalWorkerModel() {
        //load up a zonal data manager
        ZonalDataManager zdm = ZonalDataManager.getInstance();

        //load up a household array
        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterAtWorkStops.doa");

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
        SeededRandom.setSeed(2002);
        otModel.runExternalWorkerTODModel(ewam);
        ewam.writeExternalWorkerData("external.worker.ot.results.file");

        TripSynthesizer ts = new TripSynthesizer();
        //ts.synthesizeResidentTrips(ham);
        ts.synthesizeExternalWorkerTrips(ewam);
        //ts.synthesizeVisitorTrips(pam);
        ts.writeModeSkimMatrices(ResourceUtil.getProperty(rb,"trip.output.directory"),"Trips_");

    }

    public static void main(String args[]) {
        runExternalWorkerModel();
    }
}
