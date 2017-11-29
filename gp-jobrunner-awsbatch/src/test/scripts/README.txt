SHUNIT2 unit tests for the wrapper scripts
-------------------------------------------
  @see: https://github.com/kward/shunit2
  @see: http://ssb.stsci.edu/testing/shunit2/shunit2.html
  @see also: http://www.mikewright.me/shunit2-bash-testing.html

If necessary, install and configure shunit2, something like ...
    git clone https://github.com/kward/shunit2.git
    export SHUNIT2_HOME=<full_path_to>/shunit2/source/2.1
    alias shunit2=${SHUNIT2_HOME}/src/shunit2

To run the tests:
    ./test-runOnBatch.sh
