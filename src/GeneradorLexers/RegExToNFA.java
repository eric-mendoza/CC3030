package GeneradorLexers;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

/**
 * La presente clase tiene como objetivo crear un automata no determinista a partir de un regex
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 */
public class RegExToNFA  implements Serializable {
    /**
     * Atributos
     */
    private final char ADD = '+', STAR = '*', QMARK = '?', CONCAT = '.', OR = '|';
    private int stateCounter = 0;


    /**
     * Sirve para determinar cuando se ha encontrado un operador que necesita un solo operando
     *
     * @param c caracter a evaluar
     * @return verdadero: si es un operador, falso: si no lo es
     */
    public static boolean isOneOperator(char c) { // Ver si es una uno-operacion

        return c == '+' || c == '*' || c == '?';

    }


    /**
     * Sirve para determinar cuando se ha encontrado un operador que necesita dos operandos
     *
     * @param c caracter a evaluar
     * @return verdadero: si es un operador, falso: si no lo es
     */
    public static boolean isTwoOperator(char c) { // Tell whether c is an operator.

        return c == '.' || c == '|';

    }


    /**
     * Funcion que evalua las uno-operaciones
     *
     * @param operation operacion que se realizara (concatenacion, or, ...)
     * @param op1       primer estado a operar
     * @return retorna un grafo
     */
    private DirectedGraph evalSingleOp(char operation, DirectedGraph op1) {
        DirectedGraph result = new DirectedGraph();

        switch (operation) {
            case ADD:
                result = addGraphs(op1);
                break;
            case QMARK:
                result = qMarkGraphs(op1);
                break;
            case STAR:
                result = starGraphs(op1);
                break;
        }

        return result;
    }


    /**
     * Funcion que evalua las dos-operaciones
     *
     * @param operation operacion que se realizara (concatenacion, or, ...)
     * @param op1       primer estado a operar
     * @param op2       segundo estado a operar
     * @return retorna un grafo
     */
    private DirectedGraph evalDoubleOp(char operation, DirectedGraph op1, DirectedGraph op2) {
        DirectedGraph result = new DirectedGraph();

        switch (operation) {
            case CONCAT:
                result = concatenateGraphs(op1, op2);
                break;
            case OR:
                result = orGraphs(op1, op2);
                break;
        }

        return result;
    }


    /**
     * Realiza la operacion '+' de un automata
     *
     * @param op1 automata a aplicar operacion
     * @return nuevo automata
     */
    private DirectedGraph addGraphs(DirectedGraph op1) {
        // Variables a utilizar
        DirectedGraph.NodeClass nodoInicialViejo, nodoFinalViejo, nodoInicialNuevo, nodoFinalNuevo;

        // Agregar transicion desde final anterior a inicio anterior
        nodoFinalViejo = op1.getOneFinalNode();
        nodoInicialViejo = op1.getOneInicialNode();
        op1.addEdges(op1, nodoFinalViejo, nodoInicialViejo, "!");

        // Crear nodo inicial nuevo, eliminar anterior y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        nodoInicialNuevo = op1.getParticularNode(nuevoID);  // Obtener nodo inicial nuevo

        op1.addEdges(op1, nodoInicialNuevo, nodoInicialViejo, "!");  // Agregar nueva transicion

        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial en nodo viejo
        nodoInicialNuevo.setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        nodoFinalNuevo = op1.getParticularNode(nuevoID);

        op1.addEdges(op1, nodoFinalViejo, nodoFinalNuevo, "!");  // Agregar nueva transicion

        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo
        nodoFinalNuevo.setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        return op1;
    }


    /**
     * Realiza la operacion '?' de un automata
     *
     * @param op1 automata a aplicar operacion
     * @return nuevo automata
     */
    private DirectedGraph qMarkGraphs(DirectedGraph op1) {
        // Variables a utilizar
        DirectedGraph.NodeClass nodoInicialViejo, nodoFinalViejo, nodoInicialNuevo, nodoFinalNuevo;

        // Agregar transicion desde final anterior a inicio anterior
        nodoFinalViejo = op1.getOneFinalNode();
        nodoInicialViejo = op1.getOneInicialNode();

        // Crear nodo inicial nuevo, eliminar anterior y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        nodoInicialNuevo = op1.getParticularNode(nuevoID);  // Obtener nodo inicial nuevo

        op1.addEdges(op1, nodoInicialNuevo, nodoInicialViejo, "!");  // Agregar nueva transicion

        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial en nodo viejo
        nodoInicialNuevo.setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        nodoFinalNuevo = op1.getParticularNode(nuevoID);

        op1.addEdges(op1, nodoFinalViejo, nodoFinalNuevo, "!");  // Agregar nueva transicion

        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo
        nodoFinalNuevo.setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear transicion entre nuevos estados final e inicial
        op1.addEdges(op1, nodoInicialNuevo, nodoFinalNuevo, "!");

        return op1;
    }


