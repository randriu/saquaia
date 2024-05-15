#!/usr/bin/env python3.10
#$ -t 1-400
#$ -tc 400
#$ -o /pub/tmp/saquaia.std.log
#$ -e /pub/tmp/saquaia.err.log
#$ -q all.q@@blade
#$ -N SAQTRAIN
#$ -l ram_free=2G,mem_free=2G
#$ -R y

import os
os.chdir("/homes/kazi/mrazek/saquaia")
import sys
import numpy as np
sys.path.append("/homes/kazi/mrazek/saquaia")
from saqint import ts_new_intermediate_distributions
import json
import gzip

os.makedirs("sge_res/train", exist_ok=True)

task_id = max(0, int(os.getenv('SGE_TASK_ID', 1)) - 1)
d = np.load("train_pts.npz")

# print files in the archive d
print(d.files)



print(d["arr_0"].shape)
X = d["arr_0"][task_id]
print(X)

# how many intermediate time points (including the last one) to keep track of during saquaia call
# make sure this value is the same as numPoints in saquaia/code/src/main/java/core/simulation/Simulation.java::recordHistory()
num_points = 100
# time limit (seconds) per saquaia call
timeout = 60*5

res = {}

# genereate the valuation up to r13
ks = ["r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10", "r11", "r12", "r13"]
valuation = {k: X[i] for i,k in enumerate(ks)}

res["valuation"] = valuation
distributions,runtime = ts_new_intermediate_distributions(
            valuation,method="SEG",sims=500,num_points=num_points,timeout=timeout) 

res["SEG_500_distributions"] = distributions
res["SEG_500_runtime"] = runtime
res["SEG_500_sims"] = 500
json.dump(res, gzip.open(f"sge_res/train/train_{task_id:06d}.json.gz", "wt"), indent=2)


distributions,runtime = ts_new_intermediate_distributions(
            valuation,method="SEG",sims=10000,num_points=num_points,timeout=timeout) 

res["SEG_10000_distributions"] = distributions
res["SEG_10000_runtime"] = runtime
res["SEG_1000_sims"] = 10000
json.dump(res, gzip.open(f"sge_res/train/train_{task_id:06d}.json.gz", "wt"), indent=2)




distributions,runtime_ssa = ts_new_intermediate_distributions(
            valuation,method="SSA",sims=500,num_points=num_points,timeout=timeout) 
res["SSA_sims"] = 500
res["SSA_distributions"] = distributions
res["SSA_runtime"] = runtime_ssa

json.dump(res, gzip.open(f"sge_res/train/train_{task_id:06d}.json.gz", "wt"), indent=2)