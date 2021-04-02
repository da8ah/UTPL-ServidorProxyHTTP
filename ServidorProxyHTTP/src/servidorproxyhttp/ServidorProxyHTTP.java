/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorproxyhttp;

import servidorproxyhttp.persistencia.AlmacPersistenciaImp;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import servidorproxyhttp.ui.AdministradorGUI;

/**
 *
 * @author Danilo Alejandro Ochoa Hidalgo
 */
public final class ServidorProxyHTTP implements Runnable {

    private static final String FONT_RESET = "\u001B[0m";
    private static final String FONT_RED = "\u001B[31m";
    private static final String FONT_BLUE = "\u001B[34m";

    /* ATRIBUTOS (PROPIEDADES DE LA CLASE) **************************************************/
    //* EJECUCIÓN DEL SERVIDOR *//
    private static boolean EJECUTANDO = true; // Bandera de Ejecución del Servidor
    private static final int PORT = 8080; // Puerto Predeterminado

    //* PERSISTENCIA *//
    ///* La CACHÉ debe hacerce en el SERVIDOR *///
    private static IPersistencia ALMACENAMIENTO;
    protected static final Cache CACHE = Cache.getInstance();
    private static List<String> listaBloqueados;

    //* GUI para Administrador *//
    private static AdministradorGUI administradorGUI;

