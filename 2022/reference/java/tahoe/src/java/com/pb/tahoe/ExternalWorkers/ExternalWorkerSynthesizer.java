package com.pb.tahoe.ExternalWorkers;

import com.pb.tahoe.util.HouseholdArrayManager;
import com.pb.tahoe.util.ZonalDataManager;
import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.Tour;
import com.pb.tahoe.structures.TourType;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.SeededRandom;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * User: Chris
 * Date: Mar 6, 2007 - 5:10:14 PM
 */
public class ExternalWorkerSynthesizer {

    static Logger logger = Logger.getLogger(ExternalWorkerSynthesizer.class);

    private ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

    HouseholdArrayManager ham;
    ZonalDataManager zdm = ZonalDataManager.getInstance();

    private int externalWorkers = 0;

    public ExternalWorkerSynthesizer(HouseholdArrayManager ham) {
        this.ham = ham;
    }

    public TableDataSet buildExternalWorkersPop() {
        HashMap<Integer,Integer> employmentToFill = getZonalEmploymentToFill();
        //set number of external workers in zonal data
        float[][] externalWorkersData = new float[externalWorkers][2];
        int counter = 0;
        for (Integer i : employmentToFill.keySet()) {
            for (int j = 0; j < employmentToFill.get(i); j++) {
                externalWorkersData[counter][1] = i;
                externalWorkersData[counter++][0] = counter;
            }
        }

        String[] headings = {ExternalWorkerArrayManager.EXTERNAL_WORKER_ID_FIELD,
                ExternalWorkerArrayManager.EXTERNAL_WORKER_WORK_TAZ_FIELD};
        return TableDataSet.create(externalWorkersData,headings);
    }

    private HashMap<Integer,Integer> getZonalEmploymentToFill() {
        HashMap<Integer,Integer> filledEmployment = new HashMap<Integer,Integer>();
        HashMap<Integer,Integer> employmentToFill = new HashMap<Integer,Integer>();
        int overfilledEmployment = 0;

        //Figure out employment that is filled
        for (Household hh : ham.getHouseholds()) {
            //skip over first "household" or households without mandatory tours
            if (hh==null || hh.getMandatoryTours() == null) continue;
            for (Tour t : hh.getMandatoryTours()) {
                if (t.getTourType() == TourType.WORK) {
                    int dtaz = t.getDestTaz();
                    if (filledEmployment.containsKey(dtaz)) {
                        filledEmployment.put(dtaz,filledEmployment.get(dtaz) + 1);
                    } else {
                        filledEmployment.put(dtaz,1);
                    }
                }
            }
        }

        //Figure out employment to fill, and overfilled employment
        TableDataSet zonalData = zdm.getZonalTableDataSet();
        int[] zones = zonalData.getColumnAsInt(zonalData.getColumnPosition(ZonalDataManager.TAZ));
        int totalEmployment = 0;
        for (int i = 1; i < zones.length; i++) {
            //don't do anything with external zones
            if (zones[i] < ZonalDataManager.firstInternalZoneNumber) continue;
            int employment = (int) ZonalDataManager.zonalEmployment[zones[i]];
            totalEmployment += employment;
            if (employment > 0) {
                if (filledEmployment.containsKey(zones[i])) {
                    int femployment = filledEmployment.get(zones[i]);
                    if (employment > femployment) {
                        employmentToFill.put(zones[i],employment - femployment);
                    } else {
                        overfilledEmployment += femployment - employment;
                    }
                } else {
                    employmentToFill.put(zones[i],employment);
                }
            }
        }

        //create mapping of each employment spot to a zone
        ArrayList<Integer> employmentSpots = new ArrayList<Integer>();
        for (Integer i : employmentToFill.keySet()) {
            for (int j = 0; j < employmentToFill.get(i); j++) {
                employmentSpots.add(i);
            }
        }
        //Remove overfilled employment randomly from employment to fill, as well as the percentage of employment to leave empty
        overfilledEmployment = overfilledEmployment + (int) Math.round(totalEmployment*ResourceUtil.getDoubleProperty(rb,"unfilled.employment.percentage"));
        for (int i = 0; i < overfilledEmployment; i++) {
            //randomly select an employment spot to drop
            int removeZone = employmentSpots.remove((int) Math.floor(SeededRandom.getRandom()*employmentSpots.size()));
            employmentToFill.put(removeZone,employmentToFill.get(removeZone) - 1);
        }

        externalWorkers = employmentSpots.size();
        return employmentToFill;
    }



}
