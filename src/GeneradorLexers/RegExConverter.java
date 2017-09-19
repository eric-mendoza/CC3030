package GeneradorLexers;

import java.util.*;

/**
 * La presente clase tiene como objetivo ingresar una expresion en infix y retornarla en postfix
 * @author Eric Mendoza
 * @version 1.0
 * @since 23/07/207
 * Para esta clase se utilizo otra como guia, obtenida de https://gist.github.com/gmenard/6161825
 */
public class RegExConverter {

    /** Operators precedence map. */
    private static final Map<Character, Integer> precedenceMap;
    static {
        Map<Character, Integer> map = new HashMap<Character, Integer>();
        map.put('(', 1);
        map.put('|', 2);
        map.put('.', 3); // explicit concatenation operator
        map.put('?', 4);
        map.put('*', 4);
        map.put('+', 4);
        precedenceMap = Collections.unmodifiableMap(map);
    };

    /**
     * Get character precedence.
     *
     * @param c character
     * @return corresponding precedence
     */
    private static Integer getPrecedence(Character c) {
        Integer precedence = precedenceMap.get(c);
        return precedence == null ? 6 : precedence;
    }

    /**
     * Transform regular expression by inserting a '.' as explicit concatenation
     * operator.
     */
    private static String formatRegEx(String regex) {
        String res = new String();
        List<Character> allOperators = Arrays.asList('|', '?', '*', '+');  // SE LE ELIMINO KLEENE PLUS
        List<Character> binaryOperators = Arrays.asList('|');
        boolean usedEscape = false;

        for (int i = 0; i < regex.length(); i++) {
            Character c1 = regex.charAt(i);

            // Verificar que aun no este en la ultima posicion
            if (i + 1 < regex.length()) {
                // Obtener el siguiente caracter
                Character c2 = regex.charAt(i + 1);

                // Agregar el primer caracter a la respuesta
                res += c1;

                // Agregar caracter de escape
                if (!c1.equals('\\') || usedEscape){
                    // Si el primer caracter no es un parentesis y el segundo no es un operador
                    if ((!c1.equals('(') && !c2.equals(')') && !allOperators.contains(c2) && !binaryOperators.contains(c1)) || (usedEscape && !c2.equals(')') && !allOperators.contains(c2))) {
                        res += '.';
                    }
                    usedEscape = false;
                } else {
                    // Indicar que se utilizo el caracter de escape
                    usedEscape = true;
                }
            }
        }
        res += regex.charAt(regex.length() - 1);

        return res;
    }


    /**
     * Convert regular expression from infix to postfix notation using
     * Shunting-yard algorithm.
     *
     * @param regex infix notation
     * @return postfix notation
     */
    public static String infixToPostfix(String regex) {
        String postfix = "";

        Stack<Character> stack = new Stack<Character>();
        String formattedRegEx = formatRegEx(regex);
        char[] formatedRegexArray = formattedRegEx.toCharArray();

        for (int i = 0; i < formatedRegexArray.length; i++) {
            char c = formatedRegexArray[i];

            switch (c) {
                case '(':
                    stack.push(c);
                    break;

                case ')':
                    while (!stack.peek().equals('(')) {
                        postfix += stack.pop();
                    }
                    stack.pop();
                    break;

                case '\\':
                    postfix += "\\";
                    i++;
                    postfix += formatedRegexArray[i];
                    break;

                default:
                    while (stack.size() > 0) {
                        Character peekedChar = stack.peek();

                        Integer peekedCharPrecedence = getPrecedence(peekedChar);
                        Integer currentCharPrecedence = getPrecedence(c);

                        if (peekedCharPrecedence >= currentCharPrecedence) {
                            postfix += stack.pop();
                        } else {
                            break;
                        }

                    }
                    stack.push(c);
                    break;
            }

        }

        while (stack.size() > 0)
            postfix += stack.pop();

        return postfix;
    }
}