package com.pb.tahoe.visitor;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.visitor.structures.StayType;
import com.pb.tahoe.visitor.structures.VisitorType;
import com.pb.tahoe.util.TripSynthesizer;

import java.util.ResourceBundle;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Feb 11, 2007 - 10:01:52 PM
 */
public class VisitorModelRunner {

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(VisitorModelRunner.class);

    //debug mode
    static boolean debug = false;

    public static void runOvernightVisitorModel() {
        //create instances of partyArrayManager
        PartyArrayManager pam = PartyArrayManager.getInstance();

        //generate overnight visitor synthetic population
        //first define stay type to percentage to fill by hand - figure out what method to automate later
//        HashMap<StayType,Float> percentsToFill = new HashMap<StayType,Float>();
//        for (StayType st : StayType.values()) {
//            //percentsToFill.put(st,0.2f);
//            percentsToFill.put(st,0.005f);
//        }
        //Set the random seed so that population is reproducable
        SeededRandom.setSeed(2002);
        logger.info("Generating overnight visitor units to fill.");
        //HashMap<Integer,HashMap<StayType,Integer>> unitsToFill = OvernightVisitorUnitsToFill.generateUnitsToFill(percentsToFill);
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

        //Write out trip matrices
        TripSynthesizer ts = new TripSynthesizer();
        ts.synthesizeVisitorTrips(pam);
        ts.writeModeSkimMatrices(ResourceUtil.getProperty(rb,"trip.output.directory"),"OvernightVisitorTrips_");
    }

    public static void main(String args[]) {
        runOvernightVisitorModel();
    }

}
