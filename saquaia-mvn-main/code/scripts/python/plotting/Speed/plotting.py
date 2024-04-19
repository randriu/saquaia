import matplotlib.pyplot as plt
import numpy as np
import json
import os

plt.rcParams['figure.dpi'] = 200
cm = 1/2.54

max_nr_of_sims = [0,1]

# rename files
for file_name in os.listdir("times"):
    file_name_new = file_name
    if file_name.find("_lazy") > 0:
        file_name_new = file_name[0:file_name.find("_lazy")] + file_name[file_name.find("_lazy")+5:]
    os.rename(os.path.join("times", file_name), os.path.join("times", file_name_new))

for file_name in os.listdir("times"):
    with open(os.path.join("times", file_name), 'r') as f:
        nr_of_sims, time = json.load(f)
        if len(nr_of_sims) > len(max_nr_of_sims):
            max_nr_of_sims = nr_of_sims

def getTimes(file_name):
    with open(file_name, 'r') as f:
        nr_of_sims, time = json.load(f)
        times_per_sim = [0 if nr_of_sims[i] == 0 or time[i] == 0 else time[i] / nr_of_sims[i] for i in range(len(nr_of_sims))]
        return nr_of_sims, time, times_per_sim

def pretty(file_name):
    s = file_name[file_name.find("_c")+2:]
    c = s[0:s.find("_k")]
    s = s[s.find("_k")+2:]
    k = s[0:s.find(".")]
    return "c=" + c + ",k=" + k

time_per_sim_SSA = 9.1 

fig = plt.figure()
ax = plt.subplot(111)
ax.margins(x=0)
ax.set_xlabel('Number Of Simulations')
ax.set_ylabel("Time Per Simulation (in s)")

time_SSA = [x * time_per_sim_SSA for x in max_nr_of_sims]
times_per_sim_SSA = [time_per_sim_SSA for x in max_nr_of_sims]
line, = ax.plot(max_nr_of_sims, times_per_sim_SSA, "--")
line.set_label("SSA")

for file_name in os.listdir("times"):
    pretty(file_name)
    nr_of_sims, time, times_per_sim = getTimes(os.path.join("times", file_name))
    line, = ax.plot(nr_of_sims, times_per_sim)
    line.set_label(pretty(file_name))


ax.set_xlim(left=1, right=2000)
ax.set_ylim(bottom=0)
ax.legend(ncol=2, shadow=True)
fig.savefig('times.png')
plt.show()

