#!/usr/bin/python3

import re
import json
import os, shutil
import time
import collections
import random

class Reaction:
    def __init__(self, reactants: list[int], products: list[int], rate: str):
        assert len(reactants) == len(products)
        self.reactants = reactants
        self.products = products
        self.rate = rate

    def __str__(self):
        return f"{self.reactants} ->{self.rate} {self.products}"

    def num_reactants(self) -> int:
        return len(self.reactants)

    def compute_rate(self, valuation: dict[str,float]) -> float:
        rate = str(self.rate)
        params = re.findall(r'[^\W\d]\w*',rate)
        for param in params:
            value = valuation[param]
            rate = rate.replace(param,str(value))
        return eval(rate)

    def export(self, valuation: dict[str,float]) -> str:
        self_json = {}
        self_json["reactants"] = self.reactants
        self_json["products"] = self.products
        self_json["rate_constant"] = self.compute_rate(valuation)
        return self_json


    @classmethod
    def parse_reactants(cls, reactants) -> list[str]:
        reactants = reactants.split("+")
        reactants = [r for r in reactants if r != "0"]
        return reactants

    


class CRN:

    def __init__(self, name: str, species: list[str], reactions: list[Reaction], params: list[str]):
        self.name = name
        self.species = species
        self.reactions = reactions
        self.params = params
        self.species_initial = None
        self.species_bound = None
    
    def set_species_initial(self, species_initial):
        self.species_initial = [species_initial[x] for x in self.species]
    
    def set_species_bound(self, species_bound):
        self.species_bound = [species_bound[x] for x in self.species]
    
    def __str__(self) -> str:
        s = ""
        s += f"name = {self.name}\n"
        s += f"species = {self.species}\n"
        for r in self.reactions:
            s += str(r) + "\n"
        s += f"params = {self.params}\n"
        return s

    def export_crn(self, valuation: dict[str,float]):
        json_crn = {}
        json_crn["name"] = self.name
        json_crn["speciesNames"] = self.species
        json_crn["reactions"] = [r.export(valuation) for r in self.reactions]
        return json_crn

    def export(self, valuation: dict[str,float]):
        result = {}
        result["name"] = self.name
        assert self.species_initial is not None, "initial state was not set"
        result["initial_state"] = self.species_initial
        assert self.species_bound is not None, "species bounds were not set"
        result["bounds"] = self.species_bound
        result["crn"] = self.export_crn(valuation)
        return result


    @classmethod
    def create_crn_from_string(cls, name: str, equations: str):
        lines = equations.split("\n")[1:-1]
        reactions = []
        for index,line in enumerate(lines):
            line = line.replace(" ","")
            match = re.findall(r"^(.*?),(.*?)-->(.*?)$", line)[0]
            rate = match[0]
            reactants = Reaction.parse_reactants(match[1])
            products = Reaction.parse_reactants(match[2])
            reactions.append((rate, reactants, products))
        species = set()
        for _,reactants,products in reactions:
            for x in reactants:
                species.add(x)
            for x in products:
                species.add(x)
        species = list(species)
        species = sorted(species)

        reaction_list = []
        params = set()
        for rate,reactants,products in reactions:
            reactants_coefficients = [(1 if s in reactants else 0) for s in species]
            products_coefficients = [(1 if s in products else 0) for s in species]
            reaction = Reaction(reactants_coefficients,products_coefficients,rate)
            reaction_list.append(reaction)
            reaction_params = re.findall(r'[^\W\d]\w+',rate)
            for p in reaction_params:
                params.add(p)
        return CRN(name, species, reaction_list, params)


