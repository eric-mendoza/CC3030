import com.sun.org.apache.xalan.internal.xsltc.runtime.Node;

import java.util.HashSet;
import java.util.Stack;

/**
 * La presente clase tiene como objetivo crear un dfa a partir de un regex
 * @author Eric Mendoza
 * @version 1.0
 * @since 11/08/207
 */
public class RegExToDFA {
    /**
     * Atributos
     */
    private BinaryTree arbolSintactico;
    private HashSet<String> alfabeto = new HashSet<String>();
    private int positionCounter;

    /**
     * Metodo que tiene como objetivo guiar el algoritmo de construccion directa del dfa
     * @param regex expresion regular desde la que se construira el dfa
     * @return un automata finito determinista
     */
    public DirectedGraph createDFA(String regex){
        regex = augmentateRegex(regex);  // Agregar eof al regex
        regex = RegExConverter.infixToPostfix(regex);  // Convertir a postfix
        arbolSintactico = generateSyntaxTree(regex);  // Crear arbol sintacticao
        setPropiedadesArbolSintactico(arbolSintactico);

        return null;
    }

    /**
     * Metodo que tiene como objetivo setear lastpos, firstpos y nullable de las propiedades del arbol sintactico
     */
    private void setPropiedadesArbolSintactico(BinaryTree tree) {
        if (tree.isLeaf()) {
            if (!tree.getValue().equals("!")){
                // FirstPos y LastPos
                HashSet<Integer> firstPos = new HashSet<Integer>();
                HashSet<Integer> lastPos = new HashSet<Integer>();
                int position = tree.getPosition();
                firstPos.add(position);
                lastPos.add(position);
                tree.setFirstPos(firstPos);
                tree.setLastPos(lastPos);

                // nullable
                tree.setNullable(false);
            } else {
                tree.setFirstPos(new HashSet<Integer>());
                tree.setLastPos(new HashSet<Integer>());
                tree.setNullable(false);
            }
        }
        else {
            String root;
            root = tree.getValue();
            BinaryTree leftChild = tree.getLeftChild();
            BinaryTree rightChild = tree.getRightChild();

            // Setear propiedades recursivamente
            if (leftChild != null) {
                setPropiedadesArbolSintactico(leftChild);
            }
            if (rightChild != null) {
                setPropiedadesArbolSintactico(rightChild);
            }


            HashSet<Integer> firstPos = new HashSet<Integer>();
            HashSet<Integer> lastPos = new HashSet<Integer>();
            boolean nullable = false;

            // Posibilidades de operacion
            switch (root.charAt(0)){
                case '|':
                    // FirstPos
                    firstPos.addAll(rightChild.getFirstPos());
                    firstPos.addAll(leftChild.getFirstPos());

                    // LastPos
                    lastPos.addAll(rightChild.getLastPos());
                    lastPos.addAll(leftChild.getLastPos());

                    // Nullable
                    nullable = rightChild.isNullable() || leftChild.isNullable();
                    break;

                case '.':
                    // FirstPos
                    if (leftChild.isNullable()){
                        // Union de los dos
                        firstPos.addAll(rightChild.getFirstPos());
                        firstPos.addAll(leftChild.getFirstPos());
                    } else {
                        firstPos.addAll(leftChild.getFirstPos());
                    }


                    // LastPos
                    if (rightChild.isNullable()){
                        // Union de los dos
                        lastPos.addAll(rightChild.getLastPos());
                        lastPos.addAll(leftChild.getLastPos());
                    } else {
                        lastPos.addAll(rightChild.getLastPos());
                    }

                    // Nullable
                    nullable = rightChild.isNullable() && leftChild.isNullable();
                    break;

                case '*':
                    // FirstPos
                    firstPos.addAll(leftChild.getFirstPos());

                    // LastPos
                    lastPos.addAll(leftChild.getLastPos());

                    // Nullable
                    nullable = true;
                    break;
            }

            tree.setFirstPos(firstPos);
            tree.setLastPos(lastPos);
            tree.setNullable(nullable);
        }
    }

    /**
     * Metodo que tiene como objetivo generar un arbol sintactico a partir de un regex en postfix
     * @param regex expresion regular a utilizarse para generar arbol
     * @return un arbol sintactico
     */
    private BinaryTree generateSyntaxTree(String regex) {
        Stack<BinaryTree> stack = new Stack<BinaryTree>();
        BinaryTree op1, op2, result;
        HashSet<String> alphabet = new HashSet<String>();

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (RegExToNFA.isTwoOperator(c)) {
                op2 = stack.pop();
                op1 = stack.pop();
                stack.push(new BinaryTree(String.valueOf(c), op1, op2));
            } else if (RegExToNFA.isOneOperator(c)) {
                op1 = stack.pop();
                stack.push(new BinaryTree(String.valueOf(c), op1, null));
            } else {
                String letra = String.valueOf(c);
                result = new BinaryTree(letra);  // Crear nodo hoja
                if (!letra.equals("!")) {
                    result.setPosition(getPositionCounter());  // Setear posicion en arbol
                    alphabet.add(letra);  // Agregar letra a alfabeto
                }

                stack.push(result);
            }
        }

        result = stack.pop();
        alfabeto = alphabet;
        return result;
    }


    public String augmentateRegex(String regex){
        String newRegex = "(";
        newRegex = newRegex + regex + ")#";
        return newRegex;
    }

    public int getPositionCounter() {
        int result = positionCounter + 1;
        setPositionCounter(result);
        return result;
    }

    public void setPositionCounter(int positionCounter) {
        this.positionCounter = positionCounter;
    }
}
