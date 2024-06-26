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
import com.pb.tahoe.dest_time_mode.NonMandatoryDTM;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * IndivNonMandDTMTest is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Oct 3, 2006
 */
public class IndivNonMandDTMTest {
    static Logger logger = Logger.getLogger(IndivNonMandDTMTest.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ResourceBundle tahoeRb = ResourceUtil.getResourceBundle("tahoe");

        logger.info("Preparing the zonal data");
        ZonalDataManager.getInstance();
        TODDataManager.getInstance();

        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterIndivNonMandatoryTourGeneration.doa");

        DTMModel nonMandDTM = new NonMandatoryDTM(tahoeRb);

        logger.info( "Running Destination, Time of Day and Mode Choice for Individual Non-mandatory Tours");
        nonMandDTM.doWork(ham);
        nonMandDTM.printTimes(TourType.NON_MANDATORY_CATEGORY);

        File outputFile = new File(tahoeRb.getString("non-mandatory_dtm.choice.output.file"));
        DTMOutput dtmOutput = new DTMOutput();
        dtmOutput.writeIndivNonMandDTMChoiceResults(outputFile, ham.getHouseholds(), nonMandDTM.everyNth);
        logger.info("Total runtime in minutes: " + ((System.currentTimeMillis()-startTime)/60000.0f));

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.disk.object.arrays", false)) {
            String diskArrayFileName = tahoeRb.getString("DiskObjectArrayInput.file");
            ham.writeDiskObjectArray(diskArrayFileName + "_afterNonMandatoryDTM.doa");
        }


        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.reports", false)) {
            TahoeReports tr = new TahoeReports();
            tr.indDTMReport();
        }
    }
}
