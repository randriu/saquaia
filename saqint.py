#!/usr/bin/python3

import re
import json
import os, shutil
import collections

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
    
    def __init__(self, prefix: str, crn: CRN, end_t: float, sims: int, timeout: int, repeat: int, seed: int, c: float):
        self.prefix = prefix
        
        self.crn = crn
        self.end_t = end_t
        
        self.sims = sims
        self.timeout = timeout
        self.repeat = repeat
        self.seed = seed
        self.c = c

    def new(self, suffix, method, valuation):
        assert method in ["SSA","SEG"]
        
        conf = {}
        conf["type"] = method
        if method == "SEG":
            conf["baseConfig"] = {"type":"SSA"}
            conf["max_memory"] = 5000000000

        task = self.crn.export(valuation)

        result = {}
        result["type"] = "TRANSIENTNEW"
        result["name"] = self.prefix + suffix
        result["simconf"] = conf
        result["sims"]=self.sims
        result["timeout"]=self.timeout
        result["repeat"]=self.repeat
        if method == "SEG":
            assert self.c is not None
            result["c"] = self.c
        result["setting"] = task
        result["end_t"] = self.end_t
        if self.seed is not None:
            result["seed"]=seed
        
        return result

    def to_json(self,pretty=False):
        if not pretty:
            return json.dumps(self.benchmarks)
        else:
            return json.dumps(self.benchmarks,indent=2)



def saquaia_parse_output(result_file: str, species: list[str]):
    '''
    :return a distionary of state-probability pairs
    :return runtime (s)
    '''
    assert os.path.isfile(result_file)
    with open(result_file) as f:
        output = json.load(f)

    output = output[0]
    states = output["states"]
    num_samples = len(states)
    # print(f"found {num_samples} samples")
    state_count = collections.defaultdict(int)
    for state in states:
        state = tuple([int(value) for value in state])
        state_count[state] += 1
    # print(f"found {len(state_count)} distinct samples")
    state_prob = {state:count/num_samples for state,count in state_count.items()}
    computation_time = output["comp_time_sum"][-1] / 1e9

    return state_prob, computation_time


def saquaia_run(benchmark, cleanup=True):
    '''
    :param cleanup if True, auxiliary files created to communicate with saquaia will be removed
    :return a distionary of state-probability pairs
    :return runtime (s)
    '''
    pwd = os.getcwd()

    # write benchmark
    benchmark_file = f"{pwd}/benchmarks_auto.json"
    result_dir = f"{pwd}/saquaia-result"
    with open(benchmark_file,"w") as f:
        print([benchmark],file=f)

    # run saquaia
    command = f'java -jar {pwd}/saquaia-mvn-main/code/target/saquaia-jar-with-dependencies.jar -f {benchmark_file} -o {result_dir}'
    os.system(command)

    # collect result
    assert os.path.isfile(benchmark_file)
    result_file = f'{result_dir}/{benchmark["name"]}/result.json'
    species = benchmark["setting"]["crn"]["speciesNames"]
    saquaia_result = saquaia_parse_output(result_file,species)

    if cleanup:
        os.remove(benchmark_file)
        shutil.rmtree(result_dir)

    return saquaia_result

def main():
    
    # mapk
    equations = '''
        60 * c2, pSTL_on --> pSTL_off
        60 * c3 * 225, pSTL_on --> pSTLwCR
        60 * c4, pSTLwCR --> pSTL_on
        60 * c5, pSTLwCR --> pSTLwCR + mRNA
        60 * c6, mRNA --> mRNA + pSTL1_qV
        60 * c7, pSTL1_qV --> 0
        60 * c8, mRNA --> 0
        '''
    # crn = CRN.create_crn_from_string("mapk", equations)

    # ts
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
    crn.set_species_bound({x:10000 for x in crn.species})

    # try some meaningful parameter valuation (copied from Nessie)
    param_domains = {
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

    # valuation is a dictionary of parameter-value pairs
    valuation = {p:(domain[0]+domain[1])/2 for p,domain in param_domains.items()}
    
    # create benchmark template
    bf = BenchmarkFactory("prefix_",crn=crn,end_t=100,sims=1000,timeout=100000,repeat=1,seed=None,c=1.5)
    
    benchmark = bf.new("suffix", "SSA", valuation)
    distribution,runtime = saquaia_run(benchmark)
    print("\n\n>> ", sum(distribution.values()),runtime)
    
    benchmark = bf.new("suffix", "SEG", valuation)
    distribution,runtime = saquaia_run(benchmark)
    print("\n\n>> ", sum(distribution.values()),runtime)

main()