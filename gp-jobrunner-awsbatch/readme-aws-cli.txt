========================================
  AWS CLI installation notes (draft)
========================================

These are my (first draft) notes after setting up AWS Batch
on my local development machine.

Requirements:
  * conda
  * aws-cli
Environment variables:
  MINICONDA3_HOME; e.g. export MINICONDA3_HOME=/opt/miniconda3/4.3.11
Links:
  * https://aws.amazon.com/cli/
  * https://conda.io/miniconda.html
  * https://anaconda.org/conda-forge/awscli
  * https://stackoverflow.com/questions/45421163/anaconda-vs-miniconda


----------------------------------------
  Conda installation
----------------------------------------
  Skip this step if you already have conda. I chose miniconda, because it has a 
smaller disk footprint than anaconda. 

  # Download installer, e.g.
    export MINICONDA3_HOME=/opt/miniconda3/4.3.11
    mkdirs /opt/miniconda3/installer
    cd  /opt/miniconda3/installer
    curl -O https://repo.continuum.io/miniconda/Miniconda3-4.3.11-Linux-x86_64.sh
    chmod u+x Miniconda3-4.3.11-Linux-x86_64.sh
    ./Miniconda3-4.3.11-Linux-x86_64.sh
    Note: use MINICONDA3_HOME as the PREFIX.

  # Add to the path
    export PATH=${MINICONDA3_HOME}/bin:${PATH}

----------------------------------------
  AWS CLI installation
----------------------------------------
  I installed the aws cli as a conda environment on my dev
  See: https://github.com/ContinuumIO/anaconda-issues/issues/1429

  Command Log
    conda create -n awscli
    source activate awscli
    conda install pip
    pip install awscli
    conda info --envs
    conda env list
    conda list
    which aws
    conda env export > awscli_environment.yml

  Integration notes
    For <run-with-env> integration in Bash environment on my dev machine.

    (awscli) $ aws --version
    aws-cli/1.11.87 Python/2.7.13 Darwin/15.6.0 botocore/1.5.50

    Example env-custom.sh snippets ...
    # miniconda
    if [ "$1" = "miniconda2/4.3.13" ]; then
        MINICONDA2_HOME=/Broad/Applications/miniconda2
        export PATH="${PATH}:${MINICONDA2_HOME}/bin"
    fi
    # aws-cli
    if [ "$1" = "aws-cli/1.11.87" ]; then
        # depends on 'miniconda2/4.3.13'
        source activate awscli
    fi

--------------------------------------------------------------------------------
  Configure AWS Batch Permissions
--------------------------------------------------------------------------------
  Start here: https://console.aws.amazon.com/iam/home#/home
  See:
    http://docs.aws.amazon.com/batch/latest/userguide/IAM_policies.html

I added the 'AWSBatchFullAccess' Policy to the 'gpserver' group.
  https://console.aws.amazon.com/iam/home#/groups/gpserver
    

--------------------------------------------------------------------------------
  Create AWS access key for programmatic access via the aws-cli
--------------------------------------------------------------------------------
  See: 
    http://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys
  See also: 
    http://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html#Using_CreateAccessKey
    http://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html?icmpid=docs_iam_console

--------------------------------------------------------------------------------
  Create an AWS profile, default name "genepattern"
--------------------------------------------------------------------------------
  See: http://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html

