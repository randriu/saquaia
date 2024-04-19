# saquaia python interface

## 0) installing saquaia

Prerequisites: java, maven. Install `saquaia` by running

```
cd saquaia-mvn-main/code
mvn package
```

Verify installation by running

```
java -jar target/saquaia-jar-with-dependencies.jar
```

Script `saqint.py` demonstrates the usage of `saquaia` from Python (see `main()`)

```
python3 saqint.py
```

## 1) Models

From the 4 models available at Nessie, only `ts` and `mapk` are supported by `saquaia`. `main()` contains their corresponding text descriptions, which can be parsed into our internal representation for future conversion. For each model, one must specify the initial state (see Nessie) and species bounds (if unknown, just use some large number).

## 2) Creating benchmarks

`BenchmarkFactory` allows you to create multiple instantiations of the same benchmark template, e.g. trying different parameter valuation, different number of simulations, different methods (vanilla SSA vs our SEG), seed for `saquaia` simulations, `c` parameter for SEG (worth playing with: values above 1 are accepted, smaller = more precise), etc. `prefix` will be used as a prefix for benchmark names. `BenchmarkFactory::new` will create a specific benchmark given a specific method name and parameter valuation; the suffix is appended to the prefix to obtain the name of the benchmark. Each benchmark is basically a json object compatible with `saquaia` interface.

## 3) Evaluating benchmarks

`saquaia_run` method will run saquaia (transient analysis) on a given benchmark and return probability distribution over states and runtime (s). Under the hood, I store the benchmark object to a (temporary) file, run `saquaia` from the command line, `saquaia` generates a ton of outputs to `saquaia-result/BENCHMARK_NAME` folder, from which I parse the results. Since I call `saquaia` from the command line, running `saqint.py` from another location might break `saquaia` since it uses relative names for output: let me know if you need this fixed. By default, all the temporary files are removed unless `cleanup=False` is passed to `saquaia_run`.
