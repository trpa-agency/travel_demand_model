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
package com.pb.tahoe.synpop.test;

import com.pb.tahoe.synpop.SyntheticPopulation;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

/**
 * SynPopTest is a class that will call the methods necessary to generate the
 * synthetic population for Tahoe.  It will have the main method that I will use
 * to test the code.
 *
 * @author Christi Willison
 * @version 1.0,  Dec 21, 2005
 */
public class SynPopTest {

    protected static Logger logger = Logger.getLogger(SynPopTest.class);

    public static void runSynPop(){
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

    }


    public static void main(String[] args) {
        
        //Need the ZonalDataManager to combine the zonal files and calculate walk percentages
        //so that the hhs can pick their origin walk segment.
        ZonalDataManager.getInstance();

         runSynPop();



    }
}
