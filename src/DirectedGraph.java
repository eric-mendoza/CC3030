import java.util.*;

/**
 * La presente clase tiene como objetivo simular a un automata. Lo que se realizo fue modificar una clase para grafos
 *  dirigidos, agregandole las caracteristicas para simular un grafo. Cabe destacar que la estructura inicial de la clase
 *  tipo grafo no es de mi autoria, sino de "shaunak bhattacharjee"
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 * @link https://github.com/shaunak1111/Directed-Graph-Implementation-java/blob/master/DirectedGraph.java
 */

public class DirectedGraph {
    /**
     * Atributos
     */
    private  LinkedList<NodeClass> nodes = new LinkedList<DirectedGraph.NodeClass>();
    private  LinkedList<edgeContents> edges = new LinkedList<DirectedGraph.edgeContents>();
    private  HashMap<Integer, NodeClass> nodeMap = new HashMap<Integer, DirectedGraph.NodeClass>();
    private  HashSet<NodeClass> nodeInitialMap = new HashSet<DirectedGraph.NodeClass>();  // Guardar nodos iniciales
    private  HashSet<NodeClass> nodeFinalMap = new HashSet<DirectedGraph.NodeClass>();  // Guardar nodos finales
    private  HashSet<String> alphabet = new HashSet<String>();  // Simbolos aceptados por el automata

    /**
     * Contructor de la clase DirectedGraph
     */
    public DirectedGraph(){}

    /**
     * Clase que simula los estados del automata
     */
    public class NodeClass{
        /**
         * Atributos de clase nodo
         */
        boolean Final, Start;
        int  id;
        LinkedList<edgeContents> edges;

        /**
         * Constructor
         * @param name es el ID del estado, debe ser unico
         * @param isStart indica si el nodo es el de inicio de automata
         * @param isFinal indica si el nodo es el final de automata
         */
        public NodeClass(int name, boolean isStart, boolean isFinal){
            id = name;
            if (isStart) nodeInitialMap.add(this);  // Agregar a listado de inicios de automata
            if (isFinal) nodeFinalMap.add(this);  // Agregar a listado de finales de automata
            this.Final = isFinal;
            this.Start = isStart;
            edges = new LinkedList<DirectedGraph.edgeContents>();
        }

        /**
         * Retorna si el nodo es el estado final del automata
         * @return true, si es el final; false de lo contrario.
         */
        public boolean isFinal() {
            return Final;
        }

        /**
         * Se utiliza para cambiar el estado de si es el estado final del automata
         * @param aFinal es el nuevo estado del atributo
         */
        public void setFinal(boolean aFinal) {
            Final = aFinal;
            if (aFinal){
                nodeFinalMap.add(this);
            } else {
                nodeFinalMap.remove(this);
            }
        }

        /**
         * Retorna si el nodo es el estado inicial del automata
         * @return true, si es el final; false de lo contrario.
         */
        public boolean isStart() {
            return Start;
        }

        /**
         * Se utiliza para cambiar el estado inicial
         * @param start nuevo estado del atributo
         */
        public void setStart(boolean start) {
            Start = start;
            if (start){
                nodeInitialMap.add(this);
            } else {
                nodeInitialMap.remove(this);
            }
        }

        /**
         * Se utiliza para obtener el ID de un nodo
         * @return Retorna el ID del nodo
         */
        public int getId() {
            return id;
        }

        /**
         * Se utiliza para cambiar el id del nodo
         * @param id es el nuevo id, que debe ser unico
         */
        public void setId(int id) {
            this.id = id;
        }
    }

    /**
     * Clase que simula las transiciones del automata
     */
    public class edgeContents{
        /**
         * Atributos de las transiciones
         */
        NodeClass startingNode;
        NodeClass finishingNode;
        String transition;

        /**
         * Contructor de la clase de transicion.
         * @param startingNode indica desde que nodo inicia la transicion
         * @param finishingNode indica a que nodo se dirige la transicion
         * @param transition indica la condicion para que se cumpla la transicion
         */
        public edgeContents( NodeClass startingNode, NodeClass finishingNode, String transition){
            this.startingNode = startingNode;
            this.finishingNode = finishingNode;
            this.transition = transition;
        }

        /**
         * Getters y setters de transiciones
         */
        public NodeClass getFinishingNode() {
            return finishingNode;
        }

        public void setFinishingNode(NodeClass finishingNode) {
            this.finishingNode = finishingNode;
        }

        public NodeClass getStartingNode() {
            return startingNode;
        }

        public void setStartingNode(NodeClass startingNode) {
            this.startingNode = startingNode;

        }

        public String getTransition() {
            return transition;
        }

        @Override
        public boolean equals(Object obj) {
            edgeContents e = (edgeContents)obj;
            return e.finishingNode == finishingNode && e.startingNode == startingNode;
        }
    }

    /**
     * Crea un nodo ocn las caracteristicas deseadas y lo agrega al automata
     * @param g automata a agregar el nodo
     * @param nodeId identificador de nodo (debe ser unico)
     * @param isStart Inidicar si es nodo inicial de automata
     * @param isFinal indicar si es nodo final de automata
     */
    public void addNode(DirectedGraph g, int nodeId, boolean isStart, boolean isFinal){
        NodeClass nodeToAdd = new NodeClass(nodeId, isStart, isFinal);
        g.nodes.add(nodeToAdd);
        g.nodeMap.put(nodeId, nodeToAdd);
    }

