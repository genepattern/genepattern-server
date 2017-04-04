---------------------------------------------
Running multiple versions of R on Mac OS X
---------------------------------------------

If you are running GenePattern Server on a Mac and have more than
one version of R installed ... read on for instructions to modify 
your R installation.

This applies to the R for Mac OS X binary provided by CRAN, which is
installed as a Mac OS X framework in this location:

  /Library/Frameworks/R.framework

Multiple versions can be installed but only one can be active at any 
point in time.

As a workaround, edit the R 'shell wrapper' script, e.g.

  /Library/Frameworks/R.framework/Versions/3.1/Resources/bin/R

Set R_HOME and related paths to the version specific path, e.g.
  # the following line is for illustration ...
  R_HOME_DIR=/Library/Frameworks/R.framework/Versions/3.1/Resources
  # in practice this line is better because it can be copied verbatim
  # it set R_HOME_DIR based on the location of the script file
  R_HOME_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd ../ && pwd)"
  R_SHARE_DIR="${R_HOME_DIR}/share"
  R_INCLUDE_DIR="${R_HOME_DIR}/include"
  R_DOC_DIR="${R_HOME_DIR}/doc"

-------------------------------------------
* updating the R installation with a patch
-------------------------------------------
Patch files are included in the ./wrapper_scripts/R/{version} folder
for some versions of R, including 2.5 and 3.1. To apply the patch
copy the file into the R installation location, then run the patch command.
Something like,

  cd /Library/Frameworks/R.framework/Versions/__version__/Resources/bin
  patch --backup --input R_v__version__.patch

For R/3.1
  patch.file: R_v3.1.3.patch
  patch.dir:  /Library/Frameworks/R.framework/Versions/3.1/Resources/bin
Run the commands
  # copy the patch file
  cp ./3.1/R_v3.1.3.patch /Library/Frameworks/R.framework/Versions/3.1/Resources/bin
  cd /Library/Frameworks/R.framework/Versions/3.1/Resources/bin
  patch --backup --input R_v3.1.3.patch

For R/2.5
  patch file: R_v2.5.patch
  patch dir: /Library/Frameworks/R.framework/Versions/2.5/Resources/bin

----------------------------------------
* testing the R version
----------------------------------------
Use the run-with-env.sh script to test the R installation. For example,

  run-with-env.sh -c env-custom-macos.sh -u R-3.1 R --version
  run-with-env.sh -c env-custom-macos.sh -u R-3.1 Rscript --version

  run-with-env.sh -c env-custom-macos.sh -u R-2.5 R --version
  run-with-env.sh -c env-custom-macos.sh -u R-2.5 Rscript --version

-------------------------------------------
* updating the R installation by hand
-------------------------------------------
When a patch file is not present, update the R installation by hand.
See the notes above and the included patch files for reference.

For best results make a backup of the original file,
  cp -rp R R.orig
and another copy for editing,
  cp R R.updated

Hint: Use the 'diff -u' command to create a patch file.
  diff -u originalVersion newVersion > update.patch
E.g.
  diff -u R R.updated > R_v3.1.3.patch

Hint: Use the 'patch' command to apply the patch, e.g.
  patch < R_v3.1.3.patch    

----------------------------------------
* For more info
----------------------------------------
  * see: https://cran.r-project.org
  * see: https://cran.r-project.org/bin/macosx/
  * see: https://support.rstudio.com/hc/en-us/articles/200486138-Using-Different-Versions-of-R