class BenchmarkFactory:
    
    def __init__(self, prefix: str, crn: CRN, end_t: float, sims: int, seed: int, c: float):
        self.prefix = prefix
        
        self.crn = crn
        self.end_t = end_t
        
        self.sims = sims
        self.seed = seed
        self.c = c

        self.timeout = 60

    def new(self, suffix, method, valuation, num_points):
        assert method in ["SSA","SEGSSA","SEGHYB"]
        
        simconf = {}
        if method == "SSA":
            simconf["type"] = "SSA"
        else:
            simconf["type"] = "SEG"
            simconf["memory_free_target_fraction"] = 0.85
            simconf["max_segment_count_per_direction"] = 10
            simconf["min_segments_for_stats"] = 10
            simconf["memory_fraction_for_abs_cache"] = 0.1
            if method == "SEGSSA":
                simconf["baseConfig"] = {"type":"SSA"}
            else:
                simconf["baseConfig"] = {
                    "type": "HYB",
                    "threshold_tau": 5,
                    "threshold_ode": 400,
                    "factor_SSA_to_TAU": 2,
                    "factor_TAU_to_ODE": 2,
                    "ssa_config": { "type": "SSA" },
                    "tau_config": {"epsilon": 0.03, "type": "TAU"},
                    "ode_config": {"min_step": 1.0E-12, "max_step": 100.0, "scal_absolute_tolerance": 0.001, "scal_relative_tolerance": 1.0E-8, "type": "ODE"}
                }
        task = self.crn.export(valuation)

        result = {}
        if num_points is None:
            raise Exception("transient analysis in Saquaia is busted, do not use it")
            result["type"] = "TRANSIENTNEW"
        else:
            result["type"] = "VISUAL"
            result["plot_first"] = 0
        result["name"] = self.prefix + suffix
        result["simconf"] = simconf
        
        result["repeat"]=1
        result["sims"]=self.sims
        result["timeout"]=self.timeout*1000
        result["end_t"] = self.end_t
        
        if self.seed is not None:
            result["seed"]=self.seed

        if method in ["SEGSSA","SEGHYB"]:
            assert self.c is not None
            result["c"] = self.c

        result["setting"] = task        
        return result


def saquaia_parse_distribution(result_file: str, species: list[str]):
    '''
    :return a distionary of state-probability pairs
    :return runtime (s)
    '''
    raise Exception("obsolete method")
    assert os.path.isfile(result_file)
    output = json.load(open(result_file,"rt"))
    output = output[0]
    states = output["states"]
    num_samples = len(states)
    state_count = collections.defaultdict(int)

    species_max = [0 for s in species]
    for state in states:
        state = tuple([int(value) for value in state])
        state_count[state] += 1
        for s,_ in enumerate(species):
            species_max[s] = max(species_max[s],state[s])
    # print("species_max = ", species_max)

    # print(f"found {len(state_count)} distinct samples")
    state_prob = {state:count/num_samples for state,count in state_count.items()}
    state_prob = list(state_prob.items())
    computation_time = output["comp_time_sum"][-1] / 1e9

    return state_prob, computation_time


def saquaia_parse_simulations(sim_folder: str, species: list[str], end_time: float, num_points: int):
    time_states = [[] for time in range(num_points)]
    for sim_file in os.listdir(sim_folder):
        output = json.load(open(f'{sim_folder}/{sim_file}',"rt"))
        history = output["history"]
        history = history[1:] # trim initial state
        
        for state_time in history:
            time = state_time["right"]
            time_index = int(time / (end_time / num_points))-1
            time_index = min(time_index,num_points-1)
            state = state_time["left"]
            time_states[time_index].append(state)

    # convert to distributions
    distributions = []
    for time in range(num_points):
        state_count = collections.defaultdict(int)
        states = time_states[time]
        for state in states:
            state = tuple([int(value) for value in state])
            state_count[state] += 1
        state_prob = {state:count/len(states) for state,count in state_count.items()}
        state_prob = list(state_prob.items())
        distributions.append(state_prob)
    return distributions

