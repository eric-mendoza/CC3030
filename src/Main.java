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
        System.out.println("Ingrese su expresion regular (*|?+):");
        String regex;
        regex = scanner.next();

        /**
         * Crear objetos para procesos
         */
        RegExToNFA regExToNFA = new RegExToNFA();  // Conversor de regex a NFA
        NFAToDFA nfaToDFA = new NFAToDFA();
        long startTime, finishTime;

        /**
         * Normaliza la expresion a evaluar y la convierte a postfix
         */
        regex = RegExConverter.infixToPostfix(regex);
        //System.out.println(regex);

        /**
         * Evalua la expresion obteniendo asi un NFA como resultado
         */
        startTime = System.nanoTime();  // Tomar tiempo
        DirectedGraph nfa = regExToNFA.evaluate(regex);
        finishTime = System.nanoTime();  // Tomar tiempo
        double tiempo = (finishTime - startTime) / 1000000.0;  // Diferencia

        /**
         * Imprimir las caracteristicas del automata
         */
        PrintWriter pW = null;
        try {
            pW = new PrintWriter(new File("NFA.txt"));
            pW.printf(nfa.automataDescription());
            pW.printf("\nTiempo en generar NFA: " + tiempo + " milisegundos");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

        /**
         * Ultimo tiempo generacion NFA
         */
        System.out.println("Tiempo en crear NFA: ");
        System.out.println(tiempo + " milisegundos");

        /**
         * Mostrar en pantalla el automata generado
         */
        //AutomataRenderer.renderAutomata(nfa, "NFA");

        /**
         * Transformar de NFA -> DFA
         */
        startTime = System.nanoTime();
        DirectedGraph dfa = nfaToDFA.convert(nfa);
        finishTime = System.nanoTime();

        /**
         * Descripcion de dfa
         */
        tiempo = (finishTime - startTime) / 1000000.0;  // Diferencia
        try {
            pW = new PrintWriter(new File("DFA.txt"));
            pW.printf(dfa.automataDescription());
            pW.printf("\nTiempo en transformar NFA a DFA: " + tiempo + " milisegundos");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pW != null) {
                pW.close();
            }
        }

        /**
         * Ultimo tiempo generacion NFA
         */
        System.out.println("Tiempo en transformar NFA a DFA: ");
        System.out.println(tiempo + " milisegundos");

        /**
         * Mostrar en pantalla el dfa
         */
        //AutomataRenderer.renderAutomata(dfa, "DFA");

        /**
         * Simular NFA
         */
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
        }

        /**
         * Simular DFA
         */
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
        }
    }
}
