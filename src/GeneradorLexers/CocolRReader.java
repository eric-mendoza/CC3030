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
public class CocolRReader implements Serializable{
    public CocolRReader(){
        idenNFAToDFA.generateSimpleEClosure(identAutomata);
        simulator = new Simulator();
        grammar = new Grammar();
    }

    // Simulador para automatas
    private Simulator simulator;

    // Gramatica
    private Grammar grammar;

    /**
     * Algunos regex importantes
     */
    private String letterRegex = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_";
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

    private ArrayList<String> whitespaceEnBruto = new ArrayList<String>();  // Se va a guardar solo los caracteres a ignorar en bruto
    private ArrayList<String> whitespace = new ArrayList<String>();  // Cadenas de caracteres que no se toman en cuenta

    private ArrayList<String> productionsEnBruto = new ArrayList<String>();  // Guarda las lineas que contienen producciones


    /**
     * Regex para el lexer a generar
     */
    private HashMap<String, String> caracteresRegex = new HashMap<String, String>();
    private HashMap<String, String> palabrasReservadasRegex = new HashMap<String, String>();  // Se va a guardar <iden, keyword>
    private HashMap<String, String> tokensRegex = new HashMap<String, String>();  // <tokenIden, tokenRegex>
    private ArrayList<String> tokensKeysOrdered = new ArrayList<String>();  // Se guardan las llaves para acceder a ellas en orden despues


    private HashMap<String, ArrayList<String>> productions = new HashMap<String, ArrayList<String>>();  // <head, body>



