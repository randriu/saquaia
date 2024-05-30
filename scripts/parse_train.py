#%%%
import pandas as pd
import json
import numpy as np
import gzip 
import glob
from tqdm import tqdm
import pickle
# %%
for fn in glob.glob("../sge_res/train/train_*.json.gz"):
    print(fn)
    d = json.load(gzip.open(fn, "rt"))
    print(d["SEG_500_runtime"])
    print(d["SEG_10000_runtime"])
    print(d["SSA_runtime"])
    print(d["valuation"])
    print(d["SEG_500_distributions"])
    print(d["SEG_10000_distributions"])
    print(d["SSA_distributions"])
    break

# %%



#dist2d(d["SEG_10000_distributions"])
#dist2d(d["SSA_distributions"])
# %%

from saqdataset import Dataset


SEG_500 = Dataset()
SEG_10000 = Dataset()
SSA = Dataset()

stat = {}
i = 0
for fn in tqdm(glob.glob("../sge_res/train/train_*.json.gz")):
    #print(fn)
    d = json.load(gzip.open(fn, "rt"))

    stat[fn] = {
        "valuation": d["valuation"],
        **d["valuation"],
    }


    if "SSA_distributions" not in d or not d["SSA_distributions"]:
        print("No SSA distributions", fn)
    else:
        stat[fn]["SSA_runtime"] = d["SSA_runtime"]
        SSA.add(d["valuation"], d["SSA_distributions"])


    if "SEG_500_distributions" not in d or not d["SEG_500_distributions"]:
        print("No SEG_500 distributions", fn)
    else:
        stat[fn]["SEG_500_runtime"] = d["SEG_500_runtime"]
        SEG_500.add(d["valuation"], d["SEG_500_distributions"])

    if "SEG_10000_distributions" not in d or not d["SEG_10000_distributions"]:
        print("No SEG_10000 distributions", fn)
    else:
        stat[fn]["SEG_10000_runtime"] = d["SEG_10000_runtime"]
        SEG_10000.add(d["valuation"], d["SEG_10000_distributions"])

    i += 1
    if i % 2000 == 0:
        print("Checkpoint", i)
        SSA.save(f"../sge_res/checkpoint/train.SSA.{i:06d}")
        SEG_500.save(f"../sge_res/checkpoint/train.SEG_500.{i:06d}")
        SEG_10000.save(f"../sge_res/checkpoint/train.SEG_10000.{i:06d}")
        df_stat = pd.DataFrame(stat).T
        df_stat.to_pickle(f"../sge_res/checkpoint/train.stat.{i:06d}.pkl.gz")
        
        stat = {}
        SSA.clean()
        SEG_500.clean()
        SEG_10000.clean()


print("Checkpoint", i)
SSA.save(f"../sge_res/checkpoint/train.SSA.{i:06d}")
SEG_500.save(f"../sge_res/checkpoint/train.SEG_500.{i:06d}")
SEG_10000.save(f"../sge_res/checkpoint/train.SEG_10000.{i:06d}")
df_stat = pd.DataFrame(stat).T
df_stat.to_pickle(f"../sge_res/checkpoint/train.stat.{i:06d}.pkl.gz")

stat = {}
SSA.clean()
SEG_500.clean()
SEG_10000.clean()

# SEG_500.save("../sge_res/train.SEG_500")
# SEG_10000.save("../sge_res/train.SEG_10000")
# SSA.save("../sge_res/train.SSA")
# df_stat = pd.DataFrame(stat).T
# df_stat.to_pickle("../sge_res/train.stat.pkl.gz")
# %%
len(SSA.X)
# %%
