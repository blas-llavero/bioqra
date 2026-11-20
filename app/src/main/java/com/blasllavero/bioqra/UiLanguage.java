package com.blasllavero.bioqra;

import android.content.Context;
import android.content.res.Configuration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Uses the tablet's active Android locale. English is the fallback locale. */
public final class UiLanguage {
    private UiLanguage(){}
    private static final Map<String,String[]> LABELS=new HashMap<>();
    private static void add(String en,String ca,String es){LABELS.put(en,new String[]{ca,es});}
    static{
        add("← Back to challenges","← Torna als reptes","← Volver a los retos");
        add("1 · Find detailed challenge statement","1 · Cerca l'enunciat detallat","1 · Buscar el enunciado detallado");
        add("View original challenge in browser","Mostra el repte original al navegador","Ver el reto original en el navegador");
        add("Navigate official challenge sections","Navega per les seccions oficials","Navegar por las secciones oficiales");
        add("Full text","Text complet","Texto completo");
        add("🔍 Cerca","🔍 Cerca","🔍 Buscar");
        add("← Enrere","← Enrere","← Atrás");
        add("← Back","← Enrere","← Atrás");
        add("Forward →","Endavant →","Adelante →");
        add("🔍 Search","🔍 Cerca","🔍 Buscar");
        add("Endavant →","Endavant →","Adelante →");
        add("Public statement (read-only preview)","Enunciat públic (només lectura)","Enunciado público (solo lectura)");
        add("Participation dates and supporting documents","Dates i documents de participació","Fechas y documentos de participación");
        add("Open biomedical datasets","Conjunts de dades biomèdiques obertes","Conjuntos de datos biomédicos abiertos");
        add("🔎 Search open data for this challenge","🔎 Cerca dades obertes per a aquest repte","🔎 Buscar datos abiertos para este reto");
        add("Public sample DNA/RNA data for local checks (optional)","Mostres públiques d'ADN/ARN per a proves locals (opcional)","Muestras públicas de ADN/ARN para pruebas locales (opcional)");
        add("2 · Run offline sequence checks","2 · Analitza seqüències sense connexió","2 · Analizar secuencias sin conexión");
        add("3 · Ask ChatGPT for a research proposal","3 · Demana una proposta de recerca a ChatGPT","3 · Solicitar una propuesta de investigación a ChatGPT");
        add("3 · Ask ChatGPT for a solution proposal","3 · Demana una proposta de solució a ChatGPT","3 · Solicitar una propuesta de solución a ChatGPT");
        add("Results and limitations","Resultats i limitacions","Resultados y limitaciones");
        add("Save this challenge result as PDF","Desa el resultat del repte en PDF","Guardar resultado del reto en PDF");
        add("View PDF","Mostra el PDF","Ver PDF");
        add("PDF preview","Visualització del PDF","Vista previa del PDF");
        add("Previous","Anterior","Anterior");
        add("Next","Següent","Siguiente");
        add("Close","Tanca","Cerrar");
        add("Page","Pàgina","Página");
        add("Loading PDF...","S’està carregant el PDF...","Cargando PDF...");
        add("Could not display PDF","No es pot mostrar el PDF","No se puede mostrar el PDF");
        add("Research funding opportunity · Not a programming contest","Finançament de recerca · No és un concurs de programació","Financiación de investigación · No es un concurso de programación");
        add("Challenge","Repte","Reto");
        add("Impact","Impacte","Impacto");
        add("Summary","Resum","Resumen");
        add("Documents Útils","Documents útils","Documentos útiles");
        add("Applications","Sol·licituds","Solicitudes");
        add("Deadline","Termini","Plazo");
        add("Related Content","Contingut relacionat","Contenido relacionado");
        add("Full statement","Enunciat complet","Enunciado completo");
        add("📄 Open original PDF with its layout","📄 Obre el PDF original amb el seu format","📄 Abrir el PDF original con su formato");
        add("🌐 Open original source in browser","🌐 Obre la font original al navegador","🌐 Abrir la fuente original en el navegador");
        add("Manual","Manual","Manual");
        add("Automatic","Automàtic","Automático");
        add("Challenges","Reptes","Retos");
        add("AI Settings","Configuració d'IA","Configuración de IA");
        add("Choose folder for automatic PDFs","Tria la carpeta dels PDF automàtics","Elegir carpeta de los PDF automáticos");
        add("Export all saved results as PDFs","Exporta tots els resultats en PDF","Exportar todos los resultados en PDF");
        add("Search text in the current section","Cerca text a la secció actual","Buscar texto en la sección actual");
        add("No statement retrieved yet.","Encara no s'ha recuperat cap enunciat.","Todavía no se ha recuperado ningún enunciado.");
        add("No live dataset search has been performed.","Encara no s'ha fet cap cerca de dades en directe.","Todavía no se ha hecho ninguna búsqueda de datos en directo.");
    }
    public static String language(Context context){
        Configuration config=context.getResources().getConfiguration();
        Locale locale=config.getLocales().get(0);
        String code=locale.getLanguage();
        return "ca".equals(code)||"es".equals(code)?code:"en";
    }
    public static String tr(Context context,String english){
        if(english==null)return "";
        String[] translation=LABELS.get(english);
        if(translation==null)return english;
        String code=language(context);
        return "ca".equals(code)?translation[0]:"es".equals(code)?translation[1]:english;
    }
}
