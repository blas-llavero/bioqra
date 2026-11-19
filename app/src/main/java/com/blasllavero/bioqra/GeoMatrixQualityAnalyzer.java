package com.blasllavero.bioqra;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.zip.GZIPInputStream;

/**
 * Bounded, reproducible exploratory quality checks on authentic NCBI GEO
 * series_matrix files. This is not differential expression or clinical inference.
 * No internet access, patient upload, arbitrary code or model execution.
 */
public final class GeoMatrixQualityAnalyzer {
    private GeoMatrixQualityAnalyzer(){}
    private static final int MAX_ROWS=150000;
    private static final int MAX_SAMPLES=500;
    private static final long MAX_CELLS=2500000L;
    private static final long MAX_TEXT_BYTES=110000000L;

    public static final class Result {
        public final int files,parsed;
        public final String summary;
        Result(int files,int parsed,String summary){
            this.files=files;this.parsed=parsed;this.summary=summary;
        }
    }
    private static String sha256(File file) throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(FileInputStream in=new FileInputStream(file)){
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1)md.update(buffer,0,n);
        }
        StringBuilder hex=new StringBuilder();
        for(byte b:md.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return hex.toString();
    }
    private static JSONObject examine(File file,String accession) throws Exception{
        JSONObject item=new JSONObject();
        item.put("accession",accession);
        item.put("kind","GEO Series Matrix exploratory QC");
        int seriesAnnotations=0,sampleAnnotations=0,columns=0,rows=0;
        long cells=0,numeric=0,missing=0,invalid=0;
        boolean table=false,tableEnded=false,limit=false;
        long textBytes=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file)),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                textBytes+=line.length()+1;
                if(textBytes>MAX_TEXT_BYTES){limit=true;break;}
                if(line.startsWith("!Series_"))seriesAnnotations++;
                if(line.startsWith("!Sample_"))sampleAnnotations++;
                if(line.equals("!series_matrix_table_begin")){table=true;continue;}
                if(line.equals("!series_matrix_table_end")){tableEnded=true;break;}
                if(!table)continue;
                if(columns==0){
                    String[] header=line.split("\t",-1);
                    if(header.length<2 || header.length-1>MAX_SAMPLES)
                        throw new Exception("Invalid/excessive sample columns");
                    columns=header.length-1;
                    continue;
                }
                if(rows>=MAX_ROWS || cells>=MAX_CELLS){limit=true;break;}
                String[] fields=line.split("\t",-1);
                if(fields.length!=columns+1){invalid++;continue;}
                rows++;
                for(int i=1;i<fields.length;i++){
                    cells++;
                    String value=fields[i].replace("\"","").trim();
                    if(value.isEmpty()||value.equalsIgnoreCase("NA")||
                            value.equalsIgnoreCase("null")){missing++;continue;}
                    try{
                        double number=Double.parseDouble(value);
                        if(Double.isFinite(number))numeric++;
                        else invalid++;
                    }catch(NumberFormatException ex){invalid++;}
                }
            }
        }
        item.put("series_annotation_lines",seriesAnnotations);
        item.put("sample_annotation_lines",sampleAnnotations);
        item.put("sample_columns",columns);
        item.put("feature_rows_scanned",rows);
        item.put("matrix_cells_scanned",cells);
        item.put("numeric_cells",numeric);
        item.put("missing_cells",missing);
        item.put("non_numeric_cells",invalid);
        item.put("table_end_seen",tableEnded);
        item.put("scan_truncated",limit);
        boolean parsed=columns>0 && rows>0 && numeric>0;
        item.put("matrix_readable",parsed);
        item.put("status",parsed?"EXPLORATORY_QC_ONLY":"NO_USABLE_NUMERIC_MATRIX");
        item.put("warning","These are measured file-content counts, not clinical findings, differential expression, model scores or challenge validation.");
        return item;
    }

    public static Result analyze(Context context,String challengeKey) throws Exception{
        if(challengeKey==null||!challengeKey.matches("[a-f0-9]{32}"))
            throw new Exception("Invalid challenge key");
        File directory=new File(new File(context.getFilesDir(),"research-evidence"),challengeKey);
        File manifest=new File(directory,"manifest.json");
        if(!manifest.isFile() || manifest.length()>150000)
            throw new Exception("Verified evidence manifest required");
        byte[] bytes=new byte[(int)manifest.length()];
        try(FileInputStream in=new FileInputStream(manifest)){
            int offset=0;
            while(offset<bytes.length){
                int n=in.read(bytes,offset,bytes.length-offset);
                if(n<0)throw new Exception("Incomplete manifest");
                offset+=n;
            }
        }
        JSONObject source=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        JSONArray list=source.optJSONArray("downloaded_files");
        if(list==null||list.length()==0)
            return new Result(0,0,"No real local GEO files: exploratory analysis not executed.");
        JSONObject report=new JSONObject();
        report.put("analysis_type","GEO matrix data-quality and completeness inspection");
        report.put("generated_utc",Instant.now().toString());
        report.put("challenge",source.optString("challenge"));
        report.put("method","BIOQRA Java streaming parser v1; count annotation lines, sample columns, numeric/missing/non-numeric cells; bounded scanning; SHA-256 must match recorded download.");
        report.put("scientific_conclusion","NONE; no outcome labels evaluated, no statistical test, no clinical or therapeutic inference.");
        JSONArray results=new JSONArray();
        int checked=0,parsed=0;
        for(int i=0;i<list.length()&&checked<6;i++){
            JSONObject entry=list.optJSONObject(i);
            if(entry==null)continue;
            String accession=entry.optString("accession");
            String name=entry.optString("file");
            if(!accession.matches("GSE[0-9]+")||
                    !name.equals(accession+"_series_matrix.txt.gz"))continue;
            checked++;
            File file=new File(directory,name);
            JSONObject item;
            try{
                if(!file.isFile()||file.length()!=entry.optLong("bytes")||
                        !sha256(file).equals(entry.optString("sha256")))
                    throw new Exception("Missing file, size mismatch or SHA-256 mismatch");
                item=examine(file,accession);
                item.put("source_sha256",entry.optString("sha256"));
                if(item.optBoolean("matrix_readable"))parsed++;
            }catch(Exception e){
                item=new JSONObject();
                item.put("accession",accession);
                item.put("status","VALIDATION_FAILED");
                item.put("error",e.getMessage());
            }
            results.put(item);
        }
        report.put("files_checked",checked);
        report.put("numeric_matrices_parsed",parsed);
        report.put("files",results);
        report.put("analysis_ready",false);
        report.put("challenge_solved",false);
        report.put("next_requirements","Challenge-specific case/control labels, dataset suitability, correct normalization, batch-confounder assessment, statistical evaluation and independent scientific validation.");
        File pending=new File(directory,"analysis-qc.json.partial");
        try(FileOutputStream out=new FileOutputStream(pending)){
            out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        File finished=new File(directory,"analysis-qc.json");
        if(finished.exists()&&!finished.delete())throw new Exception("Cannot replace QC report");
        if(!pending.renameTo(finished))throw new Exception("Cannot finalize QC report");
        return new Result(checked,parsed,checked+" authentic files checked; "+parsed+
            " numeric matrix/matrices read successfully. QC report saved. "+
            "Scientific significance and challenge solution NOT established.");
    }
}
