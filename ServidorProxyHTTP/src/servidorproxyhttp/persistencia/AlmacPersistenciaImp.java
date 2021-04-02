/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorproxyhttp.persistencia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Scanner;
import servidorproxyhttp.IPersistencia;

/**
 *
 * @author Danilo Alejandro Ochoa Hidalgo
 */
public class AlmacPersistenciaImp implements IPersistencia {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    /* VARIABLES DE LECTURA Y ESCRITURA */
    private final String rutaRaiz = ".\\proxy\\registros\\";
    private final String[] rutaRegistros = {rutaRaiz + "registro_de_bloqueados.txt", rutaRaiz + "registro_de_temporales.txt"};
    private final File fileBloqueados = new File(rutaRegistros[0]);
    private final File fileAlmacenadosEnCache = new File(rutaRegistros[1]);

    public AlmacPersistenciaImp() {

        File rutaDirectorio = new File(this.rutaRaiz);
        if (!rutaDirectorio.exists()) {
            rutaDirectorio.mkdirs();
        }

        try {

            if (!fileBloqueados.exists()) {
                fileBloqueados.createNewFile();
            }
            if (!fileAlmacenadosEnCache.exists()) {
                fileAlmacenadosEnCache.createNewFile();
            }

        } catch (IOException ex) {
            System.out.println(ANSI_RED + "# (Persistencia) No se pudo crear el archivo #\n" + ANSI_RED + ex + ANSI_RESET);
        }

    }

    @Override
    public List<String> cargarBloqueados() {
        List<String> listadoRegistros = new ArrayList<>();

        Scanner inBloqueados;
        try {
            inBloqueados = new Scanner(fileBloqueados);

            while (inBloqueados.hasNext()) {
                listadoRegistros.add(inBloqueados.nextLine());
            }

            inBloqueados.close();

        } catch (IOException ex) {
            System.out.println(ANSI_RED + "# (Persistencia) Error al leer del archivo:\n" + ANSI_RED + ex + ANSI_RESET);
        }

        return listadoRegistros;
    }

    @Override
    public List<String> cargarAlmacenadosEnCache() {
        List<String> listadoRegistros = new ArrayList<>();

        Scanner inAlmacenadosEnCache;
        try {
            inAlmacenadosEnCache = new Scanner(fileAlmacenadosEnCache);

            while (inAlmacenadosEnCache.hasNext()) {
                listadoRegistros.add(inAlmacenadosEnCache.nextLine());
            }

            inAlmacenadosEnCache.close();

        } catch (IOException ex) {
            System.out.println(ANSI_RED + "# (Persistencia) Error al leer del archivo:\n" + ANSI_RED + ex + ANSI_RESET);
        }

        return listadoRegistros;
    }

    @Override
    public boolean guardarRegistros(List<String> listaBloqueados, List<String> listaAlmacenadosEnCache) {

        Formatter bloqueadosFormatter;
        Formatter almacenadosEnCacheFormatter;

        try {
            bloqueadosFormatter = new Formatter(fileBloqueados);
            listaBloqueados.forEach((urlBloqueado) -> {
                bloqueadosFormatter.format("%s\n", urlBloqueado);
            });
            bloqueadosFormatter.flush();
            bloqueadosFormatter.close();

            almacenadosEnCacheFormatter = new Formatter(fileAlmacenadosEnCache);
            listaAlmacenadosEnCache.forEach((urlAlmacenadoEnCache) -> {
                almacenadosEnCacheFormatter.format("%s\n", urlAlmacenadoEnCache);
            });
            almacenadosEnCacheFormatter.flush();
            almacenadosEnCacheFormatter.close();

            return true;

        } catch (FileNotFoundException ex) {
            System.out.println(ANSI_RED + "# (Persistencia) Error al escribir en el archivo:\n" + ANSI_RED + ex + ANSI_RESET);
        }

        return false;

    }

}
