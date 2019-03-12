package com.pb.tahoe.auto_ownership;

import com.pb.common.model.Alternative;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;

/**
 * This class is used for ...
 *
 * @author Chris Frazier
 * @version Feb 16, 2006
 *          Created by IntelliJ IDEA.
 */
public class AutoOwnershipLM {

    LogitModel root;


    public AutoOwnershipLM() {

        // define the structure of this simple multinomial logit model
        define();

    }


    public void attachUtilities (double[] utilities) {

        Alternative alt;

        for (int i=0; i < 5; i++) {
            alt = root.getAlternative(i);

            alt.setAvailability( utilities[i] > -99.0 );
            alt.setUtility (utilities[i]);
        }
    }



    public double[] getProportions () {

        //calculate the logsum at the root level
        root.writeUtilityHeader();
        root.getUtility();

        //calculate probabilities
        root.writeProbabilityHeader();
        root.calculateProbabilities();

        double[] proportions = root.getProbabilities();
        double[] newProportions = new double[5];
        System.arraycopy(proportions, 0, newProportions, 0, proportions.length);

        return ( newProportions );

    }

    public int getAlternativeNumber(){
        ConcreteAlternative ca = (ConcreteAlternative)root.chooseElementalAlternative();
        return (Integer) ca.getAlternativeObject();
    }


    private void define () {

        // define root level
        this.root = new LogitModel ("root", 5);
	    this.root.setDebug (false);

        // define level 1 alternatives
        ConcreteAlternative auto0  = new ConcreteAlternative ("0 auto", 1);
        ConcreteAlternative auto1  = new ConcreteAlternative ("1 auto", 2);
        ConcreteAlternative auto2  = new ConcreteAlternative ("2 auto", 3);
        ConcreteAlternative auto3  = new ConcreteAlternative ("3 auto", 4);
        ConcreteAlternative auto4p = new ConcreteAlternative ("4+ auto", 5);

        // add alternatives to root level nest to create simple multinomial logit model
        root.addAlternative (auto0);
        root.addAlternative (auto1);
        root.addAlternative (auto2);
        root.addAlternative (auto3);
        root.addAlternative (auto4p);

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();
    }
}
