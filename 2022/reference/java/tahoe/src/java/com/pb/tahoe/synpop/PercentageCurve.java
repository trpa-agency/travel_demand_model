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

import com.pb.common.math.MathUtil;
import com.pb.common.matrix.RowVector;

/**
 * PercentageCurve is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Feb 9, 2006
 */
public abstract class PercentageCurve {

    public PercentageCurve () {
    }


    public abstract float[] getPercentages (float[] args);


    public RowVector adjustAverageToControl (RowVector seedProportions, RowVector seedCategoryValues, float controlAvg) {

        float epsilon = 0.000001f;
        float[] proportions = seedProportions.copyValues1D();
        float[] newProportions = seedProportions.copyValues1D();
        float[] categories = seedCategoryValues.copyValues1D();
        float lambda = 0.0f;
        float gap = 100.0f;
        float newAvg;
        int zeroCategory = 0;

//        System.out.println("Control Avg: " + controlAvg);
//        System.out.println("Before adjusting to control average, the proportions are: ");
//       String out = "";
//        for (int i = 0; i < proportions.length; i++) {
//            if(i!=0) out+=",";
//            out += proportions[i];
//        }
//        System.out.println(out);

        if (controlAvg == 0.0f) {
            // if the control average is 0, handle the special case
            for (int i=0; i < newProportions.length; i++) {
                newProportions[i] = 0.0f;

                if (categories[i] == 0.0f)
                    zeroCategory = i;
            }
            newProportions[zeroCategory] = 1.0f;
        }
        else if (controlAvg > categories[categories.length-1]) {
            // if the control average is greater than the largest category value, handle the special case
            for (int i=0; i < newProportions.length; i++)
                newProportions[i] = 0.0f;

            newProportions[categories.length-1] = 1.0f;
        }
        else {
            // otherwise, apply the adjustment algorithm
//            System.out.println("Gap: " + gap);
            while (gap > epsilon) {

                newProportions = calculateNewProportions (proportions, categories, lambda);

                newAvg = calculateNewAverage (newProportions, categories);

                gap = calculateAbsoluteGap (newAvg, controlAvg);

//                System.out.println("Gap: " + gap);

                lambda = calculateNewLambda (lambda, newAvg, controlAvg);
            }
        }


        return ( new RowVector(newProportions) );
    }

    private static float[] calculateNewProportions (float[] oldProportions, float[] categories, float lambda) {

        float newProportions[] = new float[oldProportions.length];


        double denom = 0.0f;
//        System.out.println(("denom: " + denom));
        for (int i=0; i < oldProportions.length; i++){
            denom += oldProportions[i]* MathUtil.exp(lambda*categories[i]);
//            System.out.println("oldProportions[" + i + "]: " + oldProportions[i]);
//            System.out.println("Lambda: " + lambda);
//            System.out.println("categories["+ i + "]: " + categories[i]);
//            System.out.println("MathUtil.exp(lambda*categories[i]): " +MathUtil.exp(lambda*categories[i]));
//            System.out.println(("denom: " + denom));
        }

        if(denom == 0) throw new RuntimeException("About to divide by zero in 'calcNewProportions' method");

        for (int i=0; i < oldProportions.length; i++)
            newProportions[i] = (float)(oldProportions[i]*MathUtil.exp(lambda*categories[i])/denom);


        return newProportions;
    }


    private static float calculateNewAverage (float[] newProportions, float[] categories) {

        float average = 0.0f;
        for (int i=0; i < newProportions.length; i++)
            average += newProportions[i]*categories[i];

        return average;
    }


    private static float calculateAbsoluteGap (float newAvg, float controlAvg) {

        return ( Math.abs( newAvg - controlAvg ) );
    }


    private static float calculateNewLambda (float lambda, float newAvg, float controlAvg) {

        return ( lambda + (controlAvg - newAvg)/controlAvg );
    }
}
