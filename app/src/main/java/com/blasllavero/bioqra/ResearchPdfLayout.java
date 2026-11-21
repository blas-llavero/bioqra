package com.blasllavero.bioqra;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.ext.gfm.tables.*;
import java.io.OutputStream;
import java.util.Collections;

/** Local Markdown-to-PDF layout. No HTML execution or external resource loading. */
final class ResearchPdfLayout {
    private static final int W=595,H=842,M=45,TOP=53,BOTTOM=793;
    private static final int INK=0xff19262d, ACCENT=0xff134250;
    private final PdfDocument pdf=new PdfDocument();
    private PdfDocument.Page page;
    private Canvas canvas;
    private int y=TOP, number=0;
    private final Paint decoration=new Paint(Paint.ANTI_ALIAS_FLAG);

    static Node parse(String text){
        return Parser.builder().extensions(Collections.singletonList(TablesExtension.create()))
                .build().parse(text==null?"":text);
    }
    private TextPaint paint(float size,boolean bold,boolean mono,int color){
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);p.setColor(color);
        p.setTypeface(Typeface.create(mono?Typeface.MONOSPACE:Typeface.SANS_SERIF,
                bold?Typeface.BOLD:Typeface.NORMAL));
        return p;
    }
    private void newPage() throws Exception{
        if(page!=null){pdf.finishPage(page);page=null;}
        if(++number>150)throw new Exception("PDF exceeds 150 pages");
        page=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,number).create());
        canvas=page.getCanvas();y=TOP;
        canvas.drawText("BIOQRA  |  Research draft",M,29,paint(9,true,false,ACCENT));
        canvas.drawText(String.valueOf(number),W-M-18,818,paint(9,false,false,ACCENT));
        decoration.setColor(Color.rgb(205,219,223));
        canvas.drawLine(M,37,W-M,37,decoration);
    }
    private void ensure(int height)throws Exception{
        if(page==null||y+height>BOTTOM)newPage();
    }
    private void block(CharSequence text,int indent,float size,boolean bold,boolean mono,
                       int color,int gap,boolean shaded)throws Exception{
        if(text.length()==0)return;
        int x=M+Math.min(indent,150), width=W-M-x;
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),
                paint(size,bold,mono,color),width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false).setLineSpacing(3,1.08f)
                .setBreakStrategy(mono?Layout.BREAK_STRATEGY_SIMPLE:Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .build();
        int first=0;
        while(first<layout.getLineCount()){
            int top=layout.getLineTop(first);
            int min=layout.getLineBottom(first)-top;
            ensure(min+4);
            int end=first+1;
            while(end<layout.getLineCount()&&y+layout.getLineBottom(end)-top<=BOTTOM)end++;
            int height=layout.getLineBottom(end-1)-top;
            if(shaded){
                decoration.setColor(Color.rgb(240,244,246));
                canvas.drawRect(x-4,y-2,W-M+4,y+height+2,decoration);
            }
            canvas.save();
            canvas.clipRect(x,y,x+width,y+height);
            canvas.translate(x,y-top);
            layout.draw(canvas);
            canvas.restore();
            y+=height;first=end;
            if(first<layout.getLineCount())newPage();
        }
        y+=gap;
    }
    private static void inline(Node n,SpannableStringBuilder out,int depth){
        if(depth>80)return;
        int start=out.length();
        if(n instanceof Text)out.append(((Text)n).getLiteral());
        else if(n instanceof Code)out.append(((Code)n).getLiteral());
        else if(n instanceof SoftLineBreak)out.append(" ");
        else if(n instanceof HardLineBreak)out.append("\n");
        else if(n instanceof HtmlInline)out.append(((HtmlInline)n).getLiteral());
        else {
            for(Node child=n.getFirstChild();child!=null;child=child.getNext())
                inline(child,out,depth+1);
            if(n instanceof Link){
                String url=((Link)n).getDestination();
                if(url!=null&&!url.isEmpty())out.append(" (").append(url).append(")");
            }
        }
        int end=out.length();
        if(end<=start)return;
        Object span=null;
        if(n instanceof StrongEmphasis)span=new StyleSpan(Typeface.BOLD);
        else if(n instanceof Emphasis)span=new StyleSpan(Typeface.ITALIC);
        else if(n instanceof Code)span=new TypefaceSpan("monospace");
        else if(n instanceof Link)span=new UnderlineSpan();
        if(span!=null)out.setSpan(span,start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private static SpannableStringBuilder inlines(Node n){
        SpannableStringBuilder b=new SpannableStringBuilder();
        for(Node child=n.getFirstChild();child!=null;child=child.getNext())inline(child,b,0);
        return b;
    }
    private void table(Node table,int indent)throws Exception{
        // Vertically arranged labelled cells preserve every column on narrow PDF pages.
        java.util.List<CharSequence> headers=new java.util.ArrayList<>();
        for(Node section=table.getFirstChild();section!=null;section=section.getNext()){
            for(Node row=section.getFirstChild();row!=null;row=row.getNext()){
                if(!(row instanceof TableRow))continue;
                if(section instanceof TableHead){
                    for(Node cell=row.getFirstChild();cell!=null;cell=cell.getNext())
                        headers.add(inlines(cell));
                    continue;
                }
                int col=0;
                ensure(35);
                for(Node cell=row.getFirstChild();cell!=null;cell=cell.getNext()){
                    SpannableStringBuilder line=new SpannableStringBuilder();
                    if(col<headers.size()&&headers.get(col).length()>0){
                        line.append(headers.get(col)).append(": ");
                        line.setSpan(new StyleSpan(Typeface.BOLD),0,line.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    line.append(inlines(cell));
                    block(line,indent+8,10.5f,false,false,INK,4,true);
                    col++;
                }
                y+=8;
            }
        }
    }
    private void walk(Node node,int indent,int depth)throws Exception{
        if(depth>80)throw new Exception("Markdown nesting exceeds supported limit");
        if(node instanceof Heading){
            int level=((Heading)node).getLevel();
            ensure(50);
            block(inlines(node),indent,level==1?18:level==2?15:12.5f,
                    true,false,ACCENT,10,false);
        }else if(node instanceof Paragraph){
            block(inlines(node),indent,11,false,false,INK,9,false);
        }else if(node instanceof FencedCodeBlock||node instanceof IndentedCodeBlock){
            String code=node instanceof FencedCodeBlock?((FencedCodeBlock)node).getLiteral():
                ((IndentedCodeBlock)node).getLiteral();
            block(code.replace("\t","    "),indent+6,9,false,true,INK,10,true);
        }else if(node instanceof ThematicBreak){
            ensure(18);decoration.setColor(Color.rgb(205,219,223));
            canvas.drawLine(M+indent,y+6,W-M,y+6,decoration);y+=18;
        }else if(node instanceof TableBlock){
            table(node,indent);
        }else if(node instanceof HtmlBlock){
            block(((HtmlBlock)node).getLiteral(),indent,10,false,true,INK,8,false);
        }else if(node instanceof BulletList||node instanceof OrderedList){
            int i=node instanceof OrderedList?((OrderedList)node).getMarkerStartNumber():1;
            for(Node item=node.getFirstChild();item!=null;item=item.getNext()){
                String marker=node instanceof OrderedList?(i++)+". ":"• ";
                Node child=item.getFirstChild();
                if(child instanceof Paragraph){
                    SpannableStringBuilder line=new SpannableStringBuilder(marker);
                    line.append(inlines(child));
                    block(line,indent+12,11,false,false,INK,5,false);
                    child=child.getNext();
                }else block(marker,indent+12,11,false,false,INK,2,false);
                for(;child!=null;child=child.getNext())walk(child,indent+20,depth+1);
            }
            y+=5;
        }else{
            for(Node child=node.getFirstChild();child!=null;child=child.getNext())
                walk(child,indent+(node instanceof BlockQuote?16:0),depth+1);
        }
    }
    static void write(OutputStream out,AiPreferences.SavedResult result)throws Exception{
        ResearchPdfLayout r=new ResearchPdfLayout();
        try{
            r.block("BIOQRA — Unverified AI research draft",0,20,true,false,ACCENT,12,false);
            r.block(result.title,0,16,true,false,INK,12,false);
            r.block("Date: "+result.date+"\nAI model: "+result.model+"\nPublic source: "+result.source,
                    0,10,false,false,INK,18,false);
            r.walk(parse(result.answer),0,0);
            r.block("BIOQRA does not establish clinical efficacy or independently verify the AI proposal.",
                    0,10,true,false,ACCENT,8,false);
            if(r.page!=null){r.pdf.finishPage(r.page);r.page=null;}
            r.pdf.writeTo(out);
        }finally{
            if(r.page!=null){r.pdf.finishPage(r.page);r.page=null;}
            r.pdf.close();
        }
    }
}
