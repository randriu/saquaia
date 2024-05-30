#%%%%
import numpy as np
import pickle
import glob
from tqdm import tqdm
import h5py
# %%

ds = "SEG_10000"
var = "X"

def load_and_save(ds):
    allx = []
    for fn in tqdm(glob.glob(f"../sge_res/checkpoint/train.{ds}.*.X.pkl")):
        allx += pickle.load(open(fn, "rb"))

    ally = []
    for fn in tqdm(glob.glob(f"../sge_res/checkpoint/train.{ds}.*.Y.pkl")):
        ally += pickle.load(open(fn, "rb"))


    with h5py.File(f"{ds}.jld2", "w") as f:
        f.create_dataset("X_test", data=np.array(allx))
        # save y as array of objects of floats
        kk = f.create_dataset("Y_test", len(ally), dtype=h5py.special_dtype(vlen=float))
        for i, j in tqdm(enumerate(ally), desc="Saving Y"):
            kk[i] = j







for ds in ["SEG_500", "SEG_10000", "SSA"]:
    print(ds)
    load_and_save(ds)