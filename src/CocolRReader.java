import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * La presente clase tiene como objetivo leer un archivo Cocol/R y realizar diferentes funciones
 * @author Eric Mendoza
 * @version 1.0
 * @since 2/09/2017
 */
public class CocolRReader {
    // Regex mas basicos
    private String letterRegex = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z";
    private String digitRegex = "0|1|2|3|4|5|6|7|8|9";
    private String anyButQuoteRegex = digitRegex + "|\\.|#|$|%|&|/|=|¡|\'|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|\\+|\\?|\\!|\\||" + letterRegex;
    private String anyButApostropheRegex = digitRegex + "|\\.|#|$|%|&|/|=|¡|\"|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬|\\+|\\?|\\!|\\||" + letterRegex;

    // Vocabulary
    private String identRegex = "(" + letterRegex + ")((" + letterRegex + ")|(" + digitRegex + "))*";
    private String numberRegex = "(" + digitRegex + ")(" + digitRegex + ")*";
    private String stringRegex = "\"(" + anyButQuoteRegex + ")*\"";
    private String charRegex = "'(" + anyButApostropheRegex + ")'";

    // Regex de estructura
    private String compilerDeclaration = " *COMPILER  *(" + identRegex + ") *";
    private String endDeclaration = " *END  *(" + identRegex + ") *\\. *";
    private String charactersDeclaration = " *CHARACTERS *";
    private String keywordsDeclaration = " *KEYWORDS *";

    // CHARACTERS regexs
    private String basicSetRegex = "(" + stringRegex + ")|(" + identRegex + ")|(" + charRegex + ")|(" + charRegex + " * \\.\\. *" + charRegex + ")|(ANY)";
    private String setRegex = "(" + basicSetRegex + ")( *(\\+|-) *(" +  basicSetRegex + "))*";
    private String setDeclRegex = " *(" + identRegex + ") *= *" + "(" + setRegex + ") *\\. *";

    // KEYWORDS regex
    private String keywordDeclRegex = "(" + identRegex + ") *= *(" + stringRegex +") *\\.";

    // WhiteSpace
    private String whiteSpaceDecl = " *IGNORE *(" + setRegex + ") *\\. *";

    /**
     * Crear los automatas para verificar cada seccion
     */
    private DirectedGraph compilerDeclarationAutomata = createAutomaton(compilerDeclaration);
    private DirectedGraph charactersDeclarationAutomata = createAutomaton(charactersDeclaration);
    private DirectedGraph keywordsDeclarationAutomata = createAutomaton(keywordsDeclaration);

    private DirectedGraph keywordDeclAutomata = createAutomaton(keywordDeclRegex);
    private DirectedGraph setsDeclAutomata = createAutomaton(setDeclRegex);
    private DirectedGraph endDeclarationAutomata = createAutomaton(endDeclaration);
    private DirectedGraph whitespaceDeclarationAutomata = createAutomaton(whiteSpaceDecl);

    // Simulador para automatas
    private Simulator simulator = new Simulator();




    public boolean analizeCocolRSyntax(String filename){
        /*
      Crear variables para la creacion de automatas de verificacion de sintax
     */
        List<String> documento = readFile(filename);

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

        boolean end = false;

        int lineaInicio = 0;
        int lineaFinal = 0;
        if (documento != null) {
            // Encontrar inicio
            for (int i = 0; i < documento.size(); i++) {
                inicio = simulator.simulateNFA(compilerDeclarationAutomata, documento.get(i));
                if (inicio){
                    lineaInicio = i;
                    break;
                }
            }

            // Encontrar posicion CHARACTERS declaration
            for (int i = lineaInicio; i < documento.size(); i++) {
                characters = simulator.simulateNFA(charactersDeclarationAutomata, documento.get(i));
                if (characters) {
                    inicioCharacters = i;
                    break;
                }
            }

            // Encontrar Keywords Declaration
            int inicioBusquedaKeywords;

            if (characters){  // Ahorrarse un par de lineas de busqueda
                inicioBusquedaKeywords = inicioCharacters;
            } else {
                inicioBusquedaKeywords = lineaInicio;
            }

            for (int i = inicioBusquedaKeywords; i < documento.size(); i++) {
                keywords = simulator.simulateNFA(keywordsDeclarationAutomata, documento.get(i));
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
                whitespace = simulator.simulateNFA(whitespaceDeclarationAutomata, documento.get(i));
                if (whitespace){
                    inicioBusquedaWhiteSpace = i;
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
                end = simulator.simulateNFA(endDeclarationAutomata, documento.get(i));
                if (end) {
                    lineaFinal = i;
                    break;
                }
            }


            // Verificar que se hayan ingresado correctamente los characters
            if (characters){
                String linea;

                // establecer final de loop
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
                        charactersCorrectly = simulator.simulateNFA(setsDeclAutomata, linea);

                        // Si se encuentra una linea incorrecta
                        if (!charactersCorrectly) {
                            errores.add("Error al declarar un character. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }
                    }
                }
            }


            // Verificar que se hayan ingresado correctamente los keywords
            if (keywords){
                String linea;

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
                        if (!keywordsCorrectly) {
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

                for (int i = inicioBusquedaWhiteSpace; i < lineaFinal; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        whitespaceCorrectly = simulator.simulateNFA(whitespaceDeclarationAutomata, linea);

                        // Si se encuentra una linea incorrecta
                        if (!whitespaceCorrectly) {
                            errores.add("Error al declarar un whitespace. Linea: " + String.valueOf(i + 1));
                            existenErrores = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!inicio) System.out.println("Error: Declaracion de inicio incorrecta");
        if (!end) System.out.println("Error: No se coloco correctamente el final del documento");
        if (existenErrores){
            for (String error : errores) {
                System.out.println(error);
            }
        }

        return inicio && end && !existenErrores;
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
        //HopcroftMinimizator hopcroftMinimizator1 = new HopcroftMinimizator();
        //DirectedGraph dfaDirectoSimplificado = hopcroftMinimizator1.minimizateDFA(dfaDirecto);

        /**
         * Simular dfa
         */
        //simulator = new Simulator();
        //boolean resultado = simulator.simulateNFA(dfaDirectoSimplificado, "digit = ANY.");
        //System.out.println("Estado de aceptacion: " + resultado);

        /**
         * Graficar para verificar
         */
        //AutomataRenderer.renderAutomata(dfaDirectoSimplificado, "DFA (Por construccion directa) minimizado");

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
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return null;
        }
    }
}
