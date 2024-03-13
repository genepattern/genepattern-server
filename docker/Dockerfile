FROM docker:19.03.8-dind

RUN mkdir /testing

RUN apk update && \
    apk upgrade && \  
    apk -v --update add \
        curl \
        bash \
        python \
        py-pip \
        groff \
        less \
        mailcap \
        doas \
        && \
    apk -v --purge del py-pip && \
    rm /var/cache/apk/* 

RUN apk update && apk add openjdk8-jre && rm -rf /var/lib/apt/lists/*

# add aws cli
RUN apk add --no-cache \
        python3 \
        py3-pip \
    && pip3 install --upgrade pip \
    && pip3 install --no-cache-dir \
        awscli  boto3 \
    && rm -rf /var/cache/apk/*

RUN alias python=/usr/bin/python3

RUN aws --version

RUN adduser gpserver -u 501 -s /bin/bash -D

RUN mkdir -p  /gpinstall && \
    chmod a+rwx /gpinstall 

USER gpserver
RUN aws configure set default.region us-east-1
USER root

COPY ./* /gpinstall/

# installer is passed in, set defaults for gpserver user
ARG GP_INSTALLER

RUN    cd /gpinstall  && \
    echo getting installer from $GP_INSTALLER && \
    wget -q --no-check-certificate --header='Accept:application/octet-stream'  $GP_INSTALLER -O GPserver.bin && \
    ls -alrt && \
    chmod a+rwx GPserver.bin 

RUN  cd /gpinstall && \
     PATH=$PATH:/gpinstall && \
    ./GPserver.bin -f ./install-gp.properties

RUN chmod -R a+rwx /opt/genepattern
RUN cp /gpinstall/config_custom.yaml /opt/genepattern/resources/config_custom.yaml

RUN mv /usr/bin/python /usr/bin/python2 &&  \
     ln -s /usr/bin/python3 /usr/bin/python 


EXPOSE 8080

         


