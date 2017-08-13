import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * La presente clase tiene como objetivo ingresar una expresion regular, y guiar el proceso de transformacion
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 */
public class Main {
    public static void main(String[] args) throws IOException {
        /**
         * Pedir a usuario que ingrese un regex
         */
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese su expresion regular para construccion directa(No se aceptan ? ni +):");
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

        // Mostrar en pantalla el automata generado
        //AutomataRenderer.renderAutomata(nfa, "NFA");

        /**
         * Imprimir las caracteristicas del automata
         */
        PrintWriter pW = null;
        try {
            pW = new PrintWriter(new File("NFA.txt"));
            pW.printf(nfa.automataDescription());
            pW.printf("Tiempo en generar NFA:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }


        /**
         * Transformar de NFA -> DFA
         */
        startTime = System.nanoTime();
        DirectedGraph dfa = nfaToDFA.convert(nfa);
        finishTime = System.nanoTime();

        System.out.println("Tiempo en transformar NFA a DFA: ");
        System.out.println("\t- " + tiempo + " milisegundos");

        //Mostrar en pantalla el dfa
        //AutomataRenderer.renderAutomata(dfa, "DFA");

        /**
         * Descripcion de dfa
         */
        tiempo = (finishTime - startTime) / 1000000.0;  // Diferencia
        try {
            pW = new PrintWriter(new File("DFA.txt"));
            pW.printf(dfa.automataDescription());
            pW.printf("Tiempo en transformar NFA a DFA:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }


        /**
         * Simular NFA
         */
        /*
        // Crear simulador
        Simulator simulator = new Simulator(nfaToDFA);
        String continuar = "1";

        while (continuar.equals("1")){
            // Pedir ingreso
            System.out.println("\nIngrese cadena a simular en NFA:");
            String cadena;
            cadena = scanner.next();

            // Simular
            startTime = System.nanoTime();  // Tomar tiempos
            boolean resultado = simulator.simulateNFA(nfa, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            if (resultado) System.out.println("\t- Resultado: ACEPTADA");
            else System.out.println("\t- Resultado: RECHAZADA");

            // Mostrar tiempo
            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("\t- Tiempo de simulacion: " + tiempo + " milisegundos\n");

            // Preguntar si desea simular otro
            System.out.println("Si desea simular otra cadena, ingrese \'1\': ");
            continuar = scanner.next();
        } */

        /**
         * Simular DFA no simplificado
         */
        /*
        // Crear simulador
        continuar = "1";

        while (continuar.equals("1")){
            // Pedir ingreso
            System.out.println("\nIngrese cadena a simular en DFA:");
            String cadena;
            cadena = scanner.next();

            // Simular
            startTime = System.nanoTime();
            boolean resultado = simulator.simulateDFA(dfa, cadena);
            finishTime = System.nanoTime();  // Tomar tiempo

            if (resultado) System.out.println("\t- Resultado: ACEPTADA");
            else System.out.println("\t- Resultado: RECHAZADA");

            // Mostrar tiempo
            tiempo = (finishTime - startTime) / 1000000.0;
            System.out.println("\t- Tiempo de simulacion: " + tiempo + " milisegundos\n");

            // Preguntar si desea simular otro
            System.out.println("Si desea simular otra cadena, ingrese \'1\': ");
            continuar = scanner.next();
        }*/

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
        System.out.println("Tiempo de creacion directa de dfa:\n\t- " + tiempo + " milisegundos");

        // Mostrar en pantalla el dfa directo
        //AutomataRenderer.renderAutomata(dfaDirecto, "DFA por construccion directa");

        // Crear descripcion de documento
        pW = null;
        try {
            pW = new PrintWriter(new File("DFA_Construccion_directa.txt"));
            pW.printf(dfaDirecto.automataDescription());
            pW.printf("Tiempo en generar DFA directamente de regex:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

        /**
         * Simplificar DFA
         */
        startTime = System.nanoTime();
        DirectedGraph dfaSimplificado = hopcroftMinimizator.minimizateDFA(dfaDirecto);
        finishTime = System.nanoTime();  // Tomar tiempo

        // Mostrar tiempo
        tiempo = (finishTime - startTime) / 1000000.0;
        System.out.println("Tiempo de simplicacion de DFA:\n\t- " + tiempo + " milisegundos");

        // Mostrar en pantalla el dfa minimizado
        //AutomataRenderer.renderAutomata(dfaSimplificado, "DFA minimizado");

        // Crear descripcion de documento
        pW = null;
        try {
            pW = new PrintWriter(new File("DFA_Minimo.txt"));
            pW.printf(dfaSimplificado.automataDescription());
            pW.printf("\nTiempo de simplicacion de DFA:\n\t- " + tiempo + " milisegundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

    }
}
