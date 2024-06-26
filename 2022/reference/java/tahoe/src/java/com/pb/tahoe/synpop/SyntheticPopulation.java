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
package com.pb.tahoe.synpop;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.NDimensionalMatrixBalancerDouble;
import com.pb.common.matrix.NDimensionalMatrixDouble;
import com.pb.common.matrix.RowVector;
import com.pb.common.util.SeededRandom;
import com.pb.models.censusdata.PumsGeography;
import com.pb.tahoe.util.DataReader;
import com.pb.tahoe.util.DataWriter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SyntheticPopulation is a class that contains methods for generating
 * the Tahoe Synthetic population.  It needs PUMSData and a TAZ
 * Socio-economic file that has average size, nWorkers and average
 * income as fields.  In the case of Tahoe it also needs
 * a seed table created from CTPP data to seed the matrices before balancing.
 *
 * @author Christi Willison
 * @version 1.0,  Dec 21, 2005
 */
public class SyntheticPopulation {
    protected static Logger logger = Logger.getLogger(SyntheticPopulation.class);

     HH[][] zonalHHs;                            /**  An array of HH objects indexed by zone.  This is the synthetic population */

    AlphaToBeta taz2Census;        /** Lookup table that corresponds TAZ to their census tracts. */
    TableDataSet zoneTable;         /** The socio-economic data for the zone that has number of HHs, avg HH size,
                                                        avg # workers, and number of HHs in each income category (high, med, low) */

    float[][] finalTargets;

    PumsGeography studyArea;   /** A correspondence class that sets up look-ups between the TAZ's and the States/PUMAs. */

    Map<Integer, NDimensionalMatrixDouble>[] seed3DMatrices; //sorted by state (index into array) and census tract (key to map).
    ArrayList<HH> [][][][][] PUMSHHs_5WaySort;    //HHs read in from PUMS sorted by state, puma, hhsize, nWorkers, income.

    static final float[] zeroHHSize = {0.0f, 0.0f, 0.0f, 0.0f};
    static final float[] zeroWorker = { 0.0f, 0.0f, 0.0f, 0.0f };
    static final float[] zeroIncome = { 0.0f, 0.0f, 0.0f };