def saquaia_run(bf: BenchmarkFactory, suffix: str, method: str, valuation, num_points: int, cleanup=True):
    '''
    :param cleanup if True, auxiliary files created to communicate with saquaia will be removed
    :return a dictionary of state-probability pairs
    :return runtime (s)
    '''
    pwd = os.getcwd()

    result_dir = "/tmp/saquaia-result/pid{}-r{:05x}".format(os.getpid(), random.randint(0, 65536) )
    os.makedirs(result_dir, exist_ok=True)

    # write benchmark
    benchmark_file = f"{result_dir}/benchmarks_auto.json"
    result_dir = f"{result_dir}/saquaia-result"
    benchmark = bf.new(suffix,method,valuation,num_points)
    json.dump([benchmark], open(benchmark_file,"wt") ,indent=2)

    # run saquaia

    start_time = time.time()
    command = f'java -jar {pwd}/saquaia-mvn-main/code/target/saquaia-jar-with-dependencies.jar -f {benchmark_file} -o {result_dir}'
    os.system(command)
    runtime = time.time()-start_time

    # check timeout
    out_file = f'{result_dir}/{benchmark["name"]}/out.txt'
    assert os.path.isfile(out_file), f"{out_file} does not exist"
    with open(out_file, 'r') as file:
        lines = file.readlines()
    timeout_reached = False
    for line in lines:
        if re.search(r'benchmark timed out', line):
            timeout_reached = True
    if timeout_reached:
        return None,runtime

    # collect result
    assert os.path.isfile(benchmark_file), f"{benchmark_file} does not exist"
    result_file = f'{result_dir}/{benchmark["name"]}/result.json'
    sim_folder = f'{result_dir}/{benchmark["name"]}/0/data'
    species = benchmark["setting"]["crn"]["speciesNames"]
    distributions = saquaia_parse_simulations(sim_folder,species,benchmark["end_t"],num_points)

    if cleanup:
        os.remove(benchmark_file)
        shutil.rmtree(result_dir)

    return distributions,runtime


def marginalize(distribution, species_index):
    result = collections.defaultdict(float)
    for state,prob in distribution:
        result[state[species_index]] += prob
    result = list(result.items())
    return result

def plot_distributions(method_distribution):
    import matplotlib.pyplot as plt
    for method,points in method_distribution.items():
        xs = [x for x,y in points]
        xmin = min(xs)
        xmax = max(xs)
        ys = [0 for x in range(xmin,xmax+1)]
        for x,y in points:
            ys[x-xmin] = y
        xs = list(range(xmin,xmax+1))
        plt.plot(xs, ys, label=method)
    plt.legend()
    plt.show()

def load_benchmark_mapk():
    equations = '''
        60 * c2, pSTL_on --> pSTL_off
        60 * c3 * 225, pSTL_on --> pSTLwCR
        60 * c4, pSTLwCR --> pSTL_on
        60 * c5, pSTLwCR --> pSTLwCR + mRNA
        60 * c6, mRNA --> mRNA + pSTL1_qV
        60 * c7, pSTL1_qV --> 0
        60 * c8, mRNA --> 0
        '''
    crn = CRN.create_crn_from_string("mapk", equations)
    valuation = None
    return crn,valuation

