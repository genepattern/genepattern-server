FROM r-base:3.1.3

COPY runS3OnBatchInstallPackages.sh /usr/local/bin/runS3OnBatch.sh
RUN mkdir /build
COPY installPackages.R  /build/source/installPackages.R
COPY sources.list /etc/apt/sources.list

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    apt-get install default-jre --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 

RUN pip install awscli 

RUN apt-get update && \
    apt-get install curl --yes
    
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh

RUN  mkdir packages && \
    cd packages && \
    curl -O http://cran.r-project.org/src/base/R-3/R-3.2.5.tar.gz && \
    tar xvf R-3.2.5.tar.gz && \
    cd R-3.2.5 && \
    ./configure --with-x=no && \
    make && \
    make check && \
    make install && \
    apt-get install libxml2-dev --yes && \
    apt-get install libcurl4-gnutls-dev --yes && \
    apt-get install mesa-common-dev --yes && \
    apt-get install --yes libglu1-mesa-dev freeglut3-dev  bwidget

COPY runLocalInstallPackages.sh /usr/local/bin/runLocal.sh
COPY Rprofile.gp.site ~/.Rprofile
COPY Rprofile.gp.site /usr/lib/R/etc/Rprofile.site
 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

