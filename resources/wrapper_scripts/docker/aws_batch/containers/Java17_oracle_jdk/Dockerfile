FROM ubuntu:14.04

COPY runS3OnBatch.sh /usr/local/bin/runS3OnBatch.sh

RUN mkdir /build

COPY Dockerfile /build/Dockerfile
 
RUN apt-get update && apt-get upgrade --yes && \
    apt-get install wget --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    apt-get install software-properties-common python-software-properties --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 
RUN pip install awscli 

RUN cd /build && \
   mkdir /build/java &&\
   cd /build/java && \
   wget https://s3.amazonaws.com/genepattern-largefile-cache/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/Java17_oracle_jdk/jdk-7u80-linux-x64.tar.gz && \
   tar zxvf jdk-7u80-linux-x64.tar.gz



# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /build/java/jdk1.7.0_80
ENV PATH $JAVA_HOME/bin:$PATH

    
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh
COPY runLocal.sh /usr/local/bin/runLocal.sh

 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

