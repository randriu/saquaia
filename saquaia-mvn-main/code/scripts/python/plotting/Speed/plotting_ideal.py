import matplotlib.pyplot as plt
import numpy as np
import json
import os

plt.rcParams['figure.dpi'] = 200
cm = 1/2.54

fig = plt.figure(figsize=(12*cm,9*cm))

# 100 linearly spaced numbers
x = list(range(1,2001))

ssa = [1 for v in x]

# setting the axes at the centre
fig = plt.figure()
ax = fig.add_subplot(1, 1, 1)

ax.set_xlabel('Number Of Simulations')
ax.set_ylabel("Time Per Simulation (in s)")

# plot the function
plt.plot(x, ssa, 'g')

ax.set_xlim(left=1, right=2000)
ax.set_ylim(bottom=0)
ax.legend(ncol=2, shadow=True)
fig.savefig('times.png')
plt.show()