    //* SERVIDOR *//
    private static List<Thread> listadoHilos;
    private static Thread consola;
    private static ServerSocket servidorSocket;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        new ServidorProxyHTTP(new AlmacPersistenciaImp()).ejecutarServidor();

    }

    /* FUNCIONES ESPECÍFICAS PARA CONTROL DEL SERVIDOR **************************************************/
    //* PATRÓN DE DISEÑO APLICADO = SINGLETON *//
    ///* Única instancia de AdministradorGUI *///
    private static AdministradorGUI createAdministradorGUI() {
        if (administradorGUI == null) {
            administradorGUI = new AdministradorGUI();
            administradorGUI.getHiloDeActualizacion().start(); // (Actualizador)
        }
        return administradorGUI;
    }

    /* CONSTRUCTOR del Servidor */
    private ServidorProxyHTTP(IPersistencia iPersistencia) {

        /* INICIALIZAR REGISTROS DE PERSISTENCIA */
        ALMACENAMIENTO = iPersistencia;
        CACHE.setAlmacenadosEnCache(ALMACENAMIENTO.cargarAlmacenadosEnCache());
        listaBloqueados = ALMACENAMIENTO.cargarBloqueados();

        /* INICIALIZAR REGISTROS PARA LA INSTANCIA DEL SERVIDOR */
        listadoHilos = new ArrayList<>();
        consola = new Thread(this, "Consola");
        try {
            servidorSocket = new ServerSocket(PORT);
        } // Excepciones al intentar inicializar el Servidor en Modo Escucha
        catch (SocketException se) {
            System.out.println(FONT_RED + "No fue posible inicializar el Servidor" + FONT_RESET);
        } catch (SocketTimeoutException ste) {
            System.out.println(FONT_RED + "Tiempo expirado al intentar inicializar el Servidor" + FONT_RESET);
        } catch (IOException io) {
            System.out.println(FONT_RED + "No fue posible inicializar el modo escucha del Servidor" + FONT_RESET);
        }

    }

    /* FUNCIONES DEL SERVIDOR **************************************************/
    private void ejecutarServidor() {

        /* EJECUTAR LA CONSOLA EN UN HILO APARTE **************************************************/
        consola.start();

        while (EJECUTANDO) { // MODO ESCUCHA
            try {
                /* HILOS
                 * Se crea un nuevo Hilo para cada Socket generado por el servidor
                 * Se agrega cada Hilo creado al listadoHilos
                 * Se inicia la ejecución del Hilo
                 */
                Thread hilo = new Thread(new GestionarPeticion(servidorSocket.accept()));
                listadoHilos.add(hilo);
                hilo.start();

            } catch (SocketException e) {
                System.out.println(FONT_RED + "**************** Server Stopped *****************" + FONT_RESET);
            } catch (IOException e) {
                System.out.println(FONT_RED + "Conexión fallida con el cliente" + FONT_RESET);
            }
        }

    }

    private void terminarEjecucionDelServidor() {

        System.out.println(FONT_RED + "###************ CERRANDO SERVIDOR ************###" + FONT_RESET);
        ServidorProxyHTTP.EJECUTANDO = false;

        /* Terminar el proceso de todos los Hilos en ejecución */
        try {

            //* AdministradorGUI (Actualizador) *//
            if (ServidorProxyHTTP.administradorGUI != null) {
                if (ServidorProxyHTTP.administradorGUI.getHiloDeActualizacion().isAlive()) {
                    System.out.println(FONT_BLUE + "# Esperando terminar (" + ServidorProxyHTTP.administradorGUI.getHiloDeActualizacion().getName() + ") ***" + FONT_RESET);
                    ServidorProxyHTTP.administradorGUI.getHiloDeActualizacion().join();
                    System.out.println(FONT_BLUE + "¡Terminado (" + ServidorProxyHTTP.administradorGUI.getHiloDeActualizacion().getName() + ")!" + FONT_RESET);
                }
                //* (AdministradorGUI) Eliminación de la instancia de la GUI *//
                ServidorProxyHTTP.administradorGUI.dispose();
                ServidorProxyHTTP.administradorGUI = null;
            }

            //* Listado de Hilos (Gestión de Peticiones) *//
            for (Thread thread : ServidorProxyHTTP.listadoHilos) {
                if (thread.isAlive()) {
                    System.out.print(FONT_BLUE + "# Esperando que (" + thread.getName() + ") termine su proceso ***" + FONT_RESET);
                    thread.join();
                    System.out.println(FONT_BLUE + "¡Terminado (" + thread.getName() + ")!" + FONT_RESET);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /* Guardar los registros del Servidor (Hosts Bloqueados, URLs en CACHÉ) */
        if (ServidorProxyHTTP.ALMACENAMIENTO.guardarRegistros(ServidorProxyHTTP.listaBloqueados, ServidorProxyHTTP.CACHE.getAlmacenadosEnCache())) {
            System.out.println(FONT_BLUE + "¡Todos los registros se guardaron correctamente!" + FONT_RESET);
        }

        /* Terminar Ejecución del Servidor */
        try {
            System.out.println(FONT_BLUE + "# Terminando Ejecución del Servidor ***" + FONT_RESET);
            ServidorProxyHTTP.servidorSocket.close();
        } catch (IOException ex) {
            System.out.println(FONT_RED + "Imposible cerrar la conexión:\n" + FONT_RED + ex + FONT_RESET);
        }

    }

    /* FUNCIONES DE LA CONSOLA DEL SERVIDOR **************************************************/
    private void imprimirOpcionesDeConsola() {
        System.out.println(FONT_BLUE + "############# PROXY:(Iniciado) ##############" + FONT_RESET);
        System.out.println(FONT_RED + "Servidor Proxy iniciado en el puerto (" + PORT + ")" + FONT_RESET);
        System.out.println("Ingrese -help (-h) para obtener ayuda");
        System.out.println("Ingrese -close (-c) para detener el servidor");
        System.out.println("Ingrese -interface (-i):");
        System.out.println(" (*) para crear una GUI [-i (c)reate]");
        System.out.println(" (*) para elimar la GUI [-i (d)estroy]");
        System.out.println(FONT_BLUE + "*###########################################*" + FONT_RESET);
    }

    @Override
    public void run() {

        imprimirOpcionesDeConsola();

        Scanner cli = new Scanner(System.in);
        String comando;

        while (EJECUTANDO) {
            comando = cli.nextLine();
            String[] separadorComando = comando.split(" ");
            switch (separadorComando[0]) {
                case "-exit":
                    System.exit(0);
                    break;
                case "-help": // Imprimir Opciones de la Consola
                case "-h":
                    imprimirOpcionesDeConsola();
                    break;
                case "-close": // Terminar Ejecución del Servidor
                case "-c":

                    terminarEjecucionDelServidor();

                    break;
                case "-interface": // Crear o Eliminar la AdministradorGUI
                case "-i":
                    if (separadorComando.length > 1) {

                        switch (separadorComando[1]) {
                            case "create":
                            case "c":
                                if (administradorGUI == null || !administradorGUI.isVisible()) {
                                    ServidorProxyHTTP.createAdministradorGUI().setVisible(true);
                                }
                                break;
                            case "destroy":
                            case "d":
                                administradorGUI.dispose();
                                break;
                            default:
                                System.out.println(FONT_RED + "# (Consola): Comando no reconocido [ help(-h) ] #" + FONT_RESET);
                                break;
                        }

                    } else {
                        System.out.println(FONT_RED + "# (Consola): Comando incompleto [-(i)nterface ((c)reate | (d)estroy)]" + FONT_RESET);
                    }
                    break;
                default:
                    System.out.println(FONT_RED + "# (Consola): Comando no reconocido [ help(-h) ] #" + FONT_RESET);
                    break;
            }
        }

    }

    /* SERVICIOS DEL SERVIDOR (PARA OTRAS CLASES) **************************************************/
    //* FUNCIÓN PARA LA GESTIÓN DE PETICIONES *//
    protected static boolean isUrlDisponible(String urlHost) {
        return listaBloqueados.indexOf(urlHost) == -1;
    }

    //* FUNCIONES PARA LA AdministradorGUI *//
    public static boolean isEJECUTANDO() {
        return EJECUTANDO;
    }

    public static List<String> getAlmacenadosEnCache() {
        ArrayList almacenadosEnCache = new ArrayList(CACHE.getAlmacenadosEnCache());
        return almacenadosEnCache;
    }

    public static List<Thread> getListadoHilos() {
        ArrayList listadoHilosCopia = new ArrayList(ServidorProxyHTTP.listadoHilos);
        return listadoHilosCopia;
    }

    public static List<String> getListaBloqueados() {
        ArrayList listaBloqueadosCopia = new ArrayList(ServidorProxyHTTP.listaBloqueados);
        return listaBloqueadosCopia;
    }

    public static boolean addBloqueoAPagina(URL url) {
        if (listaBloqueados.indexOf(url.getHost()) == -1) {
            listaBloqueados.add(url.getHost());
            return true;
        }
        return false;
    }

    public static void delBloqueoAPagina(String host) {
        if (listaBloqueados.indexOf(host) != -1) {
            listaBloqueados.remove(host);
        }
    }

}
