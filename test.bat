java -cp build/lib/*;build/classes -Xmx5000M -Xss200M -Djava.library.path=build/lib/ -Xrunhprof:cpu=samples -Drekon.multithread=true -Drekon.logging=false %*
