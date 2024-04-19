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

model = "PP"

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
            "max":650,
##            "max":1300,
            "tickwidth": 0.25
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
    xlim = 80
    legend_ncol = 2
    legend_pos = (0.08, 0.92)
    legend_loc = "upper left"



# data
##datafolder = "D:\\Datein\\TUM\\Forschung\\saquaia\\results\\benchmark\\2023-06-08_16.34.50.780\\VI_Visual_ODE\\0\\plotting_data\\sim0.png.csv"
datafolder = "D:\\Datein\\TUM\\Forschung\\saquaia\\results\\benchmark\\2023-06-08_17.06.16.304\\VI_Visual_ODE\\0\\plotting_data\\sim0.png.csv"
##datafolder = "D:\\Datein\\TUM\\Forschung\\sequaia-mvn\\results\\benchmark\\20230202_visual\\PP_Visual_SSA\\plotting_data\\sim105.png.csv"
##datafolder = "D:\\Datein\\TUM\\Forschung\\saquaia\\results\\benchmark\\2023-06-08_17.16.15.345\\VI_Visual_SSA\\0\\plotting_data\\sim3.png.csv"
##datafolder = "D:\\Datein\\TUM\\Forschung\\saquaia\\results\\benchmark\\2023-06-08_17.16.15.345\\VI_Visual_SSA\\0\\plotting_data\\sim4.png.csv"

data = getData(datafolder)

cm = 1/2.54  # centimeters in inches
fig, ax = plt.subplots(1, 1, figsize=(20*cm, 8*cm))


for label in data:   
    color = dimensiondata[label]["color"] if "color" in dimensiondata[label] else "black"
    ls = dimensiondata[label]["linestyle"] if "linestyle" in dimensiondata[label] else "-"
    axisnr = dimensiondata[label]["axis"] 
    l = dimensiondata[label]["label"] if "label" in dimensiondata[label] else label
    ax.plot(data[label]["x"], data[label]["y"], color=color, linestyle=ls, label=l)

            

# y limits
ax.set_ylim(bottom = axisdata[0]["min"] if "min" in axisdata[0] else 0, top = axisdata[0]["max"] if "max" in axisdata[0] else None)

# x limit
ax.set_xlim(left=0,right=xlim)

# model label
fig.supylabel(title, x=0.02)

# x axis label
ax.set_xlabel("Time (in s)")

# y axis labels
ax.yaxis.set_label_position("right")
ax.set_ylabel(axisdata[0]["label"], color=axisdata[0]["color"])
ax.tick_params(axis='y', left=False, labelleft=False, right=True, labelright=True, colors=axisdata[0]["color"])

# legend
fig.legend(bbox_to_anchor=legend_pos, shadow=True, loc=legend_loc, numpoints=2, ncol=legend_ncol)

fig.tight_layout(pad=1.5, w_pad=0.2, h_pad=0.2)

plt.show()

exit()