    /**
     *
     * @param nodeToAdd
     * Adds a node to the graph. Node is added to the HashMap nodeMap of the graph,node is added to HashSet nodes in graph
     */
    public void addNode(DirectedGraph g, NodeClass nodeToAdd){
        g.nodes.add(nodeToAdd);
        g.nodeMap.put(nodeToAdd.id, nodeToAdd);
    }

    /**
     * Agregar y crear una transicion al automata deseado
     * @param g automata a agregar transicion
     * @param startingNode Nodo desde el que sale la transicion
     * @param finishingNode Nodo al que llega la transicion
     * @param transition Identificador de la transicion
     */
    public void addEdges(DirectedGraph g, NodeClass startingNode, NodeClass finishingNode, String transition ){
        edgeContents edge = new edgeContents(startingNode, finishingNode, transition);
        g.edges.add(edge);  // Agregando a automata
        startingNode.edges.add(edge);  // Agregando transision a transiciones de nodo inicial
    }

    /**
     * Agregar una transicion a partir de una transicion existente
     * @param g automata a agregar transicion
     * @param edgeToAdd transicion a agregar
     */
    public void addEdges(DirectedGraph g, edgeContents edgeToAdd ){
        g.edges.add(edgeToAdd);
        edgeToAdd.startingNode.edges.add(edgeToAdd);
    }

    /**
     *  Obtener el mapa de nodos
      * @return Retorna el mapa de estados del automata junto con sus identificadores
     */
    public HashMap<Integer, NodeClass> getNodeMap(){
        return nodeMap;
    }

    public void setNodeMap(HashMap<Integer, NodeClass> nodeMap){
        this.nodeMap = nodeMap;
    }

    /**
     * Obtiene un estado dependiendo de su identificador
     * @param id el identificador del nodo a buscar
     * @return retorna el estado deseado
     */
    public  NodeClass getParticularNode(int id){
        if (nodeMap.containsKey(id))
            return nodeMap.get(id);
        return null;
    }

    /**
     * Devuelve el hashmap de los nodos
     * @return hasmap de nodos
     */
    public LinkedList<NodeClass> getAllNodes(){
        return nodes;
    }

    /**
     * Es para obtener las transiciones del automata
     * @return transiciones de automata
     */
    public LinkedList<edgeContents> getEdges(){
        return edges;
    }

    /**
     * Devuelve los nodos iniciales del automata
     * @return nodo inicial
     */
    public HashSet<NodeClass> getInicialNode(){
        return nodeInitialMap;
    }

    /**
     * Obtiene los nodos finales del automata
     * @return nodo final
     */
    public HashSet<NodeClass> getFinalNode(){
        return nodeFinalMap;
    }

    /**
     * Devuelve un nodo inicial del automata
     * @return nodo inicial
     */
    public NodeClass getOneInicialNode(){
        return nodeInitialMap.iterator().next();
    }

    /**
     * Obtiene el nodo final del automata
     * @return nodo final
     */
    public NodeClass getOneFinalNode(){
        return nodeFinalMap.iterator().next();
    }

    /**
     * Recorre todo el automata para devolver su descripcion
     * @return descripcion de automata
     */
    public String automataDescription(){
        // Nodos
        LinkedList<DirectedGraph.NodeClass> nodos = getAllNodes();
        String resultNodos = "Estados: {";
        int contador = 0;
        for (DirectedGraph.NodeClass i: nodos) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodos.size()-1){
                resultNodos = resultNodos + ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador ++;
        }

        resultNodos += "\t- Son " + contador + " estados\n";

        // Nodo Inicial
        resultNodos = resultNodos + "Estado inicial: {";
        contador = 0;
        for (NodeClass i: nodeInitialMap) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodeInitialMap.size()-1){
                resultNodos = resultNodos + ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador++;
        }


        // Nodo Final
        resultNodos = resultNodos + "Estado(s) de aceptacion: {";
        contador = 0;
        for (NodeClass i: nodeFinalMap) {
            resultNodos = resultNodos + String.valueOf((i.id));
            if (contador < nodeFinalMap.size()-1){
                resultNodos += ", ";
            }
            else {
                resultNodos = resultNodos + "}\n";
            }
            contador ++;
        }

        // Simbolos
        LinkedList<DirectedGraph.edgeContents> edges = getEdges();
        String resultEdges = "Simbolos: {";

        for (String i: alphabet) {
            resultEdges = resultEdges + i;
            if (contador < alphabet.size()-1){
                resultEdges = resultEdges + ", ";
            }
            else {
                resultEdges = resultEdges + "}\n";
            }
            contador ++;
        }

        // Edges
        resultEdges = resultEdges + "Transiciones: {";
        contador = 0;
        for (DirectedGraph.edgeContents i: edges) {
            resultEdges = resultEdges + "(" + i.getStartingNode().getId() + ", " + i.getTransition() + ", " + i.getFinishingNode().getId() + ")";
            if (contador < edges.size()-1){
                resultEdges = resultEdges + ", ";
            }
            else {
                resultEdges = resultEdges + "}\n";
            }
            contador ++;
        }
        resultEdges += "\t- Son " + contador + " transiciones.";
        return resultNodos + resultEdges;
    }

    public HashSet<NodeClass> getNodeFinalMap() {
        return nodeFinalMap;
    }

    public void setNodeFinalMap(HashSet<NodeClass> nodeFinalMap) {
        this.nodeFinalMap = nodeFinalMap;
    }


    public HashSet<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(HashSet<String> alphabet) {
        this.alphabet = alphabet;
    }
}