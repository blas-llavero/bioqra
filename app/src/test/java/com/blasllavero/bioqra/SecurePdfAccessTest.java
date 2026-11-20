package com.blasllavero.bioqra;

import org.junit.Test;
import static org.junit.Assert.*;

public class SecurePdfAccessTest {
    @Test public void acceptsOnlyExactPdfFilename(){
        assertTrue(SecurePdfAccess.safeName("Research.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Other.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("../Research.pdf","Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Research.pdf","../Research.pdf"));
        assertFalse(SecurePdfAccess.safeName("Research.txt","Research.txt"));
        assertFalse(SecurePdfAccess.safeName(null,"Research.pdf"));
    }
}
