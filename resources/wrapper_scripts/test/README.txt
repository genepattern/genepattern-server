SHUNIT2 unit tests for the wrapper scripts
-------------------------------------------
@see: https://github.com/kward/shunit2
@see also: http://www.mikewright.me/blog/2013/10/31/shunit2-bash-testing/
    I followed the instructions in this blog to get started.

To set up the tests:
    git clone https://github.com/kward/shunit2.git
    export SHUNIT2_HOME=<full_path_to>/shunit2/source/2.1
    alias shunit2=${SHUNIT2_HOME}/src/shunit2

To run the tests:
    ./env-test.sh

