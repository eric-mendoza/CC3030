import java.util.HashSet;
import java.util.Stack;

/**
 * La presente clase tiene como objetivo crear un automata a partir de un regex
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 */
public class RegExEvaluator {
    /**
     * Atributos
     */
    private static final char ADD = '+', STAR = '*', QMARK = '?', CONCAT = '.', OR = '|';
    private static int stateCounter = 0;


    /**
     * Sirve para determinar cuando se ha encontrado un operador que necesita un solo operando
     * @param c caracter a evaluar
     * @return verdadero: si es un operador, falso: si no lo es
     */
    private static boolean isOneOperator(char c) { // Ver si es una uno-operacion

        return c == '+'  ||  c == '*'  ||  c == '?';

    }


    /**
     * Sirve para determinar cuando se ha encontrado un operador que necesita dos operandos
     * @param c caracter a evaluar
     * @return verdadero: si es un operador, falso: si no lo es
     */
    private static boolean isTwoOperator(char c) { // Tell whether c is an operator.

        return c == '.' || c == '|';

    }


    /**
     * Funcion que evalua las uno-operaciones
     * @param operation operacion que se realizara (concatenacion, or, ...)
     * @param op1 primer estado a operar
     * @return retorna un grafo
     */
    private static DirectedGraph evalSingleOp(char operation, DirectedGraph op1) {
        DirectedGraph result = new DirectedGraph();

        switch (operation) {
            case ADD :
                result = addGraphs(op1);
                break;
            case QMARK :
                result = qMarkGraphs(op1);
                break;
            case STAR :
                result = starGraphs(op1);
                break;
        }

        return result;
    }


    /**
     * Funcion que evalua las dos-operaciones
     * @param operation operacion que se realizara (concatenacion, or, ...)
     * @param op1 primer estado a operar
     * @param op2 segundo estado a operar
     * @return retorna un grafo
     */
    private static DirectedGraph evalDoubleOp(char operation, DirectedGraph op1, DirectedGraph op2) {
        DirectedGraph result = new DirectedGraph();

        switch (operation) {
            case CONCAT :
                result = concatenateGraphs(op1, op2);
                break;
            case OR :
                result = orGraphs(op1, op2);
                break;
        }

        return result;
    }


    /**
     * Realiza la operacion '+' de un automata
     * @param op1 automata a aplicar operacion
     * @return nuevo automata
     */
    private static DirectedGraph addGraphs(DirectedGraph op1){
        DirectedGraph result;
        DirectedGraph result2;
        DirectedGraph copyOp1;

        copyOp1 = duplicateGraph(op1);

        // Realizar estrella y concatenacion
        result = starGraphs(copyOp1);  // Obtener op1*
        result2 =  concatenateGraphs(op1, result);

        return result2;
    }


    /**
     * Realiza la operacion '?' de un automata
     * @param a automata a aplicar operacion
     * @return nuevo automata
     */
    private static DirectedGraph qMarkGraphs(DirectedGraph a){
        DirectedGraph result, epsilon;
        epsilon = createSimpleGraph("!");
        result = orGraphs(a, epsilon);

        return result;
    }


    /**
     * Realiza la operacion '*' de un automata
     * @param op1 automata a aplicar operacion
     * @return nuevo automata
     */
    private static DirectedGraph starGraphs(DirectedGraph op1){
        // Agregar transicion desde final anterior a inicio anterior
        op1.addEdges(op1, op1.getFinalNode(), op1.getInicialNode(), "!");

        // Crear nodo inicial nuevo, eliminar anterior y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        DirectedGraph.NodeClass nodoInicialViejo = op1.getInicialNode();  // Obtener nodos inicial anterior
        op1.addEdges(op1, op1.getParticularNode(nuevoID), nodoInicialViejo, "!");  // Agregar nueva transicion
        nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial en nodo viejo
        op1.getParticularNode(nuevoID).setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        DirectedGraph.NodeClass nodoFinalViejo = op1.getFinalNode();  // Obtener nodo final anterior
        op1.addEdges(op1, nodoFinalViejo, op1.getParticularNode(nuevoID), "!");  // Agregar nueva transicion
        nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo
        op1.getParticularNode(nuevoID).setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear transicion entre nuevos estados final e inicial
        op1.addEdges(op1, op1.getInicialNode(), op1.getFinalNode(), "!");
        return op1;
    }


