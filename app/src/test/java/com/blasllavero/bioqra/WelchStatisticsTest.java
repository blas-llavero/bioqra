package com.blasllavero.bioqra;

import org.junit.Test;
import static org.junit.Assert.*;

/** Numerical regression tests. Synthetic input is TEST-ONLY, not research evidence. */
public class WelchStatisticsTest {
    private static final double EPS=1e-9;

    @Test public void welchMatchesIndependentReference() {
        // Reference computed independently with scipy.stats.ttest_ind(equal_var=False)
        // scipy result: t=2.7665038057242235, p=0.02829322830507379
        WelchStatistics.TestResult x=WelchStatistics.compare(
            new double[]{1,2,4,5},new double[]{4,5,7,8,9});
        assertEquals(3.6,x.effect,EPS);
        assertEquals(2.7665038057242235,x.t,EPS);
        assertEquals(0.02829322830507379,x.p,EPS);
    }

    @Test public void bhCorrectionPreservesOriginalOrderAndMonotonicity() {
        // Reference BH (manual sorted order 0.01,0.02,0.03,0.20 => 0.04,0.04,0.04,0.20)
        double[] q=WelchStatistics.benjaminiHochberg(new double[]{0.20,0.01,0.03,0.02});
        assertArrayEquals(new double[]{0.20,0.04,0.04,0.04},q,EPS);
    }

    @Test public void invalidDataRejected() {
        try{
            WelchStatistics.compare(new double[]{1,2},new double[]{1,2,3});
            fail("Expected insufficient group size");
        }catch(IllegalArgumentException expected){}
        try{
            WelchStatistics.compare(new double[]{1,2,3},new double[]{1,Double.NaN,3});
            fail("Expected nonfinite-value rejection");
        }catch(IllegalArgumentException expected){}
        try{
            WelchStatistics.benjaminiHochberg(new double[]{0.2,1.2});
            fail("Expected invalid p rejection");
        }catch(IllegalArgumentException expected){}
    }
}
