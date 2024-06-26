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
package com.pb.tahoe.util;

/**
 * OutputDescription is a class that is used when
 * logging the column frequency of output files
 * created to show the results of the various tahoe models.
 *
 * @author Christi Willison
 * @version 1.0,  Mar 23, 2006
 */
public class OutputDescription {

    public static String[] getDescriptions (String columnName){
        String[] descriptions = null;

        //synpop description
        if (columnName.equals("person_type")){
            descriptions = new String[] {"workers_f","workers_p","students","nonworkers","preschool",
                    "schoolpred","schooldriv"};
        //synpop description
        }else if (columnName.equals("income")){
            descriptions = new String[] {"low", "med", "high"};
        //auto-ownership description
        }else if(columnName.equals("AutoOwn")){
            descriptions = new String[] {"0_cars","1_car","2_cars","3_cars","4+_cars"};
        //
        } else if(columnName.equals("M31")){
            descriptions = new String[] {"0_tours","1_Shop","1_Eat","1_Main","1_Disc","2_SS","2_SE",
                    "2_SM","2_SD","2_EE","2_EM","2_ED","2_MM","2_MD","2_DD","0_travelers"};

        } else if (columnName.equals("M32")){
            descriptions = new String[] {"adults","children","mixed","no HH tour"};

        } else if (columnName.equals("M33")){
            descriptions = new String[] {"yes","no","not elig"};

        } else if (columnName.equals("M41")){
            descriptions = new String[] {"0S_0E_0M","0S_0E_1M","0S_0E_2M","0S_0E_3M","0S_1E_0M",
                    "0S_1E_1M","0S_1E_2M","0S_1E_3M","0S_2E_0M","0S_2E_1M","0S_2E_2M","0S_2E_3M",
                    "1S_0E_0M","1S_0E_1M","1S_0E_2M","1S_0E_3M","1S_1E_0M","1S_1E_1M","1S_1E_2M",
                    "1S_1E_3M","1S_2E_0M","1S_2E_1M","1S_2E_2M","1S_2E_3M","2S_0E_0M","2S_0E_1M",
                    "2S_0E_2M","2S_0E_3M","2S_1E_0M","2S_1E_1M","2S_1E_2M","2S_1E_3M","2S_2E_0M",
                    "2S_2E_1M","2S_2E_2M","2S_2E_3M"};

        } else if (columnName.equals("M42")){
            descriptions = new String[] {"1st_FW","2nd_FW","3rd_FW","4th_FW","1st_PW","2nd_PW",
                    "3rd_PW","4th_PW","1st_STUD","2nd_STUD","3rd_STUD","4th_STUD","1st_NW",
                    "2nd_NW","3rd_NW","4th_NW","1st_SCH_PD","2nd_SCH_PD","3rd_SCH_PD",
                    "4th_SCH_PD","1st_SCH_DR","2nd_SCH_DR","3rd_SCH_DR","4th_SCH_DR"};

        } else if (columnName.equals("M431")|| columnName.equals("M432")||columnName.equals("M433")){
            descriptions = new String[] {"not elig","0_eat,disc","1_eat","1_disc","2_disc","2_ed"};

        } else if (columnName.equals("M44")){
            descriptions = new String[] {"no subtrs","1_eat","1_work","1_other","2_work","2_ew"};

        } else if (columnName.equals("M7_MC")){
            descriptions = new String[] {"SOV","HOV","Walk_trn","Drive_trn","Non_motor","School Bus"};
        }

        return descriptions;
    }

    public static String getDescription (String columnName, float value){
        String description = null;
        int testCase = (int)value;

        if(columnName.equals("M1")){
            switch(testCase){
                case(1):
                    description="0_car";
                    break;
                case(2):
                    description="1_car";
                    break;
                case(3):
                    description="2_cars";
                    break;
                case(4):
                    description="3_cars";
                    break;
                case(5):
                    description="4+_cars";
                    break;
                default:
                    description="not avail";
            }
        }else if (columnName.equals("M2")) {
            switch(testCase){
                case(1):
                    description="work_1";
                    break;
                case(2):
                    description="work_2";
                    break;
                case(3):
                    description="school_1";
                    break;
                case(4):
                    description="school_2";
                    break;
                case(5):
                    description="school_work";
                    break;
                case(6):
                    description="univ_1";
                    break;
                case(7):
                    description="univ_2";
                    break;
                case(8):
                    description="univ_work";
                    break;
                 case(9):
                    description="work_univ";
                    break;
                 case(10):
                    description="non_mand";
                    break;
                 case(11):
                    description="home";
                    break;
                 default:
                    description="not avail";
              }
            } else if(columnName.equals("JT_Freq")){
                switch(testCase){
                case(1):
                    description="0_tours";
                    break;
                case(2):
                    description="1_Shop";
                    break;
                case(3):
                    description="1_Eat";
                    break;
                case(4):
                    description="1_Main";
                    break;
                case(5):
                    description="1_Disc";
                    break;
                case(6):
                    description="2_SS";
                    break;
                case(7):
                    description="2_SE";
                    break;
                case(8):
                    description="2_SM";
                    break;
                 case(9):
                    description="2_SD";
                    break;
                 case(10):
                    description="2_EE";
                    break;
                 case(11):
                    description="2_EM";
                    break;
                 case(12):
                    description="2_ED";
                    break;
                 case(13):
                    description="2_MM";
                    break;
                 case(14):
                    description="2_MD";
                    break;
                 case(15):
                    description="2_DD";
                    break;
                 case(199):
                    description="0_travelers";
                    break;
                 default:
                    description="not avail";
              }
            } else if (columnName.equals("JT_Comp")){
                switch(testCase){
                  case(1):
                    description="adults";
                    break;
                  case(2):
                    description="children";
                    break;
                  case(3):
                    description="mixed";
                    break;
                  case(0):
                    description="no HH tour";
                    break;
                  default:
                    description="not avail";
                  }
            } else if (columnName.equals("JT_Partic")){
                switch(testCase){
                 case(1):
                   description="yes";
                   break;
                 case(2):
                   description="no";
                   break;
                 case(99):
                   description="not elig";
                   break;
                 default:
                   description="not avail";
                }
            } else if (columnName.equals("indiv_main_freq")){
                switch(testCase){
                 case(0):
                   description="not applicable";
                   break;
                 case(1):
                   description="0S_0E_0M";
                   break;
                 case(2):
                    description="0S_0E_1M";
                    break;
                 case(3):
                    description="0S_0E_2M";
                    break;
                 case(4):
                    description="0S_0E_3M";
                    break;
                 case(5):
                    description="0S_1E_0M";
                    break;
                 case(6):
                    description="0S_1E_1M";
                    break;
                 case(7):
                    description="0S_1E_2M";
                    break;
                 case(8):
                    description="0S_1E_3M";
                    break;
                 case(9):
                    description="0S_2E_0M";
                    break;
                 case(10):
                    description="0S_2E_1M";
                    break;
                 case(11):
                    description="0S_2E_2M";
                    break;
                 case(12):
                    description="0S_2E_3M";
                    break;
                 case(13):
                    description="1S_0E_0M";
                    break;
                 case(14):
                    description="1S_0E_1M";
                    break;
                 case(15):
                    description="1S_0E_2M";
                    break;
                 case(16):
                    description="1S_0E_3M";
                    break;
                 case(17):
                    description="1S_1E_0M";
                    break;
                 case(18):
                    description="1S_1E_1M";
                    break;
                 case(19):
                    description="1S_1E_2M";
                    break;
                 case(20):
                    description="1S_1E_3M";
                    break;
                 case(21):
                    description="1S_2E_0M";
                    break;
                 case(22):
                    description="1S_2E_1M";
                    break;
                 case(23):
                    description="1S_2E_2M";
                    break;
                 case(24):
                    description="1S_2E_3M";
                    break;
                 case(25):
                    description="2S_0E_0M";
                    break;
                 case(26):
                    description="2S_0E_1M";
                    break;
                 case(27):
                    description="2S_0E_2M";
                    break;
                 case(28):
                    description="2S_0E_3M";
                    break;
                 case(29):
                    description="2S_1E_0M";
                    break;
                 case(30):
                    description="2S_1E_1M";
                    break;
                 case(31):
                    description="2S_1E_2M";
                    break;
                 case(32):
                    description="2S_1E_3M";
                    break;
                 case(33):
                    description="2S_2E_0M";
                    break;
                 case(34):
                    description="2S_2E_1M";
                    break;
                 case(35):
                    description="2S_2E_2M";
                    break;
                 case(36):
                    description="2S_2E_3M";
                    break;
                 default:
                   description="not avail";
                }
            } else if (columnName.equals("indiv_main_alloc")){
                switch(testCase){
                 case(0):
                    description="not applicable";
                    break;
                 case(1):
                    description="Work_f1";
                    break;
                 case(2):
                    description="Work_f2";
                    break;
                 case(3):
                    description="Work_f3";
                    break;
                 case(4):
                    description="Work_f4";
                    break;
                 case(5):
                    description="Work_p1";
                    break;
                 case(6):
                    description="Work_p2";
                    break;
                 case(7):
                    description="Work_p3";
                    break;
                 case(8):
                    description="Work_p4";
                    break;
                 case(9):
                    description="Nonw1";
                    break;
                 case(10):
                    description="Nonw2";
                    break;
                 case(11):
                    description="Nonw3";
                    break;
                 case(12):
                    description="Nonw4";
                    break;
                 case(13):
                    description="Pred1";
                    break;
                 case(14):
                    description="Pred2";
                    break;
                 case(15):
                    description="Pred3";
                    break;
                 case(16):
                    description="Pred4";
                    break;
                 case(17):
                    description="Driv1";
                    break;
                 case(18):
                    description="Driv2";
                    break;
                 case(19):
                    description="Driv3";
                    break;
                 case(20):
                    description="Driv4";
                    break;
                 default:
                   description="not avail";
                }
            } else if (columnName.equals("indiv_w_disc_freq") || columnName.equals("indiv_nw_disc_freq") || columnName.equals("indiv_c_disc_freq")){
                switch(testCase){
                  case(0):
                    description="not applicable";
                    break;
                  case(1):
                    description="No_tours";
                    break;
                  case(2):
                    description="1_eat";
                    break;
                  case(3):
                    description="1_discr";
                    break;
                  case(4):
                    description="2_discr";
                    break;
                  case(5):
                    description="2_eat_discr";
                    break;
                  default:
                    description="not avail";
                }
            } else if (columnName.equals("indiv_atwork_freq")){

                switch(testCase){
                  case(0):
                    description="not applicable";
                    break;
                  case(1):
                    description="No_subts";
                    break;
                  case(2):
                    description="1_eat";
                    break;
                  case(3):
                    description="1_work";
                    break;
                  case(4):
                    description="1_other";
                    break;
                  case(5):
                    description="2_work";
                    break;
                  case(6):
                    description="2_EW";
                    break;
                  default:
                    description="not avail";
                }
            } else if (columnName.equals("person_type")){
                switch(testCase){
                  case(1):
                    description="workers_f";
                    break;
                  case(2):
                    description="workers_p";
                    break;
                  case(3):
                    description="nonworkers";
                    break;
                  case(4):
                    description="preschool";
                    break;
                  case(5):
                    description="schoolpred";
                    break;
                  case(6):
                    description="schooldriv";
                    break;
                  default:
                    description="not avail";
                 }
            } else if (columnName.equals("income")){
                switch(testCase){
                  case(1):
                    description="low";
                    break;
                  case(2):
                    description="medium";
                    break;
                  case(3):
                    description="high";
                    break;
                  default:
                    description="not avail";
                }
            } else description="not avail";



        return description;
    }
}