    /**
     * Realiza la concatenacion de dos automatas
     * @param op1 primer automata a concatenar
     * @param op2 segundo automata a concatenar
     * @return nuevo automata
     */
    private static DirectedGraph concatenateGraphs(DirectedGraph op1, DirectedGraph op2){
        // Obtener nodos de segundo automata
        HashSet<DirectedGraph.NodeClass> nodos2 = op2.getAllNodes();

        // Obtener transiciones automata 2
        HashSet<DirectedGraph.edgeContents> edges2 = op2.getEdges();

        DirectedGraph.NodeClass op1FinalNode = op1.getFinalNode();  // Obtener nodo final de op1


        // Copiar cada nodo a automata 1 sin copiar nodo inicial anterior
        for (DirectedGraph.NodeClass i: nodos2) {
            if (!i.isStart()){
                op1.addNode(op1, i);
            }
        }

        // Copiar cada transicion a automata 1 cambiando la transicion del nodo inicial anterior
        for (DirectedGraph.edgeContents i: edges2) {
            if (!i.getStartingNode().isStart()){
                op1.addEdges(op1, i);
            }

            else {
                i.setStartingNode(op1FinalNode);  // Cambiar nodo inicial de transicion
                op1FinalNode.setFinal(false);  // Quitarle propiedad de nodo final a op1FinalNode
                op1.addEdges(op1, i);  // Agregar a nuevo automata
            }
        }

        return op1;
    }


    /**
     * Realiza el OR de dos automatas
     * @param op1 primer automata a operar
     * @param op2 segundo automata a operar
     * @return nuevo automata
     */
    private static DirectedGraph orGraphs(DirectedGraph op1, DirectedGraph op2){
        // Obtener nodos de segundo automata
        HashSet<DirectedGraph.NodeClass> nodos2 = op2.getAllNodes();

        // Obtener transiciones automata 2
        HashSet<DirectedGraph.edgeContents> edges2 = op2.getEdges();

        // Copiar cada nodo a automata 1
        for (DirectedGraph.NodeClass i: nodos2) {
            op1.addNode(op1, i);
        }

        // Copiar cada transicion a automata 1
        for (DirectedGraph.edgeContents i: edges2) {
            op1.addEdges(op1, i);
        }

        // Crear nodo inicial nuevo, eliminar anteriores y agregar transiciones epsilon
        int nuevoID = getStateCounter();  // Nombre de nuevo nodo inicial
        int nodosInicialesViejosEliminados = 0;
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca de ser final
        while (nodosInicialesViejosEliminados < 2) {
            DirectedGraph.NodeClass nodoInicialViejo = op1.getInicialNode();  // Obtener nodos inicial anterior
            op1.addEdges(op1, op1.getParticularNode(nuevoID), nodoInicialViejo, "!");  // Agregar nueva transicion
            nodoInicialViejo.setStart(false);  // Eliminar bandera de nodo inicial en nodo viejo
            nodosInicialesViejosEliminados ++;
        }
        op1.getParticularNode(nuevoID).setStart(true);  // Agregar bandera de nodo inicial
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        // Crear nodo final nuevo, eliminar anteriores y agregar transiciones epsilon
        nuevoID = getStateCounter();  // Nombre de nuevo nodo final
        int nodosFinalesViejosEliminados = 0;
        op1.addNode(op1, nuevoID, false, false);  // Agregar nuevo nodo final sin marca
        while (nodosFinalesViejosEliminados < 2) {
            DirectedGraph.NodeClass nodoFinalViejo = op1.getFinalNode();  // Obtener nodo final anterior

            op1.addEdges(op1, nodoFinalViejo, op1.getParticularNode(nuevoID), "!");  // Agregar nueva transicion
            nodoFinalViejo.setFinal(false);  // Eliminar bandera de nodo final en nodo viejo
            nodosFinalesViejosEliminados ++;
        }
        op1.getParticularNode(nuevoID).setFinal(true);  // Agregar bandera de nodo final
        setStateCounter(nuevoID + 1);  // Cambiar conteo de estados

        return op1;
    }

