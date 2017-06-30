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

#RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
#  echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && \ 
#  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
#  add-apt-repository -y ppa:webupd8team/java && \
#  apt-get update && \
#  apt-get install -y oracle-java7-installer && \
#  rm -rf /var/lib/apt/lists/* && \
#  rm -rf /var/cache/oracle-jdk7-installer

COPY jdk-7u80-linux-x64.tar.gz /build/jdk-7u80-linux-x64.tar.gz

RUN mkdir /build/java &&\
   mv /build/jdk-7u80-linux-x64.tar.gz /build/java/jdk-7u80-linux-x64.tar.gz && \
   cd /build/java && \
   tar zxvf jdk-7u80-linux-x64.tar.gz



# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /build/java/jdk1.7.0_80
ENV PATH $JAVA_HOME/bin:$PATH

    
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh
COPY runLocal.sh /usr/local/bin/runLocal.sh

 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

