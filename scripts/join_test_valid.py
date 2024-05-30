#%%%%
import numpy as np
import pickle
import glob
from tqdm import tqdm
import h5py
# %%

def load_and_save(ds):
    allx = pickle.load(open(f"../sge_res/{ds}.SSA.X.pkl", "rb"))

    ally = pickle.load(open(f"../sge_res/{ds}.SSA.Y.pkl", "rb"))


    with h5py.File(f"parsed/{ds}.h5", "w") as f:
        f.create_dataset("X", data=np.array(allx))
        # save y as array of objects of floats
        kk = f.create_dataset("Y", len(ally), dtype=h5py.special_dtype(vlen=float))
        for i, j in tqdm(enumerate(ally), desc="Saving Y"):
            kk[i] = j







for ds in ["valid", "test"]:
    print(ds)
    load_and_save(ds)