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
 * HHSizeCurve is a class that describes the household size curve
 * that is unique to the Tahoe Basin area.
 *
 * @author Christi Willison
 * @version 1.0,  Dec 9, 2005
 */
public class HHSizeCurve extends PercentageCurve {

    static final float MIN_VALUE = 0.00001f;
    static final float MAX_VALUE = 1.0f;

    ZonalData zd;

    public HHSizeCurve (ZonalData zd) {
        this.zd = zd;
    }

    public float[] getPercentages(float[] args) {
        double a, b, c, d, z;
        double[] dProps = new double[ModelCategories.HHSize.values().length];
        float[] props = new float[ModelCategories.HHSize.values().length];


        float X = args[0];

        //HHSIZE 1
        a = -0.04361;
        b = 0.851266;
        dProps[0] = Math.min( Math.max( (a  + b/X), MIN_VALUE ), MAX_VALUE );

        //HHSIZE 3
        z= 0.010158;
        a = -0.24997;
        b =  0.253014;
        c =  -0.03903;
        dProps[2] = Math.min( Math.max( Math.max(z*X, a+b*X + c*Math.pow(X,2)), MIN_VALUE ), MAX_VALUE );

        //HHSIZE 4+
        a =  0.625236;
        b =  -0.86448;
        c =  0.38858;
        d = -0.04758;
        dProps[3] = Math.min( Math.max( a + b*X + c*Math.pow(X,2) + d*Math.pow(X,3), MIN_VALUE ), MAX_VALUE );


         //HHSIZE 2
        dProps[1] = Math.min( Math.max( (1.0 - dProps[0] - dProps[2] - dProps[3]), MIN_VALUE ), MAX_VALUE );


        // apply scaling to dProps[] to get values to sum exactly to 1.0;
        double propTot = 0.0;
        for (int i=0; i < dProps.length; i++)
            propTot += dProps[i];

       double maxPct = -99999999.9;
        int maxPctIndex = 0;
        for (int i=0; i < dProps.length; i++) {
            dProps[i] /= propTot;
            if (dProps[i] > maxPct) {
                maxPct = dProps[i];
                maxPctIndex = i;
            }
        }

        // calculate the percentage for the maximum index percentage curve from the
        // residual difference.
        double residual = 0.0;
        for (int i=0; i < dProps.length; i++)
            if (i != maxPctIndex)
                residual += dProps[i];

        dProps[maxPctIndex] = 1.0 - residual;

        for(int i=0; i < dProps.length; i++){
            props[i] = (float) dProps[i];
        }

        return props;
    }


}
