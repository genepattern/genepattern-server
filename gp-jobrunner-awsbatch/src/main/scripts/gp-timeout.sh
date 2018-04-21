############################################################
# gp-timeout.sh
#
#   Defines a helper function which cancels a command after
# the timeout interval 
#   Must be sourced rather than executed.
#
# Usage:
#   source gp-timeout.sh
############################################################

############################################################
# run_with_timeout
#   run the command, cancel after the given timeout interval
# Usage:
#   run_with_timeout timeout-sec stdout-file stderr-file cmd [args]*
# Example:
#   run_with_timeout 120 stdout.txt stderr.txt echo Hello
############################################################
run_with_timeout() { 
  local timeout_sec=$1;
  perl -e '
    my $timeout_sec=shift; 
    my $stdout_file=shift;
    my $stderr_file=shift;
    $SIG{ALRM} = sub { 
      print STDERR "Job timed out after $timeout_sec seconds\n"; 
      exit(142); 
    }; 
    open STDOUT, ">", "$stdout_file"; 
    open STDERR, ">", "$stderr_file"; 
    alarm $timeout_sec; 
    my $system_code=system @ARGV; 
    alarm 0; 
    my $exit_code=$system_code >> 8; 
    exit $exit_code;
  '   -- "${@}";
}
