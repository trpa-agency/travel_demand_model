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
package com.pb.tahoe.dest_time_mode_test;

import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.dest_time_mode.DTMModel;
import com.pb.tahoe.dest_time_mode.DTMOutput;
import com.pb.tahoe.dest_time_mode.MandatoryDTM;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * MandatoryDTMTest is a class that can be run standalone to calibrate
 * the mandatory destination choice, time of day choice and mode choice
 * models.
 *
 * @author Christi Willison
 * @version 1.0,  May 22, 2006
 */
public class MandatoryDTMTest {
    static Logger logger = Logger.getLogger(MandatoryDTMTest.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ResourceBundle tahoeRb = ResourceUtil.getResourceBundle("tahoe");

        logger.info("Preparing the zonal data");
        ZonalDataManager.getInstance();
        TODDataManager.getInstance();

        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArray();

        DTMModel mandatoryDTM = new MandatoryDTM(tahoeRb);

        logger.info( "Running Destination Choice for Mandatory Tours");
        mandatoryDTM.doWork(ham);
        mandatoryDTM.printTimes(TourType.MANDATORY_CATEGORY);

        File outputFile = new File(tahoeRb.getString("mandatory_dtm.choice.output.file"));
        DTMOutput dtmOutput = new DTMOutput();
        dtmOutput.writeMandatoryDTMChoiceResults(outputFile, ham.getHouseholds(), mandatoryDTM.everyNth);
        logger.info("Total runtime in minutes: " + ((System.currentTimeMillis()-startTime)/60000.0f));

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.disk.object.arrays", false)) {
            String diskArrayFileName = tahoeRb.getString("DiskObjectArrayInput.file");
            ham.writeDiskObjectArray(diskArrayFileName + "_afterMandatoryDTM.doa");
        }

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.reports", false)) {
            TahoeReports tr = new TahoeReports();
            tr.mandReport();
        }
    }
}
