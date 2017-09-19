package GeneradorLexers;

import javafx.util.Pair;

import java.io.*;
import java.util.*;

/**
 * La presente clase tiene como objetivo leer un archivo Cocol/R y generar el lexer
 * @author Eric Mendoza
 * @version 1.0
 * @since 2/09/2017
 */
public class CocolRReader {
    public CocolRReader(){
        idenNFAToDFA.generateSimpleEClosure(identAutomata);
    }

    // Simulador para automatas
    private Simulator simulator = new Simulator();

    /**
     * Algunos regex importantes
     */
    private String letterRegex = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z";
    private String digitRegex = "0|1|2|3|4|5|6|7|8|9";
    private String identRegex = "(" + letterRegex + ")((" + letterRegex + ")|(" + digitRegex + "))*";
    private DirectedGraph identAutomata = createAutomaton(identRegex);
    private NFAToDFA idenNFAToDFA = new NFAToDFA();
    private String lexerJavaFileName;

    /**
     * Guardar los patrones de cada seccion en hashmaps
     */
    private HashMap<String, String> caracteres = new HashMap<String, String>();  // Se va a guardar <iden, Conjunto>
    private ArrayList<String> caracteresKeysOrdered = new ArrayList<String>();  // Se guardan las llaves para acceder a ellas en orden despues


    private ArrayList<String> palabrasIgnoradas = new ArrayList<String>();  // Se va a guardar solo los caracteres a ignorar en bruto
    private ArrayList<String> whitespace = new ArrayList<String>();  // Cadenas de caracteres que no se toman en cuenta


    /**
     * Regex para el GeneratedFiles.Lexer a generar
     */
    private HashMap<String, String> caracteresRegex = new HashMap<String, String>();
    private HashMap<String, String> palabrasReservadasRegex = new HashMap<String, String>();  // Se va a guardar <iden, keyword>



    public boolean generateLexer(){
        boolean existenErrores = false;
        // IGNORE characters
        for (String palabra : palabrasIgnoradas) {

            int indexStart = palabra.indexOf("\"") + 1;
            int indexFinish = palabra.lastIndexOf("\"");

            String nuevaPalabraIgnorar = palabra.substring(indexStart, indexFinish);
            whitespace.add(nuevaPalabraIgnorar);

            // SE puede agregar funcionalidad para manejar distintos conjuntos
        }

        // CHARACTERS Regex
        Pair<Integer, String> result; // Variable de retorno de resultados
        boolean seEncontroSiguienteParte;
        // Por cada conjunto
        for (String identificador : caracteresKeysOrdered) {

            char[] futureRegex = caracteres.get(identificador).toCharArray();  // Obtener patron en bruto
            String newRegex = "";
            // Analizar metodo de creacion de conjunto
            for (int i = 0; i < futureRegex.length; i++) {

                char letter = futureRegex[i];

                switch (letter){
                    // Caso string con conjunto caracteres
                    case '"':
                        result = agregarStringRegex(i, futureRegex, newRegex);
                        i = result.getKey();
                        newRegex = result.getValue();
                        break;

                    case '\'':
                        // Avanzar al contenido dl char
                        i++;

                        // Obtener el contenido
                        newRegex = String.valueOf(futureRegex[i]);

                        // Avanzar uno más para consumir la segunda comilla
                        i++;
                        break;

                    case '+':
                        // Buscar que se va a sumar
                        seEncontroSiguienteParte = false;
                        while (!seEncontroSiguienteParte){
                            i++;  // Avanzar en la declaracion del conjunto
                            letter = futureRegex[i];

                            switch (letter){
                                case '"':
                                    seEncontroSiguienteParte = true;
                                    newRegex += "|";
                                    result = agregarStringRegex(i, futureRegex, newRegex);
                                    i = result.getKey();
                                    newRegex = result.getValue();
                                    break;

                                default:
                                    if (whitespace.contains(String.valueOf(letter))){
                                        break;
                                    } else {
                                        // En caso que se encuentre un identificador para otro conjunto
                                        seEncontroSiguienteParte = true;

                                        // Identificar el identificador del conjunto que se desea sumar
                                        Pair<Integer, String> identificadorResult = identifyIdentificator(i, futureRegex);
                                        i = identificadorResult.getKey();  // Guardar cuanto se avanzo
                                        String identificadorIdentificado = identificadorResult.getValue();  // identificador

                                        // Obtener regex del identificador de conjunto identificado
                                        String regexDelIdentificadorIdentificado = caracteresRegex.get(identificadorIdentificado);

                                        // Verificar que exista el identificador
                                        if (regexDelIdentificadorIdentificado != null){
                                            newRegex += "|" + regexDelIdentificadorIdentificado;
                                        } else {
                                            System.err.println("Error: La variable '" + identificadorIdentificado + "' no existe.");
                                            return false;
                                        }
                                        break;
                                    }
                            }

                        }

                        break;

                    case '-':
                        // Buscar que se va a sumar
                        seEncontroSiguienteParte = false;
                        while (!seEncontroSiguienteParte){
                            i++;  // Avanzar en la declaracion del conjunto
                            letter = futureRegex[i];

                            switch (letter){
                                case '"':
                                    seEncontroSiguienteParte = true;
                                    result = eliminarStringRegex(i, futureRegex, newRegex);
                                    i = result.getKey();
                                    newRegex = result.getValue();
                                    break;

                                default:
                                    if (whitespace.contains(String.valueOf(letter))){
                                        break;
                                    } else {
                                        // En caso que se encuentre un identificador para otro conjunto
                                        seEncontroSiguienteParte = true;

                                        // Identificar el identificador del conjunto que se desea sumar
                                        Pair<Integer, String> identificadorResult = identifyIdentificator(i, futureRegex);
                                        i = identificadorResult.getKey();  // Guardar cuanto se avanzo
                                        String identificadorIdentificado = identificadorResult.getValue();  // identificador encontrado

                                        // Obtener regex de ese otro conjunto
                                        String regexDelIdentificadorIdentificado = caracteresRegex.get(identificadorIdentificado);

                                        // Verificar que exista
                                        if (regexDelIdentificadorIdentificado != null){
                                            regexDelIdentificadorIdentificado = "\"" + regexDelIdentificadorIdentificado + "\"";  // Para que funcione con el mismo metodo
                                            result = eliminarStringRegex(0, regexDelIdentificadorIdentificado.toCharArray(), newRegex);  // Eliminar del regex
                                            newRegex = result.getValue();
                                        } else {
                                            System.err.println("Error: La variable '" + identificadorIdentificado + "' no existe.");
                                            return false; // Terminar el metodo
                                        }
                                        break;
                                    }
                            }
                        }

                        break;

                    case '.':
                        // Se debe buscar en el resuto de FutureRegex para ver si se encuentra otro punto
                        seEncontroSiguienteParte = false;
                        for (int j = i; j < futureRegex.length - 1; j++) {
                            i++;  // Avanzar en la declaracion del conjunto
                            letter = futureRegex[i];

                            switch (letter) {
                                case '.':
                                    seEncontroSiguienteParte = true;
                                    break;
                            }

                            if (seEncontroSiguienteParte){
                                Pair<Integer, String> resultNewRegex = extractCharRange(i, futureRegex, newRegex);
                                if (resultNewRegex != null){
                                    i = resultNewRegex.getKey();
                                    newRegex = resultNewRegex.getValue();
                                } else {
                                    return false;
                                }

                                break;  // Terminar for si se encontro otro punto
                            }
                        }
                        break;

                    case 'C':  // Podría ser un CHR()
                        // Verificar si existe el resto de declaracion CHR(
                        int j = i;  // Guardar inicio
                        j++;
                        char possible = futureRegex[j];

                        if (possible == 'H'){
                            j++;
                            possible = futureRegex[j];
                            if (possible == 'R'){
                                j++;
                                possible = futureRegex[j];
                                if (possible == '('){
                                    i = j;  // Actualizar i, porque se encontro un char

                                    boolean seEncontroSegundoParentesis = false;
                                    while (!seEncontroSegundoParentesis){
                                        i++;
                                        possible = futureRegex[i];

                                        if (possible == ')'){
                                            seEncontroSegundoParentesis = true;
                                        } else {
                                            newRegex += possible;
                                        }
                                    }

                                    int foundCharValue = Integer.valueOf(newRegex);
                                    char foundChar = (char)foundCharValue;
                                    newRegex = String.valueOf(foundChar);
                                    break;
                                }
                            }

                        }

                        // OJO: Se elimino el break, para que entonces baje a la siguiente parte como un ident normal si no es char

                    default:
                        // Verificar si es una palabra ignorada
                        if (whitespace.contains(String.valueOf(letter))){
                            break;
                        }

                        // Es un ident
                        else {
                            // Identificar el identificador del conjunto que se desea sumar
                            Pair<Integer, String> identificadorResult = identifyIdentificator(i, futureRegex);
                            i = identificadorResult.getKey();  // Guardar cuanto se avanzo
                            String identificadorIdentificado = identificadorResult.getValue();  // identificador

                            // Obtener el regex del identificador
                            String regexDelIdentificadorIdentificado = caracteresRegex.get(identificadorIdentificado);

                            // Verificar que exista
                            if (regexDelIdentificadorIdentificado != null){
                                newRegex += regexDelIdentificadorIdentificado;
                            } else {
                                System.err.println("Error: La variable '" + identificadorIdentificado + "' no existe.");
                                return false;
                            }
                        }
                        break;
                }
            }

            caracteresRegex.put(identificador, newRegex);

        }

        return true;

    }

