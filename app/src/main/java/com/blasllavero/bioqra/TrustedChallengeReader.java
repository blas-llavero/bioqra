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
            "2026.genedis.org", "mlcontests.com", "www.mlcontests.com"));

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

    public static final class Result {
        public final String text, url, status;
        Result(String text, String url, String status) {
            this.text=text; this.url=url; this.status=status;
        }
    }

    public static Result read(String link) throws Exception {
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
                doc.select("script,style,iframe,object,embed,form,footer,nav,header,aside,svg,button").remove();
                Element best=null;
                for(Element e:doc.select("main,article,[role=main],.challenge-content,.problem-statement,#content")) {
                    if(best==null || e.text().length()>best.text().length())best=e;
                }
                String text=(best!=null?best:doc.body()).text().replaceAll("\\s+"," ").trim();
                if(text.length()>18000) text=text.substring(0,18000)+" [truncated]";
                if(text.length()<100) throw new Exception(
                        "No readable public problem statement. This challenge may need JavaScript, authentication or a separate dataset.");
                return new Result(text,link,"HTTPS and organizer domain checked · public HTML only");
            }
        } finally {connection.disconnect();}
    }
}
