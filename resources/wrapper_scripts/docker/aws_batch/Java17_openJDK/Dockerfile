FROM java:7u111

COPY runS3OnBatch.sh /usr/local/bin/runS3OnBatch.sh

RUN mkdir /build

COPY Dockerfile /build/Dockerfile

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 
RUN pip install awscli 
    
RUN chmod ugo+x /usr/local/bin/runS3OnBatch.sh
 
CMD ["/usr/local/bin/runS3OnBatch.sh" ]

