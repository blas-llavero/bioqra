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
        add("No real research data downloaded","No s’han descarregat dades científiques reals","No se han descargado datos científicos reales");
        add("Research data downloaded; scientific validation pending","Dades científiques descarregades; validació pendent","Datos científicos descargados; validación pendiente");

        add("Insufficient research data","Dades insuficients per completar el projecte","Datos insuficientes para completar el proyecto");
        add("PDF or ZIP export failed","No s’ha pogut generar correctament el PDF o ZIP","No se ha podido generar correctamente el PDF o ZIP");

        add("Pause processing","Pausa","Pausar");
        add("Stop processing","Atura","Detener");
        add("Resume processing","Reprèn","Reanudar");
        add("Processing paused","Processament en pausa","Procesamiento en pausa");
        add("Processing stopped","Processament aturat","Procesamiento detenido");
        add("Processing resumed","Processament reprès","Procesamiento reanudado");

        add("Processing pending","Processament pendent","Procesamiento pendiente");
        add("Challenge statement retrieved / in preparation","Preparació de l’enunciat del repte","Preparación del enunciado del reto");
        add("Dataset search and analysis","Cerca i anàlisi de dades","Búsqueda y análisis de datos");
        add("AI proposal in preparation","Generació de la proposta amb IA","Generación de la propuesta con IA");
        add("PDF and ZIP export pending","Preparació del PDF i ZIP","Preparación del PDF y ZIP");
        add("Processing completed","Processament completat","Procesamiento completado");

        add("Project files","Fitxers del projecte","Archivos del proyecto");
        add("No readable research ZIP found","No s’ha trobat cap ZIP de recerca llegible","No se ha encontrado ningún ZIP de investigación legible");
        add("Open research ZIP","Obre el ZIP de recerca","Abrir el ZIP de investigación");
        add("Open this ZIP with a file manager","Obre aquest ZIP amb un gestor de fitxers","Abre este ZIP con un gestor de archivos");
        add("Preparing PDF...","S’està preparant el PDF...","Preparando el PDF...");
        add("Apply this model to ALL challenges","Aplica aquest model a TOTS els reptes","Aplica este modelo a TODOS los retos");
        add("Use global model","Utilitza el model global","Utiliza el modelo global");
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
        add("⚙ AI Settings · models & automation","⚙ Configuració d'IA · models i automatització","⚙ Configuración de IA · modelos y automatización");
        add("Search challenges...","Cerca reptes...","Buscar retos...");
        add("Manual Mode","Mode manual","Modo manual");
        add("Automatic Mode","Mode automàtic","Modo automático");
        add("Cancer Research","Recerca sobre el càncer","Investigación sobre el cáncer");
        add("Refresh","Actualitza","Actualizar");
        add("Hackathons","Hackatons","Hackatones");
        add("App Description","Descripció de l'aplicació","Descripción de la aplicación");
        add("How to Use","Com s'utilitza","Cómo se utiliza");
        add("Home","Inici","Inicio");
        add("Help","Ajuda","Ayuda");
        add("All sources","Totes les fonts","Todas las fuentes");
        add("Open / Upcoming","Oberts / Properament","Abiertos / Próximamente");
        add("Research benchmarks","Reptes de referència científics","Retos de referencia científicos");
        add("Educational exercises","Exercicis educatius","Ejercicios educativos");
        add("Needs verification","Pendent de verificació","Pendiente de verificación");
        add("All records","Tots els registres","Todos los registros");
        add("No matching challenges. Change the filters or refresh.","No hi ha reptes coincidents. Canvia els filtres o actualitza.","No hay retos coincidentes. Cambia los filtros o actualiza.");
        add("← Back to BIOQRA","← Torna a BIOQRA","← Volver a BIOQRA");
        add("Language","Idioma","Idioma");
        add("Device language (automatic)","Idioma del dispositiu (automàtic)","Idioma del dispositivo (automático)");
        add("Language preference saved","Preferència d'idioma desada","Preferencia de idioma guardada");
        add("AI model:","Model d'IA:","Modelo de IA:");
        add("Loading verified public challenges...","S'estan carregant els reptes públics verificats...","Cargando retos públicos verificados...");
        add("Open Bioinformatics Research & Patient Assistance","Recerca bioinformàtica oberta i assistència al pacient","Investigación bioinformática abierta y asistencia al paciente");
        add("Challenge title","Títol del repte","Título del reto");
        add("Challenge number","Número de repte","Número de reto");
        add("Official source (original language)","Font oficial (idioma original)","Fuente oficial (idioma original)");
        add("AI-generated translation — unverified","Traducció generada per IA — no verificada","Traducción generada por IA — no verificada");
        add("English","Anglès","Inglés");
        add("Catalan","Català","Catalán");
        add("Spanish","Castellà","Español");
        add("Counting DNA Nucleotides","Comptar nucleòtids d'ADN","Contar nucleótidos de ADN");
        add("Transcribing DNA into RNA","Transcriure ADN a ARN","Transcribir ADN a ARN");
        add("Complementing a Strand of DNA","Complementar una cadena d'ADN","Complementar una cadena de ADN");
        add("Computing GC Content","Calcular el contingut GC","Calcular el contenido GC");
        add("Counting Point Mutations","Comptar mutacions puntuals","Contar mutaciones puntuales");
        add("Finding a Motif in DNA","Trobar un motiu en l'ADN","Encontrar un motivo en el ADN");
        add("Batch Integration","Integració de lots","Integración de lotes");
        add("Cell-Cell Communication","Comunicació entre cèl·lules","Comunicación entre células");
        add("Denoising","Eliminació de soroll","Eliminación de ruido");
        add("Foundation Models","Models fundacionals","Modelos fundacionales");
        add("Showing","Mostrant","Mostrando");
        add("of","de","de");
        add("Page","Pàgina","Página");
        add("Translate official section with ChatGPT","Tradueix la secció oficial amb ChatGPT","Traducir la sección oficial con ChatGPT");
        add("Official source (original language). Translation is optional and requires permission.","Font oficial en l'idioma original. La traducció és opcional i necessita autorització.","Fuente oficial en el idioma original. La traducción es opcional y requiere autorización.");
        add("No official text is available for translation.","No hi ha text oficial disponible per traduir.","No hay texto oficial disponible para traducir.");
        add("This source is not approved for translation.","Aquesta font no està autoritzada per traduir-la.","Esta fuente no está autorizada para traducirla.");
        add("This section is too long for the current translation preview.","Aquesta secció és massa llarga per a la previsualització de traducció actual.","Esta sección es demasiado larga para la vista previa de traducción actual.");
        add("Connect ChatGPT and check your daily request allowance.","Connecta ChatGPT i comprova el límit diari de consultes.","Conecta ChatGPT y comprueba el límite diario de consultas.");
        add("Send the PUBLIC official section to ChatGPT?","Vols enviar aquesta secció oficial PÚBLICA a ChatGPT?","¿Quieres enviar esta sección oficial PÚBLICA a ChatGPT?");
        add("The original source stays unchanged.","La font original no es modifica.","La fuente original permanece sin cambios.");
        add("Cancel","Cancel·la","Cancelar");
        add("Translate once","Tradueix una vegada","Traducir una vez");
        add("Translating official public text...","S'està traduint el text públic oficial...","Traduciendo el texto público oficial...");
        add("Translation ready; verify against official source.","Traducció preparada; verifica-la amb la font oficial.","Traducción preparada; verifícala con la fuente oficial.");
        add("Translation unavailable: ","Traducció no disponible: ","Traducción no disponible: ");
        add("Preparing PDF and ZIP...","Preparant PDF i ZIP...","Preparando PDF y ZIP...");
        add("Processing challenge...","Processant el repte...","Procesando el reto...");
        add("Previous match","Coincidència anterior","Coincidencia anterior");
        add("Next match","Coincidència següent","Coincidencia siguiente");
        add("Search within the current challenge document","Cerca dins del document del repte actual","Buscar en el documento del reto actual");
        add("First retrieve the official challenge statement.","Primer recupera l'enunciat oficial del repte.","Primero recupera el enunciado oficial del reto.");
        add("This linked section is not accessible.","No es pot accedir a aquesta secció enllaçada.","No se puede acceder a esta sección enlazada.");
        add("Selected public page: ","Pàgina pública seleccionada: ","Página pública seleccionada: ");
        add("Official source: ","Font oficial: ","Fuente oficial: ");
        add("Source: ","Font: ","Fuente: ");
        add("Official source to check: ","Font oficial per comprovar: ","Fuente oficial para comprobar: ");
        add("Search open data for this challenge","Cerca dades obertes per a aquest repte","Buscar datos abiertos para este reto");
        add("No live dataset search has been performed.","Encara no s'ha fet cap cerca de dades en directe.","Todavía no se ha hecho ninguna búsqueda de datos en directo.");
    }
    public static final String SYSTEM="system";
    private static final String SETTINGS="bioqra_language";
    private static final String KEY="preferred_language";

    public static String preference(Context context){
        return context.getApplicationContext()
            .getSharedPreferences(SETTINGS,Context.MODE_PRIVATE).getString(KEY,SYSTEM);
    }
    public static void setPreference(Context context,String code){
        if(!SYSTEM.equals(code) && !"ca".equals(code)
                && !"es".equals(code) && !"en".equals(code))
            throw new IllegalArgumentException("Unsupported interface language");
        context.getApplicationContext().getSharedPreferences(SETTINGS,Context.MODE_PRIVATE)
            .edit().putString(KEY,code).apply();
    }
    public static String deviceLanguage(Context context){
        Configuration config=context.getResources().getConfiguration();
        Locale locale=config.getLocales().isEmpty()?Locale.ENGLISH:config.getLocales().get(0);
        String code=locale.getLanguage();
        return "ca".equals(code)||"es".equals(code)?code:"en";
    }
    public static String language(Context context){
        String preferred=preference(context);
        return SYSTEM.equals(preferred)?deviceLanguage(context):preferred;
    }
    public static String languageName(Context context){
        switch(language(context)){
            case "ca":return "Català";
            case "es":return "Español";
            default:return "English";
        }
    }
    /** Stable catalog identifiers and scientific accession codes are never translated. */
    public static String challengeTitle(Context context,String sourceTitle){
        return tr(context,sourceTitle);
    }
    public static String translateNote(Context context,String original){
        return tr(context,original);
    }
    public static String tr(Context context,String english){
        if(english==null)return "";
        String[] translation=LABELS.get(english);
        if(translation==null)return english;
        String code=language(context);
        return "ca".equals(code)?translation[0]:"es".equals(code)?translation[1]:english;
    }
}
