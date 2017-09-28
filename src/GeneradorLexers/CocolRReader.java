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
    private HashMap<String, String> caracteres = new HashMap<String, String>();  // Se va a guardar, regex sin analizar <iden, Conjunto>
    private ArrayList<String> caracteresKeysOrdered = new ArrayList<String>();  // Se guardan las llaves para acceder a ellas en orden despues

    private ArrayList<String> tokensEnBruto = new ArrayList<String>();  // Guarda las lineas que contienen tokens

    private ArrayList<String> palabrasIgnoradas = new ArrayList<String>();  // Se va a guardar solo los caracteres a ignorar en bruto
    private ArrayList<String> whitespace = new ArrayList<String>();  // Cadenas de caracteres que no se toman en cuenta


    /**
     * Regex para el lexer a generar
     */
    private HashMap<String, String> caracteresRegex = new HashMap<String, String>();
    private HashMap<String, String> palabrasReservadasRegex = new HashMap<String, String>();  // Se va a guardar <iden, keyword>
    private HashMap<String, String> tokensRegex = new HashMap<String, String>();  // <tokenIden, tokenRegex>



    /**
     * Metodo que tiene como objetivo verificar que esten correctamente declaradas cada una de las secciones de cocol/r
     * Tambien, guarda las lineas que contienen las declaraciones de los diferentes conjuntos
     * @param filename Documento con especificacion lexica
     * @return True, si la sintax es correcta; False, si existen errores
     */
    public boolean analizeCocolRSyntax(String filename){
        /**
         * Crear los automatas declarados en Cocol/R
         */
        // Regex mas basicos
        String ANY = "(\u0001)|(\u0002)|(\u0003)|(\u0004)|(\u0005)|(\u0006)|(\u0007)" +
                "|\u000E|\u000F|\u0010|\u0011|\u0012|\u0013|\u0014|\u0015|\u0016|\u0017|\u0018|\u0019|\u001A|\u001B|\u001C|\u001D|\u001E|\u001F|( )|(\\!)|\"|#|$|%|&|'|(\\()|(\\))|(\\*)|(\\+)|,|-|(\\.)|/|0|1|2|3|4|5|6|7|8|9|:|;|<|=|>|(\\?)|@|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|[|(\\\\)|]|^|_|`|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|{|(\\|)|}|~" +
                "|\u007F|\u0080|\u0081|\u0082|\u0083|\u0084|\u0085|\u0086|\u0087|\u0088|\u0089|\u008A|\u008B|\u008C|\u008D|\u008E|\u008F|\u0090|\u0091|\u0092|\u0093|\u0094|\u0095|\u0096|\u0097|\u0098|\u0099|\u009A|\u009B" +
                "|\u009C|\u009D|\u009E|\u009F|( )|¡|¢|£|¤|¥|¦|§|¨|©|ª|«|¬|\u00AD|®|¯|°|±|²|³|´|µ|¶|·|¸|¹|º|»|¼|½|¾|¿|À|Á|Â|Ã|Ä|Å|Æ|Ç|È|É|Ê|Ë|Ì|Í|Î|Ï|Ð|Ñ|Ò|Ó|Ô|Õ|Ö|×|Ø|Ù|Ú|Û|Ü|Ý|Þ|ß|à|á|â|ã|ä|å|æ|ç|è|é|ê|ë|ì|í|î|ï|ð|ñ|ò|ó|ô|õ|ö|÷|ø|ù|ú|û|ü|ý|þ";
        caracteresRegex.put("ANY", ANY);

        String anyButQuoteRegex = digitRegex + "|(\\.)|#|$|%|&|/|=|¡|\'|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|(\\+)|(\\?)|(\\!)|(\\|)| |(\\\\)|" + letterRegex;   // El esparcio podria fallar
        String anyButApostropheRegex = digitRegex + "|(\\.)|#|$|%|&|/|=|¡|\"|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|(\\+)|(\\?)|(\\!)|(\\|)| |(\\\\)|" + letterRegex;

        // Vocabulary
        String numberRegex = "(" + digitRegex + ")(" + digitRegex + ")*";
        String stringRegex = "\"(" + anyButQuoteRegex + ")*\"";
        String charRegexBasic = "'(" + anyButApostropheRegex + ")'";

        // CHARACTERS regexs
        String charRegex = "((" + charRegexBasic + ")|( *CHR *\\( *" + numberRegex  + " *\\) *))";
        String basicSetRegex = "(" + stringRegex + ")|(" + identRegex + ")|(" + charRegex + "( *\\.\\. *" + charRegex + " *)?)|( *ANY *)";
        String setRegex = "(" + basicSetRegex + ")( *(\\+|-) *(" +  basicSetRegex + "))*";
        String setDeclRegex = " *(" + identRegex + ") *= *" + "(" + setRegex + ") *\\. *";

        // KEYWORDS regex
        String keywordDeclRegex = "(" + identRegex + ") *= *(" + stringRegex +") *\\.";

        // WhiteSpace
        String whiteSpaceDecl = " *IGNORE *(" + setRegex + ") *\\. *";

        /**
         * Crear los automatas para verificar cada seccion
         */
        // Sintax de secciones
        DirectedGraph keywordDeclAutomata = createAutomaton(keywordDeclRegex);
        DirectedGraph setsDeclAutomata = createAutomaton(setDeclRegex);
        DirectedGraph whitespaceDeclarationAutomata = createAutomaton(whiteSpaceDecl);
        /* No se incluye sintax de tokens por complejidad */

        // Crear e-closure de cada automata
        NFAToDFA keywordDeclAutomataEClosure = new NFAToDFA();
        keywordDeclAutomataEClosure.generateSimpleEClosure(keywordDeclAutomata);
        NFAToDFA setsDeclAutomataEClosure = new NFAToDFA();
        setsDeclAutomataEClosure.generateSimpleEClosure(setsDeclAutomata);
        NFAToDFA whitespaceDeclarationAutomataEClosure = new NFAToDFA();
        whitespaceDeclarationAutomataEClosure.generateSimpleEClosure(whitespaceDeclarationAutomata);

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
        int inicioTokens = 0;
        boolean tokens = false;
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
                inicio = documento.get(i).contains("COMPILER");  // Devuelve True cuando encuentra COMPILER
                if (inicio){
                    lineaInicio = i;  // Guarda posicion inicio
                    lexerJavaFileName = documento.get(i).split(" ")[1];  // Guardar nombre para futuro lexer
                    identInicio = documento.get(i).split(" ")[1] + ".";  // Guarda identificador de inicio. Se le agrego un punto para coincidir con el del final
                    break;
                }
            }

            // Encontrar posicion CHARACTERS declaration
            for (int i = lineaInicio + 1; i < documento.size(); i++) {
                characters = documento.get(i).contains("CHARACTERS"); // Devuelve True cuando encuentra CHARACTERS
                if (characters) {
                    inicioCharacters = i;
                    break;
                }
            }

            // Encontrar Keywords Declaration
            int inicioBusquedaKeywords;

            if (characters){  // Ahorrarse un par de lineas de busqueda viendo si existe CHARACTERS
                inicioBusquedaKeywords = inicioCharacters + 1;
            } else {
                inicioBusquedaKeywords = lineaInicio + 1;
            }

            for (int i = inicioBusquedaKeywords; i < documento.size(); i++) {
                keywords = documento.get(i).contains("KEYWORDS");  // Devuelve True si encuentra KEYWORDS
                if (keywords) {
                    inicioKeywords = i;
                    break;
                }
            }

            // Encontrar Token declaration
            int inicioBusquedaTokens;

            if (keywords){
                inicioBusquedaTokens = inicioKeywords + 1;
            } else if (characters) {
                inicioBusquedaTokens = inicioCharacters + 1;
            } else {
                inicioBusquedaTokens = lineaInicio + 1;
            }

            for (int i = inicioBusquedaTokens; i < documento.size(); i++) {
                tokens = documento.get(i).contains("TOKENS");
                if (tokens){
                    inicioTokens = i;
                    break;
                }
            }


            // Encontrar WhiteSpace declaration
            int inicioBusquedaWhiteSpace;

            if (tokens){
                inicioBusquedaWhiteSpace = inicioTokens + 1;
            } else if (keywords){
                inicioBusquedaWhiteSpace = inicioKeywords + 1;
            } else if (characters) {
                inicioBusquedaWhiteSpace = inicioCharacters + 1;
            } else {
                inicioBusquedaWhiteSpace = lineaInicio + 1;
            }

            for (int i = inicioBusquedaWhiteSpace; i < documento.size(); i++) {
                whitespace = documento.get(i).contains("IGNORE");  // Si encuentra IGNORE indica donde esta
                if (whitespace){
                    inicioWhiteSpace = i;
                    break;
                }
            }

            // Encontrar END declaration
            int inicioBusquedaFinal;
            if (whitespace){
                inicioBusquedaFinal = inicioWhiteSpace + 1;
            } else if (tokens) {
                inicioBusquedaFinal = inicioTokens + 1;
            } else if (keywords){
                inicioBusquedaFinal = inicioKeywords + 1;
            } else if (characters){
                inicioBusquedaFinal = inicioCharacters + 1;
            } else {
                inicioBusquedaFinal = lineaInicio + 1;
            }

            for (int i = inicioBusquedaFinal; i < documento.size(); i++) {
                end = documento.get(i).contains("END");
                if (end) {
                    lineaFinal = i;
                    identFinal = documento.get(i).split(" ")[1];  // Guarda identificador de final
                    break;
                }
            }


            // CHARACTERS: Verificar que se hayan ingresado correctamente los characters
            if (characters){  // Si se encontro esta seccion...
                System.out.println("Cargando characters...");
                String linea;

                // Establecer final de loop
                int finBusquedaCharacters;
                if (keywords){
                    finBusquedaCharacters = inicioKeywords;
                }
                else if (tokens){
                    finBusquedaCharacters = inicioTokens;
                }
                else if (whitespace){
                    finBusquedaCharacters = inicioWhiteSpace;
                }
                else {
                    finBusquedaCharacters = lineaFinal;
                }

                // Analizar cada linea
                char ultimoChar;
                String subLinea;
                boolean dotFound = false;
                for (int i = inicioCharacters + 1; i < finBusquedaCharacters; i++) {
                    linea = documento.get(i);
                    subLinea = linea;

                    // No tomar en cuenta lineas vacías
                    if (!linea.equals("")){
                        // Buscar punto del final
                        while (!dotFound && i < finBusquedaCharacters){
                            ultimoChar = subLinea.charAt(subLinea.length() - 1);
                            if (ultimoChar == '.'){
                                dotFound = true;
                            } else {
                                // Si no se ha encontrado, hay que concatenar con la linea de abajo
                                i++;
                                subLinea += documento.get(i);
                            }
                        }

                        // Verificar si se encontro
                        if (dotFound){
                            // Intercambiar variables, porque sí
                            linea = subLinea;
                            dotFound = false;
                        } else {
                            errores.add("Error al declarar un character: Hace falta un punto final. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }

                        // Verificar sintax
                        charactersCorrectly = linea.contains("=");  // Verificar que la linea este bien escrita


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


            // KEYQORDS: Verificar que se hayan ingresado correctamente los keywords
            if (keywords){  // Si existe esta seccion
                String linea;
                System.out.println("Cargando keywords...");

                // establecer final de loop
                int finBusquedaKeywords;
                if (tokens){
                    finBusquedaKeywords = inicioTokens;
                }
                else if (whitespace){
                    finBusquedaKeywords = inicioWhiteSpace;
                }
                else {
                    finBusquedaKeywords = lineaFinal;
                }

                char ultimoChar;
                String subLinea;
                boolean dotFound = false;
                for (int i = inicioKeywords + 1; i < finBusquedaKeywords; i++) {
                    linea = documento.get(i);
                    subLinea = linea;
                    if (!linea.equals("")){
                        // Buscar punto del final
                        while ((i < finBusquedaKeywords) && !dotFound){
                            ultimoChar = subLinea.charAt(subLinea.length() - 1);
                            if (ultimoChar == '.'){
                                dotFound = true;
                            } else {
                                // Si no se ha encontrado, hay que concatenar con la linea de abajo
                                i++;
                                subLinea += documento.get(i);
                            }
                        }

                        // Verificar si se encontro
                        if (dotFound){
                            // Intercambiar variables, porque sí
                            linea = subLinea;
                            dotFound = false;
                        } else {
                            errores.add("Error al declarar un keyword: Hace falta un punto final. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }

                        // Verificar que tenga divisor de asignacion
                        keywordsCorrectly = linea.contains("=");

                        // Si se encuentra una linea incorrecta
                        if (keywordsCorrectly) {
                            String[] seccionesLinea = linea.split("=");
                            // Limpiar correctamente el identificador
                            Pair<Integer, String> identificatorCharacterPair = identifyIdentificator(0, seccionesLinea[0].toCharArray());

                            // Corregir el regex de la palabra reservada
                            String newRegex = seccionesLinea[1];
                            int inicioNewRegex = newRegex.indexOf("\"") + 1;
                            int endNewRegex = newRegex.lastIndexOf("\"");

                            if (inicioNewRegex == (endNewRegex + 1)) {
                                errores.add("Error al declarar el keyword '" + identificatorCharacterPair.getValue() + "', " +
                                        "faltan unas comillas. Linea: " + String.valueOf(i + 1));
                                existenErrores = true;
                                break;
                            }

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

            // TOKENS: Guardar las lineas que contienen tokens
            if (tokens){
                String linea;
                System.out.println("Cargando tokens...");

                // Establecer final de loop de busqueda
                int finBusquedaTokens;
                if (whitespace){
                    finBusquedaTokens = inicioWhiteSpace;
                }
                else {
                    finBusquedaTokens = lineaFinal;
                }

                // Obtener las lineas con tokens
                char ultimoChar;
                String subLinea;
                boolean dotFound = false;
                for (int i = inicioTokens + 1; i < finBusquedaTokens; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        subLinea = linea;
                        // Buscar punto del final
                        while ((i < finBusquedaTokens) && !dotFound){
                            ultimoChar = subLinea.charAt(subLinea.length() - 1);
                            if (ultimoChar == '.'){
                                dotFound = true;
                            } else {
                                // Si no se ha encontrado, hay que concatenar con la linea de abajo
                                i++;
                                subLinea += documento.get(i);
                            }
                        }

                        // Verificar si se encontro
                        if (dotFound){
                            // Intercambiar variables, porque sí
                            linea = subLinea;
                            dotFound = false;
                        } else {
                            errores.add("Error al declarar un token: Hace falta un punto final. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }

                        tokensEnBruto.add(linea);
                    }
                }
            }

            // WHITESPACE: Verificar que se hayan ingresado correctamente los whitespace
            if (whitespace){
                String linea;
                System.out.println("Cargando whitespaces...");

                char ultimoChar;
                String subLinea;
                boolean dotFound = false;
                for (int i = inicioWhiteSpace; i < lineaFinal; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        subLinea = linea;
                        // Buscar punto del final
                        while ((i < lineaFinal) && !dotFound){
                            ultimoChar = subLinea.charAt(subLinea.length() - 1);
                            if (ultimoChar == '.'){
                                dotFound = true;
                            } else {
                                // Si no se ha encontrado, hay que concatenar con la linea de abajo
                                i++;
                                subLinea += documento.get(i);
                            }
                        }

                        // Verificar si se encontro
                        if (dotFound){
                            // Intercambiar variables, porque sí
                            linea = subLinea;
                            dotFound = false;
                        } else {
                            errores.add("Error al declarar un whitespace: Hace falta un punto final. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }

                        whitespaceCorrectly = linea.contains("IGNORE");

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
                            nuevapalabra += ".";
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




    public boolean generateLexer(){
        Pair<Integer, String> result; // Variable de retorno de resultados
        String newRegex;
        // CHARACTERS Regex
        for (String identificador : caracteresKeysOrdered) {

            char[] futureRegex = caracteres.get(identificador).toCharArray();  // Obtener patron en bruto

            // Debe mandarse el indice del final de la linea como parametro, no se manda el último caracter porque es un punto
            result = identifyCharacterRegex(futureRegex, futureRegex.length - 2);
            if (result != null){
                newRegex = result.getValue();
                caracteresRegex.put(identificador, newRegex);
            } else {
                System.err.println("Error: Character mal declarado: '" + identificador + "'.");
                return false;
            }
        }


        // TOKENS regex
        // Por cada lina en bruto
        String tokenIdentificator, tokenRegex;
        for (String posibleToken : tokensEnBruto) {
            if (!posibleToken.equals("")){
                if (posibleToken.contains("=")){
                    // Dividir token
                    String[] tokenBrute = posibleToken.split("=");

                    // Obtener identificador token
                    result = identifyIdentificator(0, tokenBrute[0].toCharArray());
                    if (result != null){
                        tokenIdentificator = result.getValue();

                        // Obtener el regex del cuerpo de token
                        tokenRegex = identifyTokenRegex(tokenBrute[1], tokenIdentificator);

                        // Crear nuevo token
                        if (tokenRegex != null){
                            tokensRegex.put(tokenIdentificator, tokenRegex);
                        } else {
                            System.err.println("Error: Token mal declarado: '" + tokenBrute[1] + "'.");
                            return false;
                        }

                    } else {
                        System.err.println("Error: " + tokenBrute[0] + " no es un identificador correcto para token.");
                        return false;
                    }
                }

                // Es un token solo ident
                else {
                    // Verificar si el ident esta bien esccrito
                    result = identifyIdentificator(0, posibleToken.toCharArray());
                    if (result != null){
                        tokenIdentificator = tokenRegex = result.getValue();
                        tokensRegex.put(tokenIdentificator, tokenRegex);
                    } else {
                        System.err.println("Error: " + posibleToken + " no es un token. Debe utilizar un ident.");
                        return false;
                    }
                }
            }
        }

        // IGNORE palabras
        for (String palabra : palabrasIgnoradas) {
            try {
                char[] conjuntoIgnorar = palabra.toCharArray();
                result = identifyCharacterRegex(conjuntoIgnorar, conjuntoIgnorar.length - 2);
                if (result == null) {
                    System.err.println("Error: Conjunto de whitespace mal declarado: '" + palabra + "'.");
                    return false;
                }
                String nuevaPalabraIgnorar = result.getValue();
                whitespace.add(nuevaPalabraIgnorar);

            } catch (IndexOutOfBoundsException e){
                System.err.println("Error: Conjunto de whitespace mal declarado: '" + palabra + "'.");
                return false;
            }
        }


        return true;
    }


    /**
     * Metodo que tiene como objetivo parsear una linea de declaracion de regex. Para ello, lee la linea de derecha a
     * izquierda de cada declaracion, esto para que el metodo sea recursivo por la izquierda
     * @param futureRegex arreglo con la linea que debe parsearse a un regex
     * @param inicioFutureRegex es el inicio del final de un regex y es el indicador de como se va avanzando en la linea original
     * @return devuelve el regex de un character y el indice de donde termina (Como va de regreso, es el inicio)
     */
    Pair<Integer, String> identifyCharacterRegex(char[] futureRegex, int inicioFutureRegex){
        String characterRegex = "";
        Pair<Integer, String> result;
        String newRegex = "";
        boolean seEncontroSiguienteParte;

        // Analizar metodo de creacion de conjunto por cada letra identificada en COCOL/R
        for (int i = inicioFutureRegex; i >= 0; i--) {
            char letter = futureRegex[i];

            switch (letter){
                // CHARACTER: Termina con comillas, es un string con conjunto caracteres
                case '"':
                    result = getStringRegex(i, futureRegex);
                    // Actualizar contador de arreglo
                    i = result.getKey();
                    // Agregar nuevo conjunto
                    newRegex = result.getValue();
                    break;

                case '\'':
                    // Avanzar al contenido del char
                    i--;

                    // Obtener el contenido
                    newRegex = String.valueOf(futureRegex[i]);

                    // Avanzar uno más para consumir la segunda comilla
                    i--;
                    break;

                case '+':
                    // Avanzar en la declaracion del conjunto
                    i--;

                    // Buscar que es lo que se va a sumar y lo agrega
                    result = identifyCharacterRegex(futureRegex, i);
                    newRegex += "|" + result.getValue();
                    i = result.getKey();
                    break;

                case '-':
                    // Avanzar en la declaracion del conjunto
                    i--;

                    // Guardar lo que se quiere quitar
                    String notDesiredSet = newRegex;

                    // Buscar a lo que se le restara lo que no se quiere
                    result = identifyCharacterRegex(futureRegex, i);
                    if (result == null){
                        return null;
                    }
                    newRegex = result.getValue();
                    i = result.getKey();

                    // Restar ambos conjuntos
                    newRegex = eliminarStringRegex(notDesiredSet.toCharArray(), newRegex);
                    break;

                // Siempre que viene a un punto es por declaracion de un conjunto de chars
                case '.':
                    // Siempre tiene que tener otro punto a la par.
                    i--;
                    letter = futureRegex[i];

                    if (letter == '.'){
                        Pair<Integer, String> resultNewRegex = extractCharRange(i, futureRegex, newRegex);
                        if (resultNewRegex != null){
                            i = resultNewRegex.getKey();
                            newRegex = resultNewRegex.getValue();
                        } else {
                            System.err.println("Error: No se declaro correctamente un rango de characters.");
                            return null;
                        }

                        break;

                    } else {
                        System.err.println("Error: Para crear un conjunto de chars, deben existir dos puntos consecutivos "
                                + "\"..\".");
                        return null;
                    }


                case ' ' : break;
                case '\t': break;

                case ')':  // Podría ser un CHR()
                    String charFinal = "";
                    // Buscar siguiente parentesis
                    boolean seEncontroSegundoParentesis = false;
                    char possible;
                    while (!seEncontroSegundoParentesis){
                        i--;
                        possible = futureRegex[i];
                        if (possible == '('){
                            seEncontroSegundoParentesis = true;
                        } else {
                            charFinal = possible + charFinal;
                        }
                    }

                    // Asegurarse que se trata de un CHR
                    i--;
                    if (futureRegex[i] == 'R'){
                        i--;
                        if (futureRegex[i] == 'H'){
                            i--;
                            if (futureRegex[i] == 'C'){
                                int foundCharValue = Integer.valueOf(charFinal);
                                char foundChar = (char)foundCharValue;
                                charFinal = String.valueOf(foundChar);
                                newRegex = charFinal;
                                break;
                            } else {
                                System.err.println("Error: Para al reconocer un CHR.");
                                return null;
                            }
                        } else {
                            System.err.println("Error: Para al reconocer un CHR.");
                            return null;
                        }
                    } else {
                        System.err.println("Error: Para al reconocer un CHR.");
                        return null;
                    }

                default:
                    // Es un ident
                    // Identificar el identificador del conjunto que se desea sumar
                    Pair<Integer, String> identificadorResult = identifyIdentificatorInverse(i, futureRegex);
                    String identificadorIdentificado;
                    if (identificadorResult != null){
                        i = identificadorResult.getKey();  // Guardar cuanto se avanzo
                        identificadorIdentificado = identificadorResult.getValue();  // identificador
                    } else {
                        System.err.println("Error: Se encontro una variable que no existe.");
                        return null;
                    }

                    // Obtener el regex del identificador
                    String regexDelIdentificadorIdentificado = caracteresRegex.get(identificadorIdentificado);

                    // Verificar que exista
                    if (regexDelIdentificadorIdentificado != null){
                        newRegex += regexDelIdentificadorIdentificado;
                    } else {
                        System.err.println("Error: La variable '" + identificadorIdentificado + "' no existe.");
                        return null;
                    }
                    break;
            }
        }

        return new Pair<Integer, String>(0, newRegex);
    }


    private String identifyTokenRegex(String tokenExpr, String tokenIdent) {
        // Examinar sintax
        boolean goodSintax = verifyPreSyntax(tokenExpr,'[',']');
        if (!goodSintax) return null;

        goodSintax = verifyPreSyntax(tokenExpr,'{','}');
        if (!goodSintax) return null;

        goodSintax = verifyPreSyntax(tokenExpr,'(',')');
        if (!goodSintax) return null;

        // Analizar cada una de las letras
        String newRegex = "";
        int p;
        for(int i = 0; i < tokenExpr.length(); i++){
            char chr = tokenExpr.charAt(i);
            switch(chr){

                // SYMBOL: String
                case '"':
                    i++;
                    chr = tokenExpr.charAt(i);
                    while(chr != '"'){
                        // Agregar cada letra a nuevo regex
                        newRegex += chr;
                        i++;

                        // Verificar si ya termino de leer to do el string
                        if(i == tokenExpr.length()){
                            System.err.println("Error: Cerrar comillas para declaracion de String en Token " + tokenIdent);
                            return null;
                        }

                        // Obtener siguiente letra
                        chr =  tokenExpr.charAt(i);
                    }
                    break;

                // SYMBOL: Char
                case '\'':
                    i++;
                    chr = tokenExpr.charAt(i);
                    // Verificar caracteres de escape
                    if(chr == '\\' && tokenExpr.charAt(i + 2) != '\''){
                        System.err.println("Error: El char no esta declarado correctamente: '\\" + chr + tokenExpr.charAt(i + 1) + ". Se esperaba: '\\" + chr + "'");
                        return null;
                    } else {
                        newRegex += chr;
                        i++;
                    }
                    break;

                // TOKEN EXPR |
                case '|':
                    newRegex += '|';
                    break;

                // FIN TOKEN DLCR
                case '.':
                    // Largo de declaracion de token
                    i = tokenExpr.length();
                    break;

                // TOKEN FACTOR: {}
                case '{':
                    // Verificar que se cierre
                    int cantidadLlaves = 1;

                    // Copiar contador
                    p = i;
                    i++;

                    // Encontrar fin de TOKENEXPR
                    while(cantidadLlaves != 0 && p < tokenExpr.length() - 1){
                        p++;

                        if(tokenExpr.charAt(p) == '{'){
                            cantidadLlaves++;

                        } else if(tokenExpr.charAt(p) == '}'){
                            cantidadLlaves--;
                        }
                    }
                    String newRegexPrev = identifyTokenRegex(tokenExpr.substring(i, p), tokenIdent);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de token dentro de parentesis.");
                        return null;
                    }
                    newRegex += "(" + newRegexPrev + ")*";  // PUEDE DAR OF BY ONEEEEEEEEEEEEEEEEEEEEEEEEEEE

                    // Actualizar contador
                    i = p;
                    break;

                // TOKEN FACTOR: []
                case '[':
                    int cantidadCorchetes=1;
                    p = i;
                    i++;
                    // Encontrar fin de TOKENEXPR en p
                    while(cantidadCorchetes != 0 && p < tokenExpr.length() - 1){
                        p++;

                        if(tokenExpr.charAt(p) == '['){
                            cantidadCorchetes++;

                        } else if(tokenExpr.charAt(p) == ']'){
                            cantidadCorchetes--;
                        }
                    }

                    newRegexPrev = identifyTokenRegex(tokenExpr.substring(i, p), tokenIdent);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de token dentro de parentesis.");
                        return null;
                    }
                    newRegex += "((" + newRegexPrev + ")?)";

                    // Actualizar contador
                    i = p;
                    break;

                // TOKEN FACTOR: ()
                case '(':
                    int cantidadParentesis = 1;
                    p = i;
                    i++;
                    // Encontrar fin de TOKENEXPR en p
                    while(cantidadParentesis != 0 && p < tokenExpr.length() - 1){
                        p++;

                        if(tokenExpr.charAt(p) == '('){
                            cantidadParentesis++;

                        } else if(tokenExpr.charAt(p) == ')'){
                            cantidadParentesis--;
                        }
                    }

                    newRegexPrev = identifyTokenRegex(tokenExpr.substring(i, p), tokenIdent);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de token dentro de parentesis.");
                        return null;
                    }
                    newRegex += "(" + newRegexPrev + ")";

                    // Actualizar contador
                    i = p;
                    break;

                // Ignorar espacios en blanco
                case ' ' : break;
                case '\t': break;

                // SYMBOL: Podria ser un ident identificado antes
                default:
                    Pair<Integer, String> posibleIdentificadorIdentificado = identifyIdentificator(i, tokenExpr.toCharArray());

                    if (posibleIdentificadorIdentificado != null){
                        i = posibleIdentificadorIdentificado.getKey();
                        String posibleIdentificador = posibleIdentificadorIdentificado.getValue();

                        // Obtener regex guardado en charactrs
                        String regexConjuntoIdentificado = caracteresRegex.get(posibleIdentificador);

                        if (regexConjuntoIdentificado != null){
                            newRegex += "(" + regexConjuntoIdentificado + ")";
                        } else {
                            System.err.println("Error: No se ha declarado el identificador: '" + posibleIdentificador + "'.");
                            return null;
                        }


                    } else {
                        System.err.println("Error: No se ha declarado correctamente un identificador en tokens");
                        return null;
                    }

                    break;
            }
        }

        return newRegex;
    }

    private boolean verifyPreSyntax(String tokenExpr, char char1, char char2) {
        int characters = 0;
        char[] tokenExpr1 = tokenExpr.toCharArray();

        // Contar cuantos parentesis/corchetes hay
        for(int n = 0; n < tokenExpr1.length; n++){
            if (tokenExpr1[n] == char1)
                characters++;
            else if (tokenExpr1[n] == char2)
                characters--;
            if(characters < 0){
                System.err.println("Error: Declaracion incorrecta de token.\n\tVerificar los: " + char2 + ".");
                return false;
            }
        }
        if(characters!=0){
            System.err.println("Error: Declaracion incorrecta de token.\n\tVerificar los: " + char1 + ".");
            return false;
        }


        return true;
    }

    private Pair<Integer, String> extractCharRange(int i, char[] futureRegex, String newRegex) {
        // Buscar siguiente iniciador de CHAR
        char letter;
        i--;
        boolean terminarLoop = false;
        String charFinal = "";
        for (int j = i; j >= 0; j--) {
            letter = futureRegex[i];

            switch (letter) {
                case ')':  // Podría ser un CHR()
                    // Buscar siguiente parentesis
                    boolean seEncontroSegundoParentesis = false;
                    char possible;
                    while (!seEncontroSegundoParentesis){
                        i = i - 1;
                        possible = futureRegex[i];
                        if (possible == '('){
                            seEncontroSegundoParentesis = true;
                        } else {
                            charFinal = possible + charFinal;
                        }
                    }

                    // Asegurarse que se trata de un CHR
                    i--;
                    if (futureRegex[i] == 'R'){
                        i--;
                        if (futureRegex[i] == 'H'){
                            i--;
                            if (futureRegex[i] == 'C'){
                                int foundCharValue = Integer.valueOf(charFinal);
                                char foundChar = (char)foundCharValue;
                                charFinal = String.valueOf(foundChar);
                                terminarLoop = true;
                                break;
                            } else return null;
                        } else return null;
                    } else return null;


                case '\'':
                    // Avanzar al contenido del char
                    i--;

                    // Obtener el contenido
                    charFinal = String.valueOf(futureRegex[i]);

                    // Avanzar uno más para consumir la segunda comilla
                    i--;
                    terminarLoop = true;
                    break;
            }

            if (terminarLoop){
                break;
            }
            i--;  // Avanzar en la declaracion del conjunto
        }

        // Obtener todos los characters entre ambos characters
        int charEnd = newRegex.charAt(0);
        int charInicio = charFinal.charAt(0);

        if (charInicio < charEnd){
            // Reiniciar newRegex
            newRegex = "" + (char)charInicio;
            // Agregar cada caracter a nuevo regex
            for (int k = charInicio + 1; k <= charEnd; k++){
                newRegex += "|" + (char)k;
            }
        } else {
            System.err.println("Error: El rango de chars que se desea crear es invalido.");
            return null;
        }
        return new Pair<Integer, String>(i, newRegex);

    }

    /**
     * Metodo que sirve para eliminar un conjunto de caracteres de un conjunto de regex nuevo
     * @param toEliminate es el arreglo de chars en el que se encuentra el conjunto a eliminar
     * @param newRegex es el regex del que se eliminaran los caracteres
     * @return devuelve el nuevo indice para el for y el nuevo regex
     */
    private String eliminarStringRegex(char[] toEliminate, String newRegex) {
        String result = "";
        HashSet<Character> conjuntoAEliminar = new HashSet<Character>();

        // Agregar a conjunto de palabras a eliminar
        for (char aToEliminate : toEliminate) {
            conjuntoAEliminar.add(aToEliminate);
        }

        char[] newRegexChar = newRegex.toCharArray();  // Convertir el nuevo regexanterior a arreglo
        // Copiar cada letra de newRegex, menos las que se desean eliminar
        for (int j = 0; j < newRegex.length(); j++) {
            char letraActual = newRegexChar[j];  // Obtener letra
            boolean seDebeIgnorar = conjuntoAEliminar.contains(letraActual);  // Ver si se debe ignorar
            if (!seDebeIgnorar){
                if (letraActual != '|'){
                    result += letraActual + "|";  // Copiar a nuevo resultado
                }
            }
        }

        result = result.substring(0, result.length() - 1);  // eliminar el ultimo OR que se coloco

        return result;
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
            if (i > futureRegex.length - 1) break;

            char newLetter = futureRegex[i];
            preNewIdentificator += newLetter;  // Agregar nueva letra al posible identificador
            isIdent = simulator.simulateNFA(identAutomata, preNewIdentificator, idenNFAToDFA);  // Verificar si es identificadord

            if (isIdent){
                newIdentificator = preNewIdentificator;
            } else {
                found = true;
            }
            if (!found) i++;  // Aumentar contador
        }
        i--;  // Disminuir en uno el contador por preview
        if (simulator.simulateNFA(identAutomata, newIdentificator, idenNFAToDFA)) return new Pair<Integer, String>(i, newIdentificator);
        else return null;
    }


    /**
     * Metodo que sirve para identificar un identificador, pero desde su final
     * @param i el indice del for principal, donde inicia el identificador en el arreglo
     * @param futureRegex es el arreglo de chars que contiene el regex en bruto
     * @return devuelve el identificador encontrado
     */
    private Pair<Integer,String> identifyIdentificatorInverse(int i, char[] futureRegex) {
        String newIdentificator = "";  // Resultado final
        String preNewIdentificator = "";  // String de prueba
        boolean found = false;
        boolean foundAtLeastOne = false;
        boolean isIdent;  // Para verificar si son identificadores
        while (!foundAtLeastOne){
            if (i < 0) break;

            char newLetter = futureRegex[i];
            preNewIdentificator = newLetter + preNewIdentificator;  // Agregar nueva letra al posible identificador al inicio
            isIdent = simulator.simulateNFA(identAutomata, preNewIdentificator, idenNFAToDFA);  // Verificar si es identificadord

            if (isIdent){
                foundAtLeastOne = true;
            }

            if (!foundAtLeastOne) i--;  // Disminuir contador

        }

        if (foundAtLeastOne){
            i--;
            while(!found){
                if (i < 0) break;

                char newLetter = futureRegex[i];
                preNewIdentificator = newLetter + preNewIdentificator;  // Agregar nueva letra al posible identificador
                isIdent = simulator.simulateNFA(identAutomata, preNewIdentificator, idenNFAToDFA);  // Verificar si es identificador

                if (isIdent){
                    newIdentificator = preNewIdentificator;
                } else {
                    found = true;
                }
                if (!found) i--;  // Disminuir contador
            }
        }

        i++;  // Aumentar en uno el contador por preview
        if (simulator.simulateNFA(identAutomata, newIdentificator, idenNFAToDFA)) return new Pair<Integer, String>(i, newIdentificator);
        else return null;
    }

    /**
     * Metodo que sirve para agregar un conjunto de caracteres a un nuevo regex con ORs
     * @param i es el index del for prinipal, donde termina el string en el arreglo, debe devolver donde inicia
     * @param futureRegex es el arreglo de chars en el que se encuentra el conjunto en bruto
     * @return devuelve el nuevo indice para el for y el nuevo regex
     */
    public Pair<Integer, String> getStringRegex(int i, char[] futureRegex){
        // Avanzar en el retroceso de la declaracion del conjunto
        i--;

        // Variable de resultado
        String newRegex = "";

        // Obtener anterior letra
        char letter = futureRegex[i];

        // Buscar las siguientes comillas e ir creando regex
        while (letter != '"'){
            newRegex += letter;  // Agregar el primer caracter

            // Verificar que no es el ultimo caracter
            char nextLetter = futureRegex[i - 1];
            if(nextLetter != '"'){
                // Agregar OR entre cada letra
                newRegex += "|";
            }

            // Avanzar el retroceso de la declaracion del conjunto
            i--;

            // Obtener siguiente letra
            letter = nextLetter;
        }

        return new Pair<Integer, String>(i, newRegex);
    }


    private DirectedGraph createAutomaton(String regex){
        /**
         * Construccion directa de DFA
         */
        // Procesar regex
        RegExToNFA regExToNFA = new RegExToNFA();
        regex = RegExConverter.infixToPostfix(regex);  // Convertir a postfix
        return regExToNFA.evaluate(regex);

    }


    /**
     * Metodo para leer el archivo Cocol/R, este divide el documento en lineas enteras de instrucciones que terminen en punto.
     * @param filename nombre a de archivo a abrir
     * @return Lista con strings de cada linea del documento
     */
    public static List<String> readFile(String filename)
    {
        List<String> records = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            String partLine;
            while ((line = reader.readLine()) != null)
            {
                // Eliminar espacios en blanco a los lados
                partLine = line.trim();
                if (partLine.contains("/*") || partLine.contains("//")){
                    partLine = deleteComments(partLine);
                }
                records.add(partLine.trim());
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

    /**
     * Metodo que analiza una linea y elimina cualquier comentario que pueda estar dentro de él.
     * @param partLine la linea que podría tener un comentario
     * @return lineas sin comentarios
     */
    private static String deleteComments(String partLine) {
        int commentStart, commentEnd;
        int comillasStart, comillasEnd;
        boolean noDentroDeTexto = false;
        String toEliminate;
        if (partLine.contains("//")){
            commentStart = partLine.indexOf("//");
            commentEnd = partLine.length();
            // Verificar si hay comillas
            if (partLine.contains("\"")){
                comillasStart = partLine.indexOf("\"");

                // Verificar si hay segundas comillas
                comillasEnd = partLine.indexOf("\"", comillasStart + 1);
                if (comillasEnd > -1){
                    if ((commentStart < comillasStart) && (comillasEnd < commentEnd)){
                        // Elimina pedazo de comentario
                        partLine = partLine.substring(0, commentStart);
                    }
                } else {
                    // Elimina pedazo de comentario
                    partLine = partLine.substring(0, commentStart);
                }
            } else {
                // Elimina pedazo de comentario
                partLine = partLine.substring(0, commentStart);
            }
        }


        if (partLine.contains("/*")){
            commentStart = partLine.indexOf("/*");
            if (partLine.contains("*/")){
                commentEnd = partLine.indexOf("*/") + 2;
            } else {
                commentEnd = partLine.length();
            }

            // Verificar que no este dentro de comillas
            if (partLine.contains("\"")){
                comillasStart = partLine.indexOf("\"");

                // Verificar si hay segundas comillas
                comillasEnd = partLine.indexOf("\"", comillasStart + 1);
                if (comillasEnd > -1){
                    if ((commentStart < comillasStart) && (comillasEnd < commentEnd)){
                        // Elimina pedazo de comentario
                        toEliminate = partLine.substring(commentStart, commentEnd);
                        partLine = partLine.replaceAll(toEliminate, "");
                    }

                } else {
                    // Elimina pedazo de comentario
                    toEliminate = partLine.substring(commentStart, commentEnd);
                    partLine = partLine.replaceAll(toEliminate, "");
                }
            } else {
                // Elimina pedazo de comentario
                toEliminate = partLine.substring(commentStart, commentEnd);
                partLine = partLine.replace(toEliminate, "");
            }
        }

        return partLine;
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
        // 1) Characters / Tokens
        Set<String> tokens = tokensRegex.keySet();
        for (String identificador : tokens) {
            programa +=
                    "        tokensTypesAndRegexs.add(new Pair<String, String>(\"" + identificador + "\", \""
                    + tokensRegex.get(identificador) + "\"));\n";
        }

        // 2) Keywords
        Set<String> palabrasReservadas = palabrasReservadasRegex.keySet();
        for (String identificador : palabrasReservadas) {
            programa +=
                    "        tokensTypesAndRegexs.add(new Pair<String, String>(\"" + identificador + "\", \""
                            + palabrasReservadasRegex.get(identificador) + "\"));\n";
        }

        // 3) Whitespace
        if (whitespace.size() > 0){
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
        }


        // Agregar precedencia a regex
        // 1) Keywords
        programa += "\n\n        // Colocar precedencia a keywords\n";
        int precedenceCounter = 1;
        for (String keyword : palabrasReservadas) {
            programa += "        tokenPrecedence.put(\"" + keyword + "\", " + precedenceCounter + ");\n";
            precedenceCounter++;
        }

        // 2) TOKENS
        programa += "\n\n        // Colocar precedencia a resto de tokens\n";
        for (String tokenType : tokens) {
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
