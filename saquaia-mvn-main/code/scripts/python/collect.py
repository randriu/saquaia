import sys

def getAll(file_path,  benchmarks=4, ks=[10, 100, 1000], cs=[2.0, 1.5, 1.3], categories=None):
    if not categories:
        categories = [
            "nr of simulations: ",
            "total nr of simulated reactions: ",
            "average nr of simulated reactions per simulation: ",
            "total nr of simulated segments: ",
            "average nr of simulated segments per simulation: ",
            "average segment length: ",
            "time: ",
            "time per simulation: ",
            "speedup factor: ",
            "abstraction: nr of reachable interval states: ",
            "abstraction: nr of computed segments: ",
            "abstraction: nr of reused segments: ",
            "abstraction: nr of used segments: ",
            "abstraction: size: ",
            "abstraction: readable size: ",
            "exported abstraction: size: "
        ]
    print("order" + "\n" + "&".join(["SSA\n"] + [f"c={c},k={k}\n" for k in ks for c in cs]))
        
    for cat in categories:
        print("\n" + cat + "\n")
        for b in range(1,benchmarks+1):
            r = [get1(file_path, b, None, None, cat)] + [get1(file_path, b, k, c, cat) for k in ks for c in cs]
            print(f"Efficiency Benchmark {b}/{benchmarks}" + "\n" + "&".join(r))
        

def get1(file_path,  b, k, c, cat):
    search_string = "SSA simulations"
    if k and c:
        search_string = f"SEG simulation (c={c},k={k})"
    
    file = open(file_path, 'r')
    lines = file.readlines()

    right_b = False
    right_sec = False
    for line in lines:
        if f"Efficiency Benchmark {b}" in line:
            right_b = True
        if not right_b:
            continue
        if f"Starting {search_string}" in line:
            right_sec = True
        if right_sec and f"Ended {search_string}" in line:
            break
        if right_sec and cat in line:
            return line.split(cat)[-1]
    return "?\n"
            

file_path = sys.argv[1]

getAll(file_path)