    private Pair<Integer, String> extractCharRange(int i, char[] futureRegex, String newRegex) {
        // Buscar siguiente iniciador de CHAR
        char letter;
        boolean terminarLoop = false;
        String charFinal = "";
        for (int j = i; j < futureRegex.length; j++) {
            i++;  // Avanzar en la declaracion del conjunto
            letter = futureRegex[i];

            switch (letter) {
                case 'C':  // Podría ser un CHR()
                    i = i + 3;  // Avanzar tres, que estamos seguros que corresponden a las letras HR(
                    boolean seEncontroSegundoParentesis = false;
                    char possible;
                    while (!seEncontroSegundoParentesis){
                        i = i + 1;
                        possible = futureRegex[i];
                        if (possible == ')'){
                            seEncontroSegundoParentesis = true;
                        } else {
                            charFinal += possible;
                        }
                    }

                    int foundCharValue = Integer.valueOf(charFinal);
                    char foundChar = (char)foundCharValue;
                    charFinal = String.valueOf(foundChar);
                    terminarLoop = true;
                    break;

                case '\'':
                    // Avanzar al contenido del char
                    i++;

                    // Obtener el contenido
                    charFinal = String.valueOf(futureRegex[i]);

                    // Avanzar uno más para consumir la segunda comilla
                    i++;
                    terminarLoop = true;
                    break;
            }

            if (terminarLoop){
                break;
            }
        }

        // Obtener todos los characters entre ambos characters
        int charInicio = newRegex.charAt(0);
        int charEnd = charFinal.charAt(0);

        if (charInicio < charEnd){
            // Reiniciar newRegex
            newRegex = "";
            // Agregar cada caracter a nuevo regex
            for (int k = charInicio; k <= charEnd; k++){
                newRegex += (char)k;
            }
        } else {
            System.err.println("Error: El rango de chars que se desea crear es invalido.");
            return null;
        }

        return new Pair<Integer, String>(i, newRegex);

    }

    /**
     * Metodo que sirve para eliminar un conjunto de caracteres de un conjunto de regex nuevo
     * @param i es el index del arreglo, donde inicia el string en el arreglo
     * @param toEliminate es el arreglo de chars en el que se encuentra el conjunto en bruto a eliminar
     * @param newRegex es el regex que se está generando
     * @return devuelve el nuevo indice para el for y el nuevo regex
     */
    private Pair<Integer,String> eliminarStringRegex(int i, char[] toEliminate, String newRegex) {
        String result = "";
        HashSet<Character> conjuntoAEliminar = new HashSet<Character>();

        // Agregar a conjunto de palabras a eliminar
        int contadorQuotes = 0;
        while (true) {
            char letra = toEliminate[i];
            if (letra != '"'){
                conjuntoAEliminar.add(letra);
            } else {
                contadorQuotes++;
                // Si se termina de leer el string, entonces terminar for.
                if (contadorQuotes >= 2){
                    break;
                }
            }
            i++;  // Aumentar contador
        }

        char[] newRegexChar = newRegex.toCharArray();  // Convertir el nuevo regexanterior a arreglo
        // Copiar cada letra de newRegex, menos las que se desean eliminar
        for (int j = 0; j < newRegex.length(); j++) {
            char letraActual = newRegexChar[j];  // Obtener letra
            boolean seDebeIgnorar = conjuntoAEliminar.contains(letraActual);  // Ver si se debe ignorar
            if (!seDebeIgnorar){
                if (letraActual != '|'){
                    result += letraActual + "|";  // Copiar a nuvo resultado
                }
            }
        }

        result = result.substring(0, result.length() - 1);  // eliminar el ultimo OR que se coloco

        return new Pair<Integer, String>(i, result);
    }


    /**
     * Metodo que sirve para identificar un identificador
     * @param i el indice del for principal, donde inicia el identificador en el arreglo
     * @param futureRegex es el arreglo de chars que contiene el regex en bruto
     * @return devuelve el identificador encontrado
     */
    private Pair<Integer,String> identifyIdentificator(int i, char[] futureRegex) {
        String newIdentificator = "";  // Resultado final
        String preNewIdentificator = "";  // String de prueba
        boolean found = false;
        boolean isIdent = false;  // Para verificar si son identificadores
        while(!found){
            preNewIdentificator += futureRegex[i];  // Agregar nueva letra al posible identificador
            i++;  // Aumentar contador
            isIdent = simulator.simulateNFA(identAutomata, preNewIdentificator, idenNFAToDFA);  // Verificar si es identificadord

            if (isIdent){
                newIdentificator = preNewIdentificator;
            } else {
                found = true;
            }
        }

        i--;  // Disminuir en uno el contador por preview

        return new Pair<Integer, String>(i, newIdentificator);
    }

    /**
     * Metodo que sirve para agregar un conjunto de caracteres a un nuevo regex con ORs
     * @param i es el index del for prinipal, donde inicia el string en el arreglo
     * @param futureRegex es el arreglo de chars en el que se encuentra el conjunto en bruto
     * @param newRegex es el regex que se está generando
     * @return devuelve el nuevo indice para el for y el nuevo regex
     */
    public Pair<Integer, String> agregarStringRegex(int i, char[] futureRegex, String newRegex){
        i++;  // Avanzar en la declaracion del conjunto
        char letter = futureRegex[i];

        // Buscar las siguientes comillas e ir creando regex
        while (letter != '"'){
            newRegex += String.valueOf(letter);  // Agregar el primer caracter

            // Verificar que no es el ultimo caracter
            char nextLetter = futureRegex[i + 1];
            if(nextLetter != '"'){
                newRegex += "|";  // Agregar OR entre cada letra
            }

            i++;  // Avanzar en la declaracion del conjunto
            letter = nextLetter;  // Obtener siguiente letra
        }

        return new Pair<Integer, String>(i, newRegex);
    }


