import matplotlib.pyplot as plt
import numpy as np
import json

plt.rcParams['figure.dpi'] = 200
cm = 1/2.54

def prettyName(s):
    s = s[:-5]
    prefix = "Segmental ("
    if prefix in s:
        s = s[len(prefix):]
        s = s[:-1]
    elif s == "SSA as control":
        return "SSA (control)"
    return s

species = "P1"
species_pretty = "P1"

fig = plt.figure()
ax = plt.subplot(111)
ax.margins(x=0)
ax.set_xlabel('Time (in s)')
ax.set_ylabel("EMD (" + species_pretty + ")")

with open('EMDs.json', 'r') as f:
  emd_data = json.load(f)

for file_name in emd_data:
    pretty = prettyName(file_name)
    emd_data_for_species = emd_data[file_name][species]
    linestyle = "-"
    if "SSA" in pretty:
        linestyle = "--"
    line, = ax.plot([float(t) for t in emd_data_for_species], [emd_data_for_species[t] for t in emd_data_for_species], linestyle=linestyle)
    line.set_label(pretty)

ax.set_ylim(bottom=0, top=43)
ax.legend(ncol=3, shadow=True)
fig.savefig('EMD_over_time.png')
plt.show()


fig = plt.figure()
ax = plt.subplot(111)
ax.margins(x=0)
ax.set_xlabel('Time (in s)')
ax.set_ylabel("Mean (" + species_pretty + ")")

with open('means.json', 'r') as f:
    mean_data = json.load(f)

for file_name in mean_data:
    if file_name == "SSA as control.json":
        continue
    pretty = prettyName(file_name)
    mean_data_for_species = mean_data[file_name][species]
    linestyle = "-"
    if "SSA" in pretty:
        linestyle = "--"
    line, = ax.plot([float(t) for t in mean_data_for_species], [mean_data_for_species[t] for t in mean_data_for_species], linestyle=linestyle)
    line.set_label(pretty)

ax.set_ylim(bottom=0, top=77)
ax.legend(ncol=3, shadow=True)
fig.savefig('mean_over_time.png')
plt.show()


fig = plt.figure()
ax = plt.subplot(111)
ax.margins(x=0)
ax.set_xlabel('Time (in s)')
ax.set_ylabel("Variance (" + species_pretty + ")")

with open('vars.json', 'r') as f:
  var_data = json.load(f)

for file_name in var_data:
    if file_name == "SSA as control.json":
        continue
    pretty = prettyName(file_name)
    var_data_for_species = var_data[file_name][species]
    linestyle = "-"
    if "SSA" in pretty:
        linestyle = "--"
    line, = ax.plot([float(t) for t in var_data_for_species], [var_data_for_species[t] for t in var_data_for_species], linestyle=linestyle)
    line.set_label(pretty)

ax.set_ylim(bottom=0, top=4400)
ax.legend(ncol=3, shadow=True)
fig.savefig('var_over_time.png')
plt.show()

        



##cm = 1/2.54
##fig, (ax1, ax2, ax3) = plt.subplots(nrows=3, ncols=1, sharex=True, sharey=True, figsize=(12*cm,9*cm))
##
### background: show levels
####for i in range(10):
####    for ax in (ax1, ax2, ax3):
####        ax.axhline(y=2**i, color='lightgray', linewidth=0.3, linestyle='-')
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
##fig.legend(loc="upper center", ncol=3, shadow=True, numpoints=2)
##fig.tight_layout()
##fig.subplots_adjust(top=0.83) 
##
##plt.savefig('visual_comparison_pred_prey.png', bbox_inches="tight")
##fig.show()
##  

  
  



