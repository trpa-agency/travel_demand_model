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
package com.pb.tahoe.individual_tour_test;

import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.individual_tour.IndividualNonMandatoryToursModel;
import com.pb.tahoe.reports.TahoeReports;
import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.TODDataManager;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * IndivNonMandToursTest is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Oct 2, 2006
 */
public class IndivNonMandToursTest {

    static Logger logger = Logger.getLogger(IndivNonMandToursTest.class);

    public static void main(String[] args) {
        logger.info("Starting the individual non-mandatory tour model");
        long startTime = System.currentTimeMillis();
        ResourceBundle tahoeRb = ResourceUtil.getResourceBundle("tahoe");

        logger.info("Preparing the zonal data");
        ZonalDataManager.getInstance();
        TODDataManager.getInstance();

        logger.info("Creating HH and Person objects");
        HouseholdArrayManager ham = HouseholdArrayManager.getInstance();
        ham.createBigHHArrayFromDiskObject("_afterJointDTM.doa");

        IndividualNonMandatoryToursModel indiv = new IndividualNonMandatoryToursModel(tahoeRb ,ham.getHouseholds());
        indiv.runMaintenanceFrequency();
        indiv.runMaintenanceAllocation();
        indiv.runDiscretionaryWorkerFrequency();
        indiv.runDiscretionaryNonWorkerFrequency();
        indiv.runDiscretionaryChildFrequency();
        indiv.runAtWorkFrequency();
         logger.info("Total runtime in minutes: " + ((System.currentTimeMillis()-startTime)/60000.0f));

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.disk.object.arrays", false)) {
            String diskArrayFileName = tahoeRb.getString("DiskObjectArrayInput.file");
            ham.writeDiskObjectArray(diskArrayFileName + "_afterIndivNonMandatoryTourGeneration.doa");
        }

        if (ResourceUtil.getBooleanProperty(tahoeRb, "write.reports", false)) {
            TahoeReports tr = new TahoeReports();
            tr.indTourReport();
        }

    }

}
