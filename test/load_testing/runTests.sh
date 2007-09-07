#!/bin/bash 

out_dir=$1

bsub -o ${out_dir}/tutorial.out ./gp_scripts/runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/tutorial.twill 50
bsub -q priority -o ${out_dir}/pipe_mycms.out ./gp_scripts/runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/pipe_mycms.twill 200
bsub -q priority -o ${out_dir}/pipe_mycle.out ./gp_scripts/runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/pipe_mycle.twill 200
bsub -q priority -o ${out_dir}/mod_cms.out    ./gp_scripts/runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/mod_cms.twill 200
bsub -q priority -o ${out_dir}/mod_ecms.out   ./gp_scripts/runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/mod_ecms.twill 200
