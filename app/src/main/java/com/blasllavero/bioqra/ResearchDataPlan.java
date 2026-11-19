package com.blasllavero.bioqra;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small, auditable AI data-source decision; untrusted model output is parsed
 * as a bounded search phrase, never as a URL, command, or download instruction.
 */
public final class ResearchDataPlan {
    public final String geoQuery,explanation,raw;
    private ResearchDataPlan(String geoQuery,String explanation,String raw){
        this.geoQuery=geoQuery;this.explanation=explanation;this.raw=raw;
    }
    public static ResearchDataPlan parse(String response){
        if(response==null)throw new IllegalArgumentException("Empty planner response");
        Matcher match=Pattern.compile("(?im)^GEO_QUERY:\\s*([A-Za-z0-9][A-Za-z0-9 ,()\\-]{2,95})\\s*$")
            .matcher(response);
        if(!match.find())throw new IllegalArgumentException("No structured GEO search query");
        String query=match.group(1).trim().replaceAll("\\s+"," ");
        if(query.length()>95 || query.split("\\s+").length>14)
            throw new IllegalArgumentException("Planner terms too broad");
        return new ResearchDataPlan(query,
            "Model proposed a query for NCBI GEO; BIOQRA must independently verify results.",
            response.substring(0,Math.min(5000,response.length())));
    }
}