    /**
     * Metodo que tiene como objetivo verificar que esten correctamente declaradas cada una de las secciones de cocol/r
     * @param filename Documento con especificacion lexica
     * @return True, si la sintax es correcta; False, si existen errores
     */
    public boolean analizeCocolRSyntax(String filename){
        /**
         * Crear los automatas de Cocol/R
         */
        // Regex mas basicos
        String anyButQuoteRegex = digitRegex + "|(\\.)|#|$|%|&|/|=|¡|\'|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|(\\+)|(\\?)|(\\!)|(\\|)| |(\\\\)|" + letterRegex;   // El esparcio podria fallar
        String anyButApostropheRegex = digitRegex + "|(\\.)|#|$|%|&|/|=|¡|\"|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|(\\+)|(\\?)|(\\!)|(\\|)| |(\\\\)|" + letterRegex;

        // Vocabulary
        String numberRegex = "(" + digitRegex + ")(" + digitRegex + ")*";
        String stringRegex = "\"(" + anyButQuoteRegex + ")*\"";
        String charRegexBasic = "'(" + anyButApostropheRegex + ")'";

        // Regex de estructura
        String compilerDeclaration = " *COMPILER  *(" + identRegex + ") *";
        String endDeclaration = " *END  *(" + identRegex + ") *\\. *";
        String charactersDeclaration = " *CHARACTERS *";
        String keywordsDeclaration = " *KEYWORDS *";

        // CHARACTERS regexs
        String charRegex = "((" + charRegexBasic + ")|( *CHR *\\( *" + numberRegex  + " *\\) *))";
        String basicSetRegex = "(" + stringRegex + ")|(" + identRegex + ")|(" + charRegex + "( *\\.\\. *" + charRegex + " *)?)|(ANY)";
        String setRegex = "(" + basicSetRegex + ")( *(\\+|-) *(" +  basicSetRegex + "))*";
        String setDeclRegex = " *(" + identRegex + ") *= *" + "(" + setRegex + ") *\\. *";

        // KEYWORDS regex
        String keywordDeclRegex = "(" + identRegex + ") *= *(" + stringRegex +") *\\.";

        // WhiteSpace
        String whiteSpaceDecl = " *IGNORE *(" + setRegex + ") *\\. *";

        /**
         * Crear los automatas para verificar cada seccion
         */
        DirectedGraph compilerDeclarationAutomata = createAutomaton(compilerDeclaration);
        DirectedGraph charactersDeclarationAutomata = createAutomaton(charactersDeclaration);
        DirectedGraph keywordsDeclarationAutomata = createAutomaton(keywordsDeclaration);

        DirectedGraph keywordDeclAutomata = createAutomaton(keywordDeclRegex);
        DirectedGraph setsDeclAutomata = createAutomaton(setDeclRegex);
        DirectedGraph endDeclarationAutomata = createAutomaton(endDeclaration);
        DirectedGraph whitespaceDeclarationAutomata = createAutomaton(whiteSpaceDecl);

        /**
            Crear variables para la creacion de automatas de verificacion de sintax
        */
        List<String> documento = readFile(filename);
        if (documento == null){
            return false;
        }

        boolean inicio = false;
        boolean characters = false;
        int inicioCharacters = 0;
        boolean charactersCorrectly;
        int inicioKeywords = 0;
        boolean keywords = false;
        boolean keywordsCorrectly;
        boolean whitespace = false;
        boolean whitespaceCorrectly;
        int inicioWhiteSpace = 0;
        boolean existenErrores = false;
        ArrayList<String> errores = new ArrayList<String>();
        String identInicio = "", identFinal = "";
        boolean identIguales = false;
        boolean end = false;

        int lineaInicio = 0;
        int lineaFinal = 0;
        if (documento != null) {
            // Encontrar inicio
            for (int i = 0; i < documento.size(); i++) {
                inicio = simulator.simulateNFA(compilerDeclarationAutomata, documento.get(i));  // Devuelve True cuando encuentra COMPILER
                if (inicio){
                    lineaInicio = i;  // Guarda posicion inicio
                    lexerJavaFileName = documento.get(i).split(" ")[1];  // Guardar nombre para futuro lexer
                    identInicio = documento.get(i).split(" ")[1] + ".";  // Guarda identificador de inicio. Se le agrego un punto para coincidir con el del final
                    break;
                }
            }

            // Encontrar posicion CHARACTERS declaration
            for (int i = lineaInicio; i < documento.size(); i++) {
                characters = simulator.simulateNFA(charactersDeclarationAutomata, documento.get(i));  // Devuelve True cuando encuentra CHARACTERS
                if (characters) {
                    inicioCharacters = i;
                    break;
                }
            }

            // Encontrar Keywords Declaration
            int inicioBusquedaKeywords;

            if (characters){  // Ahorrarse un par de lineas de busqueda viendo si existe CHARACTERS
                inicioBusquedaKeywords = inicioCharacters;
            } else {
                inicioBusquedaKeywords = lineaInicio;
            }

            for (int i = inicioBusquedaKeywords; i < documento.size(); i++) {
                keywords = simulator.simulateNFA(keywordsDeclarationAutomata, documento.get(i));  // Devuelve True si encuentra KEYWORDS
                if (keywords) {
                    inicioKeywords = i;
                    break;
                }
            }

            // Encontrar WhiteSpace declaration
            int inicioBusquedaWhiteSpace;

            if (keywords){
                inicioBusquedaWhiteSpace = inicioKeywords;
            } else if (characters) {
                inicioBusquedaWhiteSpace = inicioCharacters;
            } else {
                inicioBusquedaWhiteSpace = lineaInicio;
            }

            for (int i = inicioBusquedaWhiteSpace; i < documento.size(); i++) {
                whitespace = simulator.simulateNFA(whitespaceDeclarationAutomata, documento.get(i));  // Si encuentra IGNORE indica donde esta
                if (whitespace){
                    inicioWhiteSpace = i;
                    break;
                }
            }

            // Encontrar END declaration
            int inicioBusquedaFinal;
            if (keywords){
                inicioBusquedaFinal = inicioKeywords;
            } else if (characters) {
                inicioBusquedaFinal = inicioCharacters;
            } else if (whitespace){
                inicioBusquedaFinal = inicioWhiteSpace;
            } else {
                inicioBusquedaFinal = lineaInicio;
            }

            for (int i = inicioBusquedaFinal; i < documento.size(); i++) {
                end = simulator.simulateNFA(endDeclarationAutomata, documento.get(i));  // Busca la palabra END
                if (end) {
                    lineaFinal = i;
                    identFinal = documento.get(i).split(" ")[1];  // Guarda identificador de final
                    break;
                }
            }


            // Verificar que se hayan ingresado correctamente los characters
            if (characters){  // Si se encontro esta seccion...
                System.out.println("Cargando characters...");
                String linea;

                // Establecer final de loop
                int finBusquedaCharacters;
                if (keywords){
                    finBusquedaCharacters = inicioKeywords;
                }
                else if (whitespace){
                    finBusquedaCharacters = inicioWhiteSpace;
                }
                else {
                    finBusquedaCharacters = lineaFinal;
                }

                // Analizar cada linea
                for (int i = inicioCharacters + 1; i < finBusquedaCharacters; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        charactersCorrectly = simulator.simulateNFA(setsDeclAutomata, linea);  // Verificar que la linea este bien escrita

                        // Si se encuentra una linea correcta
                        if (charactersCorrectly) {
                            String[] lineSections = linea.split("=");
                            Pair<Integer, String> identificatorCharacterPair = identifyIdentificator(0, lineSections[0].toCharArray());
                            caracteresKeysOrdered.add(identificatorCharacterPair.getValue());
                            caracteres.put(identificatorCharacterPair.getValue(), lineSections[1]);  // Guardar ident y conjunto

                        } else {
                            errores.add("Error al declarar un character. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }
                    }
                }
            }


            // Verificar que se hayan ingresado correctamente los keywords
            if (keywords){  // Si existe esta seccion
                String linea;
                System.out.println("Cargando keywords...");

                // establecer final de loop
                int finBusquedaKeywords;
                if (whitespace){
                    finBusquedaKeywords = inicioWhiteSpace;
                }
                else {
                    finBusquedaKeywords = lineaFinal;
                }
                for (int i = inicioKeywords + 1; i < finBusquedaKeywords; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        keywordsCorrectly = simulator.simulateNFA(keywordDeclAutomata, linea);

                        // Si se encuentra una linea incorrecta
                        if (keywordsCorrectly) {
                            String[] seccionesLinea = linea.split("=");
                            // Limpiar correctamente el identificador
                            Pair<Integer, String> identificatorCharacterPair = identifyIdentificator(0, seccionesLinea[0].toCharArray());

                            // Corregir el regex de la palabra reservada
                            String newRegex = seccionesLinea[1];
                            int inicioNewRegex = newRegex.indexOf("\"") + 1;
                            int endNewRegex = newRegex.lastIndexOf("\"");

                            newRegex = newRegex.substring(inicioNewRegex, endNewRegex);

                            palabrasReservadasRegex.put(identificatorCharacterPair.getValue(), newRegex);

                        } else {
                            errores.add("Error al declarar un keyword. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }

                    }
                }
            }

            // Verificar que se hayan ingresado correctamente los whitespace
            if (whitespace){
                String linea;
                System.out.println("Cargando whitespaces...");


                for (int i = inicioWhiteSpace; i < lineaFinal; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        whitespaceCorrectly = simulator.simulateNFA(whitespaceDeclarationAutomata, linea);

                        // Si se encuentra una linea incorrecta
                        if (whitespaceCorrectly) {
                            String[] seccionesLinea = linea.split("IGNORE");
                            String palabra = seccionesLinea[1];
                            String nuevapalabra = "";
                            for (int j = 0; j < palabra.length() - 1; j++) {
                                char c = palabra.charAt(j);
                                if (c == '\\'){
                                    nuevapalabra += "\\\\\\\\";
                                } else {
                                    nuevapalabra += c;
                                }
                            }

                            palabrasIgnoradas.add(nuevapalabra);
                        } else {
                            errores.add("Error al declarar un whitespace. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }
                    }
                }
            }
        }

        // Revisar errores
        if (inicio && end){
            identIguales = identFinal.equals(identInicio);
        }
        if (!identIguales) System.err.println("Error: Los identificadores de inicio y final no coinciden.");
        if (!inicio) System.err.println("Error: Declaracion de inicio incorrecta");
        if (!end) System.err.println("Error: No se coloco correctamente el final del documento");
        if (existenErrores){
            for (String error : errores) {
                System.err.println(error);
            }
        }

        return inicio && end && !existenErrores && identIguales;
    }


    private DirectedGraph createAutomaton(String regex){
        /**
         * Construccion directa de DFA
         */
        // Procesar regex
        RegExToNFA regExToNFA = new RegExToNFA();
        regex = RegExConverter.infixToPostfix(regex);  // Convertir a postfix
        DirectedGraph nfa = regExToNFA.evaluate(regex);


        /**
         * Simplificar DFA por construccion directa
         */
        //GeneradorLexers.HopcroftMinimizator hopcroftMinimizator1 = new GeneradorLexers.HopcroftMinimizator();
        //GeneradorLexers.DirectedGraph dfaDirectoSimplificado = hopcroftMinimizator1.minimizateDFA(dfaDirecto);

        /**
         * Simular dfa
         */
        //simulator = new GeneradorLexers.Simulator();
        //boolean resultado = simulator.simulateNFA(dfaDirectoSimplificado, "digit = ANY.");
        //System.out.println("Estado de aceptacion: " + resultado);

        /**
         * Graficar para verificar
         */
        //GeneradorLexers.AutomataRenderer.renderAutomata(dfaDirectoSimplificado, "DFA (Por construccion directa) minimizado");

        return nfa;

    }


    /**
     * Metodo para leer el archivo Cocol/R obtenido de una pagina.
     * @param filename nombre a de archivo a abrir
     * @return Lista con strings de cada linea del documento
     * @link https://alvinalexander.com/blog/post/java/how-open-read-file-java-string-array-list
     */
    public static List<String> readFile(String filename)
    {
        List<String> records = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null)
            {
                records.add(line);
            }
            reader.close();
            return records;
        }
        catch (Exception e)
        {
            System.err.print("No existe el archivo '" +  filename + "'\n");
            return null;
        }
    }

    public void generateLexerJavaFile(){
        String programa = "";

        // Agregar imports
        programa +=
                "package GeneratedFiles;\n" +
                "\n" +
                "import GeneradorLexers.*;\n" +
                "import javafx.util.Pair;\n" +
                "\n" +
                "import java.util.*;\n\n";

        // Agregar inicio de clase y variables
        programa +=
                "public class Lexer {\n" +
                "    // Variables\n" +
                "    private ArrayList<Pair<String, String>> tokensTypesAndRegexs;  // <Type, Regex>\n" +
                "    private Simulator simulador;\n" +
                "    private NFAToDFA funciones;\n" +
                "    private DirectedGraph tokenAutomata;\n" +
                "    private HashMap<String, Integer> tokenPrecedence;\n\n";

        // Constructor
        programa +=
                "    public Lexer(){\n" +
                "        // Inicializar base de datos de identificadores de regex\n" +
                "        tokensTypesAndRegexs = new ArrayList<Pair<String, String>>();\n" +
                "\n" +
                "        // Inicializar precedencia de los tokens\n" +
                "        tokenPrecedence = new HashMap<String, Integer>();\n" +
                "\n" +
                "        // Inicializar simulador\n" +
                "        simulador = new Simulator();\n" +
                "\n" +
                "        // Agregar Regex identificados\n";

        // Regex identificados
        // 1) Characters / productions
        for (String identificador : caracteresKeysOrdered) {
            programa +=
                    "        tokensTypesAndRegexs.add(new Pair<String, String>(\"" + identificador + "\", \""
                    + caracteresRegex.get(identificador) + "\"));\n";
        }

        // 2) Keywords
        Set<String> palabrasReservadas = palabrasReservadasRegex.keySet();
        for (String identificador : palabrasReservadas) {
            programa +=
                    "        tokensTypesAndRegexs.add(new Pair<String, String>(\"" + identificador + "\", \""
                            + palabrasReservadasRegex.get(identificador) + "\"));\n";
        }

        // 3) Whitespace
        programa += "        tokensTypesAndRegexs.add(new Pair<String, String>(\"whitespace\", \"";
        for (int i = 0; i < whitespace.size(); i++) {
            String palabra = whitespace.get(i);
            if (i < whitespace.size() - 1){
                programa += "(" + palabra + ")|";
            } else {
                programa += "(" + palabra + ")";
            }
        }
        programa += "\"));\n";


        // Agregar precedencia a regex
        // 1) Keywords
        programa += "\n\n        // Colocar precedencia a keywords\n";
        int precedenceCounter = 1;
        for (String keyword : palabrasReservadas) {
            programa += "        tokenPrecedence.put(\"" + keyword + "\", " + precedenceCounter + ");\n";
            precedenceCounter++;
        }

        // 2) Productions
        programa += "\n\n        // Colocar precedencia a resto de tokens\n";
        for (String tokenType : caracteresKeysOrdered) {
            programa += "        tokenPrecedence.put(\"" + tokenType + "\", " + precedenceCounter + ");\n";
            precedenceCounter++;
        }

        // 3) IGNORE
        programa += "        tokenPrecedence.put(\"whitespace\", " + precedenceCounter + ");\n\n";


        // Fin de constructor
        programa += "        // Crear automata reconocedor de tokens TokenAutomata\n" +
                "        generateTokenIdentificatorAutomata();\n" +
                "\n" +
                "        // Crear las funciones para el automata creado\n" +
                "        funciones = new NFAToDFA();\n" +
                "        funciones.generateSimpleEClosure(tokenAutomata);\n}";


        // Metodos
        programa += "\n" +
                "\n\n" +
                "    /**\n" +
                "     * Funcion que tiene como objetivo reconocer el siguiente token de un programa ingresado\n" +
                "     * @param tokens es el Arraylist donde se estan guardando todos los tokens del programa\n" +
                "     * @param programa es el string que contiene todas las lineas del programa\n" +
                "     * @param inicioLexema es desde donde se iniciara la busqueda de tokens nuevos\n" +
                "     * @return 1) Actualiza el ArrayList de tokens. 2) Una tupla que contiene: - Si se encontro nu token (true/false) - El nuevo inicio Lexema\n" +
                "     */\n" +
                "    public Pair<Boolean, Integer> nextToken(ArrayList<Pair<String, String>> tokens, String programa, Integer inicioLexema) {\n" +
                "        if (!(inicioLexema > programa.length())){\n" +
                "            // Analizar programa\n" +
                "            Pair<Integer, HashSet<DirectedGraph.NodeClass>> tokenInfo = simulador.simulateNFARecognizor(tokenAutomata, programa, funciones, inicioLexema);\n" +
                "\n" +
                "            // Obtener fin de nuevo token\n" +
                "            int finLexema = tokenInfo.getKey();\n" +
                "\n" +
                "            // Obtener el tipo de token encontrado por medio de los estados finales alcanzados\n" +
                "            HashSet<DirectedGraph.NodeClass> estadosFinales = tokenInfo.getValue();\n" +
                "\n" +
                "            // Si sí se encontro una coincidencia\n" +
                "            if (!estadosFinales.isEmpty()){\n" +
                "                // Obtener el tipo de token encontrado\n" +
                "                DirectedGraph.NodeClass estadoFinal = null;\n" +
                "                int precedenciaAnterior = 100000;\n" +
                "                int precedenciaActual;\n" +
                "                for (DirectedGraph.NodeClass estadoActual : estadosFinales) {\n" +
                "                    // Obtener precedencia del nuevo estado\n" +
                "                    precedenciaActual = estadoActual.getPrecedence();\n" +
                "\n" +
                "                    if (precedenciaActual < precedenciaAnterior){\n" +
                "                        // Solo el de precedencia mas pequena se quedara\n" +
                "                        estadoFinal = estadoActual;\n" +
                "                    }\n" +
                "                }\n" +
                "\n" +
                "                // Obtener el tipo de token\n" +
                "                if (estadoFinal != null){\n" +
                "                    String tokenType = estadoFinal.getTokenType();\n" +
                "                    if (!tokenType.equals(\"whitespace\")){\n" +
                "                        // Si es un token importante, se obtiene el lexema del programa\n" +
                "                        String lexema = programa.substring(inicioLexema, finLexema + 1);\n" +
                "\n" +
                "                        // Se actualiza el nuevo inicioLexema\n" +
                "                        inicioLexema = finLexema + 1;\n" +
                "\n" +
                "                        // Actualizar tokens\n" +
                "                        tokens.add(new Pair<String, String>(tokenType, lexema));\n" +
                "\n" +
                "                        // Devolver resultado\n" +
                "                        return new Pair<Boolean, Integer>(true, inicioLexema);\n" +
                "                    }\n" +
                "\n" +
                "                    // Si se encuentra un whitespace\n" +
                "                    else {\n" +
                "                        // Volver a llamar al metodo, con un nuevo inicio\n" +
                "                        return nextToken(tokens, programa, finLexema + 1);\n" +
                "                    }\n" +
                "\n" +
                "                } else {\n" +
                "                    // Esto nunca va a pasar\n" +
                "                    System.err.println(\"Error: Tipo de token NULO.\");\n" +
                "                    return new Pair<Boolean, Integer>(false, finLexema + 1);\n" +
                "                }\n" +
                "\n" +
                "            } else {\n" +
                "                System.err.println(\"Error: Token no conocido <\" + programa.substring(inicioLexema, finLexema + 1) + \">\");\n" +
                "                return new Pair<Boolean, Integer>(false, finLexema + 1);\n" +
                "            }\n" +
                "\n" +
                "        } else {\n" +
                "            return new Pair<Boolean, Integer>(false, inicioLexema);\n" +
                "        }\n" +
                "    }\n" +
                "\n\n" +
                "    private void generateTokenIdentificatorAutomata(){\n" +
                "        // Funcion de crear automatas\n" +
                "        RegExToNFA regExToNFA = new RegExToNFA();\n" +
                "\n" +
                "        // Stack de todos los automatas separados\n" +
                "        Stack<DirectedGraph> automatasPerToken = new Stack<DirectedGraph>();\n" +
                "\n" +
                "        // Crear un automata por cada regex conocido\n" +
                "        String newRegex, newTokenType;\n" +
                "        for (Pair<String, String> tokenTypeRegex: tokensTypesAndRegexs){\n" +
                "            // Obtener regex\n" +
                "            newRegex = tokenTypeRegex.getValue();\n" +
                "            newRegex = RegExConverter.infixToPostfix(newRegex);  // Pasar a postfix\n" +
                "\n" +
                "            // Obtener el tipo de token\n" +
                "            newTokenType = tokenTypeRegex.getKey();\n" +
                "\n" +
                "            // Crear automata nuevo\n" +
                "            DirectedGraph newAutomata = regExToNFA.evaluate(newRegex);\n" +
                "\n" +
                "            // Agregar información al nodo final de cada automata\n" +
                "            DirectedGraph.NodeClass nodoFinal = newAutomata.getOneFinalNode();\n" +
                "            nodoFinal.setPrecedence(tokenPrecedence.get(newTokenType));\n" +
                "            nodoFinal.setTokenType(newTokenType);\n" +
                "\n" +
                "            automatasPerToken.push(newAutomata);\n" +
                "        }\n" +
                "\n" +
                "        tokenAutomata = unifyAutomatas(automatasPerToken);\n" +
                "    }\n" +
                "\n" +
                "    private DirectedGraph unifyAutomatas(Stack<DirectedGraph> automatasPerToken) {\n" +
                "        // Obtener automata al que se le uniran el resto de automatas\n" +
                "        DirectedGraph op1 = automatasPerToken.pop();\n" +
                "        DirectedGraph.NodeClass nodoInicialAbsoluto = op1.getOneInicialNode();  // Obtener el unico nodo inicial\n" +
                "\n" +
                "        DirectedGraph op2;\n" +
                "        DirectedGraph.NodeClass nodoInicialViejo;\n" +
                "        while (!automatasPerToken.isEmpty()) {\n" +
                "            // Obtener siguiente automata\n" +
                "            op2 = automatasPerToken.pop();\n" +
                "\n" +
                "            // Obtener nodos de segundo automata\n" +
                "            LinkedList<DirectedGraph.NodeClass> nodos2 = op2.getAllNodes();\n" +
                "\n" +
                "            // Obtener transiciones automata 2\n" +
                "            LinkedList<DirectedGraph.edgeContents> edges2 = op2.getEdges();\n" +
                "\n" +
                "            // Copiar cada nodo a automata 1\n" +
                "            for (DirectedGraph.NodeClass i : nodos2) {\n" +
                "                op1.addNode(op1, i);\n" +
                "\n" +
                "                // Evita un bug\n" +
                "                if (i.isFinal()){\n" +
                "                    op1.getFinalNode().add(i);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Copiar cada transicion a automata 1\n" +
                "            for (DirectedGraph.edgeContents i : edges2) {\n" +
                "                op1.addEdges(op1, i);\n" +
                "            }\n" +
                "\n" +
                "            // Agregar una transicion epsilon entre nodo inicial de OP1 a OP2\n" +
                "            nodoInicialViejo = op2.getOneInicialNode();  // Obtener nodo inicial de op2\n" +
                "            op1.addEdges(op1, nodoInicialAbsoluto, nodoInicialViejo, \"!\");  // Agregar nueva transicion\n" +
                "            nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial antiguo en op1\n" +
                "\n" +
                "            // Agregar alphabeto\n" +
                "            HashSet<String> alfabetoNuevo = op1.getAlphabet();\n" +
                "            alfabetoNuevo.addAll(op2.getAlphabet());\n" +
                "            op1.setAlphabet(alfabetoNuevo);\n" +
                "        }\n" +
                "\n" +
                "        return op1;\n" +
                "    }\n" +
                "}\n";

        try{
            PrintWriter salida = new PrintWriter(new File("src/GeneratedFiles/Lexer.java"));
            salida.print(programa);
            salida.close();
        }catch(Exception e){
            System.err.println("Error: al generar archivo Lexer1.java");
        }
    }
}
