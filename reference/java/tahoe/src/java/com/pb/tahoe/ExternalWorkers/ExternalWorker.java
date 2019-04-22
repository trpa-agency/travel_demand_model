package com.pb.tahoe.ExternalWorkers;

import com.pb.tahoe.util.ZonalDataManager;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * User: Chris
 * Date: Mar 6, 2007 - 5:25:34 PM
 */
public class ExternalWorker {

    static Logger logger = Logger.getLogger(ExternalWorker.class);
    private static ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");

    private int ID;
    private int workTaz;
    //Initialize this data so that we know if it has been updated or not
    private int homeTaz = 0;
    private int skimPeriodIn = 0;
    private int skimPeriodOut = 0;

    private ZonalDataManager zdm = ZonalDataManager.getInstance();

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public void setWorkTaz(int workTaz) {
        assert workTaz >= ZonalDataManager.firstInternalZoneNumber;
        this.workTaz = workTaz;
    }

    public int getWorkTaz() {
        return workTaz;
    }

    public void setHomeTaz(int homeTaz) {
        assert homeTaz < ZonalDataManager.firstInternalZoneNumber;
        this.homeTaz = homeTaz;
    }

    public int getHomeTaz() {
        return homeTaz;
    }

    public void setSkimPeriodIn(int skimPeriodIn) {
        assert skimPeriodIn < 5;
        this.skimPeriodIn = skimPeriodIn;
    }

    public int getSkimPeriodIn() {
        return skimPeriodIn;
    }

    public void setSkimPeriodOut(int skimPeriodOut) {
        assert skimPeriodOut < 5;
        this.skimPeriodOut = skimPeriodOut;
    }

    public int getSkimPeriodOut() {
        return skimPeriodOut;
    }

    public int getAltZone (int alt) {
        return ZonalDataManager.zoneAlt[alt];
    }

    public float getExtWDcSizeAlt (int alt) {
        return zdm.externalWorkerSize[ZonalDataManager.zoneAlt[alt]];
    }

    public float getShadowPrice (int alt) {
        return zdm.externalWorkerShadowPrice[ZonalDataManager.zoneAlt[alt]];
    }

    public int getSeason() {
        if (ResourceUtil.getBooleanProperty(rb,"summer")) {
            return 1;
        } else{
            return 0;
        }
    }

    public String toString() {
        String ew = "External Worker #" + ID + "\n";
        ew += "\tOrigin TAZ: " + homeTaz + "\n";
        ew += "\tWork TAZ: " + workTaz + "\n";
        ew += "\tSkim Period Out: " + ExternalWorkerTODAlternative.getTODAlternative(skimPeriodOut,skimPeriodIn);
        return ew.toString();
    }
}
