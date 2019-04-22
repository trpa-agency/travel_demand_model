# travel_demand_model

https://trpa-agency.github.io/travel_demand_model


Model installation

1. Download the repo to any location in the computer

2. Create the file TahoeModelRunnerBackup_V2.txt in the transcad directory with following contents

C:\Temp\trpa_model\scenarios\scenario_base\
1
C:\Temp\trpa_model\
50
1
5
1000


3. Tools > Setup > addins (Open transcad in admin mode)

Type = MAcro
Name = TahoeABModel
UI = C:\Temp\trpa_model\reference\code\ui\tahoemodelui.dbd

4. Gitignore files - PUMS data need to be copied if not already present

5. Computer should have the JAva path already set. Both "java" command and "javac" command should work. That is the java location should be a JDK.