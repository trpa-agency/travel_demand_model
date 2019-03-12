package com.pb.tahoe.ExternalWorkers;

import org.apache.log4j.Logger;

/**
 * User: Chris
 * Date: Mar 12, 2007 - 11:49:38 AM
 */
public enum ExternalWorkerTODAlternative {
    AMAM(1,1),
    AMMD(1,3),
    AMPM(1,2),
    AMLN(1,4),
    MDAM(3,1),
    MDMD(3,3),
    MDPM(3,2),
    MDLN(3,4),
    PMAM(2,1),
    PMMD(2,3),
    PMPM(2,2),
    PMLN(2,4),
    LNAM(4,1),
    LNMD(4,3),
    LNPM(4,2),
    LNLN(4,4);

    protected static Logger logger = Logger.getLogger(ExternalWorkerTODAlternative.class);

    private int alternativeNumber;
    private int outSkimPeriod;
    private int inSkimPeriod;

    private ExternalWorkerTODAlternative(int outSkimPeriod, int inSkimPeriod) {
        this.inSkimPeriod = inSkimPeriod;
        this.outSkimPeriod = outSkimPeriod;
        alternativeNumber = this.ordinal() + 1;
    }

    public int getAlternativeNumber() {
        return alternativeNumber;
    }

    public int getOutSkimPeriod() {
        return outSkimPeriod;
    }

    public int getInSkimPeriod() {
        return inSkimPeriod;
    }

    public static ExternalWorkerTODAlternative getTODAlternative(int alternative) {
        assert (alternative > 0 && alternative <= ExternalWorkerTODAlternative.values().length);
        for (ExternalWorkerTODAlternative ewTODAlt : ExternalWorkerTODAlternative.values()) {
            if (ewTODAlt.alternativeNumber == alternative)
                return ewTODAlt;
        }
        //The assertion assures we should never get here
        return AMPM;
    }

    public static ExternalWorkerTODAlternative getTODAlternative(int outSkimPeriod,int inSkimPeriod) {
        assert (outSkimPeriod > 0 && outSkimPeriod < 5);
        assert (inSkimPeriod > 0 && inSkimPeriod < 5);
        for (ExternalWorkerTODAlternative ewTODAlt : ExternalWorkerTODAlternative.values()) {
            if (ewTODAlt.getOutSkimPeriod() == outSkimPeriod && ewTODAlt.getInSkimPeriod() == inSkimPeriod)
                return ewTODAlt;
        }
        return AMPM;
    }

}
