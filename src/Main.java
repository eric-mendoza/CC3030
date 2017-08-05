import java.util.HashSet;
import java.util.Scanner;

/**
 * La presente clase tiene como objetivo ingresar una expresion regular, y guiar el proceso de transformacion
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 */
public class Main {
    public static void main(String[] args) {
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
         * Tomar tiempo
         */
        startTime = System.currentTimeMillis();
        /**
         * Normaliza la expresion a evaluar y la convierte a postfix
         */
        regex = RegExConverter.infixToPostfix(regex);
        //System.out.println(regex);

        /**
         * Evalua la expresion obteniendo asi un NFA como resultado
         */
        DirectedGraph nfa = regExToNFA.evaluate(regex);
        finishTime = System.currentTimeMillis();  // Tomar tiempo en construir automata

        /**
         * Imprimir las caracteristicas del automata
         */
        System.out.println(nfa.automataDescription());

        /**
         * Mostrar en pantalla el automata generado
         */
        //AutomataRenderer.renderAutomata(nfa);

        /**
         * Ultimo tiempo generacion NFA
         */
        long tiempo = finishTime - startTime;
        System.out.println("Tiempo es: ");
        System.out.println(tiempo);

        /**
         * Transformar de NFA - DFA
         */
        nfaToDFA.evaluate(nfa);

    }
}
