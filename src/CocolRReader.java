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
    /**
     * Crear variables para la creacion de automatas de verificacion de sintax
     */
    private List<String> documento;

    // Regex mas basicos
    private String letterRegex = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z";
    private String digitRegex = "0|1|2|3|4|5|6|7|8|9";
    private String anyButQuoteRegex = digitRegex + "|\\.|#|$|%|&|/|=|¡|\'|\\\\|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬| |" + letterRegex;
    private String anyButApostropheRegex = digitRegex + "|\\.|#|$|%|&|/|=|¡|\"|\\\\|¿|´|¨|~|{|[|^|}|]|`|-|_|:|,|;|<|>|°|¬| |" + letterRegex;

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
    private String setDeclRegex = "(" + identRegex + ") *= *" + "(" + setRegex + ") *\\.";

    // KEYWORDS regex
    private String keywordDeclRegex = "(" + identRegex + ") *= *(" + stringRegex +") *\\.";

    private String test = "(\\+|-)";

    /**
     * Crear los automatas para verificar cada seccion
     */
    private DirectedGraph compilerDeclarationAutomata = createAutomaton(compilerDeclaration);
    private DirectedGraph charactersDeclarationAutomata = createAutomaton(charactersDeclaration);
    private DirectedGraph keywordsDeclarationAutomata = createAutomaton(keywordsDeclaration);

    private DirectedGraph keywordDeclAutomata = createAutomaton(keywordDeclRegex);
    private DirectedGraph setsDeclAutomata = createAutomaton(setDeclRegex);
    private DirectedGraph endDeclarationAutomata = createAutomaton(endDeclaration);

    // Simulador para automatas
    Simulator simulator = new Simulator();



    public CocolRReader(String filename){
        documento = readFile(filename);
        boolean inicio = false;
        boolean characters = false;
        int inicioCharacters = 0;
        boolean charactersCorrectly = true;
        int inicioKeywords = 0;
        boolean keywords = false;
        boolean keywordsCorrectly = true;
        ArrayList<String[]> ordenAnalisis = new ArrayList<String[]>();

        boolean end = false;

        int lineaInicio = 0;
        int lineaFinal = 0;
        if (documento != null) {
            // Encontrar inicio
            for (int i = 0; i < documento.size(); i++) {
                inicio = simulator.simulateDFA(compilerDeclarationAutomata, documento.get(i));
                if (inicio){
                    lineaInicio = i;
                    break;
                }
            }

            // Encontrar secciones opcionales
            for (int i = lineaInicio; i < documento.size(); i++) {
                characters = characters | simulator.simulateDFA(charactersDeclarationAutomata, documento.get(i));
                if (characters) inicioCharacters = i;

                keywords = keywords | simulator.simulateDFA(keywordsDeclarationAutomata, documento.get(i));
                if (keywords) inicioKeywords = i;

                end = simulator.simulateDFA(endDeclarationAutomata, documento.get(i));
                if (end) {
                    lineaFinal = i;
                    break;
                }
            }

            // Verificar que se hayan ingresado correctamente los keywords y characters
            if (characters){
                String linea;
                for (int i = inicioCharacters; i < inicioKeywords; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        charactersCorrectly = charactersCorrectly && simulator.simulateDFA(setsDeclAutomata, linea);
                    }
                }
            }


            // Verificar que se hayan ingresado correctamente los keywords y characters
            if (keywords){
                String linea;
                for (int i = inicioKeywords; i < lineaFinal; i++) {
                    linea = documento.get(i);
                    if (!linea.equals("")){
                        keywordsCorrectly = keywordsCorrectly && simulator.simulateDFA(keywordDeclAutomata, linea);
                    }
                }
            }


        }

        if (!inicio) System.out.println("Error: Declaracion de inicio incorrecta");
        if (!end) System.out.println("Error: No se coloco correctamente el final del documento");
        if (keywords && !keywordsCorrectly) System.out.println("Error: Se ingresaron incorrectamente los keywords");
        if (characters && !charactersCorrectly) System.out.println("Error: Se ingresaron incorrectamente los characters");
    }

    public boolean verifySintax(){
        //System.out.println(setDeclRegex);
        //createAutomaton(setDeclRegex);

        // Probar keywords
        boolean resultado = simulator.simulateDFA(keywordDeclAutomata, "while  \"while\".");
        System.out.println("Estado de keyword: " + resultado);

        // Probar characters
        resultado = simulator.simulateDFA(setsDeclAutomata, "digit = \'a\'.");
        System.out.println("Estado de characters: " + resultado);

        return false;
    }

    private DirectedGraph createAutomaton(String regex){
        /**
         * Construccion directa de DFA
         */
        // Procesar regex
        RegExToDFA regExToDFA = new RegExToDFA();
        regex = regExToDFA.augmentateRegex(regex);  // Agregar eof al regex
        regex = RegExConverter.infixToPostfix(regex);  // Convertir a postfix
        DirectedGraph dfaDirecto = regExToDFA.createDFA(regex);


        /**
         * Simplificar DFA por construccion directa
         */
        HopcroftMinimizator hopcroftMinimizator1 = new HopcroftMinimizator();
        DirectedGraph dfaDirectoSimplificado = hopcroftMinimizator1.minimizateDFA(dfaDirecto);

        /**
         * Simular dfa
         */
        //simulator = new Simulator();
        //boolean resultado = simulator.simulateDFA(dfaDirectoSimplificado, "digit = ANY.");
        //System.out.println("Estado de aceptacion: " + resultado);

        /**
         * Graficar para verificar
         */
        //AutomataRenderer.renderAutomata(dfaDirectoSimplificado, "DFA (Por construccion directa) minimizado");

        return dfaDirectoSimplificado;

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
