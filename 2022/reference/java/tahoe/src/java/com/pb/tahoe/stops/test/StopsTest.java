package com.pb.tahoe.stops.test;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.synpop.test.SynPopTest;
import com.pb.tahoe.auto_ownership.AutoOwnership;
import com.pb.tahoe.daily_activity_pattern_test.DAPTest;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.dest_time_mode.*;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.joint_tour.JointToursModel;
import com.pb.tahoe.individual_tour.IndividualNonMandatoryToursModel;
import com.pb.tahoe.stops.MandatoryStops;
import com.pb.tahoe.stops.JointStops;
import com.pb.tahoe.stops.NonMandatoryStops;
import com.pb.tahoe.stops.AtWorkStops;

import java.util.ResourceBundle;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * Created by CRF
 * Date: Nov 3, 2006
 */
public class StopsTest {

    static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    static Logger logger = Logger.getLogger(StopsTest.class);

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

        logger.info("Preparing the zonal data");
        ZonalDataManager.getInstance();
        TODDataManager.getInstance();

        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterAtWorkDTM.doa"); // run stop frequency and stop location for each of the mandatory tours

        boolean writeReports = ResourceUtil.getBooleanProperty(rb, "write.reports",false);
        TahoeReports tr = new TahoeReports();

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

        logger.info("Total runtime in minutes: " + ((System.currentTimeMillis()-startTime)/60000.0f));

        if (ResourceUtil.getBooleanProperty(rb, "write.disk.object.arrays", false)) {
            String diskArrayFileName = rb.getString("DiskObjectArrayInput.file");
            ham.writeDiskObjectArray(diskArrayFileName + "_afterAtWorkStops.doa");
        }

    }
}
