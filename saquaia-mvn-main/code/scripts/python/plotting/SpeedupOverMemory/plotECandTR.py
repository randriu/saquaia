import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
from math import log10, floor
import os

cm = 1/2.54  # centimeters in inches
fig, (ax, ax2) = plt.subplots(1, 2, figsize=(20*cm, 6*cm))
##fig, (ax) = plt.subplots(1, 1, figsize=(16*cm, 6*cm))
##ax2 = ax.twinx()

colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

# EC
x = []
y = []

x.append(0.0)
y.append(1)

x.append(0.3)
y.append(5.028)

x.append(0.6)
y.append(5.546)

x.append(1.0)
y.append(5.941)

x.append(2.0)
y.append(6.943)

x.append(3.5)
y.append(7.773)

x.append(5.0)
y.append(8.048)

ax.plot(x,y, label="SEG+SSA (adaptive)", color=colors[0])


# EC (not adaptive)
x = []
y = []

x.append(0.0)
y.append(1)

x.append(0.3)
y.append(0.8249)

x.append(0.6)
y.append(1.04)

x.append(1.0)
y.append(1.328)

x.append(2.0)
y.append(2.354)

x.append(3.5)
y.append(4.537)

x.append(5.0)
y.append(6.991)

ax.plot(x,y, label="SEG+SSA (non-ad.)", color=colors[0], linestyle="-.")

##ax.tick_params(axis='y', labelcolor=colors[0])

# TR

x = []
y = []

x.append(0.0)
y.append(17.8)

x.append(0.3)
y.append(28.36)

x.append(0.6)
y.append(32.4)

x.append(1.0)
y.append(35.73)

x.append(2.0)
y.append(45.4)

x.append(3.5)
y.append(57.39)

x.append(5.0)
y.append(65.49)

ax2.plot(x,y, label="SEG+HYB (adaptive)", color=colors[1])

# TR (non-adaptive)

x = []
y = []

x.append(0.0)
y.append(17.8)

x.append(0.3)
y.append(17.1)

x.append(0.6)
y.append(18.51)

x.append(1.0)
y.append(21.21)

x.append(2.0)
y.append(27.84)

x.append(3.5)
y.append(39.08)

x.append(5.0)
y.append(49.42)

ax2.plot(x,y, label="SEG+HYB (non-ad.)", color=colors[1], linestyle="-.")


##ax2.tick_params(axis='y', labelcolor=colors[1])

##ax.plot([0,5],[1,1],color=colors[0],linestyle=":",label="SSA",linewidth=3)
##ax.plot([0,5],[0.64,0.64],color=colors[0],linestyle="--",label="HYB")
##ax2.plot([0,5],[1,1],color=colors[1],linestyle=":",label="SSA",linewidth=3)
##ax2.plot([0,5],[17.8,17.8],color=colors[1],linestyle="--",label="HYB")

ax.plot([0,5],[1,1],color="k",linestyle=":",label="SSA",linewidth=2)
ax.plot([0,5],[0.64,0.64],color="k",linestyle="--",label="HYB")
ax2.plot([0,5],[1,1],color="k",linestyle=":",label='_nolegend_',linewidth=2)
ax2.plot([0,5],[17.8,17.8],color="k",linestyle="--", label='_nolegend_')

#ax.legend(shadow=True, loc="center right", labelspacing=0.2)
#ax2.legend(shadow=True, loc="center right", labelspacing=0.2)


fig.supxlabel("Memory Limit (in GB)", x=0.4)
fig.supylabel("Speedup w.r.t. SSA")
##ax.set_ylabel("Speedup w.r.t. SSA")

ax.set_xlim(left=0,right=5.0)
ax2.set_xlim(left=0,right=5.0)
ax.set_ylim(bottom=0)
ax2.set_ylim(bottom=0)

ax.set_title('E.Coli')
ax2.set_title('TSxRP')

##ax.margins(x=0)

##plt.grid(True, which="both", ls=":", color='0.8')

##fig.tight_layout(pad=0.7, w_pad=0.2, h_pad=0.2)

##lgd = fig.legend(shadow=True, ncol=3)
##fig.legend(loc = 'lower center', bbox_to_anchor = (0, -0.3, 1, 1), bbox_transform = plt.gcf().transFigure, ncol=3)

##fig.tight_layout(pad=0.7, w_pad=0.2, h_pad=0.2)

lines_labels = [ax.get_legend_handles_labels() for ax in fig.axes]
lines, labels = [sum(lol, []) for lol in zip(*lines_labels)]
fig.legend(lines, labels, shadow=True, ncol=1, loc='center right', bbox_to_anchor=(1.0, 0.53))

fig.tight_layout(rect=[0, -0.1, 0.75, 1])

plt.show()
