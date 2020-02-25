FROM docker

RUN apk update && \
    apk upgrade && \  
    apk -v --update add \
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

RUN cd /gpinstall && \
    chmod +x GPserver.bin && \
    PATH=$PATH:/gpinstall && \
    ./GPserver.bin -f install-gp.properties 


EXPOSE 8080


