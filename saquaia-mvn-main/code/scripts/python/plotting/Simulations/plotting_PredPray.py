import matplotlib.pyplot as plt
import json

plt.rcParams['figure.dpi'] = 300

t_end = 100

with open('values_concrete.json', 'r') as f:
  data_concrete = json.load(f)
  with open('values_abstract.json', 'r') as f:
    data_abstract = json.load(f)
    with open('values.json', 'r') as f:
      data_SSA = json.load(f)
      
      pred_concrete_times = [p["left"] for p in data_concrete["Pred"] if p["left"] <= t_end]
      pred_concrete_values = [p["right"] for p in data_concrete["Pred"] if p["left"] <= t_end]

      prey_concrete_times = [p["left"] for p in data_concrete["Prey"] if p["left"] <= t_end]
      prey_concrete_values = [p["right"] for p in data_concrete["Prey"] if p["left"] <= t_end]

      pred_abstract_times = [p["left"] for p in data_abstract["Pred"] if p["left"] <= t_end]
      pred_abstract_values = [p["right"] for p in data_abstract["Pred"] if p["left"] <= t_end]

      prey_abstract_times = [p["left"] for p in data_abstract["Prey"] if p["left"] <= t_end]
      prey_abstract_values = [p["right"] for p in data_abstract["Prey"] if p["left"] <= t_end]

      pred_SSA_times = [p["left"] for p in data_SSA["Pred"] if p["left"] <= t_end]
      pred_SSA_values = [p["right"] for p in data_SSA["Pred"] if p["left"] <= t_end]

      prey_SSA_times = [p["left"] for p in data_SSA["Prey"] if p["left"] <= t_end]
      prey_SSA_values = [p["right"] for p in data_SSA["Prey"] if p["left"] <= t_end]

cm = 1/2.54
fig, (ax1, ax2, ax3) = plt.subplots(nrows=3, ncols=1, sharex=True, sharey=True, figsize=(16*cm,9*cm))
#fig, (ax1, ax2, ax3, ax4) = plt.subplots(nrows=4, ncols=1, sharex=True, sharey=True, figsize=(12*cm,10*cm))

# background: show levels
##for i in range(10):
##    for ax in (ax1, ax2, ax3):
##        ax.axhline(y=2**i, color='lightgray', linewidth=0.3, linestyle='-')

pred_SSA_line, = ax1.plot(pred_SSA_times, pred_SSA_values, "purple", linewidth=1) 
pred_SSA_line.set_label('Pred (SSA)')
    
prey_SSA_line, = ax1.plot(prey_SSA_times, prey_SSA_values, "green", linewidth=1)
prey_SSA_line.set_label('Prey (SSA)')

ax1.margins(x=0)
    
pred_concrete_line, = ax2.plot(pred_concrete_times, pred_concrete_values, linewidth=1) 
pred_concrete_line.set_label('Pred (SEG)')
    
prey_concrete_line, = ax2.plot(prey_concrete_times, prey_concrete_values, linewidth=1)
prey_concrete_line.set_label('Prey (SEG)')
    
ax2.set_ylabel('Population')
ax2.margins(x=0)

#pred_abstract_linea, = ax2.plot(pred_abstract_times, pred_abstract_values, color=pred_concrete_line.get_color(), linewidth=0.8)
#pred_abstract_lineb, = ax3.plot(pred_abstract_times, pred_abstract_values, ".", dashes=[2, 1.5], linewidth=0.8, markersize=3)
pred_abstract_lineb, = ax3.plot(pred_abstract_times, pred_abstract_values, ".", markersize=2.3)
pred_abstract_lineb.set_label('Pred (sum.SEG)')

#prey_abstract_linea, = ax2.plot(prey_abstract_times, prey_abstract_values, color=prey_concrete_line.get_color(), linewidth=0.8)    
#prey_abstract_lineb, = ax3.plot(prey_abstract_times, prey_abstract_values, ".", dashes=[2, 1.5], linewidth=0.8, markersize=3)
prey_abstract_lineb, = ax3.plot(prey_abstract_times, prey_abstract_values, ".", markersize=2.3)
prey_abstract_lineb.set_label('Prey (sum.SEG)')

#ax4.plot(pred_abstract_times, pred_abstract_values, linewidth=1)
#ax4.plot(prey_abstract_times, prey_abstract_values, linewidth=1)
    
ax3.set_xlabel('Time (in s)')
ax3.margins(x=0)
ax3.set_ylim(bottom=0)
ax3.set_xlim(left=0, right=t_end)

fig.legend(loc="upper center", ncol=3, shadow=True, numpoints=3)
fig.tight_layout()
fig.subplots_adjust(top=0.83) 

plt.savefig('visual_comparison_pred_prey.png', bbox_inches="tight")
fig.show()
  

  
  



