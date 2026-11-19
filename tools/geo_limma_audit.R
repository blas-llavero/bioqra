#!/usr/bin/env Rscript
# BIOQRA independent R/Bioconductor audit of a REAL NCBI GEO Series Matrix.
# Never generate artificial patients, never infer case/control labels from
# publication title, and never treat metadata discovery as validation.
#
# Inputs: path to original .txt.gz and output directory (from allowlisted fetch).
# Outputs: audit.tsv, audit.txt, provenance.json (in downloaded folder).
# If unsupported for differential expression, record SKIPPED explicitly.
args <- commandArgs(trailingOnly=TRUE)
if (length(args) != 2L) stop("Usage: Rscript geo_limma_audit.R INPUT.txt.gz OUT_DIR")
source_file <- args[[1]]
output_dir <- args[[2]]
dir.create(output_dir, showWarnings=FALSE, recursive=TRUE)
if (!file.exists(source_file)) stop("Real GEO file not found")
if (file.info(source_file)$size > 8000000) stop("GEO file larger than safe limit")

write_status <- function(status, reason, extra=list()) {
  record <- c(list(status=status, reason=reason,
                   method="Independent GEO expression audit using limma when design is valid",
                   clinical_validation=FALSE, challenge_solved=FALSE), extra)
  if (requireNamespace("jsonlite", quietly=TRUE)) {
    jsonlite::write_json(record, file.path(output_dir,"limma-audit.json"),
                         auto_unbox=TRUE, pretty=TRUE, null="null")
  } else {
    lines <- c(paste("status:",status),paste("reason:",reason),
               "clinical_validation: FALSE", "challenge_solved: FALSE")
    writeLines(lines,file.path(output_dir,"limma-audit.txt"))
  }
  cat(status,": ",reason,"\n",sep="")
}
skip <- function(why) {write_status("SKIPPED",why);quit(status=0)}
if (!requireNamespace("limma", quietly=TRUE)) {
  skip("Bioconductor limma is not installed; no differential expression performed")
}
if (!requireNamespace("jsonlite", quietly=TRUE)) {
  skip("jsonlite is required for structured audit provenance")
}
# Read metadata only; cap line count and byte size.
connection <- gzfile(source_file, open="rt")
on.exit(close(connection),add=TRUE)
meta <- character()
inside <- FALSE
while(length(line <- readLines(connection, n=1L, warn=FALSE))>0L) {
  if(identical(line,"!series_matrix_table_begin")) {inside <- TRUE;break}
  if (length(meta)>=25000L || sum(nchar(meta,type="bytes"))>12000000L)
    skip("Metadata exceeds bounded parser limits")
  meta <- c(meta,line)
}
if(!inside)skip("No GEO series matrix data table")
extract_fields <- function(prefix) {
  selected <- meta[startsWith(meta,prefix)]
  lapply(selected,function(line) {
    values <- strsplit(line,"\t",fixed=TRUE)[[1]]
    if(length(values)<2L)return(character())
    trimws(gsub('^"|"$',"",values[-1L]))
  })
}
ids <- extract_fields("!Sample_geo_accession")
if(length(ids)!=1L)skip("Missing or duplicate sample accession field")
sample_ids <- ids[[1L]]
if(length(sample_ids)<6L||length(sample_ids)>500L ||
   any(!grepl("^GSM[0-9]+$",sample_ids))||anyDuplicated(sample_ids))
  skip("Missing, duplicate, or invalid GSM sample accessions")
characteristics <- extract_fields("!Sample_characteristics_ch1")
if(length(characteristics)==0L)skip("No study-provided sample phenotype fields")
groups <- rep(NA_character_,length(sample_ids))
metadata_fields <- list()
for(field in characteristics) {
  if(length(field)!=length(sample_ids))skip("Inconsistent phenotype column count")
  low <- tolower(field)
  metadata_fields[[length(metadata_fields)+1L]] <- field
  allowed <- grepl("^(disease state|disease status|condition|tissue type|phenotype|group|sample type|histology):[[:space:]]*",low)
  value <- trimws(sub("^[^:]+:[[:space:]]*","",low))
  control <- allowed & value %in% c("normal","healthy","non-tumor","non-tumour",
      "non-neoplastic","control","adjacent normal","normal tissue","healthy control")
  tumor <- allowed & value %in% c("tumor","tumour","cancer","carcinoma",
      "primary tumor","primary tumour","tumor tissue","tumour tissue",
      "malignant","adenocarcinoma","case")
  current <- rep(NA_character_,length(sample_ids))
  current[control] <- "control"
  current[tumor] <- "tumor"
  clash <- !is.na(groups) & !is.na(current) & groups != current
  if(any(clash))skip("Contradictory case/control labels")
  groups[!is.na(current)] <- current[!is.na(current)]
}
if(anyNA(groups) || sum(groups=="control")<3L || sum(groups=="tumor")<3L)
  skip("Fewer than three clearly labeled samples per group")
