FROM r-base:3.1.3

COPY runS3OnBatch.sh /usr/local/bin/runS3OnBatch.sh

RUN mkdir /build

COPY Dockerfile /build/Dockerfile
COPY ../../../R/installPackages.R /build/source/installPackages.R
COPY tests/affy/src/r.package.info /build/source/r.package.info

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    apt-get install default-jre --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 
RUN pip install awscli 
    
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh
RUN cd /build/source && \
    Rscript /build/source/installPackages.R /build/source/r.package.info
 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

