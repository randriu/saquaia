#!~/julia-1.10.3/julia
using HDF5
using JLD2

# Load the data
vv="SEG_500"
c = h5open("parsed/" * vv * ".h5", "r")
X_train = h5read("parsed/" * vv * ".h5", "X_test")

# print infotext
println("X_train: ", size(X_train))

Y_train = h5read("parsed/" * vv * ".h5", "Y_test")
println("Y_train: ", size(Y_train))

println("Saving data to JLD2 format")
@save "parsed/" * vv * ".jld2" X_train Y_train




# Load the data
vv="SEG_10000"
c = h5open("parsed/" * vv * ".h5", "r")
X_train = h5read("parsed/" * vv * ".h5", "X_test")

# print infotext
println("X_train: ", size(X_train))

Y_train = h5read("parsed/" * vv * ".h5", "Y_test")
println("Y_train: ", size(Y_train))

println("Saving data to JLD2 format")
@save "parsed/" * vv * ".jld2" X_train Y_train


# Load the data
vv="SSA"
c = h5open("parsed/" * vv * ".h5", "r")
X_train = h5read("parsed/" * vv * ".h5", "X_test")

# print infotext
println("X_train: ", size(X_train))

Y_train = h5read("parsed/" * vv * ".h5", "Y_test")
println("Y_train: ", size(Y_train))

println("Saving data to JLD2 format")
@save "parsed/" * vv * ".jld2" X_train Y_train
