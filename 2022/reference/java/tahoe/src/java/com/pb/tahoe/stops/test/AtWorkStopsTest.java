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
package com.pb.tahoe.stops.test;

import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.stops.AtWorkStops;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.reports.TahoeReports;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * MandatoryStopsTest is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Oct 17, 2006
 */
public class AtWorkStopsTest {

    static Logger logger = Logger.getLogger(AtWorkStopsTest.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ResourceBundle tahoeRb = ResourceUtil.getResourceBundle("tahoe");

        logger.info("Preparing the zonal data");
        ZonalDataManager.getInstance();
        TODDataManager.getInstance();

        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterNonMandatoryStops.doa"); // run stop frequency and stop location for each of the mandatory tours

        logger.info( "Running Stop Frequency, Location and Mode Choice for AtWork Tours");
        AtWorkStops atWorkStops = new AtWorkStops(tahoeRb, ham);
        atWorkStops.doSfcSlcWork();
        atWorkStops.doSmcWork();
        atWorkStops.printTimes(TourType.AT_WORK_CATEGORY);
        logger.info("Total runtime in minutes: " + ((System.currentTimeMillis()-startTime)/60000.0f));

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.disk.object.arrays", false)) {
            String diskArrayFileName = tahoeRb.getString("DiskObjectArrayInput.file");
            ham.writeDiskObjectArray(diskArrayFileName + "_afterAtWorkStops.doa");
        }
        boolean writeReports = ResourceUtil.getBooleanProperty(tahoeRb, "write.reports",false);
        if(writeReports) {
            TahoeReports tr = new TahoeReports();
            tr.stopsReport("atwork");
        }
    }
}
