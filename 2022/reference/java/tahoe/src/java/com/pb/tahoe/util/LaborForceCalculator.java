package com.pb.tahoe.util;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.synpop.ZonalData;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;

/**
 * This class calculates the number of workers per zone for the Tahoe AB model.
 * Because of the constraints of doing this at this point of development, this does the following:
 *      1) Reads the soicioeconomic file
 *      2) Calculats the labor force
 *      3) Adds this to the socioeconomic data, and writes it to a generally used file
 *
 * This whole thing is real messy becuase it should have been done a while ago and it is too late to get it real clean.
 * The urban type calculation is essentially reproduced here, but it is easier than to rewrite all of the methods
 * to handle a socioecon file without a labor type field.
 *
 * User: Chris
 * Date: Jun 12, 2007 - 12:43:01 PM
 */
public class LaborForceCalculator {

    private static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
    private static final float POPULATION_COEFFICIENT = 0.3534f;
    private static final float MEDIUM_INCOME_HH_COEFFICIENT = 0.8426f;
    private static final float HIGH_INCOME_HH_COEFFICIENT = 0.01124f;
    private static final float RURAL_HH_COEFFICIENT = 0.07537f;
    private static final float SUBURBAN_HH_COEFFICIENT = 0.1070f;
    private static final float URBAN_HH_COEFFICIENT = 0.1391f;

    public static void generateLaborForce() {
        DataWriter writer = DataWriter.getInstance();
        writer.writeOutputFile("socio.economic.data.file", calculateLaborForce());
    }

    private static TableDataSet calculateLaborForce() {

        TableDataSet origSocioData = TableDataSetLoader.loadTableDataSet(rb,"input.socio.economic.data.file");
        List<String> emp_headers = new ArrayList<String>();
        String[] colHeaders = origSocioData.getColumnLabels();
        for(String col: colHeaders){
            if(col.contains("emp"))
                emp_headers.add(col);
        }

        DataReader reader = DataReader.getInstance();
        TableDataSet zoneMappingsFile = reader.loadTableDataSet("taz.correspondence.file");
        int areaColumn = zoneMappingsFile.getColumnPosition(ZonalDataManager.SQ_AREA_IN_MILES);
        zoneMappingsFile.buildIndex(zoneMappingsFile.getColumnPosition(ZonalDataManager.TAZ));
        //set up new column
        float[] laborForce = new float[origSocioData.getRowCount()];
        //loop over all of the rows in the original socioeconomic file
        for (int i = 1; i <= origSocioData.getRowCount(); i++) {
            float modeledLaborForce = origSocioData.getValueAt(i,ZonalData.HHS_FIELD) *
                    origSocioData.getValueAt(i,ZonalData.AVGSIZE_FIELD) * POPULATION_COEFFICIENT +
                    origSocioData .getValueAt(i,ZonalData.NUM_MED_INCOME_HHS) * MEDIUM_INCOME_HH_COEFFICIENT +
                    origSocioData .getValueAt(i,ZonalData.NUM_HI_INCOME_HHS) * HIGH_INCOME_HH_COEFFICIENT;
            int zone = (int) origSocioData.getValueAt(i,ZonalData.ZONE_FIELD);

            //determine urban type
            float nOccUnits = origSocioData.getValueAt(i, ZonalData.HHS_FIELD);
            float avgHhSize = origSocioData.getValueAt(i, ZonalData.AVGSIZE_FIELD);
            float areaOfZone = zoneMappingsFile.getIndexedValueAt(zone,areaColumn);
            if(areaOfZone <= 0) areaOfZone = .001f;
            float density = ((float)Math.ceil(nOccUnits * avgHhSize));
            for(String empCol: emp_headers){
                density += origSocioData.getValueAt(i, empCol);
            }
            density /= areaOfZone;
            if (density < 600) {
                //rural
                modeledLaborForce += origSocioData.getValueAt(i,ZonalData.HHS_FIELD) * RURAL_HH_COEFFICIENT;
            } else if (density < 7500) {
                //suburban
                modeledLaborForce += origSocioData.getValueAt(i,ZonalData.HHS_FIELD) * SUBURBAN_HH_COEFFICIENT;
            } else {
                //urban
                modeledLaborForce += origSocioData.getValueAt(i,ZonalData.HHS_FIELD) * URBAN_HH_COEFFICIENT;
            }
            laborForce[i-1] = Math.round(modeledLaborForce);
        }
        origSocioData.appendColumn(laborForce,ZonalData.LABORFORCE_FIELD);
        return origSocioData;
    }
}
