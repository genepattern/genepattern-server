#!/bin/bash
# vol for source at tasklib
# working dir using -w
# "$PWD":/usr/src/myapp
# docker run --name JobConvertLineEndings -v "$PWD"/data:/jobdir/job1 -v "$PWD"/src:/tasklib/cle -w /usr/src/myapp/data perl:5.20 perl /tasklib/cle/to_host.pl /jobdir/job1/manifest /jobdir/job1/man2.cvt.txt


# <run-with-env> -u Java -u R-2.5 java <java_flags> -Dlibdir\=<libdir> -cp <libdir>gp-modules.jar<path.separator><libdir>commons-math-1.2.jar<path.separator><libdir>trove.jar<path.separator><libdir>Jama-1.0.2.jar<path.separator><libdir>colt.jar<path.separator><libdir>jsci-core.jar org.broadinstitute.marker.MarkerSelection <input.file> <cls.file> <number.of.permutations> <test.direction> <output.filename> <balanced> <complete> no <test.statistic> <random.seed> false <smooth.p.values> -l<log.transformed.data> -m<min.std> -c<confounding.variable.cls.file> -p<phenotype.test>

docker run  -v "$PWD"/src:/home/docker -v "$PWD"/data:/data  -w /home/docker -u docker liefeld/r2.5 java -Dlibdir\=./ -cp gp-modules.jar:commons-math-1.2.jar:trove.jar:Jama-1.0.2.jar:colt.jar:jsci-core.jar org.broadinstitute.marker.MarkerSelection /data/all_aml_train.gct /data/all_aml_train.cls 100 2 cms_outfile false false no 1 12345  false false -lfalse  

#docker run  -v "$PWD"/src:/home/docker -v "$PWD"/data:/data  -w /home/docker -u docker liefeld/r2.5 java -cp gp-modules.jar:commons-math-1.2.jar -version 






