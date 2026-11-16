package com.blasllavero.bioqra;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A vetted, public-only catalog; no medical or patient data. */
public final class ResearchCatalog {
    private ResearchCatalog() {}

    public static final String ROSALIND = "Rosalind";
    public static final String OPEN = "Open Problems";
    public static final String CANCER = "Cancer Grand Challenges";
    public static final String DREAM = "DREAM";
    public static final String HACK = "Hackathons";
    public static final String ML = "ML Contests";
    public static final String GRAND = "Grand Challenge";
    public static final String REVIEWED = "2026-11-16";
    public static final String[] FILTERS = {
        "All sources", GRAND, CANCER, DREAM, OPEN, HACK, ML, ROSALIND
    };

    public static final String[] AVAILABILITY_FILTERS = {
        "Open / Upcoming", "Research benchmarks", "Educational exercises",
        "Needs verification", "All records"
    };
    public static final int PAGE_SIZE = 25;

    /** Verified is a snapshot, not a guarantee: stale records are downgraded. */
    private static boolean freshSnapshot() {
        return !LocalDate.now().isAfter(LocalDate.parse(REVIEWED).plusDays(7));
    }

    public static final class Item {
        public final String id, title, source, summary, url, kind, topic, deadline;
        public Item(String id, String title, String source, String summary, String url,
                    String kind, String topic, String deadline) {
            this.id=id; this.title=title; this.source=source; this.summary=summary;
            this.url=url; this.kind=kind; this.topic=topic; this.deadline=deadline;
        }
        public String availability() {
            if ("funded_research".equals(kind)) return "RESEARCH";
            if ("benchmark".equals(kind)) return "BENCHMARK";
            if ("educational".equals(kind)) return "EDUCATIONAL";
            if ("historical".equals(kind)) return "CLOSED";
            if ("restricted".equals(kind)) return "RESTRICTED";
            if ("funding".equals(kind) && deadline != null
                    && LocalDate.now().isAfter(LocalDate.parse(deadline))) return "CLOSED";
            if ("competition_upcoming".equals(kind) && deadline != null
                    && LocalDate.now().isBefore(LocalDate.parse(deadline))) return "UPCOMING";
            if ("competition_upcoming".equals(kind) && deadline != null
                    && !LocalDate.now().isBefore(LocalDate.parse(deadline))) return "CHECK";
            if ("competition_open".equals(kind) && deadline != null
                    && LocalDate.now().isAfter(LocalDate.parse(deadline))) return "CLOSED";
            if ("competition_open".equals(kind) || "funding".equals(kind))
                return freshSnapshot() ? "OPEN" : "CHECK";
            if ("preregister".equals(kind)) return freshSnapshot() ? "UPCOMING" : "CHECK";
            return "CHECK";
        }

        public String label() {
            String state = availability();
            if ("OPEN".equals(state)) {
                if ("funding".equals(kind))
                    return "Funding applications · deadline " + deadline
                            + " · eligibility and pre-screening required · checked " + REVIEWED;
                return "Accepting submissions (checked " + REVIEWED + ") · confirm on organizer site";
            }
            if ("UPCOMING".equals(state)) {
                return "UPCOMING · " + ("preregister".equals(kind)
                        ? "pre-register with organizer" : "next submission phase opens " + deadline);
            }
            if ("BENCHMARK".equals(state)) return "Research benchmark · not a timed contest";
            if ("EDUCATIONAL".equals(state)) return "Educational exercise";
            if ("RESEARCH".equals(state)) return "Funded project · applications closed";
            if ("CLOSED".equals(state)) return "Closed · no longer accepting applications";
            if ("RESTRICTED".equals(state)) return "Restricted eligibility · check organizer conditions";
            return "Status unverified or stale · check organizer (last review " + REVIEWED + ")";
        }
    }

    private static void add(List<Item> list, String id, String title, String source,
                             String summary, String url, String kind, String topic, String deadline) {
        list.add(new Item(id, title, source, summary, url, kind, topic, deadline));
    }

    public static List<Item> initial() {
        List<Item> result = new ArrayList<>();
        String base = "https://openproblems.bio/benchmarks/";
        String[][] tasks = {
            {"batch_integration","Batch Integration","Remove technical batch effects while preserving biology"},
            {"cell_cell_communication","Cell-Cell Communication","Predict intercellular signaling"},
            {"cyto_batch_integration","Cyto Batch Integration","Compare cytometry integration algorithms"},
            {"denoising","Denoising","Reduce noise in single-cell RNA counts"},
            {"dimensionality_reduction","Dimensionality Reduction for Visualisation","Visualize high-dimensional data"},
            {"foundation_models","Foundation Models","Evaluate foundation models for single-cell biology"},
            {"ist_preprocessing","iST Preprocessing","Process imaging-based spatial transcriptomics"},
            {"label_projection","Label Projection","Transfer reference cell type annotations"},
            {"match_modalities","Match Modalities","Match multiple single-cell modalities"},
            {"perturbation_prediction","Perturbation Prediction","Predict how small molecules alter gene expression"},
            {"predict_modality","Predict Modality","Predict one omics modality from another"},
            {"spatial_decomposition","Spatial Decomposition","Estimate spatial cell-type composition"},
            {"spatial_simulators","Spatial Simulators","Assess simulated spatial transcriptomics"},
            {"spatially_variable_genes","Spatially Variable Genes","Detect spatial expression patterns"}
        };
        for (String[] t : tasks) {
            add(result, "op-"+t[0], t[1], OPEN, t[2],
                    base+t[0]+"/", "benchmark", "single-cell", null);
        }

        add(result,"cgc-gastro-transformation","Gastroesophageal transformation into cancer",CANCER,
                "Study early tissue changes with the aim of preventing stomach and oesophageal cancer",
                "https://www.cancergrandchallenges.org/apply-for-a-challenge",
                "funding","cancer prevention","2026-11-24");
        add(result,"cgc-global-variation","Global variation in gastroesophageal cancer",CANCER,
                "Find biological mechanisms behind differences in incidence between populations",
                "https://www.cancergrandchallenges.org/apply-for-a-challenge",
                "funding","cancer prevention","2026-11-24");

        String active="https://www.cancergrandchallenges.org/challenges/active-challenges";
        String[][] cgc={
            {"cancer-avoidance","Cancer avoidance","Why are certain high-risk people resistant to cancer?"},
            {"dark-proteome","The dark proteome","Find unexplored proteins relevant to cancer"},
            {"mutational-signatures","Mechanisms driving mutational signatures","Understand unexplained mutational patterns"},
            {"nervous-system","The nervous system and cancer","Explore interactions between nerves and tumors"},
            {"rewire","Rewiring cancer cells","Study ways to alter malignant cellular programs"}
        };
        for(String[] item:cgc) add(result,"cgc-"+item[0],item[1],CANCER,item[2],active,
                                   "funded_research","cancer",null);

        add(result,"dream-drug-discovery","DREAM x CACHE Target 2035 Drug Discovery",DREAM,
                "Machine learning for drug discovery; confirm registration and evaluation on the official organizer page",
                "https://dreamchallenges.org/open-challenges/","historical","drug discovery",null);
        add(result,"dream-quantum","DREAM-Q Target 2035 Quantum Challenge",DREAM,
                "Quantum-inspired approaches for biomedical target challenges; verify eligibility",
                "https://dreamchallenges.org/open-challenges/","directory","drug discovery",null);
        add(result,"dream-digital-pen","Digital Pen Feature Extraction Challenge",DREAM,
                "Analyze cognitive-assessment pen signals; check organizer dates",
                "https://dreamchallenges.org/the-digital-pen-feature-extraction-challenge/","preregister","biomedical",null);
        add(result,"hack-genedis-lung-cancer","GENEDIS 2026 NSCLC multi-omics challenge",HACK,
                "Reproducible multi-omics integration or knowledge-mining workflow for non-small-cell lung cancer; hybrid event 21 November 2026, applications closed",
                "https://2026.genedis.org/hackathon/","historical","cancer multi-omics",null);
        add(result,"dream-directory","DREAM open biomedical challenges",DREAM,
                "Crowdsourced prediction competitions; inspect official open listings",
                "https://dreamchallenges.org/open-challenges/","directory","biomedical",null);
        add(result,"hack-biohacking","BioHackathon Europe",HACK,
                "Annual ELIXIR bioinformatics coding projects; 2026 event has concluded",
                "https://biohackathon-europe.org/","historical","bioinformatics",null);
        add(result,"hack-genedis","GENEDIS 2026 hybrid hackathon",HACK,
                "Biomedical challenge day; application deadline has passed",
                "https://2026.genedis.org/hackathon/","historical","biomedical",null);
        add(result,"ml-list","ML Contests — healthcare and science",ML,
                "Public machine learning competitions; check source organizer before applying",
                "https://mlcontests.com/","directory","biomedical",null);
        // Public, non-educational challenges. Availability checked 16 Nov 2026.
        // Source websites must be opened for entry requirements; statuses expire.
        add(result, "grand-owl", "Observing What Models Learn (OWL)", GRAND,
                "Predict recurrence after prostate cancer surgery and explain the AI predictions; testing deadline 30 Nov 2026.",
                "https://owl.grand-challenge.org/", "competition_open", "cancer pathology", "2026-11-30");
        add(result, "grand-lmc2", "Light My Cells 2", GRAND,
                "Predict fluorescence microscopy channels from transmitted-light images; submissions listed as accepted.",
                "https://lightmycells2.grand-challenge.org/lightmycells2/", "competition_open", "bioimaging", "2026-12-13");
        add(result, "grand-lung", "Lung Nodule Volumetry 2026", GRAND,
                "Measure pulmonary nodule volume from CT. Restricted to vendors with CE-certified products; participation agreement required.",
                "https://lunovo26.grand-challenge.org/how-to-participate/", "restricted", "lung cancer imaging", null);
        add(result, "grand-cobra", "COBRA: Radiotherapy Cone-Beam CT Reconstruction", GRAND,
                "Build automated CT reconstruction for radiation planning. Training now; validation 1 Dec 2026 and final testing 29 Jan–15 Feb 2027.",
                "https://cobra2026.grand-challenge.org/", "competition_open", "cancer radiotherapy", "2027-02-15");
        add(result, "grand-topbrain", "TopBrain Segmentation Challenge V2", GRAND,
                "Segment brain vascular structures; final-test submission phase opens 11 Dec 2026.",
                "https://topbrain2026.grand-challenge.org/", "competition_upcoming", "neuroimaging", "2026-12-11");
        return result;
    }

    private static final Pattern OPEN_LINK = Pattern.compile(
        "href=[\\\"'](?:https://openproblems\\.bio)?/benchmarks/([a-z][a-z0-9_]{2,60})/?[\\\"']",
        Pattern.CASE_INSENSITIVE);

    /** Parse public index link IDs only; no dependence on screen text/HTML layout. */
    public static List<Item> parseOpenProblems(String html) {
        Map<String,Item> found = new LinkedHashMap<>();
        Map<String,Item> known = new LinkedHashMap<>();
        for (Item i : initial()) if (OPEN.equals(i.source)) known.put(i.id.substring(3),i);
        Matcher links=OPEN_LINK.matcher(html);
        while (links.find()) {
            String slug=links.group(1).toLowerCase(java.util.Locale.ROOT);
            Item item=known.get(slug);
            if (item!=null) found.put(item.id,item);
            // Ignore unknown task identifiers until reviewed; prevent fabricated titles.
        }
        if (found.size() < 10) throw new IllegalArgumentException(
                "Open Problems index changed; keeping reviewed snapshot");
        return new ArrayList<>(found.values());
    }
}