    /**
     * Realiza la operacion '*' de un automata
     *
     * @param op1 automata a aplicar operacion
     * @return nuevo automata
     */
    private DirectedGraph starGraphs(DirectedGraph op1) {
        // Variables a utilizar
        DirectedGraph.NodeClass nodoInicialViejo, nodoFinalViejo, nodoInicialNuevo, nodoFinalNuevo;

        // Agregar transicion desde final anterior a inicio anterior
        nodoFinalViejo = op1.getOneFinalNode();
        nodoInicialViejo = op1.getOneInicialNode();
        op1.addEdges(op1, nodoFinalViejo, nodoInicialViejo, "!");

        // Crear nodo inicial nuevo, eliminar anterior y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        nodoInicialNuevo = op1.getParticularNode(nuevoID);  // Obtener nodo inicial nuevo

        op1.addEdges(op1, nodoInicialNuevo, nodoInicialViejo, "!");  // Agregar nueva transicion

        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial en nodo viejo
        nodoInicialNuevo.setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        nodoFinalNuevo = op1.getParticularNode(nuevoID);

        op1.addEdges(op1, nodoFinalViejo, nodoFinalNuevo, "!");  // Agregar nueva transicion

        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo
        nodoFinalNuevo.setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear transicion entre nuevos estados final e inicial
        op1.addEdges(op1, nodoInicialNuevo, nodoFinalNuevo, "!");
        return op1;
    }


    /**
     * Realiza la concatenacion de dos automatas
     *
     * @param op1 primer automata a concatenar
     * @param op2 segundo automata a concatenar
     * @return nuevo automata
     */
    private DirectedGraph concatenateGraphs(DirectedGraph op1, DirectedGraph op2) {
        // Obtener nodos de segundo automata 2
        LinkedList<DirectedGraph.NodeClass> nodos2 = op2.getAllNodes();

        // Obtener transiciones automata 2
        LinkedList<DirectedGraph.edgeContents> edges2 = op2.getEdges();

        // Obtener nodo final de op1 para usarlo como inicial despues
        DirectedGraph.NodeClass op1FinalNode = op1.getOneFinalNode();


        // Copiar cada nodo a automata 1 sin copiar nodo inicial anterior
        for (DirectedGraph.NodeClass i : nodos2) {
            if (!i.isStart()) {
                op1.addNode(op1, i);
            }
        }

        // Copiar cada transicion a automata 1 cambiando la transicion del nodo inicial anterior
        for (DirectedGraph.edgeContents i : edges2) {
            if (!i.getStartingNode().isStart()) {
                op1.addEdges(op1, i);
            } else {
                i.setStartingNode(op1FinalNode);  // Cambiar nodo inicial de transicion
                op1FinalNode.setFinal(false);  // Quitarle propiedad de nodo final a op1FinalNode
                op1.addEdges(op1, i);  // Agregar a nuevo automata

                // Indicar nuevo nodo final en nuevo automata (No hace falta editar el anterior)
                op1.setNodeFinalMap(op2.getNodeFinalMap());
            }
        }


        return op1;
    }


