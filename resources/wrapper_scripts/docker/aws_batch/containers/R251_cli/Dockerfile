FROM r-base:3.1.3

RUN mkdir /build

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    apt-get install default-jre --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 
RUN pip install awscli 

RUN apt-get update && \
    apt-get install curl --yes
    
RUN  mkdir packages && \
    cd packages && \
    curl -O http://cran.r-project.org/src/base/R-2/R-2.5.1.tar.gz && \
    tar xvf R-2.5.1.tar.gz && \
    cd R-2.5.1 && \
    ./configure --with-x=no && \
    make && \
    make check && \
    make install && \
    apt-get install libxml2-dev --yes && \
    apt-get install libcurl4-gnutls-dev --yes

COPY runLocalInstallPackages.sh /usr/local/bin/runLocal.sh
COPY runS3OnBatchInstallPackages.sh /usr/local/bin/runS3OnBatch.sh
COPY Dockerfile /build/Dockerfile
COPY jobdef.json /build/jobdef.json
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh /usr/local/bin/runLocal.sh
 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