    /**
     * This method will read the TAZ correspondence file and the PUMS data
     * for the appropriate states and will sort the households by state, pums
     * size, number of workers and income category.  These will be used later
     * when we need to select hhs once the table balancing routines are done.
     */
    public void sortPUMSData() {

        //First read in the TAZ correspondence file and set up the correspondence
        //between the zones and the pumas.
        DataReader reader = DataReader.getInstance();
        if(studyArea == null) {
            studyArea = reader.setUpTAZPumaCorrespondence();
        }

        //Initialize the PUMSHHs_5WaySort array of ArrayLists.
        initializeHHArrayList();

        //Next read in the PUMS data for each state.  This will return 2 TableDataSets
        //The first will be the hh table and the second will be the person table.
        //Once we have the tables, we need to go thru and categorize the people and
        //then put the household into the proper ArrayList.
        for (int s=0; s < studyArea.getNumberOfStates();  s++){   //pumaIndex.length = number of states
            int[] pumaSet = studyArea.getStatePumaIndexArray(s);
            TableDataSet[] tables = reader.readPUMSData(studyArea.getStateLabel(s), pumaSet);

            TableDataSet pumsHHTable = tables[0];
            TableDataSet pumsPersonTable = tables[1];

            //Loop thru the tables and define model specific person types
            //based on the attributes in the person table.  The person types of all persons
            //in the household will be stored in a temporary array.
            int nWorkers;      //will be determined by person types
            int nAdults;        //will be determined by person types
            int[] tempArray = null;

            for (int r=1; r<= pumsHHTable.getRowCount(); r++){
                int nPersons = (int) pumsHHTable.getValueAt(r, "PERSONS");
                tempArray = new int[nPersons]; //initialized to 0
                nWorkers = 0;      //initialized to 0
                nAdults = 0;        //initialized to 0
                boolean retiredPersonPresent = false;

                int rowIndexOfFirstPersonInHH = (int) pumsHHTable.getValueAt(r,"FIRSTPERSONID");
                for(int p = 1; p <=nPersons; p++ ){
                    int row = p + rowIndexOfFirstPersonInHH;      //because it is the rowINDEX and not the row, we have to add 1.

                    //characterize this person and put them into the temp array.
                    int age = (int) pumsPersonTable.getValueAt(row, "AGE");
                    int school = (int) pumsPersonTable.getValueAt(row, "ENROLL");
                    int status = (int) pumsPersonTable.getValueAt(row, "ESR");
                    int hours = (int) pumsPersonTable.getValueAt(row, "HOURS");

                    int personType = -1;
                    try {
                        personType = characterizePerson (age, school, status, hours);
                        retiredPersonPresent = checkForRetired(age, status);
                    } catch (Exception e) {
                        //an exception will be thrown if -1 is returned.
                        logger.info("Check employment status of the persons in HH " + pumsHHTable.getValueAt(r, "SERIALNO")
                                          + " (" + studyArea.getStateLabel(s) + ")");
                        throw new RuntimeException(e);
                    }

                    if (personType < 5) nAdults ++;
                    if(personType ==1 || personType ==2) nWorkers++;

                    tempArray[p-1] = personType;
                } //next person

                //Once we have looped thru each person we will create a HH object (as long
                //as there is at least 1 adult)
                if(nAdults > 0 ) {    //only keep households with a least one adult.
                    //create a new HH object
                    HH hh = new HH();

                    //set the household size attribute and the size category
                    for (int a = 0; a < hh.attribs.length; a++) {
                        if (hh.attribLabels[a].equals("SIZE")) {
                            hh.attribs[a] = nPersons;
                            hh.categories.add(a, ModelCategories.getSizeCategory(nPersons));
                            break;
                        }
                    }

                    //set the number of workers in hh attribute and the worker category
                    for (int a = 0; a < hh.attribs.length; a++) {
                        if (hh.attribLabels[a].equals("WORKERS")) {
                           hh.attribs[a] = nWorkers;
                           hh.categories.add(a, ModelCategories.getWorkerCategory(nWorkers));
                           break;
                        }
                    }

                    //set the hh income attribute and the income category
                    int income = (int) pumsHHTable.getValueAt(r,"HINC");  //needed for later
                    for (int a = 0; a < hh.attribs.length; a++) {
                        if (hh.attribLabels[a].equals("INCOME")) {
                           hh.attribs[a] = income;
                           hh.categories.add(a, ModelCategories.getIncomeCategory(income));
                           break;
                        }
                    }

                    //set the personType array
                    hh.personTypes = new int[nPersons];
                    for(int j=0; j<tempArray.length; j++){
                        hh.personTypes[j] = tempArray[j];
                    }

                    //set the retiredPersonPresent boolean
                    hh.setRetiredPersonPresent(retiredPersonPresent);

                    //Now put the HH into the appropriate ArrayList
                    int puma = (int) pumsHHTable.getValueAt(r, "PUMA5");
                    PUMSHHs_5WaySort[s][studyArea.getPumaIndex(s,puma)][ModelCategories.getSizeCategory(nPersons).ordinal()][ModelCategories.getWorkerCategory(nWorkers).ordinal()][ModelCategories.getIncomeCategory(income).ordinal()].add(hh);

                } //end if  - no adults, then get the next household.
            } //next household
        } //next state
    } //end sort