def load_benchmark_ts_nessie():
    equations = '''
        σ_bB, P_A + G_uB --> G_bB
        σ_uB, G_bB --> P_A + G_uB
        σ_bA, P_B + G_uA --> G_bA
        σ_uA, G_bA --> P_B + G_uA
        ρ_uA, G_uA --> G_uA + M_A
        ρ_bA, G_bA --> G_bA + M_A
        γ_A, M_A --> M_A + P_A
        δ_mA, M_A --> 0
        δ_p, P_A --> 0
        ρ_uB, G_uB --> G_uB + M_B
        ρ_bB, G_bB --> G_bB + M_B
        γ_B, M_B --> M_B + P_B
        δ_mB, M_B --> 0
        1, P_B --> 0
        σ_bM, P_A + M_B --> PAMB
        σ_uM, PAMB --> P_A + M_B
        δ_pm, PAMB --> 0
        '''
    crn = CRN.create_crn_from_string("ts", equations)
    initial_state = {x:0 for x in crn.species}
    for s in ["G_uA","G_uB"]:
        initial_state[s] = 1
    crn.set_species_initial(initial_state)
    crn.set_species_bound({x:1000 for x in crn.species})

    # valuation is a dictionary of parameter-value pairs

    # try some meaningful parameter valuation (copied from Nessie)
    param_domain = {
        "σ_bB" :  [0,0.0005],
        "σ_uB" :  [0,0.1],
        "σ_bA" :  [0,0.0005],
        "σ_uA" :  [0,0.1],
        "ρ_uA" :  [0,500],
        "ρ_bA" :  [0,500],
        "γ_A" :  [0,12],
        "δ_mA" :  [1,20],
        "δ_p" :  [0,2],
        "ρ_uB" :  [0,500],
        "ρ_bB" :  [0,500],
        "γ_B" :  [0,12],
        "δ_mB" :  [1,20],
        "σ_uM" :  [0,2],
        "σ_bM" :  [0,0.5],
        "δ_pm" :  [0,100]
    }
    # valuation = {p:(domain[0]+domain[1])/2 for p,domain in param_domain.items()}
    
    valuation = {}
    valuation["σ_bB"] = 0.000286697
    valuation["σ_uB"] = 0.072024539
    valuation["σ_bA"] = 0.000271042
    valuation["σ_uA"] = 0.046011351
    valuation["ρ_uA"] = 196.6094971
    valuation["ρ_bA"] = 185.2874756
    valuation["γ_A"] = 9.632446289
    valuation["δ_mA"] = 8.591766357
    valuation["δ_p"] = 1.00201416
    valuation["ρ_uB"] = 379.8675537
    valuation["ρ_bB"] = 104.9957275
    valuation["γ_B"] = 10.09973145
    valuation["δ_mB"] = 10.41592407
    valuation["σ_uM"] = 1.389099121
    valuation["σ_bM"] = 0.201980591
    valuation["δ_pm"] = 89.55383301

    return crn,valuation


def load_benchmark_ts_plos():
    equations = '''
        r0, 0 --> MA
        r1, 0 --> MB
        r2, MA --> 0
        r3, MB --> 0
        r4, PA --> 0
        r5, MA --> SA
        r6, MB --> SB
        r7, MB + SA --> SA
        r8, MB + SB --> SB
        r9, PB --> 0
        r10, SA --> 0
        r11, SB --> 0
        r12, SA --> SA + PA
        r13, SB --> SB + PB
        '''
    crn = CRN.create_crn_from_string("ts", equations)
    initial_state = {x:0 for x in crn.species}
    crn.set_species_initial(initial_state)
    crn.set_species_bound({"MA":16,"MB":16,"SA":512,"SB":512,"PA":500000,"PB":500000})
    return crn

def ts_default_valuation():
    valuation = {
        "r0" : 1,
        "r1" : 1,
        "r2" : 0.1,
        "r3" : 0.1,
        "r4" : 0.1,
        "r5" : 5,
        "r6" : 5,
        "r7" : 20,
        "r8" : 20,
        "r9" : 0.1,
        "r10" : 0.01,
        "r11" : 0.01,
        "r12" : 10,
        "r13" : 10
    }
    return valuation

def ts_param_domain():
    default_valuation = ts_default_valuation()
    param_domain = {param : [value/2,value*2] for param,value in default_valuation.items()}
    return param_domain

def ts_new_valuation():
    param_domain = ts_param_domain()
    valuation =  {param:random.uniform(domain[0],domain[1]) for param,domain in param_domain.items()}
    return valuation


def ts_benchmark_factory(sims: int):
    crn = load_benchmark_ts_plos()
    bf = BenchmarkFactory("TS_",crn=crn,end_t=100,sims=sims,seed=None,c=1.1)
    return bf    


