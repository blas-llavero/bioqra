package com.blasllavero.bioqra;

/** Pure checks shared by the Android viewer and JVM regression tests. */
public final class PdfUriPolicy {
    private PdfUriPolicy(){}
    public static boolean validProject(String project){
        return project!=null && project.matches("[0-9]{6}-[A-Za-z0-9][^/\\\\\\p{Cntrl}]{0,74}")
                && !project.contains("..") && !project.endsWith(".");
    }
    public static boolean expectedName(String project,String name){
        return validProject(project) && name!=null &&
                name.equals(project.substring(7)+".pdf");
    }
    public static boolean expectedDirectory(String project,String directory){
        return validProject(project) &&
                ("Download/Bioqra/"+project+"/").equals(directory);
    }
    public static boolean pdfSignature(byte[] first){
        return first!=null && first.length>=5 && first[0]=='%' && first[1]=='P'
                && first[2]=='D' && first[3]=='F' && first[4]=='-';
    }
}
