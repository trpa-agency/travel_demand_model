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
import com.pb.common.matrix.RowVector;
import com.pb.tahoe.visitor.structures.VisitorDataStructure;
import org.apache.log4j.Logger;


/**
 * ZonalData is an object that contains its socio-economic data.  There will be one of these
 * objects for each internal zone.
 * It will call a static method to populate itself based on a row from the socio-economic data file.
 * It will also calculate size, worker and income marginals for itself
 *
 *
 * @author Christi Willison
 * @version 1.0,  Feb 8, 2006
 */
public class ZonalData {
    protected static Logger logger = Logger.getLogger(ZonalData.class);

    private static TableDataSet zoneTable;

    //field names we use from the Tahoe Socio-economic file
    public static final String ZONE_FIELD = "taz";
    public static final String TOTALHHS_FIELD = "total_residential_units";
    public static final String HHS_FIELD = "total_occ_units";
    public static final String AVGSIZE_FIELD = "persons_per_occ_unit";
    public static final String LABORFORCE_FIELD = "total_labor_force";
    public static final String NUM_LOW_INCOME_HHS = "occ_units_low_inc";
    public static final String NUM_MED_INCOME_HHS = "occ_units_med_inc";
    public static final String NUM_HI_INCOME_HHS = "occ_units_high_inc";
    public static final String RETAIL_EMP = "emp_retail";
    public static final String SERVICE_EMP = "emp_srvc";
    public static final String RECREATION_EMP = "emp_rec";
    public static final String GAMING_EMP = "emp_game";
    public static final String OTHER_EMP = "emp_other";

    //overnight data
    public static final String BEACH_FIELD = VisitorDataStructure.OVZONAL_BEACH_FIELD;


    static int hhsPosition = -1;
    static int avgSizePosition = -1;
    static int laborForcePosition = -1;
    static int nHHLowIncomePosition = -1;
    static int nHHMedIncomePosition = -1;
    static int nHHHighIncomePosition = -1;
    static int zoneNumberPosition = -1;

    
    //attributes specific to each zone.  Will be set by the populateZonalData method.
    float hhs;
    float avgHHSize;
    float avgWorkers;
    float hhsLow;
    float hhsMed;
    float hhsHi;   // in tahoe, the number of hhs in each income category is forecasted.
    int zoneNumber;

    static boolean debugMarginals = false;

    private ZonalData () {}  //a ZonalData object gets created by the populateZonalData method.
                                        //The zone table must be read in before this class can be used.


    public static void setZoneTable(TableDataSet zoneTable) {
        if(ZonalData.zoneTable == null) {                     //only need to do this once.
            ZonalData.zoneTable = zoneTable;
            hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);
            avgSizePosition = zoneTable.getColumnPosition(AVGSIZE_FIELD);
            laborForcePosition = zoneTable.getColumnPosition(LABORFORCE_FIELD);
            nHHLowIncomePosition = zoneTable.getColumnPosition(NUM_LOW_INCOME_HHS);
            nHHMedIncomePosition = zoneTable.getColumnPosition(NUM_MED_INCOME_HHS);
            nHHHighIncomePosition = zoneTable.getColumnPosition(NUM_HI_INCOME_HHS);
            zoneNumberPosition = zoneTable.getColumnPosition(ZONE_FIELD);
        }
    }

    public static ZonalData populateZonalData(int row){
        if(zoneTable==null) {
            new RuntimeException("You must read in the zoneTable first and set a reference to it using" +
                    " 'setZoneTable");
        }

        ZonalData zd = new ZonalData();

        //these are read in from the file.
        float hhs;
        float avgSize;
        float laborForce;
        float nLowIncome;
        float nMedIncome;
        float nHiIncome;
        int zoneNumber;

        //these are calculated based on the data from the file
        float avgWorkers = -1.0f;



        // assume TableDataSet is indexed by zone
        hhs = zoneTable.getValueAt(row, hhsPosition);
        avgSize = zoneTable.getValueAt(row, avgSizePosition);
        laborForce = zoneTable.getValueAt(row, laborForcePosition);
        nLowIncome = zoneTable.getValueAt(row, nHHLowIncomePosition);
        nMedIncome = zoneTable.getValueAt(row, nHHMedIncomePosition);
        nHiIncome = zoneTable.getValueAt(row, nHHHighIncomePosition);
        zoneNumber = (int) zoneTable.getValueAt(row, zoneNumberPosition);

        //Do some consistency checking
        if ( hhs > 0 && avgSize <= 0){
            throw new RuntimeException("Zone " + zoneNumber + " has " + hhs +
                    " households but persons per occupied unit is " + avgSize);
        }

        if(hhs > 0){
            avgWorkers = laborForce/hhs;
        } else if (hhs <= 0){
            avgWorkers = 0.0f;
            avgSize = 0.0f;
        }

        zd.setHHs(hhs);
        zd.setAvgHHSize(avgSize);
        zd.setAvgWorkers(avgWorkers);
        zd.setHhsLow(nLowIncome);
        zd.setHhsMed(nMedIncome);
        zd.setHhsHi(nHiIncome);

        zd.setZoneNumber(zoneNumber);

        return zd;

    }

    public static RowVector getHHSizeMarginals(ZonalData zd) {
        RowVector marginals;

        // get the marginal distributions of HHs for the zone by applying percentage curves
        HHSizeCurve pctCurve = new HHSizeCurve(zd);

        float[] args = { zd.getAvgHHSize() };
        float[] finalProps = pctCurve.getPercentages(args);
        
        marginals = new RowVector(finalProps);

        if(debugMarginals) logger.info( "Zone: " + zd.getZoneNumber());
        if(debugMarginals) logger.info(String.format("Mean Household Size Control Value = %10.5f", zd.getAvgHHSize()));
        if(debugMarginals) logMarginalsforDebug( "Size Marginals before adjustment:", "size", marginals);

        // adjust the marginal distributions returned from the percentage curves
        // such that the mean of the distributions equals the known mean.
        marginals = pctCurve.adjustAverageToControl(marginals, new RowVector(ModelCategories.getSizeAsFloat()), zd.getAvgHHSize());

        if(debugMarginals) logMarginalsforDebug("Size Marginals after adjustment:", "size", marginals);


        return marginals;
    }

    public static RowVector getWorkerMarginals(ZonalData zd, RowVector HHSizeMarginals) {
        RowVector marginals;

        HHWorkerCurve pctCurve = new HHWorkerCurve(zd);
        float[] args = {zd.getAvgWorkers(), HHSizeMarginals.getValueAt(1),HHSizeMarginals.getValueAt(2), HHSizeMarginals.getValueAt(3)};
        float[] finalProps = pctCurve.getPercentages(args);

        marginals = new RowVector(finalProps);

        if(debugMarginals) logger.info(String.format("Mean Workers Control Value = %10.5f", zd.getAvgWorkers()));
        if(debugMarginals) logMarginalsforDebug("Worker Marginals before adjustment:", "workers", marginals);

        // adjust the marginal distributions returned from the percentage curves
        // such that the mean of the distributions equals the known mean.
        marginals = pctCurve.adjustAverageToControl(marginals,new RowVector(ModelCategories.getWorkersAsFloat()), zd.getAvgWorkers());

        if(debugMarginals) logMarginalsforDebug("Worker Marginals after adjustment:", "workers", marginals);

        return marginals;
    }

    public static RowVector getIncomeMarginals ( ZonalData zd) {

        float[] finalProps = {zd.getHhsLow()/zd.getHHs(), zd.getHhsMed()/zd.getHHs(), zd.getHhsHi()/zd.getHHs()};

        double[] dProps = new double[ModelCategories.HHIncome.values().length];

        // apply truncation and scaling to dProps[] to get values to sum exactly to 1.0;
        double propTot = 0.0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] = Math.max(0.0000001, Math.min(1.0f, finalProps[i]));
            propTot += dProps[i];
        }

        double maxPct = -99999999.9;
        int maxPctIndex = 0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] /= propTot;

            if (dProps[i] > maxPct) {
                maxPct = dProps[i];
                maxPctIndex = i;
            }
        }

        // calculate the percentage for the maximum index percentage curve from the
        // residual difference.
        double residual = 0.0;

        for (int i = 0; i < finalProps.length; i++)
            if (i != maxPctIndex) {
                residual += dProps[i];
            }

        dProps[maxPctIndex] = 1.0 - residual;

        for (int i = 0; i < finalProps.length; i++)
            finalProps[i] = (float) dProps[i];

        return new RowVector(finalProps);
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    public float getHhsLow() {
        return hhsLow;
    }

    public void setHhsLow(float hhsLow) {
        this.hhsLow = hhsLow;
    }

    public float getHhsMed() {
        return hhsMed;
    }

    public void setHhsMed(float hhsMed) {
        this.hhsMed = hhsMed;
    }

    public float getHhsHi() {
        return hhsHi;
    }

    public void setHhsHi(float hhsHi) {
        this.hhsHi = hhsHi;
    }

    public float getHHs() {
           return hhs;
    }

   public float getAvgHHSize() {
       return avgHHSize;
   }

   public float getAvgWorkers() {
       return avgWorkers;
   }

   public void setHHs(float hhs) {
       this.hhs = hhs;
   }

   public void setAvgHHSize(float avgHHSize) {
       this.avgHHSize = avgHHSize;
   }

   public void setAvgWorkers(float avgWorkers) {
       this.avgWorkers = avgWorkers;
   }

    private static void logMarginalsforDebug (String description, String type, RowVector marginals) {
        logger.info(description);
        if(type.equalsIgnoreCase("workers")) {
            logger.info (ModelCategories.HHWorkers.ZERO.toString()+","+ModelCategories.HHWorkers.ONE.toString()+
                                    ","+ ModelCategories.HHWorkers.TWO.toString()+","+ ModelCategories.HHWorkers.THREE_PLUS.toString());
        } else if (type.equalsIgnoreCase("size")) {
            logger.info (ModelCategories.HHSize.ONE.toString()+","+ModelCategories.HHSize.TWO.toString() +
                                  ","+ ModelCategories.HHSize.THREE.toString()+","+ ModelCategories.HHSize.FOUR_PLUS.toString());
        } else if (type.equalsIgnoreCase("income")) {
            logger.info (ModelCategories.HHIncome.LOW.toString()+","+ModelCategories.HHIncome.MED.toString() +
                                  ","+ ModelCategories.HHIncome.HIGH.toString());
        }

        //When using the 'getValueAt' method you must start at '1'
        String marginalString = "";
        for(int i = 1; i<= marginals.size(); i++) {
            if (i != 1) marginalString += ",";
            marginalString += marginals.getValueAt(i);
        }
        logger.info(marginalString);
    }


}