    /**
     * Metodo que tiene como objetivo verificar que esten correctamente declaradas cada una de las secciones de cocol/r
     * Tambien, guarda las lineas que contienen las declaraciones de los diferentes conjuntos
     * @param filename Documento con especificacion lexica
     * @return True, si la sintax es correcta; False, si existen errores
     */
    public boolean analizeCocolRSyntax(String filename){
        // Regex mas basicos
        String ANY = "(\u0001)|(\u0002)|(\u0003)|(\u0004)|(\u0005)|(\u0006)|(\u0007)" +
                "|\u000E|\u000F|\u0010|\u0011|\u0012|\u0013|\u0014|\u0015|\u0016|\u0017|\u0018|\u0019|\u001A|\u001B|\u001C|\u001D|\u001E|\u001F|( )|(\\!)|\"|#|$|%|&|'|(\\()|(\\))|(\\*)|(\\+)|,|-|(\\.)|/|0|1|2|3|4|5|6|7|8|9|:|;|<|=|>|(\\?)|@|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|[|(\\\\)|]|^|_|`|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|{|(\\|)|}|~" +
                "|\u007F|\u0080|\u0081|\u0082|\u0083|\u0084|\u0085|\u0086|\u0087|\u0088|\u0089|\u008A|\u008B|\u008C|\u008D|\u008E|\u008F|\u0090|\u0091|\u0092|\u0093|\u0094|\u0095|\u0096|\u0097|\u0098|\u0099|\u009A|\u009B" +
                "|\u009C|\u009D|\u009E|\u009F|( )|¡|¢|£|¤|¥|¦|§|¨|©|ª|«|¬|\u00AD|®|¯|°|±|²|³|´|µ|¶|·|¸|¹|º|»|¼|½|¾|¿|À|Á|Â|Ã|Ä|Å|Æ|Ç|È|É|Ê|Ë|Ì|Í|Î|Ï|Ð|Ñ|Ò|Ó|Ô|Õ|Ö|×|Ø|Ù|Ú|Û|Ü|Ý|Þ|ß|à|á|â|ã|ä|å|æ|ç|è|é|ê|ë|ì|í|î|ï|ð|ñ|ò|ó|ô|õ|ö|÷|ø|ù|ú|û|ü|ý|þ";
        caracteresRegex.put("ANY", ANY);

        String anyButQuoteRegex = digitRegex + "|(\\.)|#|$|%|&|/|(\\\")|=|¡|\'|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|(\\+)|(\\?)|(\\!)|(\\|)| |(\\\\)|" + letterRegex;   // El esparcio podria fallar

        /*
         Crear variables para la creacion de automatas de verificacion de sintax
         */
        List<String> documento = readFile(filename);
        if (documento == null){
            return false;
        }

        boolean beginExists = false;
        boolean charactersExist = false;
        boolean keywordsExists = false;
        boolean tokensExists = false;
        boolean whitespaceExists = false;
        boolean productionsExists = false;
        boolean existenErrores = false;
        ArrayList<String> errores = new ArrayList<String>();
        String identInicio = "", identFinal = "";
        boolean identIguales = false;
        boolean endExists = false;



        int posicionLinea = 0;
        int posicionLineaBackup;
        String lineaTemp;
        // Encontrar inicio
        for (posicionLinea = posicionLinea; posicionLinea < documento.size(); posicionLinea++) {
            lineaTemp = documento.get(posicionLinea);
            beginExists = lineaTemp.contains("COMPILER ");  // Devuelve True cuando encuentra COMPILER
            if (beginExists){
                lexerJavaFileName = lineaTemp.split(" ")[1].trim();  // Guardar nombre para futuro lexer
                identInicio = lexerJavaFileName + ".";  // Guarda identificador de inicio. Se le agrego un punto para coincidir con el del final
                break;
            }
        }

            /*
            CHARACTERS
             */
        // Encontrar posicion CHARACTERS declaration
        posicionLineaBackup = posicionLinea;
        for (posicionLinea = posicionLinea + 1; posicionLinea < documento.size(); posicionLinea++) {
            lineaTemp = documento.get(posicionLinea);
            charactersExist = lineaTemp.contains("CHARACTERS"); // Devuelve True cuando encuentra CHARACTERS
            if (charactersExist) {
                break;
            }
        }

        // CHARACTERS: Verificar que se hayan ingresado correctamente los characters
        if (charactersExist){  // Si se encontro esta seccion...
            System.out.println("Cargando characters...");

            // Analizar cada linea
            char ultimoChar;
            StringBuilder subLinea;
            boolean dotFound = false, finished = false;
            int copiaPosicionLinea;
            while (true) {
                // Cargar siguiente linea
                posicionLinea++;
                lineaTemp = documento.get(posicionLinea);
                subLinea = new StringBuilder(lineaTemp);
                copiaPosicionLinea = posicionLinea;  // Para identificar errores en inicio de linea


                // Verificar si se debe terminar
                finished = ((posicionLinea >= documento.size()) || lineaTemp.contains("KEYWORDS") || lineaTemp.contains("TOKENS")
                        || lineaTemp.contains("IGNORE") || lineaTemp.contains("END")  || lineaTemp.contains("PRODUCTIONS"));

                if (finished) break;

                // No tomar en cuenta lineas vacías
                if (!lineaTemp.equals("")){
                    // Buscar punto del final
                    while (!dotFound && !finished){
                        ultimoChar = subLinea.charAt(subLinea.length() - 1);
                        if (ultimoChar == '.'){
                            dotFound = true;
                        } else {
                            // Si no se ha encontrado, hay que concatenar con la linea de abajo
                            posicionLinea++;
                            String nextLine = documento.get(posicionLinea);
                            subLinea.append(nextLine);
                            finished = ((posicionLinea >= documento.size()) || nextLine.contains("KEYWORDS") || nextLine.contains("TOKENS")
                                    || nextLine.contains("IGNORE") || nextLine.contains("END")  || nextLine.contains("PRODUCTIONS"));
                        }
                    }

                    // Verificar si se encontro
                    if (dotFound){
                        // Convertir la linea en la concatenacion de las lineas anteriores
                        lineaTemp = subLinea.toString();
                        dotFound = false;
                    } else {
                        errores.add("Error al declarar un character: Hace falta un punto final. Linea: " + String.valueOf(copiaPosicionLinea + 1));
                        existenErrores = true;
                        break;
                    }

                    // Verificar que exista signo de igual
                    if (lineaTemp.contains("=")) {
                        int signoIgual = lineaTemp.indexOf("=");
                        String[] lineSections = {lineaTemp.substring(0, signoIgual), lineaTemp.substring(signoIgual + 1)};

                        Pair<Integer, String> identificatorCharacterPair = identifyIdentificator(0, lineSections[0].toCharArray());
                        if (identificatorCharacterPair != null){
                            caracteresKeysOrdered.add(identificatorCharacterPair.getValue());
                            caracteres.put(identificatorCharacterPair.getValue(), lineSections[1]);  // Guardar ident y conjunto
                        } else {
                            errores.add("Error al declarar un character. Se utilizo un identificador invalido '" + lineSections[0].trim() + "'. Linea: " + String.valueOf(posicionLinea + 1));
                            existenErrores = true;
                            break;
                        }

                    } else {
                        errores.add("Error al declarar un character. Linea: " + String.valueOf(posicionLinea + 1));
                        existenErrores = true;
                        break;
                    }
                }
            }
        } else {
            posicionLinea = posicionLineaBackup;
        }



            /*
            KEYWORDS
             */
        // Encontrar Keywords Declaration
        posicionLineaBackup = posicionLinea;
        for (; posicionLinea < documento.size(); posicionLinea++) {
            lineaTemp = documento.get(posicionLinea);
            keywordsExists = lineaTemp.contains("KEYWORDS");  // Devuelve True si encuentra KEYWORDS
            if (keywordsExists) {
                break;
            }
        }

        // KEYWORDS: Verificar que se hayan ingresado correctamente los keywords
        if (keywordsExists){  // Si existe esta seccion
            System.out.println("Cargando keywords...");

            char ultimoChar;
            StringBuilder subLinea;
            boolean dotFound = false, finished;
            int copiaPosicionLinea;
            while (true) {
                // Cargar siguiente linea
                posicionLinea++;
                lineaTemp = documento.get(posicionLinea);
                subLinea = new StringBuilder(lineaTemp);

                // Verificar si se debe terminar
                finished = ((posicionLinea >= documento.size()) || lineaTemp.contains("TOKENS")
                        || lineaTemp.contains("IGNORE") || lineaTemp.contains("END") || lineaTemp.contains("PRODUCTIONS"));

                if (finished) break;

                // Si es una linea buena
                if (!lineaTemp.equals("")){
                    // Buscar punto del final
                    copiaPosicionLinea = posicionLinea;  // Para identificar errores en inicio de linea
                    while (!finished && !dotFound){
                        ultimoChar = subLinea.charAt(subLinea.length() - 1);
                        if (ultimoChar == '.'){
                            dotFound = true;
                        } else {
                            // Si no se ha encontrado, hay que concatenar con la linea de abajo
                            posicionLinea++;
                            String nextLine = documento.get(posicionLinea);
                            subLinea.append(nextLine);
                            finished = ((posicionLinea >= documento.size()) || nextLine.contains("TOKENS")
                                    || nextLine.contains("IGNORE") || nextLine.contains("END")  || nextLine.contains("PRODUCTIONS"));
                        }
                    }

                    // Verificar si se encontro
                    if (dotFound){
                        // Convertir la linea concatenada en la linea a analizar
                        lineaTemp = subLinea.toString();
                        dotFound = false;
                    } else {
                        errores.add("Error al declarar un keyword: Hace falta un punto final. Linea: " + String.valueOf(copiaPosicionLinea + 1));
                        existenErrores = true;
                        break;
                    }

                    // Si se encuentra una linea correcta
                    if (lineaTemp.contains("=")) {

                        String[] seccionesLinea = lineaTemp.split("=");
                        // Verificar si se utilizo un identificador valido
                        Pair<Integer, String> identificatorKeywordPair = identifyIdentificator(0, seccionesLinea[0].toCharArray());

                        if (identificatorKeywordPair == null) {
                            errores.add("Error al declarar un keyword. Se utilizó un identificador invalido '" + seccionesLinea[0].trim() + "'. Linea: " + String.valueOf(posicionLinea + 1));
                            existenErrores = true;
                            break;
                        }

                        // Obtener el valor de la palabra reservada
                        String newRegex = seccionesLinea[1];
                        int inicioNewRegex = newRegex.indexOf("\"") + 1;
                        int endNewRegex = newRegex.lastIndexOf("\"");

                        if (inicioNewRegex == (endNewRegex + 1)) {
                            errores.add("Error al declarar el keyword '" + identificatorKeywordPair.getValue() + "', " +
                                    "faltan unas comillas. Linea: " + String.valueOf(posicionLinea + 1));
                            existenErrores = true;
                            break;
                        }

                        newRegex = newRegex.substring(inicioNewRegex, endNewRegex);

                        palabrasReservadasRegex.put(identificatorKeywordPair.getValue(), newRegex);

                    } else {
                        errores.add("Error al declarar un keyword. Linea: " + String.valueOf(posicionLinea + 1));
                        existenErrores = true;
                        break;
                    }

                }
            }
        } else {
            posicionLinea = posicionLineaBackup;
        }


            /*
            TOKEN
             */
        // Encontrar Token declaration
        posicionLineaBackup = posicionLinea;
        for (; posicionLinea < documento.size(); posicionLinea++) {
            tokensExists = documento.get(posicionLinea).contains("TOKENS");
            if (tokensExists){
                break;
            }
        }

        // TOKENS: Guardar las lineas que contienen tokens
        if (tokensExists){
            System.out.println("Cargando tokens...");


            // Obtener las lineas con tokens
            char ultimoChar;
            StringBuilder subLinea;
            boolean dotFound = false, finished;
            int copiaPosicionLinea;
            while (true) {
                // Cargar siguiente linea
                posicionLinea++;
                lineaTemp = documento.get(posicionLinea);

                // Verificar si se debe terminar
                finished = ((posicionLinea >= documento.size()) || lineaTemp.contains("END") || lineaTemp.contains("IGNORE")  ||
                        lineaTemp.contains("PRODUCTIONS"));

                if (finished) break;

                if (!lineaTemp.equals("")){
                    subLinea = new StringBuilder(lineaTemp);
                    // Buscar punto del final
                    copiaPosicionLinea = posicionLinea;  // Para identificar errores en inicio de linea

                    // Ver si necesita un punto
                    if (lineaTemp.contains("=")){
                        while (!finished && !dotFound){
                            ultimoChar = subLinea.charAt(subLinea.length() - 1);
                            if (ultimoChar == '.'){
                                dotFound = true;
                            } else {
                                // Si no se ha encontrado, hay que concatenar con la linea de abajo
                                posicionLinea++;
                                String nextLine = documento.get(posicionLinea);
                                subLinea.append(nextLine);
                                finished = ((posicionLinea >= documento.size()) || nextLine.contains("IGNORE")  ||
                                        nextLine.contains("END")  || nextLine.contains("PRODUCTIONS"));
                            }
                        }

                        // Verificar si se encontro
                        if (dotFound){
                            // Convertir la linea concatenada en la linea a analizar
                            lineaTemp = subLinea.toString();
                            dotFound = false;
                        } else {
                            errores.add("Error al declarar un token: Hace falta un punto final. Linea: " + String.valueOf(copiaPosicionLinea + 1));
                            existenErrores = true;
                            break;
                        }
                    } else {
                        // Es solamente un identificador
                        // Hay que verificar si es valido
                        // Que no tenga punto al final
                        ultimoChar = subLinea.charAt(subLinea.length() - 1);
                        if (ultimoChar == '.'){
                            errores.add("Error al declarar un token. Existe un punto de más. Linea: " + String.valueOf(posicionLinea + 1));
                            existenErrores = true;
                            break;
                        }
                        Pair<Integer, String> resultado = identifyIdentificator(0, lineaTemp.toCharArray());

                        if (resultado != null){
                            // Verificar si existe el identificador
                            boolean exists = caracteres.containsKey(resultado.getValue());
                            if (!exists){
                                errores.add("Error al declarar un token. No existe la variable '" + lineaTemp.trim() + "'. Linea: " + String.valueOf(posicionLinea + 1));
                                existenErrores = true;
                                break;
                            }

                        } else {
                            errores.add("Error al declarar un token. Se utilizó un identificador invalido '" + lineaTemp.trim() + "'. Linea: " + String.valueOf(posicionLinea + 1));
                            existenErrores = true;
                            break;
                        }
                    }

                    // Guardar lineas con token
                    tokensEnBruto.add(lineaTemp);
                }
            }
        } else {
            posicionLinea = posicionLineaBackup;
        }


        /*
            WHITESPACE
        */
        // Encontrar WhiteSpace declaration
        posicionLineaBackup = posicionLinea;
        for (; posicionLinea < documento.size(); posicionLinea++) {
            whitespaceExists = documento.get(posicionLinea).contains("IGNORE");  // Si encuentra IGNORE indica donde esta
            if (whitespaceExists){
                break;
            }
        }


        // WHITESPACE: Verificar que se hayan ingresado correctamente los whitespace
        if (whitespaceExists){
            System.out.println("Cargando whitespaces...");

            char ultimoChar;
            StringBuilder subLinea;
            boolean dotFound = false, finished;
            int copiaPosicionLinea;
            posicionLinea--;  // Se debe disminuir en 1 el contador, porque IGNORE tiene una linea de declaracion
            while (true) {
                // Cargar siguiente linea
                posicionLinea++;
                lineaTemp = documento.get(posicionLinea);
                subLinea = new StringBuilder(lineaTemp);

                // Verificar si se debe terminar
                finished = ((posicionLinea >= documento.size()) ||
                        lineaTemp.contains("END") || lineaTemp.contains("PRODUCTIONS"));

                if (finished) break;

                // Si es una linea buena
                if (!lineaTemp.equals("")){
                    // Buscar punto del final
                    copiaPosicionLinea = posicionLinea;  // Para identificar errores en inicio de linea
                    while (!finished && !dotFound){
                        ultimoChar = subLinea.charAt(subLinea.length() - 1);
                        if (ultimoChar == '.'){
                            dotFound = true;
                        } else {
                            // Si no se ha encontrado, hay que concatenar con la linea de abajo
                            posicionLinea++;
                            String nextLine = documento.get(posicionLinea);
                            subLinea.append(nextLine);
                            finished = ((posicionLinea >= documento.size()) ||
                                    nextLine.contains("END")  || nextLine.contains("PRODUCTIONS"));
                        }
                    }

                    // Verificar si se encontro
                    if (dotFound){
                        // Convertir la linea concatenada en la linea a analizar
                        lineaTemp = subLinea.toString();
                        dotFound = false;
                    } else {
                        errores.add("Error al declarar un whitespace: Hace falta un punto final. Linea: " + String.valueOf(copiaPosicionLinea + 1));
                        existenErrores = true;
                        break;
                    }


                    // Si se encuentra una linea correcta
                    if (lineaTemp.contains("IGNORE")) {
                        String[] seccionesLinea = lineaTemp.split("IGNORE");
                        String palabra = seccionesLinea[1].trim();
                        whitespaceEnBruto.add(palabra);
                    } else {
                        errores.add("Error al declarar un whitespace. Linea: " + String.valueOf(posicionLinea + 1));
                        existenErrores = true;
                        break;
                    }
                }
            }
        } else {
            posicionLinea = posicionLineaBackup;
        }


        /*
            PRODUCTIONS
         */
        // Encontrar Token declaration
        posicionLineaBackup = posicionLinea;
        for (; posicionLinea < documento.size(); posicionLinea++) {
            productionsExists = documento.get(posicionLinea).contains("PRODUCTIONS");
            if (productionsExists){
                break;
            }
        }

        // PRODUCTIONS: Guardar las lineas que contienen tokens
        if (productionsExists){
            System.out.println("Cargando productions...");


            // Obtener las lineas con producciones
            char ultimoChar;
            StringBuilder subLinea;
            boolean dotFound = false, finished;
            int copiaPosicionLinea;
            while (true) {
                // Cargar siguiente linea
                posicionLinea++;
                lineaTemp = documento.get(posicionLinea);

                // Verificar si se debe terminar
                finished = (posicionLinea >= documento.size()) || lineaTemp.contains("END");

                if (finished) break;

                if (!lineaTemp.equals("")){
                    subLinea = new StringBuilder(lineaTemp);

                    // Buscar punto del final
                    copiaPosicionLinea = posicionLinea;  // Para identificar errores en inicio de linea
                    while (!finished && !dotFound){
                        ultimoChar = subLinea.charAt(subLinea.length() - 1);
                        if (ultimoChar == '.'){
                            dotFound = true;
                        } else {
                            // Si no se ha encontrado, hay que concatenar con la linea de abajo
                            posicionLinea++;
                            String nextLine = documento.get(posicionLinea);
                            subLinea.append(nextLine);
                            finished = (posicionLinea >= documento.size()) || nextLine.contains("END");
                        }
                    }

                    // Verificar si se encontro
                    if (dotFound){
                        // Convertir la linea concatenada en la linea a analizar
                        lineaTemp = subLinea.toString();
                        dotFound = false;
                    } else {
                        errores.add("Error al declarar una producción: Hace falta un punto final. Linea: " + String.valueOf(copiaPosicionLinea + 1));
                        existenErrores = true;
                        break;
                    }

                    // Guardar lineas con token
                    productionsEnBruto.add(lineaTemp);
                }
            }
        } else {
            posicionLinea = posicionLineaBackup;
        }



        /*
            END
         */
        // Encontrar END declaration
        for (; posicionLinea < documento.size(); posicionLinea++) {
            lineaTemp = documento.get(posicionLinea);
            endExists = lineaTemp.contains("END");
            if (endExists) {
                identFinal = lineaTemp.split(" ")[1].trim();  // Guarda identificador de final
                break;
            }
        }


        /*
        Revisar y mostrar errores
         */
        if (beginExists && endExists){
            identIguales = identFinal.equals(identInicio);
        }
        if (!identIguales) System.err.println("Error: Los identificadores de inicio y final no coinciden.");
        if (!beginExists) System.err.println("Error: Declaracion de inicio incorrecta");
        if (!endExists) System.err.println("Error: No se colocó correctamente el final del documento");
        if (existenErrores){
            for (String error : errores) {
                System.err.println(error);
            }
        }

        return beginExists && endExists && !existenErrores && identIguales;
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
                    int signoIgual = posibleToken.indexOf("=");
                    String[] tokenBrute = {posibleToken.substring(0, signoIgual), posibleToken.substring(signoIgual + 1)};

                    // Obtener identificador token
                    result = identifyIdentificator(0, tokenBrute[0].toCharArray());
                    if (result != null){
                        tokenIdentificator = result.getValue();

                        // Obtener el regex del cuerpo de token
                        tokenRegex = identifyTokenRegex(tokenBrute[1], tokenIdentificator);

                        // Crear nuevo token
                        if (tokenRegex != null){
                            tokensRegex.put(tokenIdentificator, tokenRegex);
                            tokensKeysOrdered.add(tokenIdentificator);
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
                        tokensKeysOrdered.add(tokenIdentificator);
                    } else {
                        System.err.println("Error: " + posibleToken + " no es un token. Debe utilizar un ident.");
                        return false;
                    }
                }
            }
        }

        // IGNORE palabras
        for (String palabra : whitespaceEnBruto) {
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

        /*
         * TIENE UN ERROR: SI SE INCLUYE ALGO DESPUES DE IDENT NO LO RECONOCE COMO ERROR
         */
        // PRODUCTION regex
        // Por cada lina en bruto
        String productionIdentificator, semAction = "", attribute = "", productionHead;
        ArrayList<String> productionBody;
        for (String posibleProduction : productionsEnBruto) {
            if (posibleProduction.contains("=")){
                // Dividir producción
                int signoIgual = posibleProduction.indexOf("=");
                String[] productionBrute = {posibleProduction.substring(0, signoIgual), posibleProduction.substring(signoIgual + 1)};

                // Obtener cabeza de produccion
                productionHead = productionBrute[0];
                result = identifyIdentificator(0, productionHead.toCharArray());
                if (result != null){
                    // Guardar identificador de produccion
                    productionIdentificator = result.getValue();

                    // Verificar si hay Atributos
                    if (productionHead.contains("<.")){  // Verificar inicio de atributo
                        char letra, siguienteLetra = ' ';
                        int letrasAtributo = productionHead.indexOf("<.") + 2;  // En que parte de cabeza buscar inicio
                        siguienteLetra = productionHead.charAt(letrasAtributo);
                        while (true) {
                            try{
                                letrasAtributo++;
                                letra = siguienteLetra;
                                siguienteLetra = productionHead.charAt(letrasAtributo);

                                // Verificar si encontro final de atributo
                                if (letra == '.' && siguienteLetra == '>'){
                                    break;
                                }

                                attribute += letra;
                            }
                            catch (Exception e){
                                System.err.println("Error: Atributo en production mal declarado: '" + productionHead + "'.");
                                return false;
                            }
                        }
                    }

                    // Verificar si hay Acciones Semánticas
                    if (productionHead.contains("(.")){  // Verificar inicio de atributo
                        char letra, siguienteLetra;
                        int letrasSemAction= productionHead.indexOf("(.") + 2;  // En que parte de cabeza buscar inicio
                        siguienteLetra = productionHead.charAt(letrasSemAction);
                        while (true) {
                            try{
                                letrasSemAction++;
                                letra = siguienteLetra;
                                siguienteLetra = productionHead.charAt(letrasSemAction);

                                // Verificar si encontro final de atributo
                                if (letra == '.' && siguienteLetra == ')'){
                                    break;
                                }

                                semAction += letra;
                            }
                            catch (Exception e){
                                System.err.println("Error: Acción semántica en production mal declarada: '" + productionHead + "'.");
                                return false;
                            }
                        }
                    }

                    // Asegurarse que no se haya dividido en mas de dos pedazos
                    String productionBodyBrute = "";
                    for (int i = 1; i < productionBrute.length; i++) {
                        productionBodyBrute += productionBrute[i];
                    }

                    // Obtener el cuerpo de la produccion
                    productionBody = identifyProductionBody(productionBodyBrute, productionIdentificator);

                    // Crear nueva produccion
                    if (productionBody != null){
                        grammar.addProductions(productionIdentificator, productionBody);
                    } else {
                        System.err.println("Error: La produccion '" + productionIdentificator + "' está mal declarada: '" + productionBrute[1] + "'.");
                        return false;
                    }

                } else {
                    System.err.println("Error: " + productionBrute[0] + " no es un identificador correcto para Production.");
                    return false;
                }
            } else {
                // Error. Production no tiene signo de igual
                System.err.println("Error: La producción '" + posibleProduction + "' no tiene cuerpo.");
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
                        // verificar si es un caracter de escape
                        if (chr == '\\'){
                            // Siempre agregar el siguiente
                            i++;
                            chr = tokenExpr.charAt(i);

                            // Agregar cada letra a nuevo regex
                            newRegex += "\\";
                            newRegex += chr;
                            i++;
                        } else {
                            // Agregar cada letra a nuevo regex
                            newRegex += chr;
                            i++;
                        }

                        // Verificar si no colocaron las dos comilllas
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
                    if(chr == '\\'){
                        if (tokenExpr.charAt(i + 2) != '\''){
                            System.err.println("Error: El char no esta declarado correctamente: '\\" + chr + tokenExpr.charAt(i + 1) + ". Se esperaba: '\\" + chr + "'");
                            return null;
                        } else {
                            i++;
                            newRegex += "\\";
                            newRegex += tokenExpr.charAt(i);
                            i++;
                        }
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
                        System.err.println("Error: Declaración incorrecta de token dentro de llaves.");
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
                        System.err.println("Error: Declaración incorrecta de token dentro de corchetes.");
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
                            System.err.println("Error: La variable '" + posibleIdentificador + "' no existe.");
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


    private ArrayList<String> identifyProductionBody(String productionBody, String productionIdentificator) {
        // Examinar sintax
        boolean goodSintax = verifyPreSyntax(productionBody,'[',']');
        if (!goodSintax) return null;

        goodSintax = verifyPreSyntax(productionBody,'{','}');
        if (!goodSintax) return null;

        goodSintax = verifyPreSyntax(productionBody,'(',')');
        if (!goodSintax) return null;

        // Analizar cada una de las letras
        String newProduction = "";
        ArrayList<String> newProductions = new ArrayList<String>();
        int p;
        for(int i = 0; i < productionBody.length(); i++){
            char chr = productionBody.charAt(i);  // leer nueva letra
            switch(chr){
                // SYMBOL: String
                case '"':
                    i++;
                    chr = productionBody.charAt(i);  // Leer la siguiente letra despues de las comillas
                    newProduction += ' ';
                    while(chr != '"'){
                        // Agregar cada letra a nueva produccion
                        newProduction += chr;
                        i++;

                        // Verificar si ya se llego al final de la produccion y no se encontraron las comillas
                        if(i == productionBody.length()){
                            System.err.println("Error: Cerrar comillas para declaracion de String en producción '" + productionIdentificator + "'.");
                            return null;
                        }

                        // Obtener siguiente letra
                        chr =  productionBody.charAt(i);
                    }

                    // Verificar si hay algun atributo
                    ///////////////////////////////////////////////////PENDIEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEENTE
                    break;

                // SYMBOL: Char
                case '\'':
                    i++;
                    chr = productionBody.charAt(i);
                    // Verificar caracteres de escape
                    if(chr == '\\' && productionBody.charAt(i + 2) != '\''){
                        System.err.println("Error: El char no esta declarado correctamente: '\\" + chr + productionBody.charAt(i + 1) + ". Se esperaba: '\\" + chr + "'");
                        return null;
                    } else {
                        newProduction += " " + String.valueOf(chr);
                        i++;
                    }

                    // Verificar si hay algun atributo
                    ///////////////////////////////////////////////////PENDIEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEENTE
                    break;

                // PRODUCTION TERM |
                case '|':
                    // Tomar lo que se encuentra a la derecha de | y volver a llamar a funcion
                    String subBodyProduction = productionBody.substring(i + 1, productionBody.length());
                    ArrayList<String> subNewProductions = identifyProductionBody(subBodyProduction, productionIdentificator);
                    i = productionBody.length();  // Hacer que se termine el procesamiento
                    if (subNewProductions != null){
                        // Agregar todas las producciones calculadas de la subparte
                        newProductions.addAll(subNewProductions);
                    } else {
                        System.err.println("Error: La producción '" + productionIdentificator + "' tiene un error en la declaracion de su cuerpo");
                        return null;
                    }
                    break;

                // FIN PRODUCTION
                case '.':
                    // Largo de declaracion de token
                    i = productionBody.length();
                    break;

                // PRODUCTION FACTOR: {}
                case '{':
                    // Verificar que se cierre
                    int cantidadLlaves = 1;

                    // Copiar contador
                    p = i;
                    i++;

                    // Encontrar fin de EXPRESSION
                    while(cantidadLlaves != 0 && p < productionBody.length() - 1){
                        p = p + 1;

                        if(productionBody.charAt(p) == '{'){
                            cantidadLlaves++;

                        } else if(productionBody.charAt(p) == '}'){
                            cantidadLlaves--;
                        }
                    }
                    ArrayList<String> newRegexPrev = identifyProductionBody(productionBody.substring(i, p), productionIdentificator);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de producción dentro de llaves. En produccion: " +
                        productionIdentificator + ".");
                        return null;
                    }

                    newProductions.addAll(newRegexPrev);  // AGREGAR LO QUE DEBE HACER LAS LLAVES

                    // Actualizar contador
                    i = p;
                    break;

                // PRODUCTIONS FACTOR: []
                case '[':
                    int cantidadCorchetes = 1;
                    p = i;
                    i++;
                    // Encontrar fin de PRODUCTION EXPRESSION en p
                    while(cantidadCorchetes != 0 && p < productionBody.length() - 1){
                        p = p + 1;

                        if(productionBody.charAt(p) == '['){
                            cantidadCorchetes++;

                        } else if(productionBody.charAt(p) == ']'){
                            cantidadCorchetes--;
                        }
                    }

                    newRegexPrev = identifyProductionBody(productionBody.substring(i, p), productionIdentificator);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de producción dentro de corchetes. En produccion: " +
                                productionIdentificator + ".");
                        return null;
                    }
                    newProductions.addAll(newRegexPrev);  // AGREGAR LO QUE DEBEN HACER LOS CORCHETES

                    // Actualizar contador
                    i = p;
                    break;

                // PRODUCTION EXPRESSION: ()
                case '(':
                    int cantidadParentesis = 1;
                    p = i;
                    i++;
                    // Encontrar fin de TOKENEXPR en p
                    while(cantidadParentesis != 0 && p < productionBody.length() - 1){
                        p = p + 1;

                        if(productionBody.charAt(p) == '('){
                            cantidadParentesis++;

                        } else if(productionBody.charAt(p) == ')'){
                            cantidadParentesis--;
                        }
                    }

                    newRegexPrev = identifyProductionBody(productionBody.substring(i, p), productionIdentificator);
                    if (newRegexPrev == null){
                        System.err.println("Error: Declaración incorrecta de producción dentro de corchetes. En produccion: " +
                                productionIdentificator + ".");
                        return null;
                    }
                    newProductions.addAll(newRegexPrev);

                    // Actualizar contador
                    i = p;
                    break;

                case ' ':
                    break;

                case '<':
                    // SE DEBE AGREGAR SOPORTE PARA ACCIONES SEMANTICAS
                    break;

                // EPSILON
                case '#':
                    newProduction += " \"\"";
                    break;

                // SYMBOL: Podria ser un ident, por lo que se va a guardar en el objeto Grammar
                default:
                    Pair<Integer, String> posibleIdentificadorIdentificado = identifyIdentificator(i, productionBody.toCharArray());

                    if (posibleIdentificadorIdentificado != null){
                        i = posibleIdentificadorIdentificado.getKey();
                        String posibleNonTerminal = posibleIdentificadorIdentificado.getValue();

                        // Agregar a la produccion de ahorita
                        newProduction += " " + posibleNonTerminal;

                        // Revisar si hay algun atributo
                        // PENDIEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEENTE


                    } else {
                        System.err.println("Error: No se ha declarado correctamente un identificador en producciones");
                        return null;
                    }

                    break;
            }
        }

        newProductions.add(newProduction);

        return newProductions;
    }


    /**
     * Metodo que sirve para verificar que los símbolos de agrupamiento se encuentren correctamente posicionados
     * @param expression Es el texto en el que se quieren examinar los signos de agrupacion
     * @param char1 Es el caracter de apertura
     * @param char2 Es el caracter de cierre
     * @return True, si se encuentran balanceados. False, no estan balanceados.
     */
    private boolean verifyPreSyntax(String expression, char char1, char char2) {
        int characters = 0;
        char[] tokenExpr1 = expression.toCharArray();
        char actual, anterior = ' ', siguiente;  // Pare verificar si estan rodeados de quotes

        // Contar cuantos parentesis/corchetes hay
        for(int n = 0; n < tokenExpr1.length; n++){
            actual = tokenExpr1[n];
            if (actual == char1)
                if (n + 1 < tokenExpr1.length){  // Ver si todavia hay un caracter a la derecha
                    siguiente = tokenExpr1[n + 1];
                    if ((siguiente != '\'' || anterior != '\'') && (siguiente != '"' || anterior != '"')){
                        characters++;
                    }
                } else {
                    characters++;
                }

            else if (actual == char2)
                if (n + 1 < tokenExpr1.length){  // Ver si todavia hay un caracter a la derecha
                    siguiente = tokenExpr1[n + 1];
                    if ((siguiente != '\'' || anterior != '\'') && (siguiente != '"' || anterior != '"')){
                        characters--;
                    }
                } else {
                    characters--;
                }
            if(characters < 0){
                System.err.println("Error: Declaracion incorrecta de token.\n\tVerificar los: " + char2 + ".");
                return false;
            }
            anterior = actual;
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
        while (true){
            // Verificar si hay caracter de escape
            if (letter != '"'){
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
            } else {
                char siguiente;
                try {
                    siguiente = futureRegex[i - 1];
                } catch (IndexOutOfBoundsException e){
                    siguiente = ' ';
                }
                if (siguiente != '\\'){
                    break;
                } else {
                    // Agregar a regex
                    newRegex += letter;

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
            }
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

    public void generateLexerJavaFile() throws IOException {
        String programa = "";

        // Agregar imports
        programa +=
                "package GeneratedFiles;\n" +
                "\n" +
                "import GeneradorLexers.*;\n" +
                "import javafx.util.Pair;\n" +
                "import java.io.FileInputStream;\n" +
                "import java.io.ObjectInputStream;\n" +
                "import java.io.IOException;\n" +
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
                "    public Lexer() throws IOException, ClassNotFoundException {\n" +
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
        // Serializar los regexs para evitar caracteres especiales
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("tokensRegex.ser"));
        out.writeObject(tokensRegex);
        out.flush();
        out.close();

        // Agregar codigo a programa para deserealizar
        programa += "\n" +
                "        // Deserealizar los tokens y sus regexs\n" +
                "        ObjectInputStream in = new ObjectInputStream(new FileInputStream(\"tokensRegex.ser\"));\n" +
                "        HashMap<String, String> tokens = (HashMap<String, String>) in.readObject();\n" +
                "        in.close();\n" +
                "\n" +
                "        // Agregar Regex identificados\n" +
                "        for (String identificador : tokens.keySet()) {\n" +
                "            tokensTypesAndRegexs.add(new Pair<String, String>(identificador, tokens.get(identificador)));\n" +
                "        }\n\n";


        // 2) Keywords
        Set<String> palabrasReservadas = palabrasReservadasRegex.keySet();
        for (String identificador : palabrasReservadas) {
            programa +=
                    "        tokensTypesAndRegexs.add(new Pair<String, String>(\"" + identificador + "\", \""
                            + palabrasReservadasRegex.get(identificador) + "\"));\n";
        }

        // 3) Whitespace
        // Serializar whitespace
        out = new ObjectOutputStream(new FileOutputStream("whiteRegex.ser"));
        out.writeObject(whitespace);
        out.flush();
        out.close();

        programa += "\n" +
                "        // Deserealizar el conjunto de whitespace\n" +
                "        in = new ObjectInputStream(new FileInputStream(\"whiteRegex.ser\"));\n" +
                "        ArrayList<String> whitespace = (ArrayList<String>) in.readObject();\n" +
                "        in.close();\n" +
                "\n";


        // Agregar el regex del whitespace
        programa += "\n" +
                "        String myWhiteSpace = \"\";\n" +
                "        if (whitespace.size() > 0){\n" +
                "            for (int i = 0; i < whitespace.size(); i++) {\n" +
                "                String palabra = whitespace.get(i);\n" +
                "                if (i < whitespace.size() - 1){\n" +
                "                    myWhiteSpace += \"(\" + palabra + \")|\";\n" +
                "                } else {\n" +
                "                    myWhiteSpace += \"(\" + palabra + \")\";\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            tokensTypesAndRegexs.add(new Pair<String, String>(\"whitespace\", myWhiteSpace));\n" +
                "        } else {\n" +
                "            myWhiteSpace = \"( )|(\" + (char)10 + \")|(\" + (char)10 + \")\";\n" +
                "            tokensTypesAndRegexs.add(new Pair<String, String>(\"whitespace\", myWhiteSpace));\n" +
                "        }\n";

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
        for (String tokenType : tokensKeysOrdered) {
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
        programa +=
                "\n" +
                        "\n" +
                        "    /**\n" +
                        "     * Funcion que tiene como objetivo reconocer el siguiente token de un programa ingresado\n" +
                        "     * @param tokens es el Arraylist donde se estan guardando todos los tokens del programa\n" +
                        "     * @param programa es el string que contiene todas las lineas del programa\n" +
                        "     * @param inicioLexema es desde donde se iniciara la busqueda de tokens nuevos\n" +
                        "     * @return 1) Actualiza el ArrayList de tokens. 2) Una tupla que contiene: - Si se encontro nu token (true/false) - El nuevo inicio Lexema\n" +
                        "     */\n" +
                        "    public Pair<Boolean, Integer> nextToken(ArrayList<Pair<String, String>> tokens, String programa, Integer inicioLexema) {\n" +
                        "        if (!(inicioLexema >= programa.length())){\n" +
                        "            // Analizar programa\n" +
                        "            Pair<Integer, DirectedGraph.NodeClass> tokenInfo = simulador.simulateNFARecognizor(tokenAutomata, programa, funciones, inicioLexema);\n" +
                        "\n" +
                        "            // Obtener fin de nuevo token\n" +
                        "            int finLexema = tokenInfo.getKey();\n" +
                        "\n" +
                        "            // Obtener el tipo de token encontrado por medio de los estados finales alcanzados\n" +
                        "            DirectedGraph.NodeClass estadoFinal = tokenInfo.getValue();\n" +
                        "\n" +
                        "            // Si sí se encontro una coincidencia\n" +
                        "            if (estadoFinal != null){\n" +
                        "                // Obtener el tipo de token\n" +
                        "                String tokenType = estadoFinal.getTokenType();\n" +
                        "\n" +
                        "                if (!tokenType.equals(\"whitespace\")){\n" +
                        "                    // Si es un token importante, se obtiene el lexema del programa\n" +
                        "                    String lexema = programa.substring(inicioLexema, finLexema + 1);\n" +
                        "\n" +
                        "                    // Se actualiza el nuevo inicioLexema\n" +
                        "                    inicioLexema = finLexema + 1;\n" +
                        "\n" +
                        "                    // Actualizar tokens\n" +
                        "                    tokens.add(new Pair<String, String>(tokenType, lexema));\n" +
                        "\n" +
                        "                    // Devolver resultado\n" +
                        "                    return new Pair<Boolean, Integer>(true, inicioLexema);\n" +
                        "                }\n" +
                        "\n" +
                        "                // Si se encuentra un whitespace\n" +
                        "                else {\n" +
                        "                    // Volver a llamar al metodo, con un nuevo inicio\n" +
                        "                    return nextToken(tokens, programa, finLexema + 1);\n" +
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
                        "\n" +
                        "\n" +
                "\n" +
                "    \n" +
                "    private String charToString(char[] charArray){\n" +
                "        String result = \"\";\n" +
                "        for (int i = 0; i < charArray.length; i++) {\n" +
                "            result += charArray[i];\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n" +
                "\n" +
                "\n" +
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

    public Grammar getGrammar() {
        return grammar;
    }
}
