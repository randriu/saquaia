import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
from math import log10, floor
import os

data_file = os.path.abspath(
    os.path.join(
        os.path.dirname( __file__ ), 
        '..', 
        '..', 
        '..',
        '..',
        '..', 
        'results', 
        'benchmark', 
        '20230216_single_sequence_speed', 
        'AllSpeed', 
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
            data[row[0]] = {"x": [], "y": []}
        data[row[0]]["x"].append(float(row[1]))
        data[row[0]]["y"].append(float(row[2]))

# convert y-axis to Logarithmic scale
#plt.yscale("log")

cm = 1/2.54  # centimeters in inches
fig = plt.figure(figsize=(20*cm, 12*cm))
ax = fig.add_subplot(1, 1, 1)

used_colors = {}

for label in data:
    if "TR" in label or "EC" in label:
        continue
    name = label[0:2] + (" (SSA)" if "SSA" in label else " (HYB)")
    line_type = "-" if "HYB" in label else "--"
    color = None if label[:2] not in used_colors else used_colors[label[:2]]
    l = ax.plot(data[label]["x"], data[label]["y"], ls=line_type, color=color, label=name)
    used_colors[label[:2]] = l[0].get_color()
    
def minor_format(x, p):
    most_significant_digit = floor(x / (10**floor(log10(x))))
    if most_significant_digit in [1,2,5,0]:
        res = '%s' % float('%.1g' % x)
        if len(res) > 2 and res[-2:] == ".0":
            res = res[0:-2]
        return res + "x"
    return ""

ax.set_yscale('log')
ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y,pos: ('{{:.{:1d}f}}x'.format(int(np.maximum(-np.log10(y),0)))).format(y)))
ax.yaxis.set_minor_formatter(minor_format)

#ax.set_xlim([0, 10000])

ax.set_xlabel("Number Of Simulations")
ax.set_ylabel("Speedup w.r.t. SSA")

ax.legend(ncol=4, columnspacing=1.3, handlelength=1.4, handletextpad=0.5, shadow=True)

##ax.margins(x=0)

plt.grid(True, which="both", ls=":", color='0.8')

fig.tight_layout(pad=0.7, w_pad=0.2, h_pad=0.2)

plt.show()
