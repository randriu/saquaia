import os

data_file = os.path.abspath(
    os.path.join(
        os.path.dirname( __file__ ), 
        '..', 
        '..', 
        'out', 
        'benchmark', 
        '20230211_5G_EC_TR', 
        'benchmark_log.txt'
    )
)
print(data_file)

import csv

data = {}

summed = {}
dic = {}

with open(data_file, newline='') as file:
    for line in file:
        line = line.strip()
        if len(line) > 15 and line[0:14] == "#### Benchmark":
            if dic and summed:
                print(label)
                for cat in dic:
                    print(f'{cat}={dic[cat] / summed[cat]}        ({summed[cat]})')
                print()
            summed = {}
            dic = {}
            label = line.split(": ")[1].split(" ####")[0]
        if "=" in line:
            cat = line.split("=")[0]
            try:
                value = float(line.split("=")[1])
                #print(f'{label}: {cat} -> {value}')
                if cat not in dic:
                    dic[cat] = 0
                dic[cat] += value
                if cat not in summed:
                    summed[cat] = 0
                summed[cat] += 1
            except ValueError:
                pass

            