source("tools/geo_pairing.R",local=TRUE)
pairing <- bioqra_paired_design(groups,metadata_fields,sample_ids)
if(pairing$status!="ELIGIBLE")skip(pairing$reason)
processing <- meta[grepl("^!(Series|Sample)_data_processing",meta)]
p <- tolower(paste(processing,collapse=" "))
if(!grepl("log2|log 2|log-transformed|log transformed",p) ||
   !grepl("normaliz|quantile|rma",p) ||
   grepl("raw counts|unnormalized",p))
  skip("Not explicitly normalized and log transformed")
platform <- unlist(extract_fields("!Series_platform_id"))
platform <- unique(platform)
if(length(platform)!=1L||!grepl("^GPL[0-9]+$",platform))
  skip("Platform not uniquely specified")
# The base R reader handles quoted GEO sample names without implicit NA recoding.
# The table can be large; read only bounded rows, and refuse any truncation.
file_connection <- gzfile(source_file,"rt")
on.exit(close(file_connection),add=TRUE)
for(i in seq_len(length(meta)+1L)) {
  consumed <- readLines(file_connection,n=1L,warn=FALSE)
  if(length(consumed)==0L)skip("Unexpected end before numeric table")
}
headers <- readLines(file_connection,n=1L,warn=FALSE)
if(length(headers)!=1L)skip("No matrix header")
headings <- gsub('^"|"$',"",strsplit(headers,"\t",fixed=TRUE)[[1]])
if(length(headings)!=length(sample_ids)+1L ||
   headings[[1L]]!="ID_REF" ||
   !identical(headings[-1L],sample_ids))skip("GSM order is inconsistent with matrix columns")
# Read a bounded complete data table (50,000 rows / 2.5M cells).
limit <- min(50000L, floor(2500000/length(sample_ids)))
rows <- character()
completed <- FALSE
repeat {
  line <- readLines(file_connection,n=1L,warn=FALSE)
  if(length(line)==0L)break
  if(identical(line,"!series_matrix_table_end")) {completed <- TRUE;break}
  rows <- c(rows,line)
  if(length(rows)>limit)skip("Matrix exceeds bounded data analysis limits")
}
if(!completed)skip("Matrix truncated or missing terminator")
if(length(rows)<10L)skip("Too few measurement features")
matrix_text <- paste(c(headers,rows),collapse="\n")
parsed <- tryCatch(read.delim(text=matrix_text,check.names=FALSE,quote='"',
                              header=TRUE, na.strings=c("","NA","null"),
                              stringsAsFactors=FALSE),
                   error=function(e)NULL)
if(is.null(parsed)||ncol(parsed)!=length(sample_ids)+1L)
  skip("Matrix rows cannot be parsed safely")
probe_ids <- as.character(parsed[[1L]])
if(anyDuplicated(probe_ids)||any(!nzchar(probe_ids)))
  skip("Missing or duplicate probe IDs; no aggregation policy specified")
values <- suppressWarnings(as.matrix(
  data.frame(lapply(parsed[-1L],as.numeric),check.names=FALSE)))
if(ncol(values)!=length(sample_ids) || any(!is.finite(values)))
  skip("Non-numeric or missing values; imputation is not permitted")
rownames(values) <- probe_ids
colnames(values) <- sample_ids
if(pairing$design=="paired") {
  # Explicit patient fixed effects model; no patient identifiers are exported.
  design <- model.matrix(~factor(pairing$patient_ids)+factor(groups,levels=c("control","tumor")))
  if(qr(design)$rank!=ncol(design))skip("Paired design matrix is rank deficient")
  coefficient <- ncol(design)
  fit <- limma::lmFit(values,design)
  fit <- limma::eBayes(fit)
} else {
  design <- model.matrix(~0+factor(groups,levels=c("control","tumor")))
  colnames(design) <- c("control","tumor")
  fit <- limma::lmFit(values,design)
  fit <- limma::contrasts.fit(fit,limma::makeContrasts(tumor-control,levels=design))
  fit <- limma::eBayes(fit)
  coefficient <- 1L
}
results <- limma::topTable(fit,coef=coefficient,number=Inf,sort.by="none",adjust.method="BH")
results$probe_id <- rownames(results)
results <- results[order(results$P.Value, results$probe_id),]
write.csv(results,file.path(output_dir,"limma-probe-results.csv"),row.names=FALSE)
write_status("EXPLORATORY_ANALYSIS_COMPLETED",
  "R limma analyzed authentic GEO numeric values with explicit nonconfounded groups. Biological validity is NOT established.",
  list(accession=basename(source_file),platform=platform,
       design=pairing$design, independent_patients=if(pairing$design=="paired") length(unique(pairing$patient_ids)) else NA_integer_,
       samples_control=sum(groups=="control"),samples_tumor=sum(groups=="tumor"),
       probes_tested=nrow(results),probes_fdr_under_0_05=sum(results$adj.P.Val<0.05),
       r_version=R.version.string,limma_version=as.character(packageVersion("limma"))))
