import matplotlib.pyplot as plt
import json
import os

plt.rcParams['figure.dpi'] = 200

t_end = 15000

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

species = ["pA", "pB", "pC"]

cm = 1/2.54
fig, (ax1, ax2) = plt.subplots(nrows=2, ncols=1, sharex=True, sharey=True, figsize=(12*cm,9*cm))


for s in species:
    times, values = getSim("values.json", s)
    line, = ax1.plot(times, values)
    #line.set_label(s)

for s in species:
    times, values = getSim("values_abstract.json", s)
    line, = ax2.plot(times, values)
    line.set_label(s)

    
ax1.margins(x=0)
ax1.set_ylim(bottom=0)
ax1.set_xlim(left=0, right=t_end)
ax1.set_ylabel('Pop. (SSA)')
ax2.set_ylabel('Pop. (SEG)')
ax2.set_xlabel('Time (in s)')

##
##
##pred_SSA_line, = ax1.plot(pred_SSA_times, pred_SSA_values, "purple", linewidth=1) 
##pred_SSA_line.set_label('Pred (SSA)')
##    
##prey_SSA_line, = ax1.plot(prey_SSA_times, prey_SSA_values, "green", linewidth=1)
##prey_SSA_line.set_label('Prey (SSA)')
##
##ax1.margins(x=0)
##ax1.set_ylim(bottom=0)
##    
##pred_concrete_line, = ax2.plot(pred_concrete_times, pred_concrete_values, linewidth=1) 
##pred_concrete_line.set_label('Pred (SEG)')
##    
##prey_concrete_line, = ax2.plot(prey_concrete_times, prey_concrete_values, linewidth=1)
##prey_concrete_line.set_label('Prey (SEG)')
##    
##ax2.set_ylabel('Population')
##ax2.margins(x=0)
##ax2.set_ylim(bottom=0)
##
###pred_abstract_linea, = ax2.plot(pred_abstract_times, pred_abstract_values, ".", color=pred_concrete_line.get_color(), linewidth=0.8, markersize=2.3)
###pred_abstract_lineb, = ax3.plot(pred_abstract_times, pred_abstract_values, ".", dashes=[2, 1.5], linewidth=0.8, markersize=2.3)
##pred_abstract_lineb, = ax3.plot(pred_abstract_times, pred_abstract_values, ".", dashes=[1.8, 1], linewidth=0.8, markersize=1.8)
##pred_abstract_lineb.set_label('Pred (acc.SEG)')
##
###prey_abstract_linea, = ax2.plot(prey_abstract_times, prey_abstract_values, ".", color=prey_concrete_line.get_color(), linewidth=0.8, markersize=2.3)    
###prey_abstract_lineb, = ax3.plot(prey_abstract_times, prey_abstract_values, ".", dashes=[2, 1.5], linewidth=0.8, markersize=2.3)
##prey_abstract_lineb, = ax3.plot(prey_abstract_times, prey_abstract_values, ".", dashes=[2, 1], linewidth=0.8, markersize=1.8)
##prey_abstract_lineb.set_label('Prey (acc.SEG)')
##    
##ax3.set_xlabel('Time (in s)')
##ax3.margins(x=0)
##ax3.set_ylim(bottom=0)
##ax3.set_xlim(left=0, right=t_end)
##
fig.legend(bbox_to_anchor=(0.6, 0.48), loc="center", ncol=3, shadow=True, numpoints=2)
fig.tight_layout()
#fig.subplots_adjust(top=0.83) 

plt.savefig('test.png', bbox_inches="tight")
fig.show()
  

  
  





