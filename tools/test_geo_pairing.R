#!/usr/bin/env Rscript
# Synthetic identifiers exclusively for algorithmic unit tests.
# They are NOT real patients and NEVER used as scientific evidence.
source("tools/geo_pairing.R")
expect <- function(condition,message)if(!isTRUE(condition))stop(message)
samples <- paste0("GSM",seq_len(6))
groups <- rep(c("control","tumor"),3)
pairing <- list(c("patient id: P1","patient id: P1",
                  "patient id: P2","patient id: P2",
                  "patient id: P3","patient id: P3"))
r <- bioqra_paired_design(groups,pairing,samples)
expect(identical(r$status,"ELIGIBLE")&&identical(r$design,"paired"),
       "Explicit three-patient pairing must pass")
unpaired <- bioqra_paired_design(groups,list(rep("group: control",6)),samples)
expect(identical(unpaired$design,"unpaired"),"No pairing metadata is unpaired")
bad <- pairing
bad[[1]][2] <- "patient id: P2"
expect(identical(bioqra_paired_design(groups,bad,samples)$status,"SKIPPED"),
       "Three differently matched subjects must be rejected")
batch <- append(pairing,list(rep("batch: B1",6)))
expect(identical(bioqra_paired_design(groups,batch,samples)$status,"SKIPPED"),
       "Batch metadata must block simplistic model")
missing <- list(rep("paired: yes",6))
expect(identical(bioqra_paired_design(groups,missing,samples)$status,"SKIPPED"),
       "No fabricated pairing assignments")
dup <- samples;dup[2] <- dup[1]
expect(identical(bioqra_paired_design(groups,pairing,dup)$status,"SKIPPED"),
       "Duplicate source accessions must fail")
cat("GEO pairing eligibility unit tests PASSED (synthetic test fixtures only)\n")
