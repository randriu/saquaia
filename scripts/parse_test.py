#%%%
import pandas as pd
import json
import numpy as np
import gzip 
import glob
from tqdm import tqdm
import pickle
# %%
for fn in glob.glob("../sge_res/test/test_*.json.gz"):
    print(fn)
    d = json.load(gzip.open(fn, "rt"))
    print(d["SSA_runtime"])
    print(d["valuation"])
    print(d["SSA_distributions"])
    break

# %%



#dist2d(d["SEG_10000_distributions"])
#dist2d(d["SSA_distributions"])
# %%

from saqdataset import Dataset


SSA = Dataset()

stat = {}
for i, fn in tqdm(enumerate(glob.glob("../sge_res/test/test_*.json.gz"))):
    #print(fn)
    d = json.load(gzip.open(fn, "rt"))
    if not d["SSA_distributions"]:
        print("No SSA distributions", fn)
        continue
    stat[fn] = {
        "SSA_runtime": d["SSA_runtime"],
        "valuation": d["valuation"],
        **d["valuation"],
    }

    SSA.add(d["valuation"], d["SSA_distributions"])



SSA.save("../sge_res/test.SSA")
df_stat = pd.DataFrame(stat).T
df_stat.to_pickle("../sge_res/test.stat.pkl.gz")
# %%
len(SSA.X)
# %%
