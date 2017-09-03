import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * La presente clase tiene como objetivo probar los diferentes metodos de creacion de automatas
 * @author Eric Mendoza
 * @version 1.0
 * @since 2/09/2017
 */
public class TestAutomatonCreators {
    public static void runTest(){
        /**
         * Pedir a usuario que ingrese un regex
         */
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese su expresion regular para construccion directa(! es epsilon):");
        String regex, inputRegex;
        inputRegex = scanner.next();

        /**
         * Crear objetos para procesos
         */
        RegExToNFA regExToNFA = new RegExToNFA();  // Conversor de regex a NFA
        NFAToDFA nfaToDFA = new NFAToDFA();
        HopcroftMinimizator hopcroftMinimizator = new HopcroftMinimizator();
        RegExToDFA regExToDFA = new RegExToDFA();
        long startTime, finishTime;

        /**
         * Normalizar expresion a evaluar y conviertir a postfix
         */
        regex = RegExConverter.infixToPostfix(inputRegex);
        //System.out.println(regex);

        /**
         * Crear NFA
         */
        startTime = System.nanoTime();  // Tomar tiempo
        DirectedGraph nfa = regExToNFA.evaluate(regex);
        finishTime = System.nanoTime();  // Tomar tiempo

        double tiempo = (finishTime - startTime) / 1000000.0;  // Diferencia
        System.out.println("Tiempo en crear NFA: ");
        System.out.println("\t- " + tiempo + " milisegundos");

        /**
         * Imprimir las caracteristicas del automata
         */
        PrintWriter pW = null;
        try {
            pW = new PrintWriter(new File("NFA.txt"));
            pW.printf(nfa.automataDescription());
            pW.printf("\nTiempo en generar NFA:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }


        /**
         * Transformar de NFA -> DFA por subconjuntos
         */
        startTime = System.nanoTime();
        DirectedGraph dfa = nfaToDFA.convert(nfa);
        finishTime = System.nanoTime();

        System.out.println("Tiempo en transformar NFA a DFA (Por subconjuntos): ");
        System.out.println("\t- " + tiempo + " milisegundos");

        /**
         * Descripcion de dfa
         */
        tiempo = (finishTime - startTime) / 1000000.0;  // Diferencia
        try {
            pW = new PrintWriter(new File("DFA.txt"));
            pW.printf(dfa.automataDescription());
            pW.printf("\nTiempo en transformar NFA a DFA (Por subconjuntos):\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

        /**
         * Simplificar DFA construido por subconjuntos
         */
        startTime = System.nanoTime();
        DirectedGraph dfaSimplificado = hopcroftMinimizator.minimizateDFA(dfa);
        finishTime = System.nanoTime();  // Tomar tiempo

        // Mostrar tiempo
        tiempo = (finishTime - startTime) / 1000000.0;
        System.out.println("Tiempo de simplicacion de DFA (Por subconjuntos):\n\t- " + tiempo + " milisegundos");


        // Crear descripcion de documento
        pW = null;
        try {
            pW = new PrintWriter(new File("DFA_Por_Subconjuntos.txt"));
            pW.printf(dfaSimplificado.automataDescription());
            pW.printf("\nTiempo de simplicacion de DFA (Por subconjuntos):\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }


        /**
         * Construccion directa de DFA
         */
        // Procesar regex
        regex = regExToDFA.augmentateRegex(inputRegex);  // Agregar eof al regex
        regex = RegExConverter.infixToPostfix(regex);  // Convertir a postfix

        startTime = System.nanoTime();
        DirectedGraph dfaDirecto = regExToDFA.createDFA(regex);
        finishTime = System.nanoTime();  // Tomar tiempo

        // Mostrar tiempo
        tiempo = (finishTime - startTime) / 1000000.0;
        System.out.println("Tiempo de creacion de DFA por construccion directa:\n\t- " + tiempo + " milisegundos");


        // Crear descripcion de documento
        pW = null;
        try {
            pW = new PrintWriter(new File("DFA_Construccion_directa.txt"));
            pW.printf(dfaDirecto.automataDescription());
            pW.printf("\nTiempo en generar DFA por construccion directa:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

        /**
         * Simplificar DFA por construccion directa
         */
        HopcroftMinimizator hopcroftMinimizator1 = new HopcroftMinimizator();
        startTime = System.nanoTime();
        DirectedGraph dfaDirectoSimplificado = hopcroftMinimizator1.minimizateDFA(dfaDirecto);
        finishTime = System.nanoTime();  // Tomar tiempo

        // Mostrar tiempo
        tiempo = (finishTime - startTime) / 1000000.0;
        System.out.println("Tiempo de simplicacion de DFA (Por construccion directa):\n\t- " + tiempo + " milisegundos");

        // Crear descripcion de documento
        pW = null;
        try {
            pW = new PrintWriter(new File("DFA_Minimo.txt"));
            pW.printf(dfaDirectoSimplificado.automataDescription());
            pW.printf("\nTiempo de simplicacion de DFA (Construccion Directa):\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }



        /**
         * Simular todos los automatas NFA, DFA, DFADirecto, DFAMinimizado, DFADirectoMinimizado
         */

        // Crear simulador
        Simulator simulator = new Simulator();
        String continuar = "1";

        while (continuar.equals("1")){
            // Pedir ingreso
            System.out.println("\nIngrese cadena a simular en todos los automatas:");
            String cadena;
            cadena = scanner.next();

            // Simular NFA
            startTime = System.nanoTime();  // Tomar tiempos
            boolean resultado = simulator.simulateNFA(nfa, cadena, nfaToDFA);
            finishTime = System.nanoTime();  // Tomar tiempo

            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("Tiempo de simulacion de NFA: " + tiempo + " milisegundos");

            if (resultado) System.out.println("\t- Resultado: ACEPTADA\n");
            else System.out.println("\t- Resultado: RECHAZADA\n");


            // Simular DFA
            startTime = System.nanoTime();  // Tomar tiempos
            resultado = simulator.simulateDFA(dfa, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("Tiempo de simulacion de DFA (Por subconjuntos): " + tiempo + " milisegundos");

            if (resultado) System.out.println("\t- Resultado: ACEPTADA\n");
            else System.out.println("\t- Resultado: RECHAZADA\n");

            // Simular DFA minimizado
            startTime = System.nanoTime();  // Tomar tiempos
            resultado = simulator.simulateDFA(dfaSimplificado, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("Tiempo de simulacion de DFA minimo (Por subconjuntos): " + tiempo + " milisegundos");

            if (resultado) System.out.println("\t- Resultado: ACEPTADA\n");
            else System.out.println("\t- Resultado: RECHAZADA\n");

            // Simular DFA por construccion directa
            startTime = System.nanoTime();  // Tomar tiempos
            resultado = simulator.simulateDFA(dfaDirecto, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("Tiempo de simulacion de DFA (Construccion directa): " + tiempo + " milisegundos");

            if (resultado) System.out.println("\t- Resultado: ACEPTADA\n");
            else System.out.println("\t- Resultado: RECHAZADA\n");

            // Simular DFA minimo por construccion directa
            startTime = System.nanoTime();  // Tomar tiempos
            resultado = simulator.simulateDFA(dfaDirectoSimplificado, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("Tiempo de simulacion de DFA minimo (Construccion directa): " + tiempo + " milisegundos");

            if (resultado) System.out.println("\t- Resultado: ACEPTADA\n");
            else System.out.println("\t- Resultado: RECHAZADA\n");

            // Preguntar si desea simular otro
            System.out.println("Si desea simular otra cadena, ingrese \'1\': ");
            continuar = scanner.next();
        }

        /**
         * Mostrar Automatas
         */
        AutomataRenderer.renderAutomata(nfa, "NFA");
        AutomataRenderer.renderAutomata(dfa, "DFA por subconjuntos");
        AutomataRenderer.renderAutomata(dfaSimplificado, "DFA (Por subconjuntos) minimizado");
        AutomataRenderer.renderAutomata(dfaDirecto, "DFA por construccion directa");
        AutomataRenderer.renderAutomata(dfaDirectoSimplificado, "DFA (Por construccion directa) minimizado");
    }
}
