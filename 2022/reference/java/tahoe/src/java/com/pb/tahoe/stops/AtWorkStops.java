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
package com.pb.tahoe.stops;

import com.pb.tahoe.structures.Household;
import com.pb.tahoe.structures.TourType;
import com.pb.tahoe.util.HouseholdArrayManager;

import java.util.ResourceBundle;

/**
 * AtWorkStops is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Oct 16, 2006
 */
public class AtWorkStops {

    private StopsHousehold stopsHH;
    private HouseholdArrayManager hhMgr;


    public AtWorkStops (ResourceBundle propertyMap, HouseholdArrayManager hhMgr) {

        this.hhMgr = hhMgr;

        stopsHH = new StopsHousehold ( propertyMap, TourType.AT_WORK_CATEGORY, TourType.getTourTypesForCategory(TourType.AT_WORK_CATEGORY) );

    }



    public void doSfcSlcWork() {

        // get the list of households to be processed
        Household[] hhList = hhMgr.getHouseholds();

        if (hhList == null)
            return;


        for (int i=1; i < hhList.length; i++) {
            stopsHH.atWorkTourSfcSlc ( hhList[i] );
        }

        stopsHH.closeSlfSlcOutputStreams();

        hhMgr.sendResults ( hhList );
    }



    public void doSmcWork() {

        // get the list of households to be processed
        Household[] hhList = hhMgr.getHouseholds();

        if (hhList == null)
            return;


        for (int i=1; i < hhList.length; i++) {
            stopsHH.atWorkTourSmc ( hhList[i] );
        }

        stopsHH.closeSmcOutputStreams();

        hhMgr.sendResults ( hhList );
    }



    public void printTimes ( short tourTypeCategory ) {

        stopsHH.printTimes( tourTypeCategory );

    }
}
