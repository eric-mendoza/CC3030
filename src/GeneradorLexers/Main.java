package GeneradorLexers;

import java.io.*;
import java.util.HashSet;
import java.util.Scanner;

/**
 * La presente clase tiene como objetivo ingresar un documento de COCOL/R y analizarlo
 * @author Eric Mendoza
 * @version 3.0
 * @since 23/07/2017
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // Ingreso del nombre del archivo con la descripcion lexica
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese el nombre del archivo con la especificación léxica en Cocol: ");
        // String archivoEspecificacionLexica = scanner.next();
        String archivoEspecificacionLexica = "CocolR";

        CocolRReader cocolRReader = new CocolRReader();

        // Verificar que el archivo este bien estructurado
        System.out.println("Cargando...");
        if (cocolRReader.analizeCocolRSyntax(archivoEspecificacionLexica + ".txt")){
            if (cocolRReader.generateLexer()){
                cocolRReader.generateLexerJavaFile();
                System.out.println("Se generó el lexer. \nSe generó grmática.");
            } else {
                System.err.println("Error en la especificación léxica.\nNo se generó el lexer");
            }

        } else {
            System.err.println("Error en la especificacion lexica. \nNo se generó el lexer.");
        }

        // LABORATORIO 8
        Grammar grammar = cocolRReader.getGrammar();
        HashSet<String> conjunto;
        while(true){
            try {
                System.out.println("\nSeleccione una de las siguientes opciones: \n\t 1) FIRST\n\t 2) FOLLOW");
                int selection = scanner.nextInt();
                switch (selection){
                    case 1:
                        System.out.println("Ingrese una cadena de símbolos: ");
                        String cadena = scanner.next();
                        conjunto = grammar.getFirstOfSymbols(cadena);
                        if (conjunto != null){
                            System.out.println("Resultado: " + conjunto.toString());
                        } else {
                            System.out.println("La cadena que ingresó contiene un símbolo inexistente en la gramática");
                        }
                        break;

                    case 2:
                        System.out.println("Ingrese un símbolo no-terminal: ");
                        String cadena2 = scanner.next();
                        conjunto = grammar.getFollowNonTeminal(cadena2);
                        if (conjunto != null){
                            System.out.println("Resultado: " + conjunto.toString());
                        } else {
                            System.out.println("La cadena que ingresó contiene un símbolo inexistente en la gramática");
                        }
                        break;

                    default:
                        System.out.println("No es una opcion!");
                }
            }

            catch (ArrayIndexOutOfBoundsException e){
                System.out.println("Ingrese una entrada válida!\n");
                scanner.next();
            }
        }

    }

}
