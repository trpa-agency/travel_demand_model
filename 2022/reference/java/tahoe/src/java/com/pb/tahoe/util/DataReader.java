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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.util.ResourceUtil;
import com.pb.models.censusdata.DataDictionary;
import com.pb.models.censusdata.PUMSDataReader;
import com.pb.models.censusdata.PumsGeography;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * DataReader is a singleton class that will do all the reading of input
 * files that the  Tahoe project needs.  In that way I don't have to pass around the
 * ResourceBundle or have read methods throughout the code.  All read
 * methods  will be in this class.
 *
 * @author Christi Willison
 * @version 1.0,  Jan 12, 2006
 */
public class DataReader {

    protected static Logger logger = Logger.getLogger(DataReader.class);
    protected ResourceBundle tahoeResources;

    private static DataReader instance = new DataReader();

    private DataReader (){
        tahoeResources = ResourceUtil.getResourceBundle("tahoe");
    }

    public static DataReader getInstance(){
        return instance;
    }


    public PumsGeography setUpTAZPumaCorrespondence(){
        //Get what we need from the properties file
        //correspondence properties
        String zoneCorrespondenceFile = tahoeResources.getString("taz.correspondence.file");
        logger.debug("Zone Correspondence File: " + zoneCorrespondenceFile);
        String[] columnFormats = ResourceUtil.getArray(tahoeResources,"taz.column.formats");
        String tazColumn = tahoeResources.getString("taz.col.header");
        String stateFIPSColumn = tahoeResources.getString("state.fips.col.header");
        String pumaColumn = tahoeResources.getString("puma.col.header");
        String stateColumn = tahoeResources.getString("state.col.header");

        // First read in the zonal correspondence file and create look-up arrays
        //   that map TAZs to STATE and PUMA FIPS
        PumsGeography modelAreaGeography = new PumsGeography();
        modelAreaGeography.setTazFieldName(tazColumn);
        modelAreaGeography.setStateFipsFieldName(stateFIPSColumn);
        modelAreaGeography.setPumaFieldName(pumaColumn);
        modelAreaGeography.setStateLabelFieldName(stateColumn);

        modelAreaGeography.readZoneIndices(zoneCorrespondenceFile, columnFormats );
        int[][] pumaSetByState = modelAreaGeography.getPumas();

        if(logger.isDebugEnabled()){
            logger.debug("After reading in the Zone Correspondence file, here is a summary: ");
            logger.debug("Number of TAZs: " + modelAreaGeography.getNumberOfZones());
            logger.debug("Number of States: " + modelAreaGeography.getNumberOfStates());
            for(int i=0 ; i<modelAreaGeography.getNumberOfStates(); i++) {
                logger.debug("Number of Pumas in " + modelAreaGeography.getStateLabel(i) +": " + modelAreaGeography.getNumberOfPumas(i));
            }
            for (int i=0; i<pumaSetByState.length; i++){
                logger.debug("Pumas in " + modelAreaGeography.getStateLabel(i) +": ");
                for (int j=0; j<pumaSetByState[i].length; j++){
                    logger.debug("\t" + pumaSetByState[i][j]);
                }
            }
            int indexForZone = modelAreaGeography.getZoneIndex(100);
            int[] statePumaIndices = modelAreaGeography.getStatePumaIndicesFromZoneIndex( indexForZone  );
            logger.debug("TAZ 100 is in state " + modelAreaGeography.getStateFIPS(statePumaIndices[0])
                                 +"(" + modelAreaGeography.getStateLabel(statePumaIndices[0]) +")"
                                 + "- PUMA " + pumaSetByState[statePumaIndices[0]][statePumaIndices[1]]);

        }

        return modelAreaGeography;

    }

