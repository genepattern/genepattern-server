FROM docker
RUN mkdir /testing
COPY entrypoint.sh /testing/entrypoint.sh

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
        && \
    apk -v --purge del py-pip && \
    rm /var/cache/apk/* 

RUN apk update && apk add openjdk8-jre && rm -rf /var/lib/apt/lists/*

RUN mkdir /gpinstall 

COPY ./* /gpinstall/

# installer is passed in
ARG GP_INSTALLER

RUN    cd /gpinstall  && \
    echo getting installer from $GP_INSTALLER && \
    wget -q --no-check-certificate --header='Accept:application/octet-stream'  $GP_INSTALLER -O GPserver.bin && \
    ls -alrt && \
    chmod +x GPserver.bin && \
    PATH=$PATH:/gpinstall && \
    ./GPserver.bin -f ./install-gp.properties

RUN cp /gpinstall/config_custom.yaml /opt/genepattern/resources/config_custom.yaml

EXPOSE 8080

         


ENTRYPOINT  ["/testing/entrypoint.sh", "$NAME"]