# def ts_new_distribution(valuation: dict, method: str, sims: int):
#     '''
#     Generate transient distributions for TS benchmark (PLOS version).
#     :param valuation a dictionary of parameter-value pairs
#     :param method "SSA" or "SEG"
#     :param sims number of simulations to execute to obtain one distribution
#     :return a list of distributions; each distribution d is a vector of probabilities where d[x] denotes the probability
#         of observing x numbers of protein SA at after 100s
#     :return Saquaia runtime
#     '''
#     raise Exception("obsolete method")
#     assert method in ["SSA","SEG"]
#     if method == "SEG":
#         method = "SEGHYB"
#     bf = ts_benchmark_factory(sims)
#     distribution,runtime = saquaia_run(bf=bf, suffix=method, method=method, valuation=valuation, cleanup=True)
#     marg_index = bf.crn.species.index("SA")
#     distribution = marginalize(distribution,marg_index)
#     return distribution,runtime


# def ts_distributions_test():
#     # validation data: use SSA with 100k sims
    
#     # training data: use SSA with 500 sims or SEG with 10000 sims, the latter should be faster AND more precise
#     # use 'runtime' output to measure Saquaia-only runtime
#     num_valuations = 1
#     points_training_ssa = []
#     points_training_seg = []
#     for _ in range(num_valuations):
#         # valuation = ts_new_valuation()
#         valuation = ts_default_valuation()

#         results = {}
#         distribution,runtime_ssa = ts_new_distribution(valuation,"SSA",sims=1000)
#         results["ssa-valid"] = distribution
        
#         # distribution,runtime = ts_new_distribution(valuation,"SSA",sims=500)
#         # runtime_ssa = runtime
#         # points_training_ssa.append( (valuation,distribution) )
#         # results["ssa-500"] = distribution

#         distribution,runtime = ts_new_distribution(valuation,"SEG",sims=10000)
#         points_training_seg.append( (valuation,distribution) )
#         runtime_seg = runtime
#         results["seg-10k"] = distribution

#         print(f"SSA runtime = {round(runtime_ssa/60,1)} min, SEG runtime = {round(runtime_seg/60,1)} min")
#         plot_distributions(results)


def ts_new_intermediate_distributions(valuation: dict, method: str, sims: int, num_points: int, timeout: int):
    '''
    Run SSA/SEG simulations to generate transient distributions for TS but also generate distributions at intermediate
    time points.
    :param num_points number of intermediate time points for which a distribution is required; i.e. distributions will
        be computed for times [1..num_points] / num_points * end_time
    :return a list of probability distributions or None if timeout is reached
    :return runtime
    '''
    assert method in ["SSA","SEG"]
    if method == "SEG":
        method = "SEGHYB"
    bf = ts_benchmark_factory(sims)
    bf.timeout = timeout
    # print("species = ", bf.crn.species)
    distributions,runtime = saquaia_run(
        bf=bf, suffix=method, method=method, valuation=valuation, num_points=num_points, cleanup=False
    )
    if distributions is None:
        return distributions,runtime
    marg_index = bf.crn.species.index("SA")
    distributions = [marginalize(d,marg_index) for d in distributions]
    return distributions,runtime


def ts_new_intermediate_distributions_test():
    # how many intermediate time points (including the last one) to keep track of during saquaia call
    # make sure this value is the same as numPoints in saquaia/code/src/main/java/core/simulation/Simulation.java::recordHistory()
    num_points = 100
    # time limit (seconds) per saquaia call
    timeout = 60*5

    num_valuations = 10
    for _ in range(num_valuations):

        # valuation is a dictionary of parameter-value pairs
        # valuation = ts_new_valuation() 
        valuation = ts_default_valuation()

        distributions,runtime_ssa = ts_new_intermediate_distributions(
            valuation,method="SSA",sims=500,num_points=num_points,timeout=timeout)        
        assert distributions is None or len(distributions) == num_points
        
        distributions,runtime_seg = ts_new_intermediate_distributions(
            valuation,method="SEG",sims=10000,num_points=num_points,timeout=timeout)
        assert distributions is None or len(distributions) == num_points
        
        print(f"SSA runtime = {round(runtime_ssa,1)} s, SEG runtime = {round(runtime_seg,1)} s")


if __name__ == "__main__":
    ts_new_intermediate_distributions_test()
