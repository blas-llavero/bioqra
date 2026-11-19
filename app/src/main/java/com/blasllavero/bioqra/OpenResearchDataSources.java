package com.blasllavero.bioqra;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Official research dataset search entry points. The links launch a live search
 * on the respective repository, NOT a claim that a matching dataset exists.
 * No dataset files, patient records or authentication are accessed by BIOQRA.
 */
public final class OpenResearchDataSources {
    private OpenResearchDataSources(){}

    public static final class Source {
        public final String name,description,url,accessNote;
        Source(String name,String description,String url,String accessNote){
            this.name=name;this.description=description;this.url=url;this.accessNote=accessNote;
        }
    }

    private static String terms(String challenge){
        String text=challenge==null?"":challenge.toLowerCase(Locale.ROOT);
        if(text.contains("gastro") || text.contains("oesoph") || text.contains("esoph"))
            return "esophageal gastric cancer";
        if(text.contains("leuk") || text.contains("leuc"))return "leukemia";
        if(text.contains("breast") || text.contains("mama"))return "breast cancer";
        if(text.contains("lung") || text.contains("pulm"))return "lung cancer";
        if(text.contains("brain") || text.contains("gliob"))return "glioblastoma";
        String cleaned=(challenge==null?"cancer":challenge)
                .replaceAll("[^\\p{L}\\p{N} ]"," ").trim().replaceAll(" +"," ");
        if(cleaned.isEmpty())return "cancer";
        return cleaned.substring(0,Math.min(85,cleaned.length()));
    }

    public static List<Source> forChallenge(String title){
        String q=terms(title);
        String e=Uri.encode(q);
        ArrayList<Source> result=new ArrayList<>();
        result.add(new Source("NCBI GEO",
                "Public expression profiling and sequencing study records",
                "https://www.ncbi.nlm.nih.gov/gds/?term="+e,
                "Inspect sample groups, accession, assays and follow-up before reuse."));
        result.add(new Source("NCI Genomic Data Commons",
                "Cancer cohorts, clinical metadata, molecular data and biospecimens",
                "https://portal.gdc.cancer.gov/",
                "Some files are controlled access; browsing metadata is not permission to download."));
        result.add(new Source("cBioPortal",
                "Public cancer cohort and molecular study catalog",
                "https://www.cbioportal.org/datasets",
                "Study coverage and outcome annotations vary."));
        result.add(new Source("Human Cell Atlas",
                "Open single-cell and multi-omics projects",
                "https://data.humancellatlas.org/",
                "Check tissue, disease, sequencing modality and reuse conditions."));
        result.add(new Source("PRIDE Archive",
                "Public proteomics and mass spectrometry studies",
                "https://www.ebi.ac.uk/pride/",
                "Identify sample preparation, protein quantification and study accessions."));
        return result;
    }
}
