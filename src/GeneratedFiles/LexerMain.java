package GeneratedFiles;

import java.util.*;
import java.util.Scanner;
import GeneradorLexers.CocolRReader;
import javafx.util.Pair;

public class LexerMain {

	public static void main(String[] args){
        // Tokens <type, lexema>
        ArrayList<Pair<String, String>> tokens = new ArrayList<Pair<String, String>>();

        // Lexer
        Lexer lexer = new Lexer();

        System.out.println("Ingrese el nombre del archivo con el código que desea lexear: ");
        java.util.Scanner scanner = new Scanner(System.in);
        String archivoEspecificacionLexica = scanner.next();

        // Leer archivo y pasarlo a List
        List<String> lineasProgramaPrueba = CocolRReader.readFile(archivoEspecificacionLexica + ".txt");

        // Unir todas las lineas en un solo string
        String programaPrueba = "";
        if (lineasProgramaPrueba != null){
            for (String linea : lineasProgramaPrueba) {
                programaPrueba += linea;
            }
        }

        // Obtener los tokens del archivo
        boolean hasNextToken = true;
        Integer inicioLexema = 0;  // Se irá actualizando con la función
        Pair<Boolean, Integer> tokenResults;


        while (hasNextToken){
            // Obtener el siguiente token
            tokenResults = lexer.nextToken(tokens, programaPrueba, inicioLexema);

            // Actulizar variable bandera
            hasNextToken = tokenResults.getKey();

            // Actualizar el puntero de donde hasta donde se ha consumido el documento
            inicioLexema = tokenResults.getValue();
        }

        // Imprimir los tokens encontrados
        for (Pair<String, String> token: tokens){
            System.out.println("<" + token.getKey() + ", " + token.getValue() + ">");
        }
    }

}