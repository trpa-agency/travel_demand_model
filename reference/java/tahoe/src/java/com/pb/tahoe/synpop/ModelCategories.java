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

import java.util.ArrayList;

/**
 * ModelCategories is a class that defines the hh categories for Lake Tahoe population.
 * To get the actual hhSize, nWorkers and Income, the user will need to ask the HH object.
 *
 * @author Christi Willison
 * @version 1.0,  Jan 10, 2006
 */
public class ModelCategories {

    public enum HHSize {ONE, TWO, THREE, FOUR_PLUS};

    public enum HHWorkers {ZERO, ONE, TWO, THREE_PLUS};

    public enum HHIncome {LOW, MED, HIGH};   //** low = 0-$34,999, med = $35,000 - 74,999, high = $75,000+ *//

    public static ArrayList getSizeWorkerIncomeCategories (int size, int workers, int income){
        ArrayList < Enum> list = new ArrayList <Enum> ();
        list.add(0, getSizeCategory(size));
        list.add(1, getWorkerCategory(workers));
        list.add(2, getIncomeCategory(income));
        return list;
    }

    public static HHSize getSizeCategory (int size) {

        if(size <= 0) throw new RuntimeException("HH Size of " + size + " is not valid");
        switch (size) {
           case 1:  return HHSize.ONE;
           case 2:  return HHSize.TWO;
           case 3:  return HHSize.THREE;
           default: return HHSize.FOUR_PLUS;       // any postive number >= 4  will return FOUR_PLUS
        }
    }

    public static int getSizeIndex (String sizeCategory) {
        if (sizeCategory.equals("1")) return HHSize.ONE.ordinal();
        else if (sizeCategory.equals("2")) return HHSize.TWO.ordinal();
        else if (sizeCategory.equals("3")) return HHSize.THREE.ordinal();
        else if (sizeCategory.equals("4+")) return HHSize.FOUR_PLUS.ordinal();
        else throw new RuntimeException("HH Size Category " + sizeCategory + " is not valid");

    }

    public static String getOrdinalSizeCategory (int ordinal){
        switch (ordinal) {
           case 0:  return HHSize.ONE.toString();
           case 1:  return HHSize.TWO.toString();
           case 2:  return HHSize.THREE.toString();
           default: return HHSize.FOUR_PLUS.toString();
        }
    }

    public static float[] getSizeAsFloat (){
        //These are used to make sure that the marginals reflect the actual
        //values.  Since the last category is '4+' we have to pick a value
        //that best represents 4+.  This might change.
        return new float[] {1.0f, 2.0f, 3.0f, 4.8f};
    }

    public static String[] getSizeLabels () {
        return new String[] { "1", "2", "3", "4+" };
    }

    public static HHWorkers getWorkerCategory (int nWorkers) {

         if(nWorkers < 0) throw new RuntimeException(nWorkers + " workers in the household  is not valid");
         switch (nWorkers) {
            case 0:  return HHWorkers.ZERO;
            case 1:  return HHWorkers.ONE;
            case 2:  return HHWorkers.TWO;
            default: return HHWorkers.THREE_PLUS;       // any postive number >= 3  will return THREE_PLUS
        }
    }

    public static int getWorkerIndex (String workerCategory) {
        if (workerCategory.equals("0")) return HHWorkers.ZERO.ordinal();
        else if (workerCategory.equals("1")) return HHWorkers.ONE.ordinal();
        else if (workerCategory.equals("2")) return HHWorkers.TWO.ordinal();
        else if (workerCategory.equals("3+")) return HHWorkers.THREE_PLUS.ordinal();
        else throw new RuntimeException("HH Worker Category " + workerCategory + " is not valid");
   }

    public static String getOrdinalWorkerCategory (int ordinal){
        switch (ordinal) {
           case 0:  return HHWorkers.ZERO.toString();
           case 1:  return HHWorkers.ONE.toString();
           case 2:  return HHWorkers.TWO.toString();
           default: return HHWorkers.THREE_PLUS.toString();
        }
    }

    public static float[] getWorkersAsFloat (){
        //These are used to make sure that the marginals reflect the actual
        //values.  Since the last category is '3+' we have to pick a value
        //that best represents 3+.  This might change.
        return new float[] {0.0f, 1.0f, 2.0f, 4.1f };
    }

