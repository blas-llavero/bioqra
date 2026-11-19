package com.blasllavero.bioqra;

import android.content.Context;
import java.util.Locale;

/**
 * Conservative evidence gates. No number of candidate downloads alone can
 * satisfy an entire scientific challenge; scientific relevance and validation
 * require a separate, challenge-specific assessment.
 */
public final class ResearchReadiness {
    private ResearchReadiness(){}

    public enum State {
        NO_DATA, SOURCE_IDENTIFIED, FILES_ACQUIRED_UNVALIDATED,
        ANALYSIS_READY, RESULTS_REPRODUCED, SCIENTIFICALLY_VALIDATED
    }
    public static final class Assessment {
        public final State state;
        public final String missing;
        public final String reason;
        Assessment(State state,String missing,String reason){
            this.state=state;this.missing=missing;this.reason=reason;
        }
        public String describe(){
            return "Evidence status: "+state.name()+"\nReason: "+reason+
                "\nOutstanding requirements: "+missing+
                "\nNo biological result or challenge solution has been validated.";
        }
    }
    public static Assessment assess(Context context,String challengeKey,
                                    PublicDatasetDiscovery.Report report){
        int count=ResearchEvidenceArchive.downloadedCount(context,challengeKey);
        if(count>0){
            return new Assessment(State.FILES_ACQUIRED_UNVALIDATED,
                "Inspect full study design, tissue/species, sample labels, normal controls, "+
                "outcome variables, batch effects, permitted reuse, matrix contents, "+
                "statistical analysis, independently reproduced metrics and challenge criteria.",
                count+" genuine repository file(s) saved with a provenance manifest; "+
                "suitability and completeness have not been evaluated.");
        }
        if(report!=null && !report.studies.isEmpty()){
            return new Assessment(State.SOURCE_IDENTIFIED,
                "Acquire genuinely reusable data files and verify labels, measurements, "+
                "sample suitability, rights, test design and scientific validity.",
                report.studies.size()+" real repository study metadata records found; "+
                "no usable measurement files confirmed.");
        }
        return new Assessment(State.NO_DATA,
            "Locate relevant public dataset files, permissions, controls, study methods "+
            "and measures before attempting computational claims.",
            "No measurements have been acquired.");
    }
}
