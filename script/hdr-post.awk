#!/usr/bin/awk -f

BEGIN {OFS=","};

## httpkit.ring-middleware.async.java8.ParallelGC.r10k.t16.c400.d600s.csv
## server, handler, sync, jvm, gc,   rate
## c[1],   c[2],    c[3], c[4], c[5], c[6]

FNR==1 {
    split(FILENAME,c,/\./);
    gsub("[rk]", "", c[6]);
    gsub("java", "", c[4]);
    print "server,handler,sync,jvm,gc,rate", $0
};


FNR!=1 {
    print c[1], c[2], c[3], c[4], c[5], c[6], $0
};