    public TableDataSet[] readPUMSData(String stateLabel, int[] pumaSet) {
        TableDataSet[] pumsTables = new TableDataSet[2];  // there will always be a household (element [0])
                                                                                      //  and a person (element [1] ) table returned.

        String hhDataFile = tahoeResources.getString(stateLabel.toUpperCase() + ".pums.hh.file");
        String psnDataFile = tahoeResources.getString(stateLabel.toUpperCase() + ".pums.psn.file");

        try {
            CSVFileReader reader = new CSVFileReader();
            pumsTables[0] = reader.readFile(new File(hhDataFile));
        } catch (IOException e) {
            logger.warn("Error reading Travel Party file " + hhDataFile);
        }
        try {
            CSVFileReader reader = new CSVFileReader();
            pumsTables[1] = reader.readFile(new File(psnDataFile));
        } catch (IOException e) {
            logger.warn("Error reading Travel Party file " + psnDataFile);
        }        
  		
        		
        
//		CSVFileWriter fileWriter = new CSVFileWriter();
//		try {
//			fileWriter.writeFile(pumsTables[0],new File(stateLabel + "_HH_TABLE.csv"));
//		} catch (IOException e) {
//            throw new RuntimeException("Error writing file " +  stateLabel + "_HH_TABLE.csv", e);
//        }
//		CSVFileWriter fileWriter2 = new CSVFileWriter();
//		try {
//			fileWriter2.writeFile(pumsTables[1],new File(stateLabel + "_PER_TABLE.csv"));
//		} catch (IOException e) {
//            throw new RuntimeException("Error writing file " +  stateLabel + "_PER_TABLE.csv", e);
//        }
		
        if(logger.isDebugEnabled()){
            CSVFileWriter writer = new CSVFileWriter();
            try {
                writer.writeFile(pumsTables[0], new File("/Temp/" + stateLabel + "_HH_TABLE.csv"),new GeneralDecimalFormat("0.#####E0",10000000,.01 ));
                writer.writeFile(pumsTables[1], new File("/Temp/" + stateLabel + "_PER_TABLE.csv"),new GeneralDecimalFormat("0.#####E0",10000000,.01 ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

         return  pumsTables;

    }

    public AlphaToBeta setUpTAZCensusTractCorrespondence (){
        String zoneCorrespondenceFile = tahoeResources.getString("taz.correspondence.file");
        String tazColumn = tahoeResources.getString("taz.col.header");
        String censusTractColumn = tahoeResources.getString("census.tract.col.header");
        return new AlphaToBeta(new File (zoneCorrespondenceFile), tazColumn, censusTractColumn);
    }

    public TableDataSet readSeedTable() {
        String[] formats =  {"NUMBER", "NUMBER", "NUMBER", "STRING", "STRING", "STRING", "NUMBER"};
        return loadTableDataSet ( "ctpp.summary.file", formats );
    }

    public TableDataSet readMergedTazTable() {
        return loadTableDataSet("merged.taz.data.file");
    }

    public TableDataSet readSocioEconomicTable() {
        return loadTableDataSet("socio.economic.data.file");
    }


    /**
     *  If you pass in the name of the property file entry that has the
     * full path to the file (i.e. file.to.read = /dir/file would mean you pass in "file.to.read")
     * the DataReader will try to load the Table.
     * @param fileProperty
     * @return TableDataSet
     */
    private TableDataSet loadTableDataSet(String fileProperty, String[] formats) {
        TableDataSet table = null;

        String file = null;
        try {
            file = ResourceUtil.getProperty(tahoeResources, fileProperty);
            if(file == null){
                logger.fatal("Property '" + fileProperty + "' could not be found in ResourceBundle");
                throw new RuntimeException("Property File does not contain " + fileProperty);
            }else {
                CSVFileReader reader = new CSVFileReader();
                if(formats == null) {
                    table =  reader.readFile(new File(file));
                } else {
                    table = reader.readFileWithFormats(new File(file), formats);
                }
            }
        } catch (IOException e) {
            logger.fatal("Can't find " + file, e);
            new RuntimeException(e);
        }

        return table;
    }

    /**
     * If you pass in the name of the property file entry that has the
     * path to the file (i.e. input.file.path = /dir/ would mean you pass in "input.file.path")
     * the DataReader will try to load the Table.  Path entries should end with a slash.
     * Method does NOT check for this. (but it should)
     * @param pathProperty
     * @param fileName
     * @return TableDataSet
     */
    public TableDataSet loadTableDataSet(String pathProperty, String fileName) {

        String path = null;
        try {
            path = ResourceUtil.getProperty(tahoeResources, pathProperty);
            if(path == null){
                logger.fatal("Property '" + pathProperty + "' could not be found in ResourceBundle");
                throw new RuntimeException("Property File does not contain " + pathProperty);
            }else {
                CSVFileReader reader = new CSVFileReader();
                return reader.readFile(new File(path + fileName));
            }
        } catch (IOException e) {
            logger.fatal("Can't find " + (path + fileName));
            e.printStackTrace();
        }
        return null;
    }

    /**
     * If you pass in the name of the file entry that has the
     * the DataReader will try to load the Table.
     * @param fileProperty
     * @return TableDataSet
     */
    public TableDataSet loadTableDataSet(String fileProperty) {

        String path = null;
        path = ResourceUtil.getProperty(tahoeResources, fileProperty);
        if(path == null){
            logger.fatal("Property '" + fileProperty + "' could not be found in ResourceBundle");
            throw new RuntimeException("Property File does not contain " + fileProperty);
        }else {
            try {
                CSVFileReader reader = new CSVFileReader();
                return reader.readFile(new File(path));
            } catch (IOException e) {
                logger.fatal("Can't find " + (path));
                throw new RuntimeException(e);
            }
        }


    }

}
