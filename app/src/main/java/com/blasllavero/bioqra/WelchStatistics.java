package com.blasllavero.bioqra;

import org.apache.commons.math3.distribution.TDistribution;
import java.util.Arrays;

/**
 * Pure, deterministic numerical core used for exploratory two-group analysis.
 * Test fixtures are mathematical checks ONLY and are not research observations.
 */
public final class WelchStatistics {
    private WelchStatistics(){}

    public static final class TestResult {
        public final double effect,t,degreesOfFreedom,p;
        TestResult(double effect,double t,double df,double p){
            this.effect=effect;this.t=t;this.degreesOfFreedom=df;this.p=p;
        }
    }
    public static TestResult compare(double[] control,double[] caseValues){
        if(control==null||caseValues==null||control.length<3||caseValues.length<3)
            throw new IllegalArgumentException("At least three independent values per group required");
        double cm=mean(control),tm=mean(caseValues);
        double v0=variance(control,cm),v1=variance(caseValues,tm);
        double a=v0/control.length,b=v1/caseValues.length,den=a+b;
        if(!Double.isFinite(den)||den<=1e-15)
            throw new IllegalArgumentException("Zero or invalid standard error");
        double df=den*den/(a*a/(control.length-1.0)+b*b/(caseValues.length-1.0));
        if(!Double.isFinite(df)||df<1)
            throw new IllegalArgumentException("Invalid Welch degrees of freedom");
        double statistic=(tm-cm)/Math.sqrt(den);
        double p=2.0*(new TDistribution(df)).cumulativeProbability(-Math.abs(statistic));
        if(!Double.isFinite(p))throw new IllegalArgumentException("Invalid Welch p value");
        return new TestResult(tm-cm,statistic,df,Math.max(0,Math.min(1,p)));
    }
    private static double mean(double[] v){
        double s=0;
        for(double x:v){
            if(!Double.isFinite(x))throw new IllegalArgumentException("Non-finite value");
            s+=x;
        }
        return s/v.length;
    }
    private static double variance(double[] v,double mean){
        double ss=0;
        for(double x:v)ss+=(x-mean)*(x-mean);
        return ss/(v.length-1.0);
    }
    /** Return adjusted p-values IN THE ORIGINAL INPUT ORDER. */
    public static double[] benjaminiHochberg(double[] p){
        if(p==null)throw new IllegalArgumentException("Missing p values");
        int m=p.length;
        Integer[] order=new Integer[m];
        for(int i=0;i<m;i++){
            if(!Double.isFinite(p[i])||p[i]<0||p[i]>1)
                throw new IllegalArgumentException("P-value out of bounds");
            order[i]=i;
        }
        Arrays.sort(order,(a,b)->Double.compare(p[a],p[b]));
        double[] adjusted=new double[m];
        double next=1;
        for(int k=m-1;k>=0;k--){
            next=Math.min(next,p[order[k]]*m/(k+1.0));
            adjusted[order[k]]=Math.min(1,next);
        }
        return adjusted;
    }
}
