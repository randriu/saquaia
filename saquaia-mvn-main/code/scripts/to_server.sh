ssh $1 'mkdir -p ~/seg'
ssh $1 'mkdir -p ~/seg/data'
ssh $1 'mkdir -p ~/seg/code'
ssh $1 'mkdir -p ~/seg/results'
ssh $1 'rm -r ~/seg/data/settings'
ssh $1 'rm ~/seg/code/saquaia.jar'
ssh $1 'rm ~/seg/data/benchmarks.json'
scp -r ./data/settings $1:seg/data/settings
scp ./data/benchmarks.json $1:seg/data/benchmarks.json
scp ./code/target/saquaia-jar-with-dependencies.jar $1:seg/code/saquaia.jar