import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
from math import log10, floor
import os
from matplotlib.ticker import (MultipleLocator, AutoMinorLocator)
import csv

def getData(file):
    with open(file, newline='') as csvfile:
        data = {}
        reader = csv.reader(csvfile, delimiter=',', quotechar='|')

        for row in reader:
            if row[0] not in data:
                data[row[0]] = {"x": [], "y": []}
            x = float(row[1])
            y = float(row[2])
            data[row[0]]["x"].append(x)
            data[row[0]]["y"].append(y)
        return data

colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
colors = colors + colors + colors + colors

model = "RP"

##########
### PP ###
##########
if model == "PP":
    title = "Pred. Prey"
    axisdata = [
        {
            "label": "Copy Number", 
            "color": "black", 
            "min":0, 
            "max":1000,
            "tickwidth": 0.25
        }
    ]
    methoddata = [
        {
            "folder": 'PP_Visual_SSA',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SSA"
        }, 
        {
            "folder": 'PP_Visual_HYB_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "HYB"
        }, 
        {
            "folder": 'PP_Visual_SEG+5GB_g0.0359_SSA_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SEG using SSA"
        }, 
        {
            "folder": 'PP_Visual_SEG+5GB_g0.0359_HYB_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SEG using HYB"
        }
    ]
    dimensiondata = {
        "Pred" : {
            "axis": 0,
            "label": "Pred",
            "color": colors[0],
            "linestyle": "-"
        }, 
        "Prey" : {
            "axis": 0,
            "label": "Prey",
            "color": colors[1],
            "linestyle": "-"
        }
    }
    compare = "Prey"
    xlim = 100
    legend_ncol = 2
    legend_pos = (0.05, 0.9)
    legend_loc = "upper left"




##########
### VI ###
##########
if model == "VI":
    title = "Viral Infection"
    axisdata = [
        {
            "label": "RNA", 
            "color": colors[0], 
            "min":0, 
            "max":50,
            "tickwidth": 0.25
        },
        {
            "label": "DNA", 
            "color": colors[1], 
            "min":0, 
            "max":220,
            "tickwidth": 0.25
        },
        {
            "label": "P & V", 
            "color": colors[2], 
            "min":0, 
            "max":15000,
            "tickwidth": 0.2
        }
    ]
    methoddata = [
        {
            "folder": 'VI_Visual_SSA',
            "sims": ["90", "91", "92", "93", "94", "95", "96", "97", "98", "99"],
            "label": "SSA"
        }, 
        {
            "folder": 'VI_Visual_HYB_1.5',
            "sims": ["90", "91", "92", "93", "94", "95", "96", "97", "98", "99"],
            "label": "HYB"
        }, 
        {
            "folder": 'VI_Visual_SEG+5GB_g0.0359_SSA_1.5',
            "sims": ["90", "91", "92", "93", "94", "95", "96", "97", "89", "88"],
            "label": "SEG using SSA"
        }, 
        {
            "folder": 'VI_Visual_SEG+5GB_g0.0359_HYB_1.5',
            "sims": ["90", "89", "88", "93", "94", "95", "96", "97", "98", "99"],
            "label": "SEG using HYB"
        }
    ]
    dimensiondata = {
        "RNA" : {
            "axis": 0,
            "label": "RNA",
            "color": colors[0],
            "linestyle": "-"
        }, 
        "DNA" : {
            "axis": 1,
            "label": "Prey",
            "color": colors[1],
            "linestyle": "-"
        }, 
        "P" : {
            "axis": 2,
            "label": "P",
            "color": colors[2],
            "linestyle": "-"
        }, 
        "V" : {
            "axis": 2,
            "label": "V",
            "color": colors[2],
            "linestyle": ":"
        }
    }
    compare = "RNA"
    xlim = 200
    legend_ncol = 1
    legend_pos = (0.05, 0.9)
    legend_loc = "upper left"



##########
### TS ###
##########
if model == "TS":
    title = "Toggle Switch"
    axisdata = [
        {
            "label": "p", 
            "color": "black", 
            "min":0, 
            "max":15000,
            "tickwidth": 0.3
        },
        {
            "label": "s", 
            "color": "black", 
            "min":0, 
            "max":300,
            "tickwidth": 0.25
        }
    ]
    methoddata = [
        {
            "folder": 'TS_Visual_SSA',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SSA"
        }, 
        {
            "folder": 'TS_Visual_HYB_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "HYB"
        }, 
        {
            "folder": 'TS_Visual_SEG+5GB_g0.0359_SSA_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SEG using SSA"
        }, 
        {
            "folder": 'TS_Visual_SEG+5GB_g0.0359_HYB_1.5',
            "sims": ["98", "96", "97", "95", "99"],
            "label": "SEG using HYB"
        }
    ]
    dimensiondata = {
        "pA" : {
            "axis": 0,
            "label": "pA",
            "color": colors[2],
            "linestyle": "-"
        }, 
        "pB" : {
            "axis": 0,
            "label": "pB",
            "color": colors[3],
            "linestyle": "-"
        },
        "sA" : {
            "axis": 1,
            "label": "sA",
            "color": colors[0],
            "linestyle": "-"
        }, 
        "sB" : {
            "axis": 1,
            "label": "sB",
            "color": colors[1],
            "linestyle": "-"
        }
    }
    compare = "pA"
    xlim = 50000
    legend_ncol = 4
    legend_pos = (0.05, 0.91)
    legend_loc = "upper left"



