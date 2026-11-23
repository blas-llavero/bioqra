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
        add("Automatically translate public challenge sections (uses ChatGPT requests)","Tradueix automàticament les seccions públiques dels reptes (utilitza consultes de ChatGPT)","Traducir automáticamente las secciones públicas de los retos (utiliza consultas de ChatGPT)");
        add("Authorize automatic public translations?","Vols autoritzar les traduccions automàtiques de continguts públics?","¿Quieres autorizar las traducciones automáticas de contenidos públicos?");
        add("BIOQRA will send approved PUBLIC source sections to your connected ChatGPT account when viewed. Each uncached section can use one of your daily AI requests. Source originals stay unchanged. No patient data is transmitted.","BIOQRA enviarà les seccions PÚBLIQUES aprovades al teu compte de ChatGPT quan les visualitzis. Cada secció sense traducció desada pot consumir una consulta diària d'IA. Els originals no es modificaran i no s'enviaran dades de pacients.","BIOQRA enviará las secciones PÚBLICAS autorizadas a tu cuenta de ChatGPT cuando las visualices. Cada sección sin traducción guardada puede consumir una consulta diaria de IA. Los originales no se modificarán y no se enviarán datos de pacientes.");
        add("Enable","Activa","Activar");
        add("ChatGPT account","Compte de ChatGPT","Cuenta de ChatGPT");
        add("ChatGPT login","Inicia sessió a ChatGPT","Iniciar sesión en ChatGPT");
        add("Default model (new challenges / inherited selections)","Model predeterminat (reptes nous i seleccions heretades)","Modelo predeterminado (retos nuevos y selecciones heredadas)");
        add("Local AI attempts remaining today: %d","Consultes d'IA disponibles avui: %d","Consultas de IA disponibles hoy: %d");
        add("Exporting %d research PDFs...","S'estan exportant %d PDF de recerca...","Exportando %d PDF de investigación...");
        add("Exported %d/%d PDFs.","S'han exportat %d/%d PDF.","Se han exportado %d/%d PDF.");
        add("%d models offered by the connected account. Auto prefers the highest-tier available model by name and reasoning mode; not an official capability ranking.","El compte connectat ofereix %d models. Auto prefereix el model de nivell més alt disponible segons el nom i el mode de raonament; no és una classificació oficial.","La cuenta conectada ofrece %d modelos. Auto prefiere el modelo de nivel más alto disponible según el nombre y el modo de razonamiento; no es una clasificación oficial.");
        add("Refreshing available ChatGPT models...","S'estan actualitzant els models de ChatGPT disponibles...","Actualizando los modelos de ChatGPT disponibles...");
        add("Model list unavailable: %s","Llista de models no disponible: %s","Lista de modelos no disponible: %s");
        add("Custom PDF folder: %s/Bioqra/YYMMDD-Project/","Carpeta PDF personalitzada: %s/Bioqra/YYMMDD-Project/","Carpeta PDF personalizada: %s/Bioqra/YYMMDD-Project/");
        add("Default PDF folder: Downloads/Bioqra/YYMMDD-Project/ (no setup needed).","Carpeta PDF predeterminada: Downloads/Bioqra/YYMMDD-Project/ (sense configuració).","Carpeta PDF predeterminada: Downloads/Bioqra/YYMMDD-Project/ (sin configuración).");
        add("No saved AI results to export.","No hi ha resultats d'IA desats per exportar.","No hay resultados de IA guardados para exportar.");
        add("Auto · highest-tier model available","Auto · model de nivell més alt disponible","Auto · modelo de nivel más alto disponible");
        add("Unavailable saved global model: %s","Model global desat no disponible: %s","Modelo global guardado no disponible: %s");
        add("AI plan usage authorized","Ús del pla d'IA autoritzat","Uso del plan de IA autorizado");
        add("AI plan usage not authorized","Ús del pla d'IA no autoritzat","Uso del plan de IA no autorizado");
        add("Not connected","No connectat","No conectado");
        add("Connecting...","Connectant...","Conectando...");
        add("ChatGPT ✓ · Account & sign out","ChatGPT ✓ · Compte i tanca la sessió","ChatGPT ✓ · Cuenta y cerrar sesión");
        add("Log in once to connect this device.","Inicia sessió una vegada per connectar aquest dispositiu.","Inicia sesión una vez para conectar este dispositivo.");
        add("Session saved securely on this device. Automatic token renewal is used where supported.","Sessió desada de manera segura al dispositiu. Renovació automàtica de credencials quan estigui disponible.","Sesión guardada de forma segura en el dispositivo. Renovación automática de credenciales cuando esté disponible.");
        add("Connected: %s","Connectat: %s","Conectado: %s");
        add("Plan use: %s","Ús del pla: %s","Uso del plan: %s");
        add("authorized","autoritzat","autorizado");
        add("not authorized","no autoritzat","no autorizado");
        add("You can reconnect or sign out locally.","Pots tornar a connectar-te o tancar la sessió localment.","Puedes volver a conectarte o cerrar la sesión localmente.");
        add("Reconnect","Torna a connectar","Volver a conectar");
        add("Sign out","Tanca la sessió","Cerrar sesión");
        add("Sign out of BIOQRA?","Vols tancar la sessió de BIOQRA?","¿Quieres cerrar la sesión de BIOQRA?");
        add("Connect a ChatGPT account to load available models.","Connecta un compte de ChatGPT per carregar els models disponibles.","Conecta una cuenta de ChatGPT para cargar los modelos disponibles.");
        add("Original saved result (language may differ from your selected language).","Resultat original desat (pot estar en un idioma diferent del seleccionat).","Resultado original guardado (puede estar en un idioma diferente del seleccionado).");
        add("Automatic processing: %s","Processament automàtic: %s","Procesamiento automático: %s");
        add("Automatic processing paused: previous attempt has no saved result for %s","Processament automàtic en pausa: l'intent anterior no té cap resultat desat per a %s","Procesamiento automático en pausa: el intento anterior no tiene ningún resultado guardado para %s");
        add("Official Grand Challenge index refreshed: %d accepting/upcoming entries · Rosalind and Open Problems available","Índex oficial de Grand Challenge actualitzat: %d entrades obertes o properes · Rosalind i Open Problems disponibles","Índice oficial de Grand Challenge actualizado: %d entradas abiertas o próximas · Rosalind y Open Problems disponibles");
        add("Mode: %s","Mode: %s","Modo: %s");
        add("Automatic — public research starts when a challenge opens","Automàtic — la recerca pública comença en obrir un repte","Automático — la investigación pública comienza al abrir un reto");
        add("Manual — offline tools and no AI usage","Manual — eines fora de línia sense ús d'IA","Manual — herramientas sin conexión ni uso de IA");
        add("SAVED AI DRAFT (not reverified)","ESBORRANY D'IA DESAT (no verificat de nou)","BORRADOR DE IA GUARDADO (sin nueva verificación)");
        add("No research proposal or independent validation yet.","Encara no hi ha cap proposta de recerca ni validació independent.","Todavía no hay ninguna propuesta de investigación ni validación independiente.");
        add("No solution proposal or independent verification yet.","Encara no hi ha cap proposta de solució ni verificació independent.","Todavía no hay ninguna propuesta de solución ni verificación independiente.");
        add("PDFs save automatically to Downloads/Bioqra/YYMMDD-Project/.","Els PDF es desen automàticament a Downloads/Bioqra/YYMMDD-Project/.","Los PDF se guardan automáticamente en Downloads/Bioqra/YYMMDD-Project/.");
        add("PDFs save to Bioqra/YYMMDD-Project/ inside your selected folder.","Els PDF es desen a Bioqra/YYMMDD-Project/ dins la carpeta seleccionada.","Los PDF se guardan en Bioqra/YYMMDD-Project/ dentro de la carpeta seleccionada.");
        add("Official link identified. Retrieving the public section...","S'ha identificat l'enllaç oficial. S'està recuperant la secció pública...","Se ha identificado el enlace oficial. Recuperando la sección pública...");
        add("This section was not identified in the public official statement. BIOQRA will not invent missing challenge details. Use the original organizer page for more information.","Aquesta secció no s'ha identificat a l'enunciat oficial públic. BIOQRA no inventarà informació del repte. Consulta la pàgina de l'organitzador.","Esta sección no se ha identificado en el enunciado oficial público. BIOQRA no inventará información del reto. Consulta la página del organizador.");
        add("Find detailed challenge statement","Cerca l'enunciat detallat del repte","Buscar el enunciado detallado del reto");
        add("Scientific benchmark · Results require real evaluation data and metrics","Repte de referència científica · Calen dades i mètriques reals per validar els resultats","Reto de referencia científica · Son necesarios datos y métricas reales para validar los resultados");
        add("Source listing · Check that the retrieved webpage contains the actual challenge rules and dataset.","Llista de fonts · Comprova que la pàgina recuperada contingui les normes i les dades del repte.","Listado de fuentes · Comprueba que la página recuperada contenga las reglas y los datos del reto.");
        add("Not checked. Dates must be verified on the official organizer website.","No s'ha comprovat. Cal verificar les dates al web oficial de l'organitzador.","Sin comprobar. Debes verificar las fechas en la web oficial del organizador.");
        add("Ready. Only public HTML from an approved HTTPS source can be retrieved.","Preparat. Només es pot recuperar HTML públic de fonts HTTPS autoritzades.","Preparado. Solo puede recuperarse HTML público de fuentes HTTPS autorizadas.");
        add("Detailed statement URL: not identified yet","URL de l'enunciat detallat: encara no identificada","URL del enunciado detallado: aún no identificada");
        add("Mode: ","Mode: ","Modo: ");
        add("Manual (no AI)","Manual (sense IA)","Manual (sin IA)");
        add("Automatic (ChatGPT)","Automàtic (ChatGPT)","Automático (ChatGPT)");
        add("AI model: configured for this challenge on the main list; otherwise inherits the global AI Settings model. Automatic use is limited to ","Model d'IA: configurat per a aquest repte a la llista principal; en cas contrari hereta el model global. L'ús automàtic està limitat a ","Modelo de IA: configurado para este reto en la lista principal; en caso contrario hereda el modelo global. El uso automático está limitado a ");
        add("ChatGPT can prepare a research proposal and an unexecuted methodology, but it cannot establish that this cancer research challenge is solved. The organizer may require an eligible research team, experimental data and scientific validation.","ChatGPT pot preparar una proposta de recerca i una metodologia sense executar, però no pot demostrar que s'hagi resolt aquest repte de recerca oncològica. L'organitzador pot exigir un equip acreditat, dades experimentals i validació científica.","ChatGPT puede preparar una propuesta de investigación y una metodología sin ejecutar, pero no puede demostrar que se haya resuelto este reto de investigación oncológica. El organizador puede exigir un equipo acreditado, datos experimentales y validación científica.");
        add("No research dataset files downloaded yet.","Encara no s'han descarregat fitxers de dades de recerca.","Todavía no se han descargado archivos de datos de investigación.");
        add("Export research evidence and proposal (ZIP)","Exporta les evidències i la proposta de recerca (ZIP)","Exportar evidencias y propuesta de investigación (ZIP)");
        add("Search official public repositories for this research challenge. Search results are not verified data matches; inspect study provenance, sample size, licensing, metadata and whether individual-level access needs approval.","Cerca repositoris públics oficials per a aquest repte. Els resultats de cerca no són coincidències de dades verificades; revisa la procedència, la mida de la mostra, la llicència, les metadades i els permisos.","Busca repositorios públicos oficiales para este reto. Los resultados no son coincidencias de datos verificadas; revisa procedencia, tamaño de muestra, licencia, metadatos y permisos.");
        add("No research dataset files downloaded yet.","Encara no s'han descarregat fitxers de recerca.","Todavía no se han descargado archivos de investigación.");
        add("Local checks completed. No network/AI usage.","Comprovacions locals completades. Sense ús de xarxa ni IA.","Comprobaciones locales completadas. Sin uso de red ni IA.");
        add("Data is not independently validated.","Les dades no s'han validat de manera independent.","Los datos no se han validado de forma independiente.");
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
    /** Localizes parameterized status messages without modifying technical values. */
    public static String format(Context context,String english,Object... arguments){
        String template=tr(context,english);
        return String.format(Locale.forLanguageTag(language(context)),template,arguments);
    }

    public static String tr(Context context,String english){
        if(english==null)return "";
        String[] translation=LABELS.get(english);
        if(translation==null)return english;
        String code=language(context);
        return "ca".equals(code)?translation[0]:"es".equals(code)?translation[1]:english;
    }
}
