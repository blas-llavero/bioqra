#!/usr/bin/env Rscript
# Deterministic synthetic NUMERICAL FIXTURE to verify implementation only.
# These are not patient observations and must NEVER be cited as clinical data.
stopifnot(requireNamespace("limma",quietly=TRUE))
source("tools/geo_pairing.R")
groups <- rep(c("control","tumor"),4)
people <- rep(paste0("T",seq_len(4)),each=2L)
ids <- paste0("GSM",seq_len(8))
metadata <- list(paste0("patient id: ",people))
decision <- bioqra_paired_design(groups,metadata,ids)
stopifnot(identical(decision$status,"ELIGIBLE"),
          identical(decision$design,"paired"))
# All four paired differences are positive; baseline varies strongly across
# patients to ensure the patient factor is actually included in the design.
baseline <- c(1,100,4,50)
diff <- c(2,3,2,3)
values <- matrix(0,nrow=20,ncol=8)
for(gene in seq_len(20L)) {
  values[gene,] <- as.vector(rbind(baseline+gene*0.1,
                                   baseline+gene*0.1+diff))
}
design <- model.matrix(~factor(people)+factor(groups,levels=c("control","tumor")))
stopifnot(qr(design)$rank==ncol(design))
fit <- limma::lmFit(values,design)
fit <- limma::eBayes(fit)
coef <- fit$coefficients[,ncol(design)]
stopifnot(length(coef)==20L,all(is.finite(coef)),
          all(abs(coef-2.5)<1e-8))
cat("Patient fixed-effect limma mathematical fixture PASSED (not research evidence)\n")
