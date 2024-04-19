ssh $1 "cp ~/seg/results/benchmark_log.txt ~/seg/results/benchmark/$2/benchmark_log.txt"
ssh $1 "cd ~/seg/results/benchmark && zip -r $2.zip $2"
scp -r $1:~/seg/results/benchmark/$2.zip ./results/benchmark/$2.zip
ssh $1 "cd ~/seg/results/benchmark && rm $2.zip"