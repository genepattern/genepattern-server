FROM ubuntu:14.04

# Note: FROM java and FROM r-base work too but take much longer apt-get updating

RUN apt-get update && apt-get upgrade --yes && \ 
	apt-get install -y wget && \
	apt-get install --yes bc vim libxpm4 libXext6 libXt6 libXmu6 libXp6 zip unzip

RUN apt-get update && apt-get upgrade --yes && \
    apt-get install build-essential --yes && \
    apt-get install python-dev groff  --yes && \
    apt-get install default-jre --yes && \
    wget --no-check-certificate https://bootstrap.pypa.io/get-pip.py && \
    apt-get install software-properties-common --yes && \
    add-apt-repository ppa:fkrull/deadsnakes-python2.7 --yes && \
    apt-get update --yes && \
    apt-get install python2.7 --yes && \
    python get-pip.py 

RUN pip install awscli 

RUN mkdir /home/gistic
WORKDIR /home/gistic

COPY runMatlab.sh /usr/local/bin/runMatlab.sh
COPY runS3OnBatch.sh /usr/local/bin/runS3OnBatch.sh
RUN mkdir /home/gistic/MCRInstaller
#COPY MCRInstaller.zip.2014a /home/gistic/MCRInstaller/MCRInstaller.zip
# COPY environment /etc/environment
RUN cd /home/gistic/MCRInstaller && \
   wget https://www.mathworks.com/supportfiles/downloads/R2014a/deployment_files/R2014a/installers/glnxa64/MCR_R2014a_glnxa64_installer.zip && \
   unzip MCR_R2014a_glnxa64_installer.zip

COPY matlab.conf /etc/ld.so.conf.d/matlab.conf

RUN  chmod a+x /usr/local/bin/runMatlab.sh && \
	cd MCRInstaller && \
     	/home/gistic/MCRInstaller/install -mode silent -agreeToLicense yes 



CMD ["/bin/bash", "runMatlab.sh"]

