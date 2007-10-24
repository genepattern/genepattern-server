args <- commandArgs(T)
source(args[1])
do.call(args[2], as.list(args[3:length(args)]))
q(save='no')
