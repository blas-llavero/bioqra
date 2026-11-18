package com.blasllavero.bioqra;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only extraction of public research challenge text from approved domains.
 * No cookies, browser credentials, form submission, scripts or file downloads.
 * HTTPS and a trusted hostname do not guarantee the safety or truth of content.
 */
public final class TrustedChallengeReader {
    private TrustedChallengeReader() {}

    private static final Set<String> EXACT = new HashSet<>(Arrays.asList(
            "rosalind.info", "www.rosalind.info", "openproblems.bio",
            "www.openproblems.bio", "dreamchallenges.org", "www.dreamchallenges.org",
            "cancergrandchallenges.org", "www.cancergrandchallenges.org",
            "grand-challenge.org", "www.grand-challenge.org",
            "biohackathon-europe.org", "www.biohackathon-europe.org",
            "2026.genedis.org", "mlcontests.com", "www.mlcontests.com",
            "precision.fda.gov", "challenges.synapse.org",
            "challenge.physionet.org", "moody-challenge.physionet.org",
            "physionet.org", "www.physionet.org",
            "www.codabench.org", "codabench.org",
            "www.cancer.gov", "cancer.gov",
            "www.synapse.org", "synapse.org",
            "openchallenges.io", "www.openchallenges.io"));

    public static boolean approved(String raw) {
        try {
            URI uri = new URI(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null || uri.getFragment() != null
                    || uri.getPort() != -1) return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = IDN.toASCII(host).toLowerCase(Locale.ROOT);
            if (host.endsWith(".") || host.equals("localhost")) return false;
            return EXACT.contains(host) || host.endsWith(".grand-challenge.org");
        } catch(Exception e) { return false; }
    }

    /** Review the HTTPS host and link extension before downloading a small public PDF. */
    public static boolean approvedPdf(String link){
        if(!approved(link))return false;
        try{
            URI uri=new URI(link);
            String path=uri.getPath()==null?"":uri.getPath().toLowerCase(Locale.ROOT);
            return path.endsWith(".pdf") || path.contains("/pdfs/");
        }catch(Exception e){return false;}
    }
    private static byte[] readLimited(InputStream in,int limit) throws Exception{
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){
                out.write(buf,0,n);
                if(out.size()>limit)throw new Exception("Public document exceeds size limit");
            }
            return out.toByteArray();
        }
    }
    /** Download a bounded public PDF without following redirects or sending credentials. */
    public static String fetchPdf(android.content.Context context,String link) throws Exception {
        if(!approvedPdf(link))throw new Exception("Unapproved public PDF URL.");
        HttpURLConnection c=(HttpURLConnection)new URL(link).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(18000);
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept","application/pdf");
        c.setRequestProperty("User-Agent","BIOQRA/0.5 public challenge brief reader");
        try{
            int code=c.getResponseCode();
            if(code!=200)throw new Exception("Organizer PDF HTTP "+code+" (redirects not followed).");
            String mime=c.getContentType();
            if(mime!=null && !(mime.toLowerCase(Locale.ROOT).contains("pdf")
                    || mime.toLowerCase(Locale.ROOT).contains("octet-stream")))
                throw new Exception("Document is not a PDF.");
            byte[] bytes;
            try(InputStream input=c.getInputStream()){bytes=readLimited(input,4_000_000);}
            if(bytes.length<5 || bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'
                    || bytes[3]!='F'||bytes[4]!='-')
                throw new Exception("Invalid PDF signature.");
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(
                    context.getApplicationContext());
            try(com.tom_roush.pdfbox.pdmodel.PDDocument pdf=
                    com.tom_roush.pdfbox.pdmodel.PDDocument.load(bytes)){
                if(pdf.isEncrypted() || pdf.getNumberOfPages()>30)
                    throw new Exception("Encrypted/oversized PDF is not processed.");
                com.tom_roush.pdfbox.text.PDFTextStripper stripper=
                    new com.tom_roush.pdfbox.text.PDFTextStripper();
                String text=stripper.getText(pdf).replaceAll("\\s+"," ").trim();
                if(text.length()<160)throw new Exception("No searchable text in PDF (OCR not performed).");
                return text.substring(0,Math.min(text.length(),18000));
            }
        }finally{c.disconnect();}
    }

    public static final class Result {
        public final String text, url, status;
        Result(String text, String url, String status) {
            this.text=text; this.url=url; this.status=status;
        }
    }

    public static Result read(String link) throws Exception {
        Page page = fetchPage(link);
        return new Result(page.text, page.url, "HTTPS and organizer domain checked · public HTML only");
    }

    /** The original document retains links, while text is stripped of navigation. */
    public static final class Page {
        public final Document document;
        public final String text, url;
        Page(Document document, String text, String url) {
            this.document=document; this.text=text; this.url=url;
        }
    }

    public static Page fetchPage(String link) throws Exception {
        if (!approved(link)) throw new IllegalArgumentException(
                "Automatic retrieval blocked: this source is not in the reviewed HTTPS allowlist.");
        HttpURLConnection connection=(HttpURLConnection)new URL(link).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "text/html");
        connection.setRequestProperty("User-Agent", "BIOQRA/0.4 public challenge reader");
        try {
            int code=connection.getResponseCode();
            if (code>=300 && code<400) {
                String next=connection.getHeaderField("Location");
                throw new Exception("The organizer redirects to another page. Open the official website manually; BIOQRA did not follow the redirect.");
            }
            if (code!=200) throw new Exception("Organizer returned HTTP "+code);
            String type=connection.getContentType();
            if (type==null || !type.toLowerCase(Locale.ROOT).contains("text/html"))
                throw new Exception("This page is not public HTML; private files and PDFs are not retrieved automatically.");
            try(InputStream in=connection.getInputStream();
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[8192]; int n;
                while((n=in.read(buf))!=-1) {
                    out.write(buf,0,n);
                    if(out.size()>600_000) throw new Exception(
                            "The public page is too large to inspect safely.");
                }
                Document doc=Jsoup.parse(out.toString(StandardCharsets.UTF_8.name()),link);
                // Never treat webpage text as code or privileged instructions.
                // Use a separate copy so the resolver can inspect safe link metadata.
                Document readable=doc.clone();
                readable.select("script,style,iframe,object,embed,form,footer,nav,header,aside,svg,button").remove();
                Element best=null;
                for(Element e:readable.select("main,article,[role=main],.challenge-content,.problem-statement,#content")) {
                    if(best==null || e.text().length()>best.text().length())best=e;
                }
                String text=(best!=null?best:readable.body()).text().replaceAll("\\s+"," ").trim();
                if(text.length()>18000) text=text.substring(0,18000)+" [truncated]";
                if(text.length()<100) throw new Exception(
                        "No readable public problem statement. This challenge may need JavaScript, authentication or a separate dataset.");
                return new Page(doc,text,link);
            }
        } finally {connection.disconnect();}
    }
}
