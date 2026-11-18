package com.blasllavero.bioqra;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import java.util.Locale;

/**
 * Converts approved public challenge HTML into readable plain text without
 * executing scripts or interpreting webpage instructions. Used both for the
 * screen preview and for the user's authorized AI request.
 */
public final class ChallengeTextFormatter {
    private ChallengeTextFormatter(){}

    private static void newlines(StringBuilder out,int count){
        int last=out.length()-1;
        while(last>=0 && (out.charAt(last)==' ' || out.charAt(last)=='\t')){
            out.deleteCharAt(last--);
        }
        if(out.length()==0)return;
        int present=0;
        for(int i=out.length()-1;i>=0 && out.charAt(i)=='\n';i--)present++;
        for(int i=present;i<count;i++)out.append('\n');
    }

    private static void writeText(StringBuilder out,String text){
        if(text==null || text.isEmpty())return;
        String cleaned=text.replace('\u00a0',' ')
                .replaceAll("[\\s\\p{Zs}]+"," ");
        if(cleaned.trim().isEmpty()){
            if(out.length()>0 && out.charAt(out.length()-1)!=' '
                    && out.charAt(out.length()-1)!='\n')out.append(' ');
            return;
        }
        if(cleaned.startsWith(" ") && (out.length()==0
                || Character.isWhitespace(out.charAt(out.length()-1))))
            cleaned=cleaned.substring(1);
        out.append(cleaned);
    }

    private static boolean heading(String tag){
        return tag.matches("h[1-6]");
    }
    private static boolean paragraph(String tag){
        return tag.equals("p")||tag.equals("blockquote")||tag.equals("figure")
                ||tag.equals("figcaption")||tag.equals("address");
    }
    private static boolean list(String tag){
        return tag.equals("ul")||tag.equals("ol")||tag.equals("dl");
    }

    private static void walk(Node node,StringBuilder out,int depth){
        if(depth>80)return;  // public HTML may be malformed or deeply nested
        if(node instanceof TextNode){
            writeText(out,((TextNode)node).getWholeText());
            return;
        }
        if(!(node instanceof Element)){
            for(Node child:node.childNodes())walk(child,out,depth+1);
            return;
        }
        Element e=(Element)node;
        String tag=e.normalName();
        if(tag.equals("script")||tag.equals("style")||tag.equals("noscript")
                ||tag.equals("template")||tag.equals("form")||tag.equals("nav")
                ||tag.equals("footer")||tag.equals("aside")||tag.equals("svg")
                ||tag.equals("iframe"))return;
        if(tag.equals("br")||tag.equals("hr")){
            newlines(out,tag.equals("hr")?2:1);
            return;
        }
        if(tag.equals("pre")){
            newlines(out,2);
            out.append(e.wholeText().replace("\r\n","\n")
                    .replace('\r','\n').trim());
            newlines(out,2);
            return;
        }
        if(heading(tag)||paragraph(tag)||list(tag)||tag.equals("table")){
            newlines(out,2);
        }else if(tag.equals("li")||tag.equals("tr")||tag.equals("dt")
                ||tag.equals("dd")||tag.equals("section")||tag.equals("article")){
            newlines(out,1);
        }else if(tag.equals("div")){
            // Div elements often represent layout, not prose; one break avoids
            // merging separate content blocks without adding excessive gaps.
            newlines(out,1);
        }
        if(tag.equals("li"))out.append("• ");
        else if(tag.equals("dt"))out.append("— ");
        if(tag.equals("td")||tag.equals("th")){
            Element previous=e.previousElementSibling();
            if(previous!=null && (previous.normalName().equals("td")
                    || previous.normalName().equals("th")))out.append(" | ");
        }
        for(Node child:e.childNodes())walk(child,out,depth+1);
        if(heading(tag)||paragraph(tag)||list(tag)||tag.equals("table")){
            newlines(out,2);
        }else if(tag.equals("li")||tag.equals("tr")||tag.equals("dt")
                ||tag.equals("dd")||tag.equals("section")||tag.equals("article")
                ||tag.equals("div")){
            newlines(out,1);
        }
    }

    public static String extract(Element root){
        if(root==null)return "";
        StringBuilder b=new StringBuilder();
        walk(root,b,0);
        return b.toString().replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\\n[ \\t]+","\\n")
                .replaceAll("\\n{3,}","\\n\\n").trim();
    }

    /** Do not cut in the middle of a sentence if a paragraph boundary is near. */
    public static String truncate(String input,int max){
        if(input==null || input.length()<=max)return input==null?"":input;
        int cut=input.lastIndexOf("\n\n",max);
        if(cut<max/2)cut=input.lastIndexOf(' ',max);
        if(cut<max/2)cut=max;
        return input.substring(0,cut).trim()+"\n\n[Excerpt truncated]";
    }
}
