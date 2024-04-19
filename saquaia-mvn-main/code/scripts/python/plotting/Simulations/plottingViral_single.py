import matplotlib.pyplot as plt
import json
import os

plt.rcParams['figure.dpi'] = 200

t_end = 200

##with open('values_concrete.json', 'r') as f:
##  data_concrete = json.load(f)
##  with open('values_abstract.json', 'r') as f:
##    data_abstract = json.load(f)
##    with open('values.json', 'r') as f:
##      data_SSA = json.load(f)


def getSim(file_name, s):
    with open(file_name, 'r') as f:
        data = json.load(f)
        times = [p["left"] for p in data[s] if p["left"]]
        values = [p["right"] for p in data[s] if p["left"]]
        return times[::20], values[::20]

species = ["RNA"]
speciesTwin = ["DNA"]
speciesTwin2 = ["P"]
speciesTwin3 = ["V"]

cm = 1/2.54
fig, ax1 = plt.subplots(nrows=1, ncols=1, sharex=True, sharey=True, figsize=(20*cm,12*cm))
ax1Twin = ax1.twinx()
ax1Twin3 = ax1.twinx()
ax1Twin3.spines["right"].set_position(("axes", 1.2))
ax1Twin2 = ax1.twinx()
ax1Twin2.spines["right"].set_position(("axes", 1.45))

colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

for s in species:
    times, values = getSim("values.json", s)
    line, = ax1.plot(times, values)
    ax1Twin.plot(-1,-1)
    ax1Twin2.plot(-1,-1)
    line.set_label(s)

for s in speciesTwin:
    times, values = getSim("values.json", s)
    line, = ax1Twin.plot(times, values, "--")
    ax1Twin2.plot(-1,-1)
    line.set_label(s)

for s in speciesTwin2:
    times, values = getSim("values.json", s)
    line, = ax1Twin2.plot(times, values, ":")
    line.set_label(s)

for s in speciesTwin3:
    times, values = getSim("values.json", s)
    line, = ax1Twin3.plot(times, values, "-.", color=colors[3])
    line.set_label(s)

    
ax1.margins(x=0)
ax1.set_ylim(bottom=0,top=50)
ax1Twin.set_ylim(bottom=0,top=200)
ax1Twin2.set_ylim(bottom=0,top=14000)
ax1Twin3.set_ylim(bottom=0,top=2500)
ax1.set_xlim(left=0, right=t_end)

##.set_ylabel('sin', color=color)  # we already handled the x-label with ax1
##ax2.plot(t, data2, color=color)
##ax2.tick_params(axis='y', labelcolor=color)


ax1.set_ylabel('RNA', color=colors[0])
ax1.tick_params(axis='y', labelcolor=colors[0])
ax1Twin.set_ylabel('DNA', color=colors[1])
ax1Twin.tick_params(axis='y', labelcolor=colors[1])
ax1Twin2.set_ylabel('P', color=colors[2])
ax1Twin2.tick_params(axis='y', labelcolor=colors[2])
ax1Twin3.set_ylabel('V', color=colors[3])
ax1Twin3.tick_params(axis='y', labelcolor=colors[3])
ax1.set_xlabel('Time (in s)')

fig.legend(bbox_to_anchor=(0.3, 0.9), loc="center", ncol=4, shadow=True, numpoints=2)
fig.tight_layout()
fig.subplots_adjust(top=0.83)
#fig.subplots_adjust(top=0.83) 

plt.savefig('test.png', bbox_inches="tight")
fig.show()
  

  
  





