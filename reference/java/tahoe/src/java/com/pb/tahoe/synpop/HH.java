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

import com.pb.tahoe.util.ChoiceModelApplication;
import com.pb.tahoe.util.ZonalDataManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * HH is a class that holds
 *
 * @author Christi Willison
 * @version 1.0,  Jan 13, 2006
 */
public class HH implements Cloneable {
    protected static Logger logger = Logger.getLogger(HH.class);

    private int hhNumber;
    private int zoneNumber;

    public int[] attribs;       //this array holds the "actual" values, the category variable holds the category based on those values.
    public String[] attribLabels;
    public ArrayList <Enum> categories;  //the array list holds the category based on the actual value.  (i.e. if HHSize =5, then category is FOUR_PLUS)

    public int[] personTypes;
    int retiredPersonPresent;
    public boolean adjusted;


    public HH () {

        this.attribLabels = new String[3];
        attribLabels[0] = "SIZE";
        attribLabels[1] = "WORKERS";
        attribLabels[2] = "INCOME";

        this.attribs = new int[3];
        this.categories = new ArrayList <Enum> ();
    }

    public void setHhNumber(int hhNumber) {
        this.hhNumber = hhNumber;
    }

    public int getHHNumber () {
        return hhNumber;
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    /**
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the origin for this tour
     */
    public int getInitialOriginWalkSegment (int taz) {
        double[] proportions = new double[ZonalDataManager.WALK_SEGMENTS];
        for (int i=0; i < ZonalDataManager.WALK_SEGMENTS; i++)
            proportions[i] = ZonalDataManager.getWalkPct(i, taz);
        return ChoiceModelApplication.getMonteCarloSelection(proportions);
    }

    public void setRetiredPersonPresent(boolean retired){
        if(retired) this.retiredPersonPresent = 1;
        else this.retiredPersonPresent = 0;
    }

    public int isRetiredPersonPresent(){
        return retiredPersonPresent;
    }

    public boolean isAdjusted() {
        return adjusted;
    }

    public void setAdjusted(boolean adjusted) {
        this.adjusted = adjusted;
    }

    public Object clone() throws CloneNotSupportedException {
        HH o = null;
        try{
            o= (HH) super.clone();
        }catch(CloneNotSupportedException e){
            logger.fatal("Error: HH can't clone");
            System.exit(1);
        }
        //clone references
        if(this.attribLabels!=null){
            o.attribLabels = new String[this.attribLabels.length];
            System.arraycopy(this.attribLabels,0,o.attribLabels,0,this.attribLabels.length);
        }

        if(this.attribs!=null){
            o.attribs = new int[this.attribs.length];
            System.arraycopy(this.attribs,0,o.attribs,0,this.attribs.length);
        }
        if(this.categories!=null){
            o.categories = new ArrayList <Enum> ();
            for (Enum categories: this.categories) o.categories.add(categories);
        }
        if(this.personTypes!=null){
            o.personTypes = new int[this.personTypes.length];
            System.arraycopy(this.personTypes,0,o.personTypes,0,this.personTypes.length);
        }

        o.retiredPersonPresent = this.retiredPersonPresent;

        return o;


    }
}
