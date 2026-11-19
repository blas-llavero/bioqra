# BIOQRA pairing-design eligibility. No sample group or patient is invented.
# This module is purely a metadata validator, and has no network access.

bioqra_paired_design <- function(groups, characteristics, sample_ids) {
  n <- length(sample_ids)
  reject <- function(reason) list(status="SKIPPED", reason=reason, design=NULL)
  if (length(groups)!=n || n<6L || anyNA(groups) ||
      any(!groups %in% c("control","tumor")) ||
      anyDuplicated(sample_ids)) {
    return(reject("Missing or ambiguous group labels/GSM accessions"))
  }
  if (!is.list(characteristics) || length(characteristics)==0L)
    return(reject("No sample characteristics to validate independence"))
  ids <- NULL
  pairing_marker <- FALSE
  for (field in characteristics) {
    if(length(field)!=n)
      return(reject("Inconsistent sample-characteristic column lengths"))
    values <- tolower(trimws(field))
    labels <- trimws(sub(":.*$", "", values))
    if(any(grepl("^(batch|batch id|batch_id|treatment|timepoint|time point|time_point)$",labels))) {
      return(reject("Batch, treatment or timepoint field present; unadjusted models prohibited"))
    }
    parent_keys <- c("patient id","patient_id","patient","subject id","subject_id",
                     "donor id","donor_id","pair id","pair_id")
    if (any(labels %in% parent_keys)) {
      if(!all(labels %in% parent_keys))
        return(reject("Patient identifiers are inconsistently labelled between samples"))
      parsed <- trimws(sub("^[^:]*:", "",field))
      if(any(!grepl("^[A-Za-z0-9_.-]{1,64}$", parsed)))
        return(reject("Patient identifier missing or unsafe"))
      if(is.null(ids)) ids <- parsed
      else if(!identical(ids,parsed))
        return(reject("Contradictory patient identifiers across characteristics"))
    }
    if(any(grepl("^(paired|pairing|matched)",labels))) pairing_marker <- TRUE
  }
  if(is.null(ids)) {
    if(pairing_marker)
      return(reject("Paired design mentioned but patient identifiers absent"))
    return(list(status="ELIGIBLE",reason="No explicit pairing or confounding fields",
                design="unpaired",patient_ids=NULL))
  }
  patients <- unique(ids)
  if(length(patients)<3L)
    return(reject("Paired model requires at least three independent patient pairs"))
  if(any(tabulate(match(ids,patients),nbins=length(patients))!=2L))
    return(reject("Each patient must have exactly one tumor and one control sample"))
  for (patient in patients) {
    g <- groups[ids==patient]
    if(!identical(sort(g),c("control","tumor")))
      return(reject("Every patient must have one control and one tumor sample"))
  }
  list(status="ELIGIBLE",
       reason="Exactly two explicitly annotated samples per patient, each one control and one tumor",
       design="paired", patient_ids=ids)
}
