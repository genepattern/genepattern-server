FROM ubuntu

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

RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh
COPY runLocal.sh /usr/local/bin/runLocal.sh
 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

