import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
from math import log10, floor
import os
from matplotlib.ticker import (MultipleLocator, AutoMinorLocator)
import csv

file1 = os.path.abspath(
    os.path.join(
        os.path.dirname( __file__ ), 
        '..', 
        '..', 
        '..', 
        '..', 
        '..', 
        'results', 
        'benchmark', 
        '20230205_visual',
        'PP_Visual_SSA', 
        '0', 
        'plotting_data',
        'sim003.png.csv'
    )
)

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

data1 = getData(file1)

rename = {}

# RP
##title = "Repressilator"
##y_axis_labels = ["Copy Number"]
##axis_id_for_label = {
##    "pA": 0,
##    "pB": 0,
##    "pC": 0
##}
##color_for_label = {
##    "pA": colors[0],
##    "pB": colors[1],
##    "pC": colors[2]
##}
##color_for_axis = None
##linestyle_for_label = None
##xlim = 25000
##ylim_top = {
##    0: None
##}
##ylim_bot = {
##    0: 0
##}
##ncol=6

# VI
##title = "Viral Infection"
##y_axis_labels = ["RNA", "DNA", "P & V"]
##axis_id_for_label = {
##    "RNA": 0,
##    "V": 2,
##    "P": 2,
##    "DNA": 1
##}
##color_for_label = {
##    "RNA": colors[0],
##    "V": colors[2],
##    "P": colors[2],
##    "DNA": colors[1]
##}
##color_for_axis = {
##    0: colors[0],
##    1: colors[1],
##    2: colors[2]
##}
##linestyle_for_label = {
##    "V": ":"
##}
##xlim = 200
##ylim_top = {
##    0: 50
##}
##ylim_bot = {
##    0: 0,
##    1: 0,
##    2: 0
##}
##ncol=1

# TS
##title = "Toggle Switch"
##y_axis_labels = ["s", "p"]
##axis_id_for_label = {
##    "sA": 0,
##    "sB": 0,
##    "pA": 1,
##    "pB": 1
##}
##color_for_label = {
##    "sA": colors[0],
##    "sB": colors[1],
##    "pA": colors[2],
##    "pB": colors[3]
##}
##color_for_axis = None
##linestyle_for_label = None
##xlim = 50000
##ylim_top = {
##    0: 300,
##    1: 15000
##}
##ylim_bot = {
##    0: 0,
##    1: 0
##}
##ncol=6

# PP
title = "Pred. Prey"
y_axis_labels = ["Copy Number"]
axis_id_for_label = {
    "Pred": 0,
    "Prey": 0
}
color_for_label = {
    "Pred": colors[0],
    "Prey": colors[1]
}
color_for_axis = None
linestyle_for_label = None
xlim = 150
ylim_top = {
    0: None
}
ylim_bot = {
    0: 0
}
ncol=6
axis_distance =  0.3

# EC
##title = "E.Coli"
##y_axis_labels = ["product", "other", "lactose"]
##axis_id_for_label = {
##    "lactose": 2,
##    "product": 0,
##    "Ribosome": 1,
##    "LacZ": 1,
##    "LacY": 1,
##    "dgrRbsLacZ": 1
##}
##color_for_label = {
##    "lactose": colors[0],
##    "product": colors[1],
##    "Ribosome": colors[2],
##    "LacZ": colors[3],
##    "LacY": colors[4],
##    "dgrRbsLacZ": colors[5]
##}
##color_for_axis = {
##    2: colors[0],
##    0: colors[1]
##}
##linestyle_for_label = None
##xlim = 2000
##ylim_top = {
##    0: 10**6 * 3,
##    1: 500,
##    2: 100000
##}
##ylim_bot = {
##    0: 0,
##    1: 0,
##    2: 0
##}
##ncol=3

# TR
##title = "TS x RP"
##y_axis_labels = ["TS", "RP", ]
##axis_id_for_label = {
####    "sA": 0,
####    "sB": 0,
##    "pA": 0,
##    "pB": 0,
##    "pA2": 1,
##    "pB2": 1,
##    "pC": 1
##}
##color_for_label = {
##    "pA": colors[0],
##    "pB": colors[1],
##    "pA2": colors[2],
##    "pB2": colors[3],
##    "pC": colors[4],
##}
##rename = {
##    "pA": "pA (TS)",
##    "pB": "pB (TS)",
##    "pA2": "pA (RP)",
##    "pB2": "pB (RP)",
##    "pC": "pC (RP)"
##}
##color_for_axis = None
##linestyle_for_label = None
##xlim = 50000
##ylim_top = {
##    0: 20000,
##    1: 200000
##}
##ylim_bot = {
##    0: 0,
##    1: 0
##}
##ncol=7
##axis_distance =  0.3


cm = 1/2.54  # centimeters in inches
fig, ax1 = plt.subplots(1, 1, figsize=(20*cm, 7*cm))

# prepare axis
axs = []
ax1.get_yaxis().set_visible(False)
for i in range(len(y_axis_labels)):
    ax1Twin = ax1.twinx()
    
    ax1Twin.spines["right"].set_position(("axes", 1.0 + i * axis_distance))

    axs.append(ax1Twin)

# plot data
for label in data1:
    print(label)
    if label not in axis_id_for_label:
        continue
    ax = axs[axis_id_for_label[label]]
    color = color_for_label[label]
    ls = linestyle_for_label[label] if linestyle_for_label != None and label in linestyle_for_label and linestyle_for_label[label] != None else "-"
    renamed = label if not rename or label not in rename else rename[label]
    ax.plot(data1[label]["x"], data1[label]["y"], color=color, linestyle=ls, label=renamed)

# limits & axis colors
ax1.set_xlim(left=0,right=xlim)
for i in range(len(y_axis_labels)):
    ylb = None
    if ylim_bot and i in ylim_bot and ylim_bot[i] != None:
        ylb = ylim_bot[i]
    ylt = None
    if ylim_top and i in ylim_top and ylim_top[i] != None:
        ylt = ylim_top[i]
    axs[i].set_ylim(top=ylt, bottom=ylb)

    axs[i].set_ylabel(y_axis_labels[i])
    if color_for_axis and i in color_for_axis and color_for_axis[i] != None:
        axs[i].set_ylabel(y_axis_labels[i], color = color_for_axis[i])
        axs[i].tick_params(axis='y', labelcolor=color_for_axis[i])

# scale changes
##axs[0][0].set_yscale('symlog')

# legend
##fig.legend(bbox_to_anchor=(0.5, 0.95), loc="center", ncol=6, shadow=True, numpoints=2)
##fig.legend(shadow=True, loc="upper center", numpoints=2, ncol=6)
##fig.legend(shadow=True, loc="upper left", numpoints=2, ncol=3)
##fig.legend(shadow=True, loc="upper left", numpoints=2, ncol=1)

##fig.legend(bbox_to_anchor=(0.03, 1.0), shadow=True, loc="upper left", numpoints=2, ncol=3)
##fig.legend(bbox_to_anchor=(0.03, 1.0), shadow=True, loc="upper left", numpoints=2, ncol=6)
fig.legend(bbox_to_anchor=(0.05, 0.95), shadow=True, loc="upper left", numpoints=2, ncol=ncol)

##fig.suptitle("test")
##fig.supxlabel("Time (in s)")
fig.supylabel(title)

fig.tight_layout(pad=0.7, w_pad=0.2, h_pad=0.2)

plt.show()

