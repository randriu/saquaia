import matplotlib.pyplot as plt
import numpy as np
import json
import math

plt.rcParams['figure.dpi'] = 200
cm = 1/2.54


def getDistribution(file_name, t, d):
    distribution = {}
    with open(file_name, 'r') as f:
        data = json.load(f)
        for data_i in data:
            if data_i["time"] == t:
                for pair in data_i["distribution"]:
                    #print(pair)
                    value = pair[0]["vector"][d]
                    mass = pair[1]
                    if value in distribution:
                        distribution[value] = distribution[value] + mass
                    else:
                        distribution[value] = mass
                return distribution

def orderDistribution(distribution):
    x = [value for value in distribution]
    mass = [distribution[value] for value in distribution]
    order = np.argsort(x)
    x = np.array(x)[order]
    mass = np.array(mass)[order]
    return x, mass

def cumulative(xAndMass):
    x, mass = xAndMass
    comulative_mass = []
    sum = 0
    for m in mass:
        sum = sum + m
        comulative_mass.append(sum)
    return x, comulative_mass

def toAbstract(xAndMass, c=2):
    x, mass = xAndMass
    massAbs = {}
    for i in range(len(x)):
        x_i = x[i]
        mass_i = mass[i]
        level = 0 if x_i == 0 else int(math.floor(math.log(x_i, c))+1)
        if level not in massAbs:
            massAbs[level] = 0
        massAbs[level] = massAbs[level] + mass_i
    return massAbs

def uniform(comulative_mass, c=2):
    print(comulative_mass)
    x = []
    mass = []
    for level in sorted(comulative_mass):
        if level == 0:
            x.append(level)
            mass.append(comulative_mass[level])
            print("yay")
            continue
        min_x = int(round(c**(level-1)))
        max_x = int(round(c**level))-1
        print(min_x, max_x, level)
        if min_x >= max_x:
            x.append(level)
            mass.append(comulative_mass[level])
            continue
        for x_i in range(min_x, max_x+1):
            x.append(x_i)
            mass.append(comulative_mass[level] / (max_x + 1 - min_x))
    return x, mass
    

def addDistribution(file_name, t, d, cum, label, ax):
    x, mass = orderDistribution(getDistribution(file_name, t, d))
    if (cum):
        x, mass = cumulative((x, mass))
    linestyle = "-"
    if "SSA" in label:
        linestyle = "--"
    line, = ax.plot(x, mass, linestyle=linestyle)
    line.set_label(label)

c = 1.5

fig = plt.figure(figsize=(12*cm,9*cm))
ax = plt.subplot(111)

levels_SSA = toAbstract(orderDistribution(getDistribution("SSA.json", 200, 1)), c)
print(sum(levels_SSA.values()))
ax.bar([l-0.2 for l in sorted(levels_SSA)], [levels_SSA[l] for l in sorted(levels_SSA)], width=0.4, align='center', label="SSA")
levels_SEG = toAbstract(orderDistribution(getDistribution("Segmental (c=" + str(c) + ",k=100).json", 200, 1)), c)
print(sum(levels_SEG.values()))
ax.bar([l+0.2 for l in sorted(levels_SEG)], [levels_SEG[l] for l in sorted(levels_SEG)], width=0.4, align='center', label="abstract Segmental")
ax.set_xlabel("RNA Interval")
ax.set_ylabel("Mass")

all_levels = {l for l in levels_SSA} | {l for l in levels_SEG}

ax.set_xticks(sorted(all_levels))
ax.set_xticklabels([i if i == 0 or int(round(c**(i-1))) >= int(round(c**(i))-1) else "[" + str(int(round(c**(i-1)))) + "," + str(int(round(c**(i))-1)) + "]" for i in sorted(all_levels)], rotation = 45)
ax.legend(shadow=True)
fig.tight_layout()
fig.savefig('RNA_per_interval.png')
plt.show()


fig = plt.figure(figsize=(12*cm,9*cm))
ax = plt.subplot(111)


#x1, mass1 = uniform(toAbstract(orderDistribution(getDistribution("SSA.json", 200, 1)), c), c)
x1, mass1 = orderDistribution(getDistribution("SSA.json", 200, 1))
ax.bar([v-0.2 for v in x1], mass1, width=0.4, align='center', label="SSA")
#x2, mass2 = orderDistribution(getDistribution("Segmental (c=" + str(c) + ",k=100).json", 200, 1))
x2, mass2 = uniform(toAbstract(orderDistribution(getDistribution("Segmental (c=" + str(c) + ",k=100).json", 200, 1)), c), c)
ax.bar([v+0.2 for v in x2], mass2, width=0.4, align='center', label="abstract Segmental")
ax.set_xlim(left=-1, right=38)
ax.set_xlabel("RNA")
ax.set_ylabel("Mass")
ax.legend(shadow=True)
fig.tight_layout()
fig.savefig('RNA_predicted.png')
plt.show()

input()



