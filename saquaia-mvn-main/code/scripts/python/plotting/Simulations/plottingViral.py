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
        return times, values

species = ["RNA"]
speciesTwin = ["DNA"]
speciesTwin2 = ["P"]
speciesTwin3 = ["V"]

cm = 1/2.54
fig, (ax1, ax2) = plt.subplots(nrows=2, ncols=1, sharex=True, sharey=True, figsize=(12*cm,9*cm))
ax1Twin = ax1.twinx()
ax1Twin2 = ax1.twinx()
ax2Twin = ax2.twinx()
ax2Twin2 = ax2.twinx()
ax1Twin.get_shared_y_axes().join(ax1Twin, ax2Twin)
ax1Twin2.get_shared_y_axes().join(ax1Twin2, ax2Twin2)
ax1Twin2.spines["right"].set_position(("axes", 1.35))
ax2Twin2.spines["right"].set_position(("axes", 1.35))


for s in species:
    times, values = getSim("values.json", s)
    line, = ax1.plot(times, values)
    ax1Twin.plot(-1,-1)
    ax1Twin2.plot(-1,-1)

for s in speciesTwin:
    times, values = getSim("values.json", s)
    line, = ax1Twin.plot(times, values, "--")
    ax1Twin2.plot(-1,-1)

for s in speciesTwin2:
    times, values = getSim("values.json", s)
    line, = ax1Twin2.plot(times, values, ":")

for s in species:
    times, values = getSim("values.json", s)
    line, = ax2.plot(times, values)
    ax2Twin.plot(-1,-1)
    ax2Twin2.plot(-1,-1)
    line.set_label(s)

for s in speciesTwin:
    times, values = getSim("values.json", s)
    line, = ax2Twin.plot(times, values, "--")
    ax2Twin2.plot(-1,-1)
    line.set_label(s)

for s in speciesTwin2:
    times, values = getSim("values.json", s)
    line, = ax2Twin2.plot(times, values, ":")
    line.set_label(s)

    
ax1.margins(x=0)
ax1.set_ylim(bottom=0,top=40)
ax1Twin.set_ylim(bottom=0)
ax1Twin2.set_ylim(bottom=0)
ax1.set_xlim(left=0, right=t_end)

##.set_ylabel('sin', color=color)  # we already handled the x-label with ax1
##ax2.plot(t, data2, color=color)
##ax2.tick_params(axis='y', labelcolor=color)

colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

ax1.set_ylabel('RNA (SSA)', color=colors[0])
ax1.tick_params(axis='y', labelcolor=colors[0])
ax2.set_ylabel('RNA (SEG)', color=colors[0])
ax2.tick_params(axis='y', labelcolor=colors[0])
ax1Twin.set_ylabel('DNA (SSA)', color=colors[1])
ax1Twin.tick_params(axis='y', labelcolor=colors[1])
ax2Twin.set_ylabel('DNA (SEG)', color=colors[1])
ax2Twin.tick_params(axis='y', labelcolor=colors[1])
ax1Twin2.set_ylabel('P (SSA)', color=colors[2])
ax1Twin2.tick_params(axis='y', labelcolor=colors[2])
ax2Twin2.set_ylabel('P (SEG)', color=colors[2])
ax2Twin2.tick_params(axis='y', labelcolor=colors[2])
ax2.set_xlabel('Time (in s)')

fig.legend(bbox_to_anchor=(0.35, 0.9), loc="center", ncol=4, shadow=True, numpoints=2)
fig.tight_layout()
fig.subplots_adjust(top=0.83)
#fig.subplots_adjust(top=0.83) 

plt.savefig('test.png', bbox_inches="tight")
fig.show()
  

  
  