    /**
     * Realiza el OR de dos automatas
     *
     * @param op1 primer automata a operar
     * @param op2 segundo automata a operar
     * @return nuevo automata
     */
    private DirectedGraph orGraphs(DirectedGraph op1, DirectedGraph op2) {
        // Obtener nodos de segundo automata
        LinkedList<DirectedGraph.NodeClass> nodos2 = op2.getAllNodes();

        // Obtener transiciones automata 2
        LinkedList<DirectedGraph.edgeContents> edges2 = op2.getEdges();

        // Copiar cada nodo a automata 1
        for (DirectedGraph.NodeClass i : nodos2) {
            op1.addNode(op1, i);
        }

        // Copiar cada transicion a automata 1
        for (DirectedGraph.edgeContents i : edges2) {
            op1.addEdges(op1, i);
        }

        // Crear nodo inicial nuevo, eliminar anteriores y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        DirectedGraph.NodeClass nuevoNodoInicial = op1.getParticularNode(nuevoID);

        //      OP1
        DirectedGraph.NodeClass nodoInicialViejo = op1.getOneInicialNode();  // Obtener nodo inicial de op1
        op1.addEdges(op1, nuevoNodoInicial, nodoInicialViejo, "!");  // Agregar nueva transicion
        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial antiguo en op1

        //      OP2
        nodoInicialViejo = op2.getOneInicialNode();  // Obtener nodo inicial de op2
        op1.addEdges(op1, nuevoNodoInicial, nodoInicialViejo, "!");  // Agregar nueva transicion
        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial antiguo en op1

        //      Setear nuevo nodo inicial
        nuevoNodoInicial.setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        DirectedGraph.NodeClass nuevoNodoFinal = op1.getParticularNode(nuevoID);  // Obtener nuevo nodo final

        //      OP1
        DirectedGraph.NodeClass nodoFinalViejo = op1.getOneFinalNode();  // Obtener nodo final de op1
        op1.addEdges(op1, nodoFinalViejo, nuevoNodoFinal, "!");  // Agregar nueva transicion a final nuevo
        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo

        //      OP2
        nodoFinalViejo = op2.getOneFinalNode();  // Obtener nodo final de op2
        op1.addEdges(op1, nodoFinalViejo, nuevoNodoFinal, "!");  // Agregar nueva transicion a final nuevo
        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo op2

        //      Setear nuevo nodo final
        nuevoNodoFinal.setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        return op1;
    }

    /**
     * Crea un automata simple a partir de una transicion indicada
     *
     * @param transition unica transicion del nuevo automata
     * @return un automata de una sola transicion con dos nodos
     */
    private DirectedGraph createSimpleGraph(String transition) {
        DirectedGraph result = new DirectedGraph();

        // Agregar estados
        int numberStates = getStateCounter();  // Obtener el numero de estados total
        result.addNode(result, numberStates, true, false);  // Crear nodo inicial
        result.addNode(result, numberStates + 1, false, true);  // Crear nodo final

        // Agregar transicion
        result.addEdges(result, result.getParticularNode(numberStates), result.getParticularNode(numberStates + 1), transition);

        setStateCounter(numberStates + 2);  // Aumentar estados usados
        return result;
    }


    /**
     * Funcion para evaluar una expresion regular en postfix
     *
     * @param expr expresion regular en postfix
     * @return retorna el grafo del automata
     */
    public DirectedGraph evaluate(String expr) {
        Stack<DirectedGraph> stack = new Stack<DirectedGraph>();
        DirectedGraph op1, op2, result;
        HashSet<String> alphabet = new HashSet<String>();

        boolean ingresaronCaracterEscape;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Verificar si ingresaron un caracter de escape
            ingresaronCaracterEscape = c == '\\';

            // Si no ingresaron un caracter de escape se hace toki normal
            if (!ingresaronCaracterEscape){
                if (isTwoOperator(c)) {
                    op2 = stack.pop();
                    op1 = stack.pop();
                    result = evalDoubleOp(c, op1, op2);
                    stack.push(result);
                } else if (isOneOperator(c)) {
                    op1 = stack.pop();
                    result = evalSingleOp(c, op1);
                    stack.push(result);
                } else {
                    stack.push(createSimpleGraph(String.valueOf(c)));
                    String letra = String.valueOf(c);
                    if (!letra.equals("!")) {
                        alphabet.add(letra);  // Agregar letra a alfabeto
                    }
                }
            } else {
                // Moverse al siguiente caracter
                i++;
                c = expr.charAt(i);

                stack.push(createSimpleGraph(String.valueOf(c)));
                String letra = String.valueOf(c);
                alphabet.add(letra);  // Agregar letra a alfabeto
            }


        }

        result = stack.pop();
        result.setAlphabet(alphabet);
        return result;
    }


    /**
     * Devuelve el contador de estados, el cual indica el numero de estados que se han estado creando
     *
     * @return el identificador del siguiente estado
     */
    private int getStateCounter() {
        return stateCounter;
    }


    /**
     * Sirve para aumentar contador de estados
     *
     * @param stateCounter cambio en numero
     */
    private void setStateCounter(int stateCounter) {
        this.stateCounter = stateCounter;
    }
}