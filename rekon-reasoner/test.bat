java -cp build/lib/*;build/classes;test/config -Xmx5000M -Xss200M -Djava.library.path=build/lib/ -Xrunhprof:cpu=samples %*

REM Possible command-line options - alternative to using "rekon.options" config file in "test/config" dir
REM
REM -Drekon.multithread=true
REM -Drekon.novalue-substitutions=true
REM -Drekon.logging.warnings=true
REM -Drekon.logging.progress=false
