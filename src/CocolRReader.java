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
    private String endDeclaration = " *END  *(" + identRegex + ") *\\.";
    private String charactersDeclaration = " *CHARACTERS *";
    private String keywordsDeclaration = " *KEYWORDS *";

    // CHARACTERS regexs
    private String basicSetRegex = "(" + stringRegex + ")|(" + identRegex + ")|(" + charRegex + ")|(" + charRegex + " * \\.\\. *" + charRegex + ")|(ANY)";
    private String setRegex = "(" + basicSetRegex + ")( *(\\+|-) *(" +  basicSetRegex + "))*";
    private String setDeclRegex = "(" + identRegex + ") *= *" + "(" + setRegex + ") *\\.";

    private String test = "(\\+|-)";

    /**
     * Crear los automatas para verificar cada seccion
     */
    private DirectedGraph compilerDeclarationAutomata = createAutomaton(compilerDeclaration);
    private DirectedGraph charactersDeclarationAutomata = createAutomaton(charactersDeclaration);
    private DirectedGraph setsDeclAutomata = createAutomaton(setDeclRegex);
    private DirectedGraph endDeclarationAutomata = createAutomaton(endDeclaration);



    public CocolRReader(String filename){
        documento = readFile(filename);
        if (documento != null) {
            for (String linea : documento) {

            }
        }
    }

    public boolean verifySintax(){
        System.out.println(setDeclRegex);
        createAutomaton(setDeclRegex);
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
        System.out.println(regex);
        DirectedGraph dfaDirecto = regExToDFA.createDFA(regex);


        /**
         * Simplificar DFA por construccion directa
         */
        HopcroftMinimizator hopcroftMinimizator1 = new HopcroftMinimizator();
        DirectedGraph dfaDirectoSimplificado = hopcroftMinimizator1.minimizateDFA(dfaDirecto);

        /**
         * Simular dfa
         */
        Simulator simulator = new Simulator();
        boolean resultado = simulator.simulateDFA(dfaDirectoSimplificado, "Hola = jamon.");
        System.out.println("Estado de aceptacion: " + resultado);

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