    /**
     *   This method will ask the DataReader to read in the seed tables.  These
     * tables are produced outside of the code based on the CTPP summary tables
     * for the Lake Tahoe region.  NDimensionalMatrices will be created and
     * stored in a Hashmap array according to the state index.
     *
     */
    public void create3DSeedMatrices(){
        DataReader reader = DataReader.getInstance();

        if(studyArea == null) {  //should have been read in by earlier method.
            studyArea = reader.setUpTAZPumaCorrespondence();
        }

        TableDataSet seedTable = reader.readSeedTable(); //pass in formats like we do above.

        initializeSeedHashMap();     //2-d array of Hashmaps, the key is the census tract and the value is the NDimMatrix
        int[] dimensions = {ModelCategories.HHSize.values().length, ModelCategories.HHWorkers.values().length, ModelCategories.HHIncome.values().length};
        int[] indices = new int[dimensions.length];

        NDimensionalMatrixDouble matrix = null;
        for(int r=1; r<=seedTable.getRowCount(); r++){
            int state = (int) seedTable.getValueAt(r,"State");
            int tract = (int) seedTable.getValueAt(r,"Tract");
            String size = seedTable.getStringValueAt(r,"HHSize");
            String workers = seedTable.getStringValueAt(r,"HHWorkers");
            String income = seedTable.getStringValueAt(r,"HHIncome");
            int value = (int) seedTable.getValueAt(r, "nHHs");
            matrix = seed3DMatrices[studyArea.getStateIndex (state)].get(tract);
            if(matrix == null){
                matrix = new NDimensionalMatrixDouble("", dimensions.length, dimensions);
                seed3DMatrices[studyArea.getStateIndex(state)].put(tract,matrix);
            }
            indices[0] = ModelCategories.getSizeIndex(size);
            indices[1] = ModelCategories.getWorkerIndex(workers);
            indices[2] = ModelCategories.getIncomeIndex(income);

            matrix.setValue (value, indices);
        }

        //The ctpp data has number of hhs, but we want percentage of hhs.  So go thru
        //the matrices and divide each element by the sum.
        for (Map<Integer, NDimensionalMatrixDouble> seed3DMatrice : seed3DMatrices) {
            for (Integer t: seed3DMatrice.keySet()) {
                NDimensionalMatrixDouble newM = seed3DMatrice.get(t);
                double sum = newM.getSum();
                if(sum != 0.0){
                    newM = newM.matrixMultiply(1.0/sum);
                }
                seed3DMatrice.put(t, newM );
            }
        }

        //Finally, make sure that cells that are nonsensical (HHSize 1, 2 Workers) are set to 0
        //and that cells that are possible but happened to be 0 in the CTPP
        //data are set to 0.00001
        int[] cellCoords = new int[dimensions.length];
        for (Map<Integer, NDimensionalMatrixDouble> seed3DMatrice : seed3DMatrices) {
            for (NDimensionalMatrixDouble mtx : seed3DMatrice.values()) {
                for (int j = 0; j < ModelCategories.HHSize.values().length; j++) {
                    for (int k = 0; k < ModelCategories.HHWorkers.values().length; k++) {
                        for (int m = 0; m < ModelCategories.HHIncome.values().length; m++) {
                            cellCoords[0] = j;
                            cellCoords[1] = k;
                            cellCoords[2] = m;

                            if (k > (j + 1)) {
                                mtx.setValue(0.0, cellCoords);
                            } else if (mtx.getValue(cellCoords) == 0.0) {
                                mtx.setValue(0.00001, cellCoords);
                            }
                        }
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            int state=0;
            for (Map<Integer, NDimensionalMatrixDouble> seed3DMatrice : seed3DMatrices) {
                int tract = 0;
                for (NDimensionalMatrixDouble m : seed3DMatrice.values()) {
                    m.printMatrixDelimited("\t" , ("/Users/christi/model_data/tahoe/debug/Matrix" + state + "_" + tract + ".txt"));
                    tract++;
                }
                state++;
            }
        }
    }

    /**
     * This method will go thru the socio-economic file, generate the marginals
     * balance the 3-D matrix and return a HH array.
     */
    public void buildPop() {
        DataReader reader = DataReader.getInstance();
        zoneTable = reader.readSocioEconomicTable();
        ZonalData.setZoneTable(zoneTable);

        //initialize the array that will hold the synthesized households
        zonalHHs = new HH[zoneTable.getRowCount()][];
        //initialize this array which will store the final zone percentages for size, workers and income
        finalTargets = new float[zoneTable.getRowCount()][];

        taz2Census = reader.setUpTAZCensusTractCorrespondence();

        int hhsPosition = zoneTable.getColumnPosition(ZonalData.HHS_FIELD);

        // generate the synthetic population for each zone
        int hhNumber = 1;
        for (int row = 1; row <= zoneTable.getRowCount(); row++) {
            int HHsInZone = (int) zoneTable.getValueAt(row, hhsPosition);
            if (HHsInZone > 0) {
                //pass in the row, and the marginals will be calculated.  Then the seed
                //tables will be used to balance a 3-D matrix that indicates the number
                //of hhs in each category.  The hhs are then selected from the PUMS.

                zonalHHs[row-1] =  getZonalHHs(row);
                for (HH aHh : zonalHHs[row-1]) {
                    aHh.setHhNumber(hhNumber);
                    hhNumber++;
                }

            } else {
                // TableDataSet values are stored with zero based indexing
                setZonalFinalTargets(row - 1, new RowVector(zeroHHSize), new RowVector(zeroWorker), new RowVector(zeroIncome));
            }
        }

    }

    public void writeResults () {
        DataWriter writer = DataWriter.getInstance();

        // write households to output file
        writer.writeHHTableDataSet(zonalHHs);

        // write zonal targets to output file
        writer.writeTargetsTableDataSet(finalTargets);

    }


    private HH[] getZonalHHs(int rowNumber) {

        // create a ZonalData object for the zone passed in
        ZonalData zd = ZonalData.populateZonalData(rowNumber);

        //baseLogger.info("Getting HHs for Zone " + zd.getZoneNumber());
        // get the marginals (with control average adjustments) for each hhsize and worker dimensions
        RowVector HHSizeMarginals = ZonalData.getHHSizeMarginals(zd);
        RowVector WorkerMarginals = ZonalData.getWorkerMarginals(zd, HHSizeMarginals);
        RowVector IncomeMarginals = ZonalData.getIncomeMarginals(zd);

       NDimensionalMatrixBalancerDouble mb3 = new NDimensionalMatrixBalancerDouble();

        // get the 3D CTPP seed matrix for the census tract in which the zone is located
        //we need the corresponding state index and the census tract for the zone.
        int tazNumber = zd.getZoneNumber();
        int indexForZone = studyArea.getZoneIndex(tazNumber);
        int[] statePumaIndices = studyArea.getStatePumaIndicesFromZoneIndex( indexForZone);
        NDimensionalMatrixDouble seed = seed3DMatrices[statePumaIndices[0]].get(taz2Census.getBetaZone(tazNumber));

        //		mb3.setTrace(true);
        mb3.setSeed(seed);
        mb3.setTarget(HHSizeMarginals, 0);
        mb3.setTarget(WorkerMarginals, 1);
        mb3.setTarget(IncomeMarginals, 2);

        // save final target marginals in an array to be written out as a TableDataSet later on.
        // TableDataSet values are stored with zero based indexing
         setZonalFinalTargets(rowNumber - 1, HHSizeMarginals, WorkerMarginals, IncomeMarginals);

        mb3.balance();

        //get and print the balanced matrix
        NDimensionalMatrixDouble mb3Balanced = mb3.getBalancedMatrix();

        if (logger.isDebugEnabled()) {
            print3WayTable(mb3Balanced);
            logger.info("Balanced proportions matrix total = " + mb3Balanced.getSum());
            logger.info(" ");
            logger.info(" ");
            logger.info(" ");
        }

        //get and print the balanced fractional hhs matrix
        NDimensionalMatrixDouble balancedHHFractional = mb3Balanced.matrixMultiply(zd.getHHs());

        if (logger.isDebugEnabled()) {
            print3WayTable(balancedHHFractional);
            logger.info("Balanced fractional HHs matrix total = " + balancedHHFractional.getSum());
            logger.info(" ");
            logger.info(" ");
            logger.info(" ");
        }

        //get and print the balanced discretized hhs matrix
        NDimensionalMatrixDouble balancedHHDiscretized = balancedHHFractional.discretize();

        if (logger.isDebugEnabled()) {
            print3WayTable(balancedHHDiscretized);
            logger.info("Balanced discretized HHs matrix total = " + balancedHHDiscretized.getSum());
            logger.info(" ");
            logger.info(" ");
        }

        // create a household list by taking each household from the balanced
        // discretized matrix and selecting a matching household record from
        // the PUMS data.
        return matchHHsToPUMS(zd, balancedHHDiscretized);
    }

    private HH[] matchHHsToPUMS(ZonalData zd, NDimensionalMatrixDouble hhs3D) {
        ArrayList pumsHHs;
        int hhs;
        boolean adjusted = false;
        int[] location = new int[3];
        ArrayList <HH> tempHHs = new ArrayList <HH>();
        HH[] hhsFromPUMS = null;

        int indexForZone = studyArea.getZoneIndex(zd.getZoneNumber());
        int[] statePumaIndices = studyArea.getStatePumaIndicesFromZoneIndex( indexForZone  );


        // loop over dimensions of the 3-D matrix
        int[] shape = hhs3D.getShape();

        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    location[0] = i;
                    location[1] = j;
                    location[2] = k;
                    hhs = (int) hhs3D.getValue(location);
                    if (logger.isDebugEnabled()) logger.debug("Zone " + zd.getZoneNumber() + ", Looking for " + hhs + 
                            " household/s in the following cell, " +
                            ModelCategories.getOrdinalSizeCategory(i) + "," + ModelCategories.getOrdinalWorkerCategory(j) + "," +
                            ModelCategories.getOrdinalIncomeCategory(k));

                    if (hhs > 0) {
                        adjusted = false;
                        pumsHHs = PUMSHHs_5WaySort[statePumaIndices[0]][statePumaIndices[1]][i][j][k];
                        if(logger.isDebugEnabled()) logger.debug("Found " + pumsHHs.size() + " that match the criteria");

                        while (pumsHHs.size() == 0) {
                            // adjust hhsize downward until a combination is found with PUMS hhs in it.
                            if (i > 0) {
                                logger.debug("Adjusting hh size down");
                                i--;

                                // adjust workers downward if hhsize becomes smaller than workers.
                                if ((j > 0) && (i < (j - 1))) {
                                    logger.debug("Adjusting number of workers down because of hh size decrement");
                                    j--;
                                }

                                // if we're at i==0, j==0, then adjust k downward, but start adjustments over
                                // again with original i,j.
                                if ((i == 0) && (j == 0) && (k > 0)) {
                                    i = location[0];
                                    j = location[1];
                                    k--;
                                    logger.debug("We moved the income level down but went back to the original size/worker criteria");
                                }

                                pumsHHs = PUMSHHs_5WaySort[statePumaIndices[0]][statePumaIndices[1]][i][j][k];
                                adjusted = true;
                            } else {
                                // adjust workers downward if hhsize becomes smaller than workers.
                                logger.info("We are already at the lowest hhSize so we will try to adjust the number of workers down");
                                if (j > 0) {
                                    logger.debug("Adjusting number of workers down");
                                    j--;
                                }

                                // if we're at i==0, j==0, then adjust k downward, but start adjustments over
                                // again with original i,j.
                                if ((i == 0) && (j == 0) && (k > 0)) {
                                    i = location[0];
                                    j = location[1];
                                    k--;
                                    logger.debug("We moved the income level down but went back to the original size/worker criteria");
                                }

                                pumsHHs = PUMSHHs_5WaySort[statePumaIndices[0]][statePumaIndices[1]][i][j][k];
                                adjusted = true;
                            }
                        }

                        hhsFromPUMS = chooseRandomHHsFromMatchingSet(hhs, pumsHHs);


                        for (HH aHh : hhsFromPUMS) {
                            aHh.setZoneNumber(zd.getZoneNumber());
                            aHh.categories.add(0, ModelCategories.HHSize.values()[i]);
                            aHh.categories.add(1, ModelCategories.HHWorkers.values()[j]);
                            aHh.categories.add(2, ModelCategories.HHIncome.values()[k]);
                            aHh.setAdjusted(adjusted);
                            tempHHs.add(aHh);
                        }

                        i = location[0];
                        j = location[1];
                        k = location[2];
                    }
                }
            }
        }

        hhsFromPUMS = new HH[tempHHs.size()];

        for (int m = 0; m < tempHHs.size(); m++) {
            hhsFromPUMS[m] = (HH) tempHHs.get(m);
        }
        logger.debug("Zone " + zd.getZoneNumber() + " has "  + hhsFromPUMS.length + " households after choosing from PUMS");
        return hhsFromPUMS;
    }

