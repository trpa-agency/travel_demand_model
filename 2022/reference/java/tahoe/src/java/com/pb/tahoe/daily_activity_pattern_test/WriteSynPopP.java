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

import com.pb.common.datafile.TableDataSet;
import com.pb.tahoe.util.DataReader;
import com.pb.tahoe.util.DataWriter;

/**
 * WriteSynPopP is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Apr 4, 2006
 */
public class WriteSynPopP {

    public static void main(String[] args){
        DataReader reader = DataReader.getInstance();
        TableDataSet hhTable = reader.loadTableDataSet("daily.activity.pattern.outputFile");

        DataWriter writer = DataWriter.getInstance();
        writer.writeSynPopPTableDataSet(hhTable);
    }
}
