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
        add("Language","Idioma","Idioma");
        add("Device language (default)","Idioma del dispositiu (predeterminat)","Idioma del dispositivo (predeterminado)");
        add("Catalan","Català","Catalán");
        add("Spanish","Castellà","Español");
        add("English","Anglès","Inglés");
        add("Official statement saved locally","Enunciat oficial desat localment","Enunciado oficial guardado localmente");
        add("Official statement retrieved","Enunciat oficial recuperat","Enunciado oficial recuperado");
        add("Official challenge source unavailable","La font oficial del repte no està disponible","La fuente oficial del reto no está disponible");
        add("Loading official challenge statement...","S'està carregant l'enunciat oficial del repte...","Cargando el enunciado oficial del reto...");
        add("Official challenge statement unavailable","No s'ha pogut recuperar l'enunciat oficial del repte","No se ha podido recuperar el enunciado oficial del reto");
        add("Official source","Font oficial","Fuente oficial");
        add("Automatic Mode","Mode automàtic","Modo automático");
        add("Manual Mode","Mode manual","Modo manual");
        add("Processing challenge...","S'està processant el repte...","Procesando el reto...");
        add("Preparing PDF and ZIP...","S'estan preparant el PDF i el ZIP...","Preparando el PDF y el ZIP...");
        add("← Back to BIOQRA","← Torna a BIOQRA","← Volver a BIOQRA");
        add("ChatGPT account","Compte de ChatGPT","Cuenta de ChatGPT");
        add("ChatGPT login","Inicia sessió a ChatGPT","Iniciar sesión en ChatGPT");
        add("Default model (new challenges / inherited selections)","Model predeterminat (reptes nous i heretats)","Modelo predeterminado (retos nuevos y heredados)");
        add("Loading the model list for your ChatGPT account...","S'està carregant la llista de models del compte de ChatGPT...","Cargando los modelos de tu cuenta de ChatGPT...");
        add("Refresh models from ChatGPT","Actualitza els models de ChatGPT","Actualizar modelos de ChatGPT");
        add("Backup, Restore & PDF","Còpia de seguretat, restauració i PDF","Copia de seguridad, restauración y PDF");
        add("No matching challenges. Change the filters or refresh.","No hi ha reptes coincidents. Canvia els filtres o actualitza.","No hay retos coincidentes. Cambia los filtros o actualiza.");
        add("Loading verified public challenges...","Carregant els reptes públics verificats...","Cargando los retos públicos verificados...");
        add("Open Bioinformatics Research & Patient Assistance","Recerca oberta en bioinformàtica i suport al pacient","Investigación abierta en bioinformática y apoyo al paciente");
        add("Cancer Research","Recerca sobre el càncer","Investigación sobre el cáncer");
        add("Refresh","Actualitza","Actualizar");
        add("Hackathons","Hackatons","Hackatones");
        add("App Description","Descripció de l'aplicació","Descripción de la aplicación");
        add("How to Use","Com s'utilitza","Cómo se utiliza");
        add("Home","Inici","Inicio");
        add("Help","Ajuda","Ayuda");
        add("AI model:","Model d'IA:","Modelo de IA:");
        add("Official challenge statement unavailable","No es pot recuperar l'enunciat oficial del repte","No se puede recuperar el enunciado oficial del reto");
        add("Processing error","Error de processament","Error de procesamiento");
        add("AI attempt not completed; review required","Intent d'IA incomplet; cal revisar-lo","Intento de IA incompleto; requiere revisión");
        add("Open challenge","Obre el repte","Abrir el reto");
        add("Connecting...","S'està connectant...","Conectando...");
        add("Cancel","Cancel·la","Cancelar");
        add("Reconnect","Torna a connectar","Reconectar");
        add("Sign out","Tanca la sessió","Cerrar sesión");
        add("Processing completed:","Processament completat:","Procesamiento completado:");
        add("AI plan usage authorized","Ús d'IA autoritzat","Uso de IA autorizado");
        add("AI plan usage not authorized","Ús d'IA no autoritzat","Uso de IA no autorizado");
        add("Account & sign out","Compte i tancament de sessió","Cuenta y cierre de sesión");
        add("Choose ZIP","Tria el ZIP","Elegir ZIP");
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
        add("No live dataset search has been performed.","Encara no s'ha fet cap cerca de dades en directe.","Todavía no se ha hecho ninguna búsqueda de datos en directo.");
    }
    public static String language(Context context){
        String preference=context.getApplicationContext().getSharedPreferences(
                "bioqra_language",Context.MODE_PRIVATE).getString("selection","device");
        if("ca".equals(preference)||"es".equals(preference)||"en".equals(preference))
            return preference;
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
