package GeneratedFiles;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.Scanner;
import GeneradorLexers.CocolRReader;
import GeneradorLexers.Grammar;
import GeneradorLexers.PDFA;
import javafx.util.Pair;

public class CompilerMain {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Lexer
        Lexer lexer = new Lexer();
        ArrayList<Pair<String, String>> foundToknsBrute = lexProgram(lexer);

        ArrayList<String> foundTokens = new ArrayList<String>();
        // IGuardar solo el tipo de tokens
        for (Pair<String, String> token: foundToknsBrute){
            //System.out.println("<" + token.getKey() + ", " + token.getValue() + ">");
            foundTokens.add(token.getKey());
        }

        // Deserealizar gramatica y automata para parsear
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("grammar.ser"));
        Grammar grammar = (Grammar) in.readObject();
        in.close();

        if (foundTokens.size() > 0){
            // Calcular lr0
            grammar.setNames(0);
            PDFA lr0 = grammar.createLR0();
            lr0.createSLRTable();
            lr0.createDescriptionDocument();
            if (grammar.parse(foundTokens, lr0)){
                System.out.println("Parseo exitoso.");
            } else {
                System.err.println("Se encontraron errores en el programa ingresado.");
            }
        } else {
            System.err.println("No se identificó ningún token.");
        }

    }

    public static ArrayList<Pair<String, String>> lexProgram(Lexer lexer){
        // Tokens <type, lexema>
        ArrayList<Pair<String, String>> tokens = new ArrayList<Pair<String, String>>();

        // Leer archivo y pasarlo a List
        List<String> lineasProgramaPrueba = CocolRReader.readFile("ProgramaPrueba" + ".txt");

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
        return tokens;
    }

}