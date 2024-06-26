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
package com.pb.tahoe.util;

import com.pb.common.datafile.TableDataSet;
import com.pb.tahoe.synpop.ZonalData;

import java.util.ArrayList;
import java.util.List;

/**
 * UrbanTypeCalculator is a class that calculates the employment and population
 * density of each zone in the region and assigns the zone a value of 1, 2 or 3 depending
 * on that density.
 *
 * landType = 1 if density < 600 (rural)
 * landType = 2 if 600 <= density < 7500
 * landType = 3 if density >= 7500 (urban)
 *
 * @author Christi Willison
 * @version 1.0,  Jul 4, 2006
 */
public class UrbanTypeCalculator {

    //TODO: check these definitions with Chris
    public static final int NUM_URBTYPES = 4;   //1=rural, 2=suburban, 3=urban, 4=external (undefined)

    private UrbanTypeCalculator(){};

    public static void createUrbTypeTable(){
        TableDataSet urbTypeTable = calculateUrbanType();
        writeUrbTypeFile(urbTypeTable);
    }


    /**
     * This will calculate the urban types of the zone based on population
     * and employment densitys for the internal zones.  For external zones,
     * the urbtype will automatically be set to 4.
     * @return TableDataSet
     */
    private static TableDataSet calculateUrbanType (){
        DataReader reader = DataReader.getInstance();
        TableDataSet zoneMappingsFile = reader.loadTableDataSet("taz.correspondence.file");
        int areaColumn = zoneMappingsFile.getColumnPosition(ZonalDataManager.SQ_AREA_IN_MILES);

        TableDataSet socioEconFile = reader.readSocioEconomicTable();
        socioEconFile.buildIndex(socioEconFile.getColumnPosition(ZonalData.ZONE_FIELD));

        List<String> emp_headers = new ArrayList<String>();
        String[] colHeaders = socioEconFile.getColumnLabels();
        for(String col: colHeaders){
            if(col.contains("emp"))
                emp_headers.add(col);
        }

        float[] popDensity = new float[zoneMappingsFile.getRowCount()];
        float[] empDensity = new float[zoneMappingsFile.getRowCount()];
        int[] tazNums = zoneMappingsFile.getColumnAsInt(1);

        float[] sum = new float[zoneMappingsFile.getRowCount()];
        int[] landType = new int[zoneMappingsFile.getRowCount()];

        float avgHhSize = -1.0f;
        float nOccUnits = -1.0f;
        int zone = -1;
        float areaOfZone = -1.0f;
        for(int r=1; r <= zoneMappingsFile.getRowCount(); r++){
            zone = (int) zoneMappingsFile.getValueAt(r, ZonalData.ZONE_FIELD);
         //   if (zone >= ZonalDataManager.firstInternalZoneNumber) {
            try{
                nOccUnits = socioEconFile.getIndexedValueAt(zone, ZonalData.HHS_FIELD);
                avgHhSize = socioEconFile.getIndexedValueAt(zone, ZonalData.AVGSIZE_FIELD);
                areaOfZone = zoneMappingsFile.getValueAt(r, areaColumn);
                if(areaOfZone <= 0) areaOfZone = .001f;
                popDensity[r-1] = ((float)Math.ceil(nOccUnits * avgHhSize))/areaOfZone;
                for(String empCol: emp_headers){
                    empDensity[r-1] += socioEconFile.getIndexedValueAt(zone, empCol);
                }
                empDensity[r-1] /= areaOfZone;
                sum[r-1] = empDensity[r-1] + popDensity[r-1];

                if(sum[r-1] < 600 ) landType[r-1] = 1;
                else if (sum[r-1] < 7500) landType[r-1] = 2;
                else landType[r-1] = 3;
            //} else {
            } catch (Exception e) {
                //Zone is not in the socio file => zone is external so set everything to -999
                popDensity[r-1] = -999;
                empDensity[r-1] = -999;
                sum[r-1] = -999;
                landType[r-1] =4;
            }

        }
         TableDataSet table = new TableDataSet();
        table.appendColumn(tazNums, "taz");
        table.appendColumn(popDensity, "population_density" );
        table.appendColumn(empDensity, "employment_density");
        table.appendColumn(sum, "emp_plus_pop");
        table.appendColumn(landType, "urbtype");

        return table;

    }

    private static void writeUrbTypeFile(TableDataSet table){

        DataWriter writer = DataWriter.getInstance();
        writer.writeOutputFile("urban.type.file", table);
    }

    public static void main(String[] args) {
        UrbanTypeCalculator.calculateUrbanType();
    }



}
