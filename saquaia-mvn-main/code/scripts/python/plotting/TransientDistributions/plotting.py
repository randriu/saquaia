import matplotlib.pyplot as plt
import numpy as np
import json

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

def addDistribution(file_name, t, d, cum, label, ax):
    x, mass = orderDistribution(getDistribution(file_name, t, d))
    if (cum):
        x, mass = cumulative((x, mass))
    linestyle = "-"
    if "SSA" in label:
        linestyle = "--"
    line, = ax.plot(x, mass, linestyle=linestyle)
    line.set_label(label)

d = 0
xlim_min = 0
xlim_max = 400
ylim_min = 0
ylim_max = 1
step = 10
species = "RNA"
com = False

massfunc_string = "PDF"
if com:
    massfunc_string = "CDF"

fig = plt.figure()
ax = plt.subplot(111)

for t in range(step,step*20+1,step):
    print(t)
    addDistribution("SSA.json", t, d, com, "t=" + str(t), ax)

ax.margins(x=0)
ax.set_ylim(bottom=ylim_min, top=ylim_max)
ax.set_xlim(left=xlim_min,right=xlim_max)
ax.set_xlabel(species +" Population")
ax.set_ylabel(massfunc_string)
ax.legend(ncol=2, shadow=True)
fig.savefig('SSA_' + massfunc_string + '_over_time.png')
plt.show()



settings = [("SSA", "SSA")#,
            #("SSA as control", "SSA2"),
##            ("Segmental (c=1.3,k=10)", "c=1.3,k=10"),
##            ("Segmental (c=1.3,k=100)", "c=1.3,k=100"),
##            ("Segmental (c=1.3,k=1000)", "c=1.3,k=1000"),
##            ("Segmental (c=1.5,k=10)", "c=1.5,k=10"),
##            ("Segmental (c=1.5,k=100)", "c=1.5,k=100"),
##            ("Segmental (c=1.5,k=1000)", "c=1.5,k=1000"),
##            ("Segmental (c=2,k=10)", "c=2,k=10"),
##            ("Segmental (c=2,k=100)", "c=2,k=100"),
##            ("Segmental (c=2,k=1000)", "c=2,k=1000"),
##            ("Tau-leaping", "Tau-leaping")
            ]

for t in range(step,step*20+1,step):
    print(t)
    fig = plt.figure(figsize=(12*cm,9*cm))
    ax = plt.subplot(111)
    for setting in settings:
        print(setting)
        addDistribution(setting[0] + ".json", t, d, com, setting[1], ax)

        ax.margins(x=0)
        ax.set_ylim(bottom=ylim_min, top=ylim_max)
        ax.set_xlim(left=xlim_min,right=xlim_max)
        ax.set_xlabel(species +" Population at t=" + str(t))
        ax.set_ylabel(massfunc_string)
        if com:
            fig.legend(bbox_to_anchor=(1, 0.18), loc="lower right", ncol=1, shadow=True, numpoints=2, labelspacing=0.2)
        else:
            fig.legend(bbox_to_anchor=(0.9, 0.9), loc="upper right", ncol=2, shadow=True, numpoints=2, labelspacing=0.2)
        fig.tight_layout()
        #fig.subplots_adjust(top=0.7) 
        fig.savefig('t' + str(t) + '_' + massfunc_string + '.png')
        #plt.show()



  
  



