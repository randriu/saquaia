import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
from math import log10, floor
import os
from matplotlib.ticker import (MultipleLocator, AutoMinorLocator)

data_file = os.path.abspath(
    os.path.join(
        os.path.dirname( __file__ ), 
        '..', 
        '..', 
        '..', 
        'out', 
        'benchmark', 
        '20230122_sequence', 
        'VI_SequenceSpeed', 
        'plotting_data',
        'comparison.png.csv'
    )
)
print(data_file)

import csv

data = {}

with open(data_file, newline='') as csvfile:

    reader = csv.reader(csvfile, delimiter=',', quotechar='|')

    for row in reader:
        if row[0] not in data:
            data[row[0]] = {"x": {0: [], 1: [], 2: [], 3: []}, "y": {0: [], 1: [], 2: [], 3: []}}
        x = float(row[1])
        y = float(row[2])
        sub = 0 if x <= 1000 else 1 if x <= 10000 else 2 if x <= 20000 else 3
        x = x if x <= 1000 else x - 1000 if x <= 10000 else x - 10000 if x <= 20000 else x - 20000
##        print(x)
##        print(y)
##        print(sub)
        data[row[0]]["x"][sub].append(x)
        data[row[0]]["y"][sub].append(y)

cm = 1/2.54  # centimeters in inches
fig, axs = plt.subplots(2, 4, figsize=(20*cm, 12*cm))
print(axs)

colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
lines = []
line_labels = []

for label in data:
    print(label)
    axs_row = axs[0] if "SSA" in label else axs[1]
    line_type = "-" if not "reset" in label else "--"
    color = colors[0] if "SSA" in label else colors[1]
    new_label = "w/ artifact" if not "reset" in label else "w/o artifact"
    
    for sub in range(4):
        ax = axs_row[sub]
        l = ax.plot(data[label]["x"][sub], data[label]["y"][sub], label=new_label, ls=line_type, color=color)
        if label not in line_labels:
            line_labels.append(label)
            lines.append(l)

# remove unnecessary x margins
for sub in range(4):
    axs[0][sub].margins(x=0)
    axs[1][sub].margins(x=0)
    

    
# join y axis
axs[0][0].get_shared_y_axes().join(axs[0][0], *axs[0])
axs[1][0].get_shared_y_axes().join(axs[1][0], *axs[1])

# join x axis
for sub in range(4):
    axs[0][sub].get_shared_x_axes().join(axs[0][sub], axs[1][sub])

# remove unneccessary y labels
for sub in range(1,4):
    axs[0][sub].tick_params(labelleft=False)
    axs[1][sub].tick_params(labelleft=False)
    
# remove unneccessary x labels
for sub in range(4):
    axs[0][sub].tick_params(labelbottom=False)

make_log = True
if make_log: 
    # make speedup log scale
    axs[0][0].set_yscale('log')
    axs[1][0].set_yscale('log')
    # add log lines
    for sub in range(4):
        axs[0][sub].grid(True, which="both", ls=":", color='0.6', axis='y')
        axs[1][sub].grid(True, which="both", ls=":", color='0.6', axis='y')

# fix x labels in task 2
axs[0][1].set_xticks([0, 9000])
axs[1][1].set_xticks([0, 9000])

# label tasks
task_labels = ["Task 1\n\"base\"", "Task 2\n\"more\"", "Task 3\n\"longer\"", "Task 4\n\"elsewhere\""]
for sub in range(4):
    axs[0][sub].set_title(task_labels[sub], fontsize="small")

# label base simulators
axs[0][0].set_ylabel("Seg. using SSA", color=colors[0])
axs[1][0].set_ylabel("Seg. using HYB", color=colors[1])
##axs[0][3].yaxis.set_label_position('right')
##axs[1][3].yaxis.set_label_position('right')
    
# legends
legend_top_left = True
if legend_top_left:
    axs[0][0].legend(loc='upper left', shadow=True)
    axs[1][0].legend(loc='upper left', shadow=True)
else:
    axs[0][3].legend(loc='lower right', shadow=True)
    axs[1][3].legend(loc='lower right', shadow=True)

# global x and y labels
fig.supxlabel("Number Of Simulations in Task")
fig.supylabel("Speedup w.r.t. SSA")

fig.tight_layout(pad=0.7, w_pad=0.2, h_pad=0.2)

plt.show()