    private HH[] chooseRandomHHsFromMatchingSet(int hhCount, ArrayList pumsHHs) {
        int[] randomNumbers = new int[pumsHHs.size()];
        int[] index;
        HH[] hhList = new HH[hhCount];

        int hh = 0;

        while (hh < hhCount) {
            for (int i = 0; i < randomNumbers.length; i++)
                randomNumbers[i] = (int) (1000000000 * SeededRandom.getRandom());

            index = NDimensionalMatrixDouble.indexSort(randomNumbers);

            int last = Math.min(randomNumbers.length, (hhCount - hh));

            try {
                for (int i = 0; i < last; i++) {
                    hhList[hh] = (HH) ((HH) pumsHHs.get(index[i])).clone();
                    hh++;
                }
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        return hhList;
    }

    /**
     * This method gets the array of HashMaps ready to be filled.
     * The HashMap will hold the NDimensionalMatrixDouble arrays that
     * are the 3-D seed table.  The key to the HashMap is the census tract.
     */
    private void initializeSeedHashMap() {
        int nStates = studyArea.getNumberOfStates();
        seed3DMatrices = new HashMap [nStates] ;
        for(int s = 0; s< nStates; s++){
            seed3DMatrices[s] = new HashMap <Integer, NDimensionalMatrixDouble > ();
         }
    }


    private void initializeHHArrayList() {
        int nStates = studyArea.getNumberOfStates();
        int nPumas =  studyArea.getTotalNumberOfPumas();
        int nSizeCategories = ModelCategories.HHSize.values().length;
        int nWorkerCategories = ModelCategories.HHWorkers.values().length;
        int nIncomeCategories = ModelCategories.HHIncome.values().length;
        PUMSHHs_5WaySort = new ArrayList[nStates][nPumas][nSizeCategories][nWorkerCategories][nIncomeCategories] ;
        for(int s = 0; s< nStates; s++){
            for(int p = 0; p<nPumas; p++){
                for (int z=0; z<nSizeCategories; z++){
                    for(int w=0; w<nWorkerCategories; w++){
                        for(int i=0; i<nIncomeCategories; i++){
                            PUMSHHs_5WaySort[s][p][z][w][i] = new ArrayList <HH > ();
                        }
                    }
                }
            }
        }
    }


    private int characterizePerson (int age, int school, int empStatus, int hours) throws Exception {
        int type = -1;
        switch (empStatus) {
            //non-workers
            case 0:
            case 3:
            case 6:
                if ((age > 17) && (school < 2)) {
                    type = 3;                                                //non-worker
                } else if (age <= 5) {
                    type = 4;                                                 //preschool
                } else if ((age > 5) && (age <= 15)) {
                    type = 5;                                                 //student - predriver
                } else if ((age > 15) && (age <= 17)) {
                    type = 6;                                                 //student - driver
                } else if((age > 17) && (age < 21) && (school >=2)) {
                    type = 6;                                                  //student - driver
                } else if(age >=21 && school >=2)
                    type=3;                                               //non-working adult student but not considered a student-driver in the classical sense.
                break;

            //workers
            case 1:
            case 2:
            case 4:
            case 5:
                if ((age > 17) && (hours >= 35)) {
                    type = 1;                                                 //full-time worker
                } else if ((age > 17) && (hours < 35)) {
                    type = 2;                                                 //part-time worker
                } else
                    type = 6;                                                //student-driver that happens to work but they are not counted as a worker
                break;
        } //end switch
        if (type == -1) {
            logger.warn("Can't find personType for the following person");
            logger.warn("Age: " + age + " School: " + school + " EmpStatus: " + empStatus + " Hours: "  + hours);
            throw new Exception("invalid person type");
        }
        return type;
    }

    private boolean checkForRetired(int age, int empStatus) {
        boolean retired = false;
        switch (empStatus) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                retired = false;
                break;
            case 6:
                if (age > 65) {
                    retired = true;
                    break;
                }
        } //end switch
        return retired;
    }


    private void setZonalFinalTargets(int arrayPos, RowVector HHSizeMarginals, RowVector WorkerMarginals, RowVector IncomeMarginals) {
        this.finalTargets[arrayPos] = new float[1 + HHSizeMarginals.size() +
            WorkerMarginals.size() + IncomeMarginals.size()];

        int hhsPosition = zoneTable.getColumnPosition(ZonalData.HHS_FIELD);
        int HHsInZone = (int) zoneTable.getValueAt(arrayPos + 1, hhsPosition);

        this.finalTargets[arrayPos][0] = (int) zoneTable.getValueAt(arrayPos + 1, zoneTable.getColumnPosition(ZonalData.ZONE_FIELD));

        for (int i = 0; i < HHSizeMarginals.size(); i++)
            this.finalTargets[arrayPos][1 + i] = HHSizeMarginals.getValueAt(i + 1) * HHsInZone;

        for (int i = 0; i < WorkerMarginals.size(); i++)
            this.finalTargets[arrayPos][1 + i + HHSizeMarginals.size()] = WorkerMarginals.getValueAt(i +1) * HHsInZone;

        for (int i = 0; i < IncomeMarginals.size(); i++)
            this.finalTargets[arrayPos][1 + i + HHSizeMarginals.size() + WorkerMarginals.size()] = IncomeMarginals.getValueAt(i + 1) * HHsInZone;
    }

    private void print3WayTable (NDimensionalMatrixDouble table) {
            String[] s0 = ModelCategories.getSizeLabels();
            String[] s1 = ModelCategories.getWorkerLabels();
            String[] s2 = ModelCategories.getIncomeLabels();
            int[] loc = new int[3];

            for (int i = 0; i < s0.length; i++) {
                logger.info("Household Size = " + s0[i]);

                logger.info(",Workers");

                String thirdLine = "Income,";
                for (String aS1 : s1) {
                    thirdLine +=  aS1 +",";
                }
                thirdLine += "Total";
                logger.info(thirdLine);

                double value;
                double tot=0.0f;
                float[] ctots = new float[s1.length];    //total hhs in each worker category for a given hh size
                float[] rtots = new float[s2.length];   //total hhs in each income category  for a given hh size
                for (int j = 0; j < s2.length; j++) {
                    String valueLine = s2[j] + ",";
                    for (int k = 0; k < s1.length; k++) {
                        loc[0] = i;    //hhsize
                        loc[1] = k;   //workers
                        loc[2] = j;    //income
                        value = table.getValue(loc);
                        valueLine += String.format("%15.6f", value) + ",";
                        rtots[j] += value;
                        ctots[k] += value;
                        tot += value;
                    }
                    valueLine += String.format("%15.6f", rtots[j]);
                    logger.info(valueLine);
                }

                String totalLine = "Total,";
                for (int k = 0; k < s1.length; k++)
                    totalLine += String.format("%15.6f", ctots[k]) +",";
                totalLine += String.format("%15.6f", tot);

                logger.info(totalLine);
                logger.info("");
            }
        }


}
