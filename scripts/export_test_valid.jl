#!~/julia-1.10.3/julia
using HDF5
using JLD2

# Load the data
c = h5open("parsed/test.h5", "r")
X_test = h5read("parsed/test.h5", "X")

Y_test = h5read("parsed/test.h5", "Y")
println("Saving data to JLD2 format")
@save "parsed/test.jld2" X_test Y_test



X_valid = h5read("parsed/valid.h5", "X")
Y_valid = h5read("parsed/valid.h5", "Y")
println("Saving data to JLD2 format")
@save "parsed/valid.jld2" X_valid Y_valid

