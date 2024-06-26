#tahoe_population_shift.py
import sys,os,shutil,random
random.seed(211)

base_dir = sys.argv[1]
pop_file = os.path.join(base_dir,"SynPopHPlusAutoOwnership.csv")
pop_transfer_file = os.path.join(base_dir,"pop_transfer.csv")
pop_increase_file = os.path.join(base_dir,"pop_increase.csv")
pop_decrease_file = os.path.join(base_dir,"pop_decrease.csv")

#pop_transfer: from_zone,to_zone,number
#pop_increase: zone,percentage_increase (0.5 means zone will have 150% of population afterwards)
#pop_decrease: zone,percentage_decrease (0.5 means zone will have 50% of population afterwards)

first = True
pop_transfer = {}
if os.path.exists(pop_transfer_file):
    for line in open(pop_transfer_file):
        if line.strip() == "":
            continue
        if first:
            first = False
            continue
        data = line.strip().split(",")
        taz = float(data[0])
        if not taz in pop_transfer:
            pop_transfer[taz] = []
        pop_transfer[taz].append([str(float(data[1])),float(data[2])])

first = True
pop_increase = {}
if os.path.exists(pop_increase_file):
    for line in open(pop_increase_file):
        if line.strip() == "":
            continue
        if first:
            first = False
            continue
        data = line.strip().split(",")
        pop_increase[float(data[0])] = float(data[1])

first = True
pop_decrease = {}
if os.path.exists(pop_decrease_file):
    for line in open(pop_decrease_file):
        if line.strip() == "":
            continue
        if first:
            first = False
            continue
        data = line.strip().split(",")
        pop_decrease[float(data[0])] = float(data[1])

ls = os.linesep
#loop over file three times: transfer, then increase, then decrease
counter = 1.0 #for hh id
tf = pop_file + ".tmp"
tfo = open(tf,"wb")
first = True
for line in open(pop_file):
    if first:
        tfo.write(line)
        first = False
        continue
    data = line.strip().split(",")
    taz = float(data[1])
    if taz in pop_transfer:
        for entry in pop_transfer[taz]:
            if entry[1] > 0:
                data[1] = entry[0]
                entry[1] -= 1
                break
        data[0] = str(counter)
        tfo.write(",".join(data) + ls)
        counter += 1
    elif taz in pop_increase:
        data[0] = str(counter)
        tfo.write(",".join(data) + ls)
        counter += 1
        inc = pop_increase[taz]
        while inc > 1.0: #add population for 100% increases
            data[0] = str(counter)
            tfo.write(",".join(data) + ls)
            counter += 1
            inc -= 1.0
        if random.random() < inc:
            data[0] = str(counter)
            tfo.write(",".join(data) + ls)
            counter += 1
    elif taz in pop_decrease:
        if random.random() > pop_decrease[taz]: #only write if not removed 
            data[0] = str(counter)
            tfo.write(",".join(data) + ls)
            counter += 1
    else:
        data[0] = str(counter)
        tfo.write(",".join(data) + ls)
        counter += 1
tfo.close()
if os.path.exists(pop_file + ".orig"):
    os.remove(pop_file + ".orig")
if os.path.exists(pop_file + ".final"):
    os.remove(pop_file + ".final")
os.rename(pop_file,pop_file + ".orig")
os.rename(tf,pop_file)
shutil.copyfile(pop_file,pop_file + ".final")
