# Explicit published GEO sample TITLE parser, limited to a reviewed grammar.
# Not a generic inference rule: no sample-order pairing and no invented labels.
bioqra_geo_title_pairs <- function(titles, accession) {
  fail <- function(reason) list(status="SKIPPED",reason=reason)
  if(!identical(accession,"GSE20347"))return(fail("Title grammar not reviewed for this accession"))
  titles <- trimws(tolower(titles))
  normal_pattern <- "^normal adjacent esophageal tissue from patient ([a-z]+) ([0-9]{1,4})$"
  tumor_pattern <- "^esophageal squamous cell carcinoma from patient ([a-z]+) ([0-9]{1,4})$"
  is_normal <- grepl(normal_pattern,titles)
  is_tumor <- grepl(tumor_pattern,titles)
  if(length(titles)!=34L||!all(is_normal|is_tumor)||
     sum(is_normal)!=17L || sum(is_tumor)!=17L)
     return(fail("Published sample titles do not match expected two-group grammar"))
  extract <- function(x,pattern){
    sub(pattern,"\\1-\\2",x)
  }
  patient <- character(length(titles))
  patient[is_normal] <- extract(titles[is_normal],normal_pattern)
  patient[is_tumor] <- extract(titles[is_tumor],tumor_pattern)
  groups <- ifelse(is_normal,"control","tumor")
  if(length(unique(patient))!=17L ||
     any(vapply(split(groups,patient), function(x)
                !identical(sort(x),c("control","tumor")),logical(1))))
     return(fail("Published sample titles do not contain 17 unique tumor-normal pairs"))
  list(status="ELIGIBLE", reason="Exact reviewed GEO source-title syntax, 17 paired patients",
       groups=groups,patient_ids=patient,annotation_source="GSE20347 GEO sample titles",
       pairing_verified_from_source=TRUE)
}