    /**
     * Crea un automata simple a partir de una transicion indicada
     * @param transition unica transicion del nuevo automata
     * @return un automata de una sola transicion con dos nodos
     */
    private static DirectedGraph createSimpleGraph(String transition){
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
     * @param expr expresion regular en postfix
     * @return retorna el grafo del automata
     */
    public static DirectedGraph evaluate(String expr) {
        Stack<DirectedGraph> stack = new Stack<DirectedGraph>();
        DirectedGraph op1, op2, result;

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (isTwoOperator(c)) {
                op2 = stack.pop();
                op1 = stack.pop();
                result = evalDoubleOp(expr.charAt(i), op1, op2);
                stack.push(result);
            }
            else if (isOneOperator(c)){
                op1 = stack.pop();
                result = evalSingleOp(expr.charAt(i), op1);
                stack.push(result);
            }
            else
                stack.push(createSimpleGraph(String.valueOf(expr.charAt(i))));
        }

        result = stack.pop();
        return result;
    }


    /**
     * Devuelve el contador de estados, el cual indica el numero de estados que se han estado creando
     * @return el identificador del siguiente estado
     */
    public static int getStateCounter() {
        return stateCounter;
    }


    /**
     * Sirve para aumentar contador de estados
     * @param stateCounter cambio en numero
     */
    public static void setStateCounter(int stateCounter) {
        RegExEvaluator.stateCounter = stateCounter;
    }

    /**
     * Toma un automata y crea otro nuevo con nodos y transiciones con mismo ID
     * @param a automata a clonar
     * @return nuevo automata
     */
    public static DirectedGraph duplicateGraph( DirectedGraph a){
        DirectedGraph a2 = new DirectedGraph();
        // Obtener nodos de segundo automata
        HashSet<DirectedGraph.NodeClass> nodos2 = a.getAllNodes();

        // Obtener transiciones automata 2
        HashSet<DirectedGraph.edgeContents> edges2 = a.getEdges();

        // Copiar cada nodo a copia automata 1
        for (DirectedGraph.NodeClass i: nodos2) {
            a2.addNode(a2, i.getId(), i.isStart(), i.isFinal());
        }

        // Copiar cada transicion a copia automata 1
        for (DirectedGraph.edgeContents i: edges2) {
            a2.addEdges(a2, i.getStartingNode(), i.getFinishingNode(), i.getTransition());
        }

        // Cambiar numeros y adaptar transiciones
        // Obtener nodos de nuevo automata
        HashSet<DirectedGraph.NodeClass> nodosNuevos = a2.getAllNodes();

        // Obtener transiciones nuevo automata 2
        HashSet<DirectedGraph.edgeContents> edgesNuevos = a2.getEdges();

        // Cambiar
        int nuevoEstado, viejoEstado;
        for (DirectedGraph.NodeClass i: nodosNuevos) {
            viejoEstado = i.getId();
            nuevoEstado = getStateCounter();
            setStateCounter(nuevoEstado + 1);

            // Buscar transiciones que contengan al nodo a cambiar
            for (DirectedGraph.edgeContents k: edgesNuevos) {
                // Cambiar nodos de inicio
                if (k.getStartingNode().getId() == viejoEstado){
                    k.setStartingNode(i);
                }

                // Cambiar nodos de final
                if (k.getFinishingNode().getId() == viejoEstado){
                    k.setFinishingNode(i);
                }
            }

            i.setId(nuevoEstado);  // Cambiar estado de nodo actual
        }

        return a2;
    }
}