    public static String[] getWorkerLabels() {
        return new String[] {"0", "1", "2", "3+"};
    }

    public static HHIncome getIncomeCategory (int income) {

        if(income < 35000) return HHIncome.LOW;     //income can be negative
        if(income < 75000) return HHIncome.MED;
        return HHIncome.HIGH;   //will return HIGH for any value over $75,000.
    }

    public static int getIncomeIndex (String incomeCategory) {
        if (incomeCategory.equalsIgnoreCase("low")) return HHIncome.LOW.ordinal();
        else if (incomeCategory.equalsIgnoreCase("med") || incomeCategory.equalsIgnoreCase("medium")) return HHIncome.MED.ordinal();
        else if (incomeCategory.equalsIgnoreCase("high") || incomeCategory.equalsIgnoreCase("hi")) return HHIncome.HIGH.ordinal();
        else throw new RuntimeException("HH Income Category " + incomeCategory + " is not valid");
   }

    public static String getOrdinalIncomeCategory (int ordinal){
        switch (ordinal) {
           case 0:  return HHIncome.LOW.toString();
           case 1:  return HHIncome.MED.toString();
           default: return HHIncome.HIGH.toString();       
        }
    }

    public static String[] getIncomeLabels(){
        return new String[] {HHIncome.LOW.toString(), HHIncome.MED.toString(), HHIncome.HIGH.toString()};
    }

    public static void main(String[] args) {
        System.out.println("There are " + HHIncome.values().length + " income categories: ");
        System.out.println("\t "+HHIncome.LOW.ordinal()+".  " + HHIncome.LOW.toString());
        System.out.println("\t "+HHIncome.MED.ordinal()+".  " + HHIncome.MED.toString());
        System.out.println("\t "+HHIncome.HIGH.ordinal()+".  " + HHIncome.HIGH.toString());

        System.out.println("There are " + HHSize.values().length + " hh size categories: ");
        System.out.println("\t "+HHSize.ONE.ordinal()+".  " + HHSize.ONE.toString());
        System.out.println("\t "+HHSize.TWO.ordinal()+".  " + HHSize.TWO.toString());
        System.out.println("\t "+HHSize.THREE.ordinal()+".  " + HHSize.THREE.toString());
        System.out.println("\t "+HHSize.FOUR_PLUS.ordinal()+".  " + HHSize.FOUR_PLUS.toString());

        System.out.println("There are " + HHWorkers.values().length + " hh worker categories: ");
        System.out.println("\t "+HHWorkers.ZERO.ordinal()+".  " + HHWorkers.ZERO.toString());
        System.out.println("\t "+HHWorkers.ONE.ordinal()+".  " + HHWorkers.ONE.toString());
        System.out.println("\t "+HHWorkers.TWO.ordinal()+".  " + HHWorkers.TWO.toString());
        System.out.println("\t "+HHWorkers.THREE_PLUS.ordinal()+".  " + HHWorkers.THREE_PLUS.toString());

        System.out.println("If HH is size 3, Category is " + ModelCategories.getSizeCategory(3));
        System.out.println("If HH is size 8, Category is " + ModelCategories.getSizeCategory(8));
        System.out.println("If HH is size 4, Category is " + ModelCategories.getSizeCategory(4));

        System.out.println("If number of workers is 3, Category is " + ModelCategories.getWorkerCategory(3));
        System.out.println("If number of workers  is 8, Category is " + ModelCategories.getWorkerCategory(8));
        System.out.println("If number of workers  is 0, Category is " + ModelCategories.getWorkerCategory(0));
        System.out.println("If number of workers  is 1, Category is " + ModelCategories.getWorkerCategory(1));

        System.out.println("If income is 15,000, Category is " + ModelCategories.getIncomeCategory(15000));
        System.out.println("If income is 35,000, Category is " + ModelCategories.getIncomeCategory(35000));
        System.out.println("If income is 115,000, Category is " + ModelCategories.getIncomeCategory(115000));
        System.out.println("If income is 0, Category is " + ModelCategories.getIncomeCategory(0));

    }
}
