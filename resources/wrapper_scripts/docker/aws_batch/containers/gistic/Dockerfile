FROM ubuntu:14.04

# Note: FROM java and FROM r-base work too but take much longer apt-get updating

MAINTAINER Jeltje van Baren, jeltje.van.baren@gmail.com


RUN apt-get update && apt-get upgrade --yes && \ 
	apt-get install -y wget && \
	apt-get install --yes bc vim libxpm4 libXext6 libXt6 libXmu6 libXp6 zip unzip

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev --yes && \
    apt-get install default-jre --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    python get-pip.py 

RUN pip install awscli 

RUN mkdir /home/gistic
WORKDIR /home/gistic

ADD ./run.sh /home/gistic/
RUN mkdir /home/gistic/MCRInstaller
COPY MCRInstaller.zip.2014a /home/gistic/MCRInstaller/MCRInstaller.zip
COPY environment /etc/environment
COPY matlab.conf /etc/ld.so.conf.d/matlab.conf

#RUN wget ftp://ftp.broadinstitute.org/pub/GISTIC2.0/GISTIC_2_0_22.tar.gz \
#	&& tar xvf GISTIC_2_0_22.tar.gz
# RUN ./MCRInstaller.bin -P bean421.installLocation="/home/gistic/MATLAB_Compiler_Runtime" -silent

RUN  chmod a+x run.sh && \
	cd MCRInstaller && \
	unzip MCRInstaller.zip && \
     	/home/gistic/MCRInstaller/install -mode silent -agreeToLicense yes 

RUN cat /etc/environment >> /root/.bashrc


CMD ["/bin/bash", "/home/gistic/run.sh"]

