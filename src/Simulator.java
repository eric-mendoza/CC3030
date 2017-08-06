import java.util.HashSet;

/**
 * La presente clase tiene como objetivo simular un DFA o NFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 5/08/207
 */
public class Simulator {
    /**
     * Atributos de simulador
     */
    private NFAToDFA funciones;

    /**
     * Constructor
     * @param nfaToDFA para obtener e-closures
     */
    public Simulator(NFAToDFA nfaToDFA){
        funciones = nfaToDFA;
    }

    public boolean simulateNFA(DirectedGraph nfa, String expr){
        // Obtener estado inicial de nfa
        DirectedGraph.NodeClass nodoInicial = nfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = nfa.getOneFinalNode();

        // Obtener conjunto de estados iniciales
        HashSet<DirectedGraph.NodeClass> currentStates = funciones.getEClosure(nodoInicial);
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Moverse hacia siguiente conjunto de estados
            currentStates = funciones.moveT(currentStates, String.valueOf(c));
        }

        return currentStates.contains(nodoFinal);
    }

    public boolean simulateDFA(DirectedGraph dfa, String expr){
        // Obtener estado inicial de dfa
        DirectedGraph.NodeClass nodoInicial = dfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = dfa.getOneFinalNode();

        // Fijar estado inicial
        DirectedGraph.NodeClass currentState = nodoInicial;

        // Obtener alfabeto
        HashSet<String> alphabet = dfa.getAlphabet();

        // Recorrer automata
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            String letra = String.valueOf(c);

            if (alphabet.contains(letra)){
                // Moverse hacia siguiente conjunto de estados
                currentState = funciones.move(currentState, letra);
            }

            else return false;
        }

        return currentState.isFinal();
    }
}
