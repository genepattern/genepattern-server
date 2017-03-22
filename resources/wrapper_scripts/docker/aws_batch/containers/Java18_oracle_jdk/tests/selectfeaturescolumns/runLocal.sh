#!/bin/bash
# vol for source at tasklib
# working dir using -w
# "$PWD":/usr/src/myapp


#  java -cp src/SelectFeaturesColumns.jar:src/gp-modules.jar org.genepattern.modules.selectfeaturescolumns.SelectFeaturesColumns jobX/all_aml_train.gct testout.txt -t1-2

docker run -v "$PWD"/job_12/:/jobdir/job_12 -v "$PWD"/src:/tasklib/sfc -v "$PWD"/data:/data -w /jobdir/job_12 liefeld/java18_oracle java -cp /tasklib/sfc/SelectFeaturesColumns.jar:/tasklib/sfc/gp-modules.jar org.genepattern.modules.selectfeaturescolumns.SelectFeaturesColumns /data/all_aml_train.gct /jobdir/job_12/testout.gct -t1-2

#Java17_Oracle_Generic