##########
### RP ###
##########
if model == "RP":
    title = "Repressilator"
    axisdata = [
        {
            "label": "Copy Number", 
            "color": "black", 
            "min":0, 
            "max":75000,
            "tickwidth": 0.25
        }
    ]
    methoddata = [
        {
            "folder": 'RP_Visual_SSA',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SSA"
        }, 
        {
            "folder": 'RP_Visual_HYB_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "HYB"
        }, 
        {
            "folder": 'RP_Visual_SEG+5GB_g0.0359_SSA_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SEG using SSA"
        }, 
        {
            "folder": 'RP_Visual_SEG+5GB_g0.0359_HYB_1.5',
            "sims": ["95", "96", "97", "98", "99"],
            "label": "SEG using HYB"
        }
    ]
    dimensiondata = {
        "pA" : {
            "axis": 0,
            "label": "pA",
            "color": colors[0],
            "linestyle": "-"
        }, 
        "pB" : {
            "axis": 0,
            "label": "pB",
            "color": colors[1],
            "linestyle": "-"
        }, 
        "pC" : {
            "axis": 0,
            "label": "pC",
            "color": colors[2],
            "linestyle": "-"
        }
    }
    compare = "pA"
    xlim = 25000
    legend_ncol = 3
    legend_pos = (0.05, 0.91)
    legend_loc = "upper left"





cm = 1/2.54  # centimeters in inches
x = len(methoddata)
y = 2
z = len(axisdata)
fig, subs = plt.subplots(y, x, figsize=(x * 10*cm, (y*5+2)*cm), sharex='all', sharey='all')
subsfull, subsscatter = subs

# axis sharing
subs2 = [[[subs[iy][ix]] + [None for iz in range(1,z)] for iy in range(y)] for ix in range(x)]
## create other axis
for iz in range(1, z):
    for ix in range(x):
        subs2[ix][0][iz] = subs2[ix][0][0].twinx()
        # subs2[ix][0][iz].tick_params(axis='y', left=False, labelleft=False, right=z<=1, labelright=False)
        if ix != 0:
            subs2[0][0][iz].get_shared_y_axes().join(subs2[0][0][iz], subs2[ix][0][iz])
    # subs2[-1][0][iz].tick_params(axis='y', right=True, labelright=True)
    pos = 1.0 + sum([axisdata[i]["tickwidth"] if "tickwidth" in axisdata[i] else 0.25 for i in range(iz)])
    subs2[-1][0][iz].spines.right.set_position(("axes", pos))
   


# data
datafolder = os.path.abspath(
    os.path.join(
        os.path.dirname( __file__ ), 
        '..', 
        '..', 
        '..', 
        '..', 
        '..', 
        'results', 
        'benchmark', 
        '20230520_visual'
    )
)
data = [[getData(os.path.abspath(os.path.join(datafolder, methoddata[xi]["folder"], '0', 'plotting_data', f'sim{i}.png.csv'))) for i in methoddata[xi]["sims"]] for xi in range(x)]

# plot data
for xi in range(x):
    di = 0
    for d in data[xi]:
        for label in d:
            if label not in dimensiondata:
                continue
            color = dimensiondata[label]["color"] if "color" in dimensiondata[label] else "black"
            ls = dimensiondata[label]["linestyle"] if "linestyle" in dimensiondata[label] else "-"
            axisnr = dimensiondata[label]["axis"] 
            l = dimensiondata[label]["label"] if "label" in dimensiondata[label] else label
            if xi != 0:
                l = None
            if di == 0:
                subs2[xi][0][axisnr].plot(d[label]["x"], d[label]["y"], color=color, linestyle=ls, label=l)
            if label == compare:
                subs2[xi][1][0].plot(d[label]["x"], d[label]["y"], color=color if di == 0 else colors[di+4], linestyle=ls, alpha=0.8)
        di = di + 1

# y limits
for iz in range(z):
    subs2[0][0][iz].set_ylim(bottom = axisdata[iz]["min"] if "min" in axisdata[iz] else 0, top = axisdata[iz]["max"] if "max" in axisdata[iz] else None)

# x limit
subs2[0][0][0].set_xlim(left=0,right=xlim)

# model label
fig.supylabel(title, x=0.02)

# x axis label
# fig.supxlabel("Time (in s)")
for xi in range(x):
    subs2[xi][1][0].set_xlabel("Time (in s)")

# y axis labels
for iz in range(z):
    if iz == 0:
        subs2[-1][0][0].yaxis.set_label_position("right")
        subs2[-1][0][0].set_ylabel(axisdata[iz]["label"], color=axisdata[iz]["color"])
        subs2[-1][1][0].yaxis.set_label_position("right")
        subs2[-1][1][0].set_ylabel(dimensiondata[compare]["label"], color=axisdata[iz]["color"])
    else:
        subs2[-1][0][iz].set_ylabel(axisdata[iz]["label"], color=axisdata[iz]["color"])
for ix in range(x):
    for iy in range(y):
        for iz in range(z):
            if not subs2[ix][iy][iz]:
                continue
            subs2[ix][iy][iz].tick_params(axis='y', left=False, labelleft=False, right=ix == x-1, labelright=ix == x-1, colors=axisdata[iz]["color"])

# method labels
for xi in range(x):
    subs2[xi][0][0].set_title(methoddata[xi]["label"])
    
    
# left labels
subs2[0][0][0].set_ylabel("1x Simulation")
subs2[0][1][0].set_ylabel(f'{len(methoddata[0]["sims"])}x {compare} Traj.')

# legend
fig.legend(bbox_to_anchor=legend_pos, shadow=True, loc=legend_loc, numpoints=2, ncol=legend_ncol)

fig.tight_layout(pad=1.5, w_pad=0.2, h_pad=0.2)

plt.show()

exit()

