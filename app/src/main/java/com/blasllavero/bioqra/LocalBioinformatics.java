package com.blasllavero.bioqra;

import java.util.Locale;

/** Small reproducible offline examples; not a general scientific validator. */
public final class LocalBioinformatics {
    private LocalBioinformatics() {}

    public static String inspect(String data) {
        if(data==null || data.trim().isEmpty())
            return "No sample sequence supplied. Paste PUBLIC sample data to run a local check.";
        String sequence = data.replaceAll("(?m)^>.*(?:\r?\n|$)","")
                .replaceAll("\\s+","").toUpperCase(Locale.ROOT);
        if(sequence.isEmpty() || sequence.length()>200000)
            return "Provide a DNA or RNA sequence of 1–200,000 bases.";
        if(!sequence.matches("[ACGTU]+"))
            return "Local sequence checker supports plain DNA/RNA bases A, C, G, T or U only. No sequence was processed.";
        if(sequence.indexOf('T')>=0 && sequence.indexOf('U')>=0)
            return "Mixed DNA/RNA alphabet; please supply one molecule type.";
        int a=0,c=0,g=0,t=0,u=0;
        for(int i=0;i<sequence.length();i++){
            switch(sequence.charAt(i)){
                case 'A':a++;break;
                case 'C':c++;break;
                case 'G':g++;break;
                case 'T':t++;break;
                case 'U':u++;break;
            }
        }
        StringBuilder result=new StringBuilder();
        result.append("OFFLINE SEQUENCE CHECK (not independent challenge validation)\n");
        result.append("Length: ").append(sequence.length())
                .append("\nA: ").append(a).append("  C: ").append(c)
                .append("  G: ").append(g).append("  T: ").append(t)
                .append("  U: ").append(u).append("\n");
        result.append(String.format(Locale.ROOT,"GC content: %.4f%%\n",
                100.0*(c+g)/sequence.length()));
        if(u==0){
            String complement=sequence.replace('A','x').replace('T','A').replace('x','T')
                    .replace('C','y').replace('G','C').replace('y','G');
            result.append("RNA transcript (T→U): ")
                    .append(sequence.length()<=250?sequence.replace('T','U'):"[length exceeds preview]")
                    .append("\nReverse complement: ")
                    .append(sequence.length()<=250?new StringBuilder(complement).reverse():"[length exceeds preview]");
        }
        result.append("\n\nThese computations check only supplied public sample data. They do not establish that a competition's problem has been solved.");
        return result.toString();
    }
}
