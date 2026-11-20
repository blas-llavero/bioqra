#!/usr/bin/env Rscript
# Verification of authentic GEO sample metadata; prints only pair counts.
args <- commandArgs(trailingOnly=TRUE)
if(length(args)!=2L)stop("Usage: Rscript test_real_external_pairs.R MATRIX.gz GPL")
source("tools/geo_external_title_pairing.R")
file <- gzfile(args[[1L]],"rt")
on.exit(close(file),add=TRUE)
fields <- list()
for(i in seq_len(35000L)){
  line <- readLines(file,n=1L,warn=FALSE)
  if(!length(line)||identical(line,"!series_matrix_table_begin"))break
  for(key in c("!Sample_title","!Sample_source_name_ch1","!Sample_geo_accession")){
    if(startsWith(line,paste0(key,"\t"))){
      v <- strsplit(line,"\t",fixed=TRUE)[[1L]][-1L]
      fields[[key]] <- trimws(gsub('^"|"$',"",v))
    }
  }
}
titles<-fields[["!Sample_title"]]
tissue<-fields[["!Sample_source_name_ch1"]]
gsm<-fields[["!Sample_geo_accession"]]
if(is.null(gsm)||length(gsm)!=length(titles)||anyDuplicated(gsm) ||
   any(!grepl("^GSM[0-9]+$",gsm)))stop("GSM annotations invalid")
r<-bioqra_external_title_pairs(titles,tissue,"GSE23400",args[[2L]])
cat("Real GEO external tumor-normal pair eligibility: ",r$status,"\n",sep="")
cat("Reason: ",r$reason,"\n",sep="")
if(r$status=="ELIGIBLE"){
  cat("Distinct real annotated patient pairs: ",r$independent_patient_pairs,"\n",sep="")
}
if(r$status!="ELIGIBLE")stop("GEO external pair check failed")
