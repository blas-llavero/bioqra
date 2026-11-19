#!/usr/bin/env Rscript
# This uses ORIGINAL publicly downloaded GEO titles, no invented patients.
args <- commandArgs(trailingOnly=TRUE)
if(length(args)!=1L)stop("Provide real GEO series matrix path")
source("tools/geo_title_pairing.R")
f <- gzfile(args[[1]],"rt")
on.exit(close(f),add=TRUE)
titles <- NULL
for(i in seq_len(30000)) {
    l<-readLines(f,n=1L,warn=FALSE)
    if(length(l)==0L || identical(l,"!series_matrix_table_begin"))break
    if(startsWith(l,"!Sample_title\t")) {
        raw<-strsplit(l,"\t",fixed=TRUE)[[1L]][-1L]
        titles<-trimws(gsub('^"|"$',"",raw))
    }
}
if(is.null(titles))stop("No published source sample titles")
result<-bioqra_geo_title_pairs(titles,"GSE20347")
cat("Real GEO title-pairing eligibility:",result$status,"\nReason:",result$reason,"\n")
if(identical(result$status,"ELIGIBLE")){
    cat("Verified pairs:",length(unique(result$patient_ids)),
        "\nPublic GSM sample titles:",length(titles),"\n")
}
# Eligibility is assessed independently of compatibility with limma expression
# processing. No numeric biological results are produced by this script.
