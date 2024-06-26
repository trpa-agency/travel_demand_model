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
package com.pb.tahoe.daily_activity_pattern_test;

import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.daily_activity_pattern.DAPModel;
import com.pb.tahoe.structures.PersonType;
import com.pb.tahoe.util.DataWriter;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * WriteSynPopP is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Apr 4, 2006
 */
public class DAPTest {

    protected static Logger logger = Logger.getLogger(DAPTest.class);

    public static void runPatternModelForAllPersonTypes(ResourceBundle rb){
        DataWriter writer = DataWriter.getInstance();
        String[] cmdargs = {PersonType.PRESCHOOLER, PersonType.PREDRIVER, PersonType.DRIVER, PersonType.FULL_TIME,PersonType.PART_TIME, PersonType.NON_WORKER};
        DAPModel model = new DAPModel(rb);

        for (String cmdarg : cmdargs) {
            String fieldInOutput = null;
            if(cmdarg.equals(PersonType.PRESCHOOLER)){
                fieldInOutput = DataWriter.PRESCHOOL_FIELD;
            }else if(cmdarg.equals(PersonType.PREDRIVER)){
                fieldInOutput = DataWriter.SCHOOLPRED_FIELD;
            }else if(cmdarg.equals(PersonType.DRIVER)){
                fieldInOutput = DataWriter.SCHOOLDRIV_FIELD;
            }else if(cmdarg.equals(PersonType.FULL_TIME)){
                fieldInOutput = DataWriter.WORKERS_F_FIELD;
            }else if(cmdarg.equals(PersonType.PART_TIME)){
                fieldInOutput = DataWriter.WORKERS_P_FIELD;
            }else if(cmdarg.equals(PersonType.NON_WORKER)){
                fieldInOutput = DataWriter.NONWORKERS_FIELD;
            }
            model.runDailyActivityPatternChoice(cmdarg, fieldInOutput);
            if(cmdarg.equals(PersonType.PRESCHOOLER)){
                model.writePatternChoicesToFile();
            }

        }
        model.writePatternChoicesToFile();
        writer.writeSynPopPTableDataSet(model.hhTable);
    }

    public static void main(String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        DataWriter writer = DataWriter.getInstance();
        if(args.length == 0) {  //just run everyone in order.
            String[] cmdargs = {PersonType.PRESCHOOLER, PersonType.PREDRIVER, PersonType.DRIVER, PersonType.FULL_TIME,PersonType.PART_TIME, PersonType.NON_WORKER};
            DAPModel model = new DAPModel(rb);

            for (String cmdarg : cmdargs) {
                String fieldInOutput = null;
                if(cmdarg.equals(PersonType.PRESCHOOLER)){
                    fieldInOutput = DataWriter.PRESCHOOL_FIELD;
                }else if(cmdarg.equals(PersonType.PREDRIVER)){
                    fieldInOutput = DataWriter.SCHOOLPRED_FIELD;
                }else if(cmdarg.equals(PersonType.DRIVER)){
                    fieldInOutput = DataWriter.SCHOOLDRIV_FIELD;
                }else if(cmdarg.equals(PersonType.FULL_TIME)){
                    fieldInOutput = DataWriter.WORKERS_F_FIELD;
                }else if(cmdarg.equals(PersonType.PART_TIME)){
                    fieldInOutput = DataWriter.WORKERS_P_FIELD;
                }else if(cmdarg.equals(PersonType.NON_WORKER)){
                    fieldInOutput = DataWriter.NONWORKERS_FIELD;
                }
                model.runDailyActivityPatternChoice(cmdarg, fieldInOutput);
                if(cmdarg.equals(PersonType.PRESCHOOLER)){
                    model.writePatternChoicesToFile();
                }
            }
            model.writePatternChoicesToFile();
            writer.writeSynPopPTableDataSet(model.hhTable);

        } else {   //user is passing in personType to run
            DAPModel model = new DAPModel(rb);
            String fieldInOutput = null;
            if(args[0].equals(PersonType.PRESCHOOLER)){
                fieldInOutput = DataWriter.PRESCHOOL_FIELD;
            }else if(args[0].equals(PersonType.PREDRIVER)){
                fieldInOutput = DataWriter.SCHOOLPRED_FIELD;
            }else if(args[0].equals(PersonType.DRIVER)){
                fieldInOutput = DataWriter.SCHOOLDRIV_FIELD;
            }else if(args[0].equals(PersonType.FULL_TIME)){
                fieldInOutput = DataWriter.WORKERS_F_FIELD;
            }else if(args[0].equals(PersonType.PART_TIME)){
                fieldInOutput = DataWriter.WORKERS_P_FIELD;
            }else if(args[0].equals(PersonType.NON_WORKER)){
                fieldInOutput = DataWriter.NONWORKERS_FIELD;
            }

            model.runDailyActivityPatternChoice(args[0], fieldInOutput);
            if(args[0].equals(PersonType.NON_WORKER)){
                writer.writeSynPopPTableDataSet(model.hhTable);
            }

        }
        logger.info("End of Daily Activity Pattern module");
    }
}
