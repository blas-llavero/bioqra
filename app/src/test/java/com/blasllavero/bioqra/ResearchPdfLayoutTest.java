package com.blasllavero.bioqra;

import org.junit.Test;
import static org.junit.Assert.*;
import org.commonmark.node.*;
import org.commonmark.ext.gfm.tables.TableBlock;

public class ResearchPdfLayoutTest {
    @Test public void markdownStructureAndCodeArePreserved(){
        Node document=ResearchPdfLayout.parse("## Research\n\n**Evidence** and *limits*.\n\n"
            +"| Gene | Effect |\n| --- | --- |\n| TP53 | 0.7 |\n\n"
            +"~~~python\nx = a ** 2\n    # comment\n~~~\n");
        Node heading=document.getFirstChild();
        assertTrue(heading instanceof Heading);
        assertEquals(2,((Heading)heading).getLevel());
        Node paragraph=heading.getNext();
        assertTrue(paragraph.getFirstChild() instanceof StrongEmphasis);
        Node table=paragraph.getNext();
        assertTrue(table instanceof TableBlock);
        Node code=table.getNext();
        assertTrue(code instanceof FencedCodeBlock);
        assertEquals("x = a ** 2\n    # comment\n",((FencedCodeBlock)code).getLiteral());
    }
}
