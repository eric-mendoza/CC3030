import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
        String archivoEspecificacionLexica = scanner.next();

        CocolRReader cocolRReader = new CocolRReader();

        // Verificar que el archivo este bien estructurado
        System.out.println("Cargando...");
        if (cocolRReader.analizeCocolRSyntax(archivoEspecificacionLexica + ".txt")){
            System.out.println("Se acepta el documento");

        } else {
            System.out.println("Error en la especificacion lexica. \nNo se generó el lexer.");
        }
    }

}
