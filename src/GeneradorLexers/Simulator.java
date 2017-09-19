package GeneradorLexers;

import javafx.util.Pair;

import java.util.HashSet;
import java.util.Stack;

/**
 * La presente clase tiene como objetivo simular un DFA o NFA
 * @author Eric Mendoza
 * @version 1.0
 * @since 5/08/207
 */
public class Simulator {

    /**
     * Constructor
     */
    public Simulator(){
    }

    public boolean simulateNFA(DirectedGraph nfa, String expr){
        /*
            Atributos de simulador
        */
        // Funciones
        NFAToDFA nfaToDFA = new NFAToDFA();

        // Eclosure
        nfaToDFA.generateSimpleEClosure(nfa);

        // Obtener estado inicial de nfa
        DirectedGraph.NodeClass nodoInicial = nfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = nfa.getOneFinalNode();

        // Obtener conjunto de estados iniciales
        HashSet<DirectedGraph.NodeClass> currentStates = nfaToDFA.getEClosure(nodoInicial);
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Moverse hacia siguiente conjunto de estados
            currentStates = nfaToDFA.moveT(currentStates, String.valueOf(c));
        }

        return currentStates.contains(nodoFinal);
    }

    /**
     * Este método tiene como objetivo reconocer tokens. Guarda todos los conjuntos de estados por los que pasa en un stack
     * y se detiene cuando el conjunto de nuevos estados está vacío.
     * @param nfa es el automata reconocedor de tokens, tiene varios finales
     * @param programa es el string del programa completo que se utilizara para encontrar tokens
     * @param nfaToDFA es un objeto que contiene el e-closure del automata generado
     * @param inicioLexema indica el inicio del lexema a encontrar
     * @return devuelve un par, siendo el primer valor el final del lexema y el otroun conjunto de estados finales.
     */
    public Pair<Integer, HashSet<DirectedGraph.NodeClass>> simulateNFARecognizor(DirectedGraph nfa, String programa, NFAToDFA nfaToDFA, int inicioLexema){
        // Crear historial de conjunto de estados
        Stack<HashSet<DirectedGraph.NodeClass>> statesHistorial = new Stack<HashSet<DirectedGraph.NodeClass>>();

        // Copiar inicio de lexema
        int i = inicioLexema;

        // Obtener estado inicial de nfa
        DirectedGraph.NodeClass nodoInicial = nfa.getOneInicialNode();

        // Obtener conjunto de estados iniciales
        HashSet<DirectedGraph.NodeClass> currentStates = nfaToDFA.getEClosure(nodoInicial);

        // Iniciar simulación
        char c;
        while (!currentStates.isEmpty()) {
            // Guardar conjunto de estados anterior
            statesHistorial.push(currentStates);

            // Acceder a inicio de lexema
            if (i < programa.length()){
                c = programa.charAt(i);

                // Moverse hacia siguiente conjunto de estados
                currentStates = nfaToDFA.moveT(currentStates, String.valueOf(c));
            } else {
                break;
            }
            i++;
        }

        // Verificar los conjuntos de estados finales en historial
        boolean finalFound = false;
        HashSet<DirectedGraph.NodeClass> estadosFinales = new HashSet<DirectedGraph.NodeClass>();
        while (!finalFound){
            // Terminar ciclo si ya se revisaron todos los conjuntos del historial
            if (statesHistorial.isEmpty()){
                break;
            }

            // Obtener el ultimo conjunto de estados
            currentStates = statesHistorial.pop();

            // Revisar todos los estados y ver si hay uno final
            for (DirectedGraph.NodeClass estado : currentStates) {
                if (estado.isFinal()){
                    // Ya no regresar mas en el historial
                    finalFound = true;

                    // Agregar estado final a conjunto de estados finales
                    estadosFinales.add(estado);
                }
            }
        }

        // Actualizar variable finLexema dependiendo si encontro final
        int finLexema;
        if (finalFound){
            finLexema = inicioLexema + statesHistorial.size() - 1;
        } else {
            finLexema = i - 1;
        }

        return new Pair<Integer, HashSet<DirectedGraph.NodeClass>>(finLexema, estadosFinales);
    }


    // Tiene el nfaToDFA ya hecho con eclosure
    public boolean simulateNFA(DirectedGraph nfa, String expr, NFAToDFA nfaToDFA){
        // Obtener estado inicial de nfa
        DirectedGraph.NodeClass nodoInicial = nfa.getOneInicialNode();

        // Obtener nodo final de nfa
        DirectedGraph.NodeClass nodoFinal = nfa.getOneFinalNode();

        // Obtener conjunto de estados iniciales
        HashSet<DirectedGraph.NodeClass> currentStates = nfaToDFA.getEClosure(nodoInicial);
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Moverse hacia siguiente conjunto de estados
            currentStates = nfaToDFA.moveT(currentStates, String.valueOf(c));
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
                currentState = move(currentState, letra);
            }

            else return false;
        }

        return currentState.isFinal();
    }

    private DirectedGraph.NodeClass move(DirectedGraph.NodeClass nodo, String symbol){
        // Analizar cada una de las transiciones del estado
        for (DirectedGraph.edgeContents transicion: nodo.edges) {
            // Si una transicion es igual a el simbolo, retornar estado
            if (transicion.getTransition().equals(symbol)){
                return transicion.getFinishingNode();
            }
        }

        return null;
    }
}
