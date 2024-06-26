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

/**
 * HHWorkerCurve is a class that describes the household worker curve
 * that is unique to the Tahoe Basin area.
 *
 * @author Christi Willison
 * @version 1.0,  Dec 9, 2005
 */
public class HHWorkerCurve extends PercentageCurve {
static final float MIN_VALUE = 0.00001f;
	static final float MAX_VALUE = 1.0f;

    ZonalData zd;

    public HHWorkerCurve (ZonalData zd) {
        this.zd = zd;
    }

    /**
     *  X is the zonal average Workers per Household
     */
    public float[] getPercentages (float[] args) {

    	// apply household workers model to get 0, 1, 2, 3+ worker proportions
        double[] dPcts = new double[ModelCategories.HHWorkers.values().length];
        float[] pcts = new float[ModelCategories.HHWorkers.values().length];


        // Average zonal workers per household is truncated between 0.4 and 2.5.
        double X = (float)Math.max (0.4, Math.min(2.2,args[0]));    //avg workers/per hh
        double P1 = args[1];                                                        //proportion of 1 person Hhs
        double P2 = args[2];                                                         //proportion of 2 person Hhs
        double P3 = args[3];                                                         //proportion of 3 person Hhs

        double a, b, c, d, e;                                                            //coefficients

        a = 1.196081;
        b =	-0.98359;
        c =0.234957;
        d = -0.07594;
        e = -0.15539;
        dPcts[0] = Math.min( Math.max( (a + b*X + c*Math.pow(X,2) + d*P2 + e*P3), MIN_VALUE ), MAX_VALUE );

        a =-0.00234;
        b = 0.20968;
        c = -0.27909;
        d = 0.192956;
        e = 0.177659;
        dPcts[2] = Math.min( Math.max( (a + b*X + c*P1 + d*P2 + e*P3), MIN_VALUE ), MAX_VALUE );

        a = 0.287976;
        b = -0.65507;
        c = 0.58083;
        d = -0.14703;
        e = -0.10885;
        dPcts[3] = Math.min( Math.max( (a + b*X + c*Math.pow(X,2) + d*Math.pow(X,3) + e*P2), MIN_VALUE ), MAX_VALUE );

        dPcts[1] = Math.min( Math.max( (1.0 - (dPcts[0] + dPcts[2] + dPcts[3])), MIN_VALUE ), MAX_VALUE );


		// apply scaling to dPcts[] to get values to sum exactly to 1.0;
		double propTot = 0.0;
		for (int i=0; i < dPcts.length; i++)
			propTot += dPcts[i];

		double maxPct = -99999999.9;
		int maxPctIndex = 0;
		for (int i=0; i < dPcts.length; i++) {
			dPcts[i] /= propTot;
			if (dPcts[i] > maxPct) {
				maxPct = dPcts[i];
				maxPctIndex = i;
			}
		}

		// calculate the percentage for the maximum index percentage curve from the
		// residual difference.
		double residual = 0.0;
		for (int i=0; i < dPcts.length; i++)
			if (i != maxPctIndex)
				residual += dPcts[i];

		dPcts[maxPctIndex] = 1.0 - residual;


        for(int i=0; i< dPcts.length; i++)
            pcts[i] = (float) dPcts[i];

        return pcts;
    }

    

}
