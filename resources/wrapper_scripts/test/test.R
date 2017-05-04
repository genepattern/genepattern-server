#
# Example Rscript command
# for testing
#

main <- function() {
    cat (' ### R.Version() ### \n');
    print(R.Version());

    cat (' ### sessionInfo() ### \n');
    print(sessionInfo());

    cat (' ### .libPaths() ### \n');
    print(.libPaths());

#    cat (' ### Sys.getenv() ### \n');
#    Sys.getenv();
#    #print(Sys.getenv()); 

    #cat( R.version.string );
    #cat('\n');
    #cat('Hello, world!\n');

    #write("prints to stdout", stdout());
    ## write(cat(sessionInfo()), stdout());
}

main();