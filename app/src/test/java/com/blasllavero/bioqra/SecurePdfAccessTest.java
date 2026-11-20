package com.blasllavero.bioqra;

import org.junit.Test;
import static org.junit.Assert.*;

public class SecurePdfAccessTest {
    @Test public void unavailableOwnerIsNotMistakenForAnotherPackage(){
        assertTrue(SecurePdfAccess.ownerMatchesIfReported(null,"com.blasllavero.bioqra"));
        assertTrue(SecurePdfAccess.ownerMatchesIfReported(
                "com.blasllavero.bioqra","com.blasllavero.bioqra"));
        assertFalse(SecurePdfAccess.ownerMatchesIfReported(
                "com.unrelated.app","com.blasllavero.bioqra"));
    }

    @Test public void acceptsOnlyExactPdfFilename(){
        assertTrue(SecurePdfAccess.safeName("Research.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Other.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("../Research.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Research.pdf","../Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Research.txt","Research.txt"));
        assertFalse(SecurePdfAccess.safeName(null,"Research.pdf"));
    }
}
