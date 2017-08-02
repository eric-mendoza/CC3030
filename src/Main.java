
/**
 * La presente clase tiene como objetivo ingresar una expresion regular, y guiar el proceso de transformacion
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 */
public class Main {
    public static void main(String[] args) {
        /**
         * Tomar tiempo
         */
        long startTime = System.currentTimeMillis();
        /**
         * Normaliza la expresion a evaluar y la convierte a postfix
         */
        String regex = RegExConverter.infixToPostfix("1");
        //System.out.println(regex);

        /**
         * Evalua la expresion obteniendo asi un NFA como resultado
         */
        DirectedGraph nfa = RegExEvaluator.evaluate(regex);
        long finishTime = System.currentTimeMillis();  // Tomar tiempo en construir automata

        /**
         * Imprimir las caracteristicas del automata
         */
        //System.out.println(nfa.automataDescription());

        /**
         * Mostrar en pantalla el automata generado
         */
        //AutomataRenderer.renderAutomata(nfa);

        /**
         * Ultimo tiempo
         */

        long tiempo = finishTime - startTime;
        System.out.println("Tiempo es: ");
        System.out.println(tiempo);

    }
}
