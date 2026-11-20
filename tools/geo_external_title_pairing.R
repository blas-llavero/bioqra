# Accession-specific independent GEO source title pairing: GSE23400.
# Exact source codes AE<integer>N / AE<integer>T; does not infer sample order.
bioqra_external_title_pairs <- function(titles, sources, accession, platform) {
  skip <- function(reason) list(status="SKIPPED",reason=reason)
  if(!identical(accession,"GSE23400") ||
     !platform %in% c("GPL96","GPL97"))
    return(skip("No reviewed source grammar for this accession or platform"))
  if(length(titles)!=length(sources) || length(titles)<6L ||
     !length(titles) %in% c(106L,102L))
    return(skip("Unexpected sample count or mismatched source columns"))
  labels <- tolower(trimws(gsub('^"|"$',"",titles)))
  sources <- tolower(trimws(gsub('^"|"$',"",sources)))
  if(!all(grepl("^ae[0-9]{1,5}[nt]$",labels)))
    return(skip("Unrecognized GSE23400 source title label"))
  group <- ifelse(grepl("n$",labels),"control","tumor")
  if(any((group=="control" & sources!="adjacent normal tissue")|
         (group=="tumor" & sources!="tumor tissue")))
    return(skip("Source tissue classification contradicts GEO sample title"))
  patient <- sub("^ae([0-9]{1,5})[nt]$","ae\1",labels)
  # Avoid R substitution ambiguity: regenerate using substring with explicit
  # removal of the final N/T class indicator.
  patient <- substr(labels,1,nchar(labels)-1L)
  cohorts <- split(group,patient)
  if(length(cohorts)*2L!=length(labels) ||
     any(vapply(cohorts,function(x)!identical(sort(x),c("control","tumor")),logical(1))))
    return(skip("Incomplete or duplicated patient tumor/control pairs"))
  expected <- if(platform=="GPL96")53L else 51L
  if(length(cohorts)!=expected)
    return(skip("Observed pair count contradicts reviewed source study"))
  list(status="ELIGIBLE",reason="Exact independently checked GEO source-title pairings",
       design="paired",groups=group,patient_ids=patient,
       independent_patient_pairs=expected,annotation_source="GSE23400 original GEO Sample_title + Sample_source_name_ch1",
       clinical_validation=FALSE)
}
