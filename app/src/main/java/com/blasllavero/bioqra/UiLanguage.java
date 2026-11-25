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
        add("Translate displayed text with ChatGPT","Tradueix amb ChatGPT el text que es mostra","Traducir con ChatGPT el texto mostrado");
        add("Translation uses your authorized ChatGPT account and daily request allowance. Public source excerpts and saved AI proposals may be translated and cached locally. Original text is always retained. If ChatGPT is unavailable, the source text remains visible.","La traducció utilitza el compte de ChatGPT autoritzat i les peticions diàries disponibles. Els enunciats públics i les propostes es poden traduir i desar localment. Es conserva sempre el text original. Si ChatGPT no està disponible, es mostra l'original.","La traducción utiliza la cuenta de ChatGPT autorizada y las solicitudes diarias disponibles. Los enunciados públicos y las propuestas pueden traducirse y guardarse localmente. Se conserva siempre el texto original. Si ChatGPT no está disponible, se muestra el original.");
        add("Show original official text","Mostra l'enunciat oficial original","Mostrar el enunciado oficial original");
        add("Show original AI proposal","Mostra la proposta d'IA original","Mostrar la propuesta de IA original");
        add("Show translated text","Mostra el text traduït","Mostrar el texto traducido");
        add("SAVED AI DRAFT (not reverified)","ESBORRANY D'IA DESAT (sense tornar a verificar)","BORRADOR DE IA GUARDADO (sin volver a verificar)");
        add("Manual (no AI)","Manual (sense IA)","Manual (sin IA)");
        add("Automatic (ChatGPT)","Automàtic (ChatGPT)","Automático (ChatGPT)");
        add(" · Read-only · No contest submissions"," · Només lectura · Sense enviar candidatures"," · Solo lectura · Sin enviar solicitudes");
        add("AI model: configured for this challenge on the main list; otherwise inherits the global AI Settings model. Automatic use is limited to ","Model d'IA: configurat per al repte a la llista principal; si no, s'utilitza el model global de Configuració d'IA. Límit local en automàtic: ","Modelo de IA: configurado para el reto en la lista principal; si no, se utiliza el modelo global de Configuración de IA. Límite local en automático: ");
        add(" requests per day on this device."," peticions diàries en aquest dispositiu."," solicitudes diarias en este dispositivo.");
        add("No research proposal or independent validation yet.","Encara no hi ha cap proposta de recerca ni validació independent.","Todavía no hay propuesta de investigación ni validación independiente.");
        add("No solution proposal or independent verification yet.","Encara no hi ha cap proposta de solució ni verificació independent.","Todavía no hay propuesta de solución ni verificación independiente.");
        add("AI DRAFT — UNVERIFIED","ESBORRANY D'IA — SENSE VERIFICAR","BORRADOR DE IA — SIN VERIFICAR");
        add("AI RESEARCH PROPOSAL — UNVERIFIED","PROPOSTA DE RECERCA AMB IA — SENSE VERIFICAR","PROPUESTA DE INVESTIGACIÓN CON IA — SIN VERIFICAR");
        add("BIOQRA has not run any generated program or validated a benchmark.","BIOQRA no ha executat cap programa generat ni validat cap repte de referència.","BIOQRA no ha ejecutado ningún programa generado ni validado ninguna prueba de referencia.");
        add("Automatic — challenge processing starts from the main list","Automàtic — el processament comença des del llistat principal","Automático — el procesamiento comienza desde la lista principal");
        add("Manual — offline tools and no AI usage","Manual — eines locals sense utilitzar la IA","Manual — herramientas locales sin utilizar la IA");
        add("Current competitions are prioritized. Open / Upcoming is a dated snapshot, not a guarantee of registration. Use All records to include educational and historical entries.","Es prioritzen les convocatòries actuals. «Obert / Properament» és una instantània, no una garantia d'inscripció. Selecciona «Tots els registres» per incloure exercicis i reptes històrics.","Se priorizan las convocatorias actuales. «Abierto / Próximamente» es una instantánea, no una garantía de inscripción. Selecciona «Todos los registros» para incluir ejercicios y retos históricos.");
        add("Priority: real cancer research opportunities. Projects already funded are hidden; use All records in Challenges to see historical projects. Confirm applications with each organizer.","Prioritat per a oportunitats reals de recerca sobre el càncer. Els projectes finançats no es mostren; utilitza «Tots els registres» per consultar-los. Confirma els terminis amb l'organitzador.","Prioridad para oportunidades reales de investigación sobre el cáncer. Los proyectos financiados no se muestran; utiliza «Todos los registros» para consultarlos. Confirma los plazos con el organizador.");
        add("Showing confirmed open/upcoming opportunities only. Historical hackathons and unverified directories are available under Challenges → All records.","Només es mostren oportunitats obertes o properes confirmades. Els esdeveniments històrics i els directoris no verificats apareixen a Reptes → Tots els registres.","Solo se muestran oportunidades abiertas o próximas confirmadas. Los eventos históricos y los directorios no verificados aparecen en Retos → Todos los registros.");
        add("BIOQRA is an experimental open-source research catalog, not a medical device. Challenge links open in your browser.\n\n","BIOQRA és un catàleg experimental de recerca de codi obert, no un dispositiu mèdic. Els enllaços als reptes s'obren al navegador.\n\n","BIOQRA es un catálogo experimental de investigación de código abierto, no un dispositivo médico. Los enlaces a los retos se abren en el navegador.\n\n");
        add("ChatGPT can prepare a research proposal and an unexecuted methodology, but it cannot establish that this cancer research challenge is solved. The organizer may require an eligible research team, experimental data and scientific validation.","ChatGPT pot preparar una proposta de recerca i una metodologia encara no executada, però això no demostra que el repte s'hagi resolt. L'organitzador pot exigir un equip elegible, dades experimentals i validació científica.","ChatGPT puede preparar una propuesta de investigación y una metodología todavía no ejecutada, pero eso no demuestra que el reto se haya resuelto. El organizador puede exigir un equipo elegible, datos experimentales y validación científica.");
        add("Only bounded exploratory QC is performed on downloaded real GEO matrices. No differential expression, clinical prediction, or challenge solution is established.","Només es fa un control de qualitat exploratori i acotat de matrius GEO reals. No s'ha demostrat expressió diferencial, predicció clínica ni resolució del repte.","Solo se realiza un control de calidad exploratorio y limitado de matrices GEO reales. No se ha demostrado expresión diferencial, predicción clínica ni resolución del reto.");
        add("The app checks Rosalind and Open Problems automatically. Competition availability is reviewed periodically and may change; always check registration with the official organizer.","L'aplicació comprova Rosalind i Open Problems automàticament. La disponibilitat dels concursos pot canviar; comprova la inscripció amb l'organitzador.","La aplicación comprueba Rosalind y Open Problems automáticamente. La disponibilidad de los concursos puede cambiar; comprueba la inscripción con el organizador.");
        add("Creating a single ZIP containing settings, saved AI results and PDFs.","Creant un ZIP amb la configuració, els resultats d'IA i els PDF.","Creando un ZIP con la configuración, los resultados de IA y los PDF.");
        add("ChatGPT ✓ · Account & sign out","ChatGPT ✓ · Compte i tancament de sessió","ChatGPT ✓ · Cuenta y cierre de sesión");
        add("Log in once to connect this device.","Inicia sessió una vegada per connectar aquest dispositiu.","Inicia sesión una vez para conectar este dispositivo.");
        add("Session saved securely on this device. Automatic token renewal is used where supported.","Sessió desada de manera segura al dispositiu. Renovació automàtica de credencials quan sigui compatible.","Sesión guardada de forma segura en el dispositivo. Renovación automática de credenciales cuando sea compatible.");
        add("Saved PDF unavailable","El PDF desat no està disponible","El PDF guardado no está disponible");
        add("Checking saved PDF...","Comprovant el PDF desat...","Comprobando el PDF guardado...");
        add("No saved AI result for this challenge; PDF not created.","No hi ha cap resultat d'IA desat per a aquest repte; no s'ha generat el PDF.","No hay ningún resultado de IA guardado para este reto; no se ha generado el PDF.");
        add("This link is not in BIOQRA's approved research-source allowlist.","Aquest enllaç no és a la llista de fonts de recerca permeses de BIOQRA.","Este enlace no está en la lista de fuentes de investigación permitidas de BIOQRA.");
        add("Blocked: URL is not a trusted HTTPS research domain. Open only after independent review.","Enllaç bloquejat: la URL no és d'una font HTTPS autoritzada. Obre-la només després de verificar-la.","Enlace bloqueado: la URL no es de una fuente HTTPS autorizada. Ábrela solo después de verificarla.");
        add("A verified public challenge statement is required. No AI request was sent.","Cal un enunciat públic verificat. No s'ha enviat cap petició a la IA.","Se necesita un enunciado público verificado. No se ha enviado ninguna solicitud a la IA.");
        add("Local daily ChatGPT request limit reached. Change it in AI Settings or try tomorrow.","S'ha assolit el límit diari local de ChatGPT. Modifica'l a Configuració d'IA o torna-ho a provar demà.","Se ha alcanzado el límite diario local de ChatGPT. Modifícalo en Configuración de IA o vuelve a probar mañana.");
        add("Automatic research started","S'ha iniciat la recerca automàtica","Investigación automática iniciada");
        add("Automatic queue finished","Cua automàtica finalitzada","Cola automática finalizada");
        add("Automatic research paused: ChatGPT authorization unavailable","Recerca automàtica en pausa: no hi ha autorització de ChatGPT","Investigación automática en pausa: no hay autorización de ChatGPT");
        add("Automatic research paused: daily request limit reached","Recerca automàtica en pausa: s'ha arribat al límit diari de peticions","Investigación automática en pausa: se ha alcanzado el límite diario de solicitudes");
        add("Research step failed for ","Ha fallat un pas de recerca per a ","Ha fallado un paso de investigación de ");
        add("Retrieving official statement: ","Recuperant l'enunciat oficial: ","Recuperando el enunciado oficial: ");
        add("Discovering public datasets: ","Cercant conjunts de dades públics: ","Buscando conjuntos de datos públicos: ");
        add("Generating research proposal: ","Generant la proposta de recerca: ","Generando la propuesta de investigación: ");
        add("Preparing PDF and ZIP: ","Preparant el PDF i el ZIP: ","Preparando el PDF y el ZIP: ");
        add("Processing completed: ","Processament completat: ","Procesamiento completado: ");
        add("Processing: ","Processant: ","Procesando: ");
        add("Automatic research stopped","S'ha aturat la recerca automàtica","Se ha detenido la investigación automática");
        add("Automatic research paused","Recerca automàtica en pausa","Investigación automática en pausa");
        add("Mode: ","Mode: ","Modo: ");
        add("Search challenges...","Cerca reptes...","Buscar retos...");
        add("Filter bioinformatics challenges by source","Filtra els reptes de bioinformàtica per font","Filtrar retos de bioinformática por fuente");
        add("Filter by competition availability","Filtra per disponibilitat del repte","Filtrar por disponibilidad del reto");
        add("Showing ","Mostrant ","Mostrando ");
        add(" of "," de "," de ");
        add("PDF preview unavailable: ","No es pot mostrar el PDF: ","No se puede mostrar el PDF: ");
        add("Automatic retrieval unavailable: ","No s'ha pogut recuperar automàticament: ","No se pudo recuperar automáticamente: ");
        add("Official Grand Challenge index refreshed: ","Índex oficial de Grand Challenge actualitzat: ","Índice oficial de Grand Challenge actualizado: ");
        add("Grand Challenge live search unavailable; saved list kept. ","No es pot actualitzar Grand Challenge; es conserva la llista desada. ","No se puede actualizar Grand Challenge; se conserva la lista guardada. ");
        add("Refreshing Rosalind and Open Problems...","Actualitzant Rosalind i Open Problems...","Actualizando Rosalind y Open Problems...");
        add("Saving PDF in Bioqra/","Desant el PDF a Bioqra/","Guardando el PDF en Bioqra/");
        add("AI attempt unavailable: ","Intent d'IA no disponible: ","Intento de IA no disponible: ");
        add("Automatic queue paused: ","Cua automàtica en pausa: ","Cola automática en pausa: ");
        add("No saved AI result for this challenge; PDF not available yet.","No hi ha cap resultat d'IA desat per a aquest repte; el PDF encara no està disponible.","No hay ningún resultado de IA guardado para este reto; el PDF aún no está disponible.");
        add("Saved result unavailable","No hi ha cap resultat desat","No hay resultados guardados");
        add("AI analysis in progress. No scientific result validated.","Anàlisi d'IA en curs. No hi ha resultats científics validats.","Análisis de IA en curso. No hay resultados científicos validados.");
        add("PDFs save automatically to Downloads/Bioqra/YYMMDD-Project/.","Els PDF es desen automàticament a Downloads/Bioqra/YYMMDD-Project/.","Los PDF se guardan automáticamente en Downloads/Bioqra/YYMMDD-Project/.");
        add("PDFs save to Bioqra/YYMMDD-Project/ inside your selected folder.","Els PDF es desen a Bioqra/YYMMDD-Project/ dins de la carpeta seleccionada.","Los PDF se guardan en Bioqra/YYMMDD-Project/ dentro de la carpeta seleccionada.");
        add("⚙ AI Settings · models & automation","⚙ Configuració d'IA · models i automatització","⚙ Configuración de IA · modelos y automatización");
        add("Automatic Mode selected. Connect ChatGPT in Settings to start.","Mode automàtic seleccionat. Connecta ChatGPT a Configuració per començar.","Modo automático seleccionado. Conecta ChatGPT en Configuración para empezar.");
        add("No eligible challenges for the current filters.","No hi ha reptes compatibles amb els filtres actuals.","No hay retos compatibles con los filtros actuales.");
        add("One ChatGPT login authorizes automatic analysis of the public challenges you open. Research, eligible open-data downloads, proposal generation and PDF saving proceed without extra buttons. Private patient data and contest submissions are excluded.","Un únic inici de sessió amb ChatGPT permet la recerca automàtica dels reptes públics. La cerca de dades, la proposta i els PDF es generen sense botons addicionals. S'exclouen les dades privades de pacients i l'enviament de candidatures.","Un único inicio de sesión en ChatGPT permite investigar automáticamente los retos públicos. La búsqueda de datos, la propuesta y los PDF se generan sin botones adicionales. Se excluyen los datos privados de pacientes y el envío de solicitudes.");
        add("A local limit of up to 40 ChatGPT attempts per day applies by default; provider plan restrictions also apply.","El límit local predeterminat és de 40 intents diaris amb ChatGPT; també s'apliquen els límits del proveïdor.","El límite local predeterminado es de 40 intentos diarios con ChatGPT; también se aplican los límites del proveedor.");
        add("AI-generated plans are proposals, not validated experiments, clinical findings or cancer treatments.","Les propostes creades amb IA no són experiments validats, troballes clíniques ni tractaments contra el càncer.","Las propuestas generadas con IA no son experimentos validados, hallazgos clínicos ni tratamientos contra el cáncer.");
        add("Import all · restore ZIP backup","Importa-ho tot · restaura el ZIP de còpia","Importar todo · restaurar copia ZIP");
        add("Export all · settings + results + PDFs (.zip)","Exporta-ho tot · configuració, resultats i PDF (.zip)","Exportar todo · configuración, resultados y PDF (.zip)");
        add("Restore complete BIOQRA backup?","Vols restaurar la còpia completa de BIOQRA?","¿Restaurar la copia completa de BIOQRA?");
        add("Export settings (.json)","Exporta la configuració (.json)","Exportar configuración (.json)");
        add("Import settings (.json)","Importa la configuració (.json)","Importar configuración (.json)");
        add("Export saved AI results (.json)","Exporta els resultats d'IA desats (.json)","Exportar resultados de IA guardados (.json)");
        add("Import saved AI results (.json)","Importa els resultats d'IA desats (.json)","Importar resultados de IA guardados (.json)");
        add("Optional: choose another PDF destination","Opcional: tria una altra carpeta per als PDF","Opcional: elegir otra carpeta para los PDF");
        add("Default PDF location: Downloads/Bioqra/YYMMDD-Project/Project.pdf. ","Ubicació predeterminada del PDF: Downloads/Bioqra/YYMMDD-Project/Project.pdf. ","Ubicación predeterminada del PDF: Downloads/Bioqra/YYMMDD-Project/Project.pdf. ");
        add("Backups and PDFs stay on your device until you share them.","Les còpies de seguretat i els PDF es conserven al dispositiu fins que els comparteixis.","Las copias de seguridad y los PDF permanecen en el dispositivo hasta que los compartas.");
        add("No saved AI results to export.","No hi ha resultats d'IA desats per exportar.","No hay resultados de IA guardados para exportar.");
        add("PDF destination configured. New AI drafts will be saved automatically.","Carpeta PDF configurada. Les noves propostes es desaran automàticament.","Carpeta PDF configurada. Las nuevas propuestas se guardarán automáticamente.");
        add("ChatGPT connected. Automatic mode starts research from the main challenge list.","ChatGPT connectat. El mode automàtic inicia la recerca des del llistat principal.","ChatGPT conectado. El modo automático inicia la investigación desde la lista principal.");
        add("Ready. Only public HTML from an approved HTTPS source can be retrieved.","Preparat. Només es pot recuperar HTML públic de fonts HTTPS autoritzades.","Preparado. Solo se puede recuperar HTML público de fuentes HTTPS autorizadas.");
        add("Scientific benchmark · Results require real evaluation data and metrics","Repte científic de referència · Calen dades i mètriques reals de validació","Prueba científica de referencia · Se requieren datos y métricas reales de validación");
        add("Source listing · Check that the retrieved webpage contains the actual challenge rules and dataset.","Llistat de fonts · Comprova que la pàgina contingui les normes i dades reals del repte.","Listado de fuentes · Comprueba que la página contenga las normas y datos reales del reto.");
        add("Detailed statement URL: not identified yet","URL de l'enunciat detallat: encara no identificat","URL del enunciado detallado: aún no identificado");
        add("📄 Open original document (PDF/web)","📄 Obre el document original (PDF/web)","📄 Abrir documento original (PDF/web)");
        add("Not checked. Dates must be verified on the official organizer website.","Sense verificar. Cal comprovar les dates al web oficial de l'organitzador.","Sin verificar. Comprueba las fechas en la web oficial del organizador.");
        add("Search official public repositories for this research challenge. Search results are not verified data matches; inspect study provenance, sample size, licensing, metadata and whether individual-level access needs approval.","Cerca en repositoris públics oficials. Els resultats de cerca no són dades verificades: revisa'n la procedència, la mida de mostra, la llicència, les metadades i els permisos.","Busca en repositorios públicos oficiales. Los resultados de búsqueda no son datos verificados: revisa la procedencia, el tamaño de muestra, la licencia, los metadatos y los permisos.");
        add("No research dataset files downloaded yet.","Encara no s'han descarregat fitxers de dades científiques.","Todavía no se han descargado archivos de datos científicos.");
        add("Export research evidence and proposal (ZIP)","Exporta les dades i la proposta de recerca (ZIP)","Exportar los datos y la propuesta de investigación (ZIP)");
        add("Local checks completed. No network/AI usage.","Comprovacions locals completades. Sense xarxa ni IA.","Comprobaciones locales completadas. Sin uso de red ni IA.");
        add("PDFs save by default to Downloads/Bioqra.","Els PDF es desen per defecte a Downloads/Bioqra.","Los PDF se guardan por defecto en Downloads/Bioqra.");
        add("First retrieve the official challenge statement.","Primer cal recuperar l'enunciat oficial del repte.","Primero hay que recuperar el enunciado oficial del reto.");
        add("This section was not identified in the public official statement. BIOQRA will not invent missing challenge details. Use the original organizer page for more information.","No s'ha identificat aquesta secció a l'enunciat oficial. BIOQRA no inventarà informació. Consulta la pàgina original.","No se ha identificado esta sección en el enunciado oficial. BIOQRA no inventará información. Consulta la página original.");
        add("Official link identified. Retrieving the public section...","S'ha identificat l'enllaç oficial. Recuperant la secció pública...","Enlace oficial identificado. Recuperando la sección pública...");
        add("This linked section is not accessible.","No es pot accedir a aquesta secció enllaçada.","No se puede acceder a esta sección enlazada.");
        add("Checking official organizer links for the detailed challenge statement...","Comprovant els enllaços oficials de l'organitzador per recuperar l'enunciat...","Comprobando los enlaces oficiales del organizador para recuperar el enunciado...");
        add("No data files downloaded.","No s'han descarregat fitxers de dades.","No se han descargado archivos de datos.");

        // Entire visible system messages are translated locally, without ChatGPT.
        add("Custom PDF folder: ","Carpeta de PDF personalitzada: ","Carpeta de PDF personalizada: ");
        add("Connect a ChatGPT account first","Connecta primer un compte de ChatGPT","Conecta primero una cuenta de ChatGPT");
        add(" · Session saved securely on this device. Automatic token renewal is used where supported."," · Sessió desada de manera segura al dispositiu. Renovació automàtica de credencials quan sigui compatible."," · Sesión guardada de forma segura en el dispositivo. Renovación automática de credenciales cuando sea compatible.");
        add("Not connected","No connectat","No conectado");
        add("Connected ChatGPT account","Compte de ChatGPT connectat","Cuenta de ChatGPT conectada");
        add("ChatGPT: ","ChatGPT: ","ChatGPT: ");
        add("Log in once to connect this device.","Inicia sessió una vegada per connectar aquest dispositiu.","Inicia sesión una vez para conectar este dispositivo.");
        add("Auto · highest-tier model available","Automàtic · model de gamma més alta disponible","Automático · modelo de gama más alta disponible");
        add("Model list unavailable: Connect a ChatGPT account first","Llista de models no disponible: connecta primer el compte de ChatGPT","Lista de modelos no disponible: conecta primero la cuenta de ChatGPT");
        add("Connect a ChatGPT account to load available models.","Connecta el compte de ChatGPT per carregar els models disponibles.","Conecta la cuenta de ChatGPT para cargar los modelos disponibles.");
        add("Refreshing available ChatGPT models...","Actualitzant els models de ChatGPT disponibles...","Actualizando los modelos de ChatGPT disponibles...");
        add("Model list unavailable: ","Llista de models no disponible: ","Lista de modelos no disponible: ");
        add("Local AI attempts remaining today: ","Intents d'IA disponibles avui: ","Intentos de IA disponibles hoy: ");
        add("Unavailable saved global model: ","Model global desat no disponible: ","Modelo global guardado no disponible: ");
        add(" models offered by the connected account. Auto prefers the highest-tier available model by name and reasoning mode; not an official capability ranking."," models disponibles al compte connectat. El mode automàtic prioritza els models de gamma alta pel nom i el mode de raonament; no és una classificació oficial de capacitats."," modelos disponibles en la cuenta conectada. El modo automático prioriza los modelos de gama alta por nombre y modo de razonamiento; no es una clasificación oficial de capacidades.");
        add("PDFs save automatically to Downloads/Bioqra/YYMMDD-Project/.","Els PDF es desen automàticament a Downloads/Bioqra/YYMMDD-Project/.","Los PDF se guardan automáticamente en Downloads/Bioqra/YYMMDD-Project/.");
        add("PDFs save to Bioqra/YYMMDD-Project/ inside your selected folder.","Els PDF es desen a Bioqra/YYMMDD-Project/ dins de la carpeta seleccionada.","Los PDF se guardan en Bioqra/YYMMDD-Project/ dentro de la carpeta seleccionada.");
        add("PDFs save by default to Downloads/Bioqra.","Per defecte, els PDF es desen a Downloads/Bioqra.","De forma predeterminada, los PDF se guardan en Downloads/Bioqra.");
        add("Default PDF folder: Downloads/Bioqra/YYMMDD-Project/ (no setup needed).","Carpeta predeterminada de PDF: Downloads/Bioqra/YYMMDD-Project/ (no cal configurar-la).","Carpeta predeterminada de PDF: Downloads/Bioqra/YYMMDD-Project/ (no requiere configuración).");
        add("Create a single ZIP containing settings, saved AI results and PDFs, or use the individual JSON buttons below. ChatGPT tokens/passwords and unattended consent are never exported.","Crea un únic ZIP amb la configuració, els resultats d'IA desats i els PDF, o utilitza els botons JSON individuals. Les credencials de ChatGPT i l'autorització per a l'ús automàtic mai no s'exporten.","Crea un único ZIP con la configuración, los resultados de IA guardados y los PDF, o utiliza los botones JSON individuales. Las credenciales de ChatGPT y la autorización para el uso automático nunca se exportan.");
        add("Default PDF location: Downloads/Bioqra/YYMMDD-Project/Project.pdf. You can optionally choose another parent folder. New dates create separate project folders; exporting twice on the same day updates that file. No folder configuration is needed for the default location.","Ubicació predeterminada dels PDF: Downloads/Bioqra/YYMMDD-Project/Project.pdf. Opcionalment pots triar una altra carpeta principal. Cada data nova crea una carpeta de projecte diferent; si exportes dues vegades el mateix dia, s'actualitza el fitxer. No cal configurar cap carpeta per utilitzar la ubicació predeterminada.","Ubicación predeterminada de los PDF: Downloads/Bioqra/YYMMDD-Project/Project.pdf. Opcionalmente puedes elegir otra carpeta principal. Cada fecha nueva crea una carpeta de proyecto distinta; al exportar dos veces el mismo día se actualiza el archivo. No hace falta configurar ninguna carpeta para usar la ubicación predeterminada.");
        add("Paste public sample DNA/RNA sequence (no patient records)","Enganxa una seqüència pública de mostra d'ADN/ARN (sense dades de pacients)","Pega una secuencia pública de muestra de ADN/ARN (sin datos de pacientes)");
        add("Translation requires a connected and authorized ChatGPT account. Go to AI Settings to sign in.","Per traduir l'enunciat, connecta i autoritza el compte de ChatGPT a Configuració d'IA.","Para traducir el enunciado, conecta y autoriza la cuenta de ChatGPT en Configuración de IA.");
        add("Translating official statement with ChatGPT...","Traduint l'enunciat oficial amb ChatGPT...","Traduciendo el enunciado oficial con ChatGPT...");
        add("Official statement could not be translated. The original remains visible. Check your ChatGPT connection and daily limit in AI Settings.","No s'ha pogut traduir l'enunciat. Es mostra l'original. Comprova la connexió de ChatGPT i el límit diari a Configuració d'IA.","No se ha podido traducir el enunciado. Se muestra el original. Comprueba la conexión de ChatGPT y el límite diario en Configuración de IA.");
        add("Translation paused: today's local ChatGPT request limit has been reached. The original remains visible; cached translations are preserved.","Traducció en pausa: s'ha arribat al límit local de peticions de ChatGPT d'avui. Es mostra l'original i es conserven els fragments ja traduïts.","Traducción en pausa: se ha alcanzado el límite local de solicitudes de ChatGPT de hoy. Se muestra el original y se conservan los fragmentos ya traducidos.");
        add("Official statement translation could not be completed. You can retry when ChatGPT is available; existing translated fragments remain saved.","No s'ha pogut completar la traducció de l'enunciat. Es podrà tornar a provar quan ChatGPT estigui disponible; es conserven els fragments traduïts.","No se pudo completar la traducción del enunciado. Se podrá volver a intentar cuando ChatGPT esté disponible; se conservan los fragmentos traducidos.");
        add("Translation is off in AI Settings; the original official statement is shown.","La traducció està desactivada a Configuració d'IA; es mostra l'enunciat oficial original.","La traducción está desactivada en Configuración de IA; se muestra el enunciado oficial original.");
        add("No dataset loaded. ","No s'ha carregat cap conjunt de dades. ","No se ha cargado ningún conjunto de datos. ");
        add("ChatGPT authorization required","Cal autoritzar ChatGPT","Se requiere autorización de ChatGPT");
        add("Return to the main screen and reconnect to authorize plan usage. No request was sent.","Torna a la pantalla principal i connecta el compte per autoritzar-ne l'ús. No s'ha enviat cap petició.","Vuelve a la pantalla principal y conecta la cuenta para autorizar su uso. No se ha enviado ninguna solicitud.");
        add("Checking your saved model selection...","Comprovant el model desat...","Comprobando el modelo guardado...");
        add("1 attempt","1 intent","1 intento");
        add("3 attempts","3 intents","3 intentos");
        add("5 attempts","5 intents","5 intentos");
        add("10 attempts","10 intents","10 intentos");
        add("20 attempts","20 intents","20 intentos");
        add("40 attempts (default)","40 intents (predeterminat)","40 intentos (predeterminado)");
        add("Open / Upcoming","Obert / Properament","Abierto / Próximamente");
        add("All sources","Totes les fonts","Todas las fuentes");
        add("All records","Tots els registres","Todos los registros");
        add("Research benchmarks","Reptes de referència","Pruebas de referencia");
        add("Needs verification","Pendent de verificació","Pendiente de verificación");
        add("Educational exercises","Exercicis educatius","Ejercicios educativos");
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
        add("Automatic Open Bioinformatics Research & Patient Assistance","Recerca oberta automàtica en bioinformàtica i suport al pacient","Investigación abierta automática en bioinformática y apoyo al paciente");
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
        add("The organizer redirects to another page. Open the official website manually; BIOQRA did not follow the redirect.","L'organitzador redirigeix cap a una altra pàgina. Obre manualment el web oficial; BIOQRA no ha seguit la redirecció.","El organizador redirige a otra página. Abre manualmente el sitio web oficial; BIOQRA no ha seguido la redirección.");
        add("Verified detailed challenge statement unavailable","L'enunciat detallat i verificat del repte no està disponible","El enunciado detallado y verificado del reto no está disponible");
        add("The public page is too large to inspect safely.","La pàgina pública és massa gran per examinar-la amb seguretat.","La página pública es demasiado grande para examinarla con seguridad.");
        add("This page is not public HTML; private files and PDFs are not retrieved automatically.","Aquesta pàgina no és HTML públic; els fitxers privats i els PDF no es recuperen automàticament.","Esta página no es HTML público; los archivos privados y PDF no se recuperan automáticamente.");
        add("No readable public problem statement. This challenge may need JavaScript, authentication or a separate dataset.","No s'ha trobat un enunciat públic llegible. El repte pot requerir JavaScript, autenticació o dades separades.","No se ha encontrado un enunciado público legible. El reto puede requerir JavaScript, autenticación o datos separados.");
        add("Automatic retrieval blocked: this source is not in the reviewed HTTPS allowlist.","Recuperació automàtica bloquejada: aquesta font no és a la llista HTTPS autoritzada.","Recuperación automática bloqueada: esta fuente no está en la lista HTTPS autorizada.");
        add("Educational exercise · ","Exercici educatiu · ","Ejercicio educativo · ");
        add("Remove technical batch effects while preserving biology","Elimina els efectes tècnics de lot tot preservant els senyals biològics","Elimina los efectos técnicos de lote conservando las señales biológicas");
        add("Predict intercellular signaling","Prediu la senyalització intercel·lular","Predice la señalización intercelular");
        add("Compare cytometry integration algorithms","Compara algorismes d'integració de citometria","Compara algoritmos de integración de citometría");
        add("Reduce noise in single-cell RNA counts","Redueix el soroll en els recomptes d'ARN de cèl·lula única","Reduce el ruido en los recuentos de ARN unicelular");
        add("Visualize high-dimensional data","Visualitza dades d'alta dimensionalitat","Visualiza datos de alta dimensionalidad");
        add("Evaluate foundation models for single-cell biology","Avalua models fundacionals per a biologia de cèl·lula única","Evalúa modelos fundacionales para biología unicelular");
        add("Process imaging-based spatial transcriptomics","Processa transcriptòmica espacial basada en imatges","Procesa transcriptómica espacial basada en imágenes");
        add("Transfer reference cell type annotations","Transfereix anotacions de tipus cel·lular de referència","Transfiere anotaciones de tipos celulares de referencia");
        add("Match multiple single-cell modalities","Relaciona diferents modalitats de cèl·lula única","Relaciona distintas modalidades unicelulares");
        add("Predict how small molecules alter gene expression","Prediu com les molècules petites modifiquen l'expressió gènica","Predice cómo las moléculas pequeñas modifican la expresión génica");
        add("Predict one omics modality from another","Prediu una modalitat òmica a partir d'una altra","Predice una modalidad ómica a partir de otra");
        add("Estimate spatial cell-type composition","Estima la composició espacial dels tipus cel·lulars","Estima la composición espacial de los tipos celulares");
        add("Assess simulated spatial transcriptomics","Avalua simulacions de transcriptòmica espacial","Evalúa simulaciones de transcriptómica espacial");
        add("Detect spatial expression patterns","Detecta patrons espacials d'expressió","Detecta patrones espaciales de expresión");
        add("Study early tissue changes with the aim of preventing stomach and oesophageal cancer","Estudia canvis primerencs dels teixits per prevenir el càncer d'estómac i d'esòfag","Estudia cambios tempranos de los tejidos para prevenir el cáncer de estómago y esófago");
        add("Find biological mechanisms behind differences in incidence between populations","Identifica mecanismes biològics de les diferències d'incidència entre poblacions","Identifica mecanismos biológicos de las diferencias de incidencia entre poblaciones");
        add("Why are certain high-risk people resistant to cancer?","Per què algunes persones amb un risc alt són resistents al càncer?","¿Por qué algunas personas de alto riesgo son resistentes al cáncer?");
        add("Find unexplored proteins relevant to cancer","Identifica proteïnes poc explorades relacionades amb el càncer","Identifica proteínas poco exploradas relacionadas con el cáncer");
        add("Understand unexplained mutational patterns","Investiga patrons mutacionals encara no explicats","Investiga patrones mutacionales todavía no explicados");
        add("Explore interactions between nerves and tumors","Estudia les interaccions entre nervis i tumors","Estudia las interacciones entre nervios y tumores");
        add("Study ways to alter malignant cellular programs","Investiga com modificar programes cel·lulars malignes","Investiga cómo modificar programas celulares malignos");
        add("Machine learning for drug discovery; confirm registration and evaluation on the official organizer page","Aprenentatge automàtic per descobrir fàrmacs; comprova la inscripció i l'avaluació al web oficial","Aprendizaje automático para descubrir fármacos; comprueba la inscripción y evaluación en la web oficial");
        add("Quantum-inspired approaches for biomedical target challenges; verify eligibility","Mètodes inspirats en la computació quàntica per a reptes biomèdics; verifica els requisits","Métodos inspirados en computación cuántica para retos biomédicos; verifica los requisitos");
        add("Analyze cognitive-assessment pen signals; check organizer dates","Analitza senyals de bolígrafs per a avaluació cognitiva; comprova les dates oficials","Analiza señales de bolígrafos para evaluación cognitiva; comprueba las fechas oficiales");
        add("Reproducible multi-omics integration or knowledge-mining workflow for non-small-cell lung cancer; hybrid event 21 November 2026, applications closed","Flux de treball reproduïble d'integració multiòmica o extracció de coneixement sobre càncer de pulmó no microcític; jornada híbrida el 21 de novembre de 2026, inscripcions tancades","Flujo de trabajo reproducible de integración multiómica o extracción de conocimiento sobre cáncer de pulmón no microcítico; jornada híbrida el 21 de noviembre de 2026, inscripciones cerradas");
        add("Crowdsourced prediction competitions; inspect official open listings","Concursos de predicció col·laborativa; consulta les convocatòries oficials obertes","Concursos de predicción colaborativa; consulta las convocatorias oficiales abiertas");
        add("Annual ELIXIR bioinformatics coding projects; 2026 event has concluded","Projectes anuals de programació bioinformàtica d'ELIXIR; l'edició de 2026 ha finalitzat","Proyectos anuales de programación bioinformática de ELIXIR; la edición de 2026 ha finalizado");
        add("Biomedical challenge day; application deadline has passed","Jornada de reptes biomèdics; el termini d'inscripció ha acabat","Jornada de retos biomédicos; el plazo de inscripción ha terminado");
        add("Public machine learning competitions; check source organizer before applying","Competicions públiques d'aprenentatge automàtic; comprova l'organitzador abans de participar","Competiciones públicas de aprendizaje automático; comprueba el organizador antes de participar");
        add("FDA-linked genomics, AI and biomedical data science challenges; inspect each official task and submission window.","Reptes de genòmica, IA i ciència de dades biomèdiques vinculats a l'FDA; consulta cada tasca i el termini oficial.","Retos de genómica, IA y ciencia de datos biomédicos vinculados a la FDA; consulta cada tarea y plazo oficial.");
        add("Biomedical, cancer, neurological and data-science challenge discovery; confirm eligibility and dates on each challenge page.","Cerca de reptes biomèdics, oncològics, neurològics i de ciència de dades; verifica els requisits i les dates de cada repte.","Búsqueda de retos biomédicos, oncológicos, neurológicos y de ciencia de datos; verifica requisitos y fechas de cada reto.");
        add("Open clinical signal processing and physiological AI challenges; confirm current submissions on each official site.","Reptes oberts de processament de senyals clínics i d'IA fisiològica; comprova si accepten propostes al web oficial.","Retos abiertos de procesamiento de señales clínicas y de IA fisiológica; comprueba si aceptan propuestas en la web oficial.");
        add("Current and previous physiologic data analysis challenges; check the official timetable and evaluation instructions.","Reptes actuals i anteriors d'anàlisi de dades fisiològiques; consulta el calendari i les instruccions oficials d'avaluació.","Retos actuales y anteriores de análisis de datos fisiológicos; consulta el calendario y las instrucciones oficiales de evaluación.");
        add("Benchmark competitions and evaluation tasks; check challenge-specific data, rules, scoring and deadline.","Competicions de referència i tasques d'avaluació; comprova les dades, les normes, la puntuació i el termini de cada repte.","Competiciones de referencia y tareas de evaluación; comprueba datos, reglas, puntuación y plazo de cada reto.");
        add("Predict recurrence after prostate cancer surgery and explain the AI predictions; testing deadline 30 Nov 2026.","Prediu la recurrència després de cirurgia de càncer de pròstata i explica les prediccions de la IA; prova fins al 30 de novembre de 2026.","Predice la recurrencia tras cirugía de cáncer de próstata y explica las predicciones de IA; prueba hasta el 30 de noviembre de 2026.");
        add("Predict fluorescence microscopy channels from transmitted-light images; submissions listed as accepted.","Prediu canals de microscòpia de fluorescència a partir d'imatges de llum transmesa; s'indica que s'accepten trameses.","Predice canales de microscopía de fluorescencia a partir de imágenes de luz transmitida; se indica que se aceptan envíos.");
        add("Measure pulmonary nodule volume from CT. Restricted to vendors with CE-certified products; participation agreement required.","Mesura el volum de nòduls pulmonars mitjançant TC. Restringit a proveïdors amb productes certificats CE; cal un acord de participació.","Mide el volumen de nódulos pulmonares mediante TC. Restringido a proveedores con productos certificados CE; se requiere un acuerdo de participación.");
        add("Build automated CT reconstruction for radiation planning. Training now; validation 1 Dec 2026 and final testing 29 Jan–15 Feb 2027.","Desenvolupa la reconstrucció automatitzada de TC per planificar radioteràpia. Entrenament actual; validació l'1 de desembre de 2026 i prova final del 29 de gener al 15 de febrer de 2027.","Desarrolla la reconstrucción automatizada de TC para planificar radioterapia. Entrenamiento actual; validación el 1 de diciembre de 2026 y prueba final del 29 de enero al 15 de febrero de 2027.");
        add("Segment brain vascular structures; final-test submission phase opens 11 Dec 2026.","Segmenta estructures vasculars cerebrals; la fase de lliurament de la prova final s'obre l'11 de desembre de 2026.","Segmenta estructuras vasculares cerebrales; la fase de entrega de la prueba final se abre el 11 de diciembre de 2026.");
        add("No live dataset search has been performed.","Encara no s'ha fet cap cerca de dades en directe.","Todavía no se ha hecho ninguna búsqueda de datos en directo.");
    }

    /** Build locale-specific listing metadata while preserving official titles,
     * source labels, identifiers, exact dates, and the original scientific record. */
    public static String challengeLabel(Context context,ResearchCatalog.Item item){
        String code=language(context);
        if("en".equals(code))return item.label();
        boolean ca="ca".equals(code);
        String state=item.availability();
        String checked=item.verifiedOn==null?"":item.verifiedOn;
        String date=item.deadline==null?"":item.deadline;
        if("OPEN".equals(state)){
            if("funding".equals(item.kind))
                return ca?"Sol·licituds de finançament · termini "+date
                        +" · cal comprovar els requisits · revisat "+checked
                        :"Solicitudes de financiación · plazo "+date
                        +" · comprobar requisitos · revisado "+checked;
            return ca?"S'accepten candidatures (comprovat el "+checked
                    +") · confirma-ho al web oficial"
                    :"Se aceptan solicitudes (comprobado el "+checked
                    +") · confírmalo en la web oficial";
        }
        if("UPCOMING".equals(state)){
            if("preregister".equals(item.kind))
                return ca?"PROPERAMENT · preinscripció amb l'organitzador"
                        :"PRÓXIMAMENTE · preinscripción con el organizador";
            return ca?"PROPERAMENT · la fase següent de lliurament s'obre el "+date
                    :"PRÓXIMAMENTE · la siguiente fase de entrega se abre el "+date;
        }
        if("BENCHMARK".equals(state))return ca?
                "Repte de referència · sense termini de concurs":"Reto de referencia · sin plazo de concurso";
        if("EDUCATIONAL".equals(state))return ca?"Exercici educatiu":"Ejercicio educativo";
        if("RESEARCH".equals(state))return ca?"Projecte finançat · inscripcions tancades":
                "Proyecto financiado · inscripciones cerradas";
        if("CLOSED".equals(state))return ca?"Tancat · no s'accepten sol·licituds":
                "Cerrado · no se aceptan solicitudes";
        if("RESTRICTED".equals(state))return ca?
                "Participació restringida · comprova els requisits de l'organitzador":
                "Participación restringida · comprueba los requisitos del organizador";
        return ca?"Estat no verificat o desactualitzat · confirma'l amb l'organitzador (revisió "+checked+")"
                :"Estado sin verificar o desactualizado · confírmalo con el organizador (revisión "+checked+")";
    }
    /** Live organizer excerpts contain unpredictable scraping noise. Translate
     * their known semantics using parsed metadata, never by changing dates. */
    public static String challengeSummary(Context context,ResearchCatalog.Item item){
        String code=language(context);
        if("en".equals(code))return item.summary;
        if(item.summary!=null && item.summary.startsWith("Official index: ")
                && ("live_open".equals(item.kind)||"live_upcoming".equals(item.kind))){
            boolean ca="ca".equals(code);
            String date=item.deadline;
            if("live_upcoming".equals(item.kind))
                return (ca?"Índex oficial: s'anuncia una futura fase de lliurament":
                        "Índice oficial: se anuncia una próxima fase de entrega")
                        +(date==null?"":ca?" el "+date:" el "+date)
                        +(ca?". Comprova la inscripció i els requisits al web de l'organitzador."
                        :". Comprueba la inscripción y los requisitos en la web del organizador.");
            return ca?"Índex oficial: s'hi anuncien lliuraments oberts. Comprova al web de l'organitzador les dates, la inscripció i els requisits."
                    :"Índice oficial: se anuncian entregas abiertas. Comprueba en la web del organizador las fechas, la inscripción y los requisitos.";
        }
        return tr(context,item.summary);
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
        String code=language(context);
        if("en".equals(code))return english;
        if(translation!=null)return "ca".equals(code)?translation[0]:translation[1];
        // Only match carefully delimited UI status prefixes. Source material,
        // research text, code and verified document titles stay untouched.
        String[] prefixes={"Retrieving official statement: ","Discovering public datasets: ",
            "Generating research proposal: ","Preparing PDF and ZIP: ",
            "Processing completed: ","Processing: ","Research step failed for ",
            "Automatic retrieval unavailable: ","PDF preview unavailable: ",
            "Saving PDF in Bioqra/","Automatic queue paused: ",
            "AI attempt unavailable: ","Official Grand Challenge index refreshed: ",
            "Grand Challenge live search unavailable; saved list kept. "};
        for(String prefix:prefixes){
            if(english.startsWith(prefix)){
                String[] label=LABELS.get(prefix);
                if(label!=null)return ("ca".equals(code)?label[0]:label[1])
                    +english.substring(prefix.length());
            }
        }
        return english;
    }
}
