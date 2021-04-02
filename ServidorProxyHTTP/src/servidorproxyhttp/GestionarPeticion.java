/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorproxyhttp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;

/**
 *
 * @author Danilo Alejandro Ochoa Hidalgo
 */
public class GestionarPeticion extends Thread implements Runnable {

    private static final String FONT_RESET = "\u001B[0m";
    private static final String FONT_RED = "\u001B[31m";
    private static final String FONT_BLUE = "\u001B[34m";
    private static final String FONT_PURPLE = "\u001B[35m";
    private static final String FONT_GREEN = "\u001B[32m";
    private static final String FONT_YELLOW = "\u001B[33m";
    private static final String FONT_CYAN = "\u001B[36m";

    private Socket clienteSocket;
    private HttpURLConnection proxyToServerCon;

    public GestionarPeticion(Socket cliente) {
        this.clienteSocket = cliente;
        try {
            this.clienteSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        /* INICIA EL PROCESO PARA GESTIONAR LA PETICIÓN **************************************************/
        try {

            BufferedReader proxyToClientBuffR = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
            String[] requestString = proxyToClientBuffR.readLine().split(" ");

            switch (requestString[0]) {
                case "GET": // HTTP

                    /* SE ATIENDE LA PETICIÓN **************************************************/
                    URL url = new URL(requestString[1]);
                    atenderPeticionHTTP(url);

                    break;
                case "CONNECT": // HTTPS No soportado
                    System.out.println(FONT_RED + "(No soportado) HTTPS URL: " + FONT_PURPLE + requestString[1].substring(0, requestString[1].indexOf(":")) + FONT_RESET);
                    break;
            }
            clienteSocket.close();

            if (this.isAlive()) {
                this.join();
                this.finalize();
            }

        } // Excepciones al intentar manejar la Petición del Cliente 
        catch (MalformedURLException e) {
            System.out.println(FONT_RED + "# (" + this.getName() + ") URL con estructura NO soportada" + FONT_RESET);
        } catch (IOException e) {
            System.out.println(FONT_RED + "# (" + this.getName() + ") NO se pudo manejar la Petición del Cliente" + FONT_RESET);
        } catch (InterruptedException e) {
            System.out.println(FONT_RED + "# NO se pudo detener el proceso (" + this.getName() + ")" + FONT_RESET);
        } catch (Throwable e) {
            System.out.println(FONT_RED + "# NO se pudieron liberar los recursos de (" + this.getName() + ")" + FONT_RESET);
        }

    }

    /* ATENDER PETICIÓN HTTP **************************************************/
    private void atenderPeticionHTTP(URL url) {

        // 1. COMPROBAR DISPONIBILIDAD DEL URL
        if (ServidorProxyHTTP.isUrlDisponible(url.getHost())) {

            // 2. COMPROBAR SI EL ARCHIVO ESTÁ EN CACHÉ
            String contentMimeType;
            if ((contentMimeType = ServidorProxyHTTP.CACHE.isEnCache(url.toString())) != null) {

                // 2.1 (Caché) Responder a la Petición
                System.out.println(FONT_GREEN + "(Caché)" + FONT_BLUE + " URL: " + FONT_PURPLE + url.toString() + FONT_RESET);
                responderPeticion(url, contentMimeType); // Respuesta

            } else {

                // 2.2 (NO Caché) Descargar Archivo Solicitado
                System.out.println(FONT_BLUE + "URL: " + FONT_PURPLE + url.toString() + FONT_RESET);
                try {
                    proxyToServerCon = (HttpURLConnection) url.openConnection(); // Conexión con el Servidor
                    proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    proxyToServerCon.setRequestProperty("Content-Language", "en-US");
                    proxyToServerCon.setUseCaches(false);

                    int responseCode = proxyToServerCon.getResponseCode();
                    String contentType = proxyToServerCon.getContentType().replace(" ", "");

                    /* Acción según Código de Respuesta */
                    switch (responseCode) {
                        //* URL respuesta con Código OK *//
                        case HttpURLConnection.HTTP_OK:
                            // 2.2.1 Almacenar en Caché el Archivo Solicitado
                            if (almacenarEnCache(url, contentType)) { // Almacenar

                                // 2.2.2 (Caché) Responder a la Petición
                                responderPeticion(url, contentType); // Respuesta

                            }
                            proxyToServerCon.disconnect();
                            break;
                        //* URL respuesta con Otro Código *//
                        ///* Recurso del URL movido a otro URL *///
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            System.out.println(FONT_RED + "Recurso Movido. El Servidor respondió con HTTP code: " + responseCode + FONT_RESET);
                            url = new URL(proxyToServerCon.getHeaderField("Location"));
                            if ("http".equals(url.getProtocol())) {
                                proxyToServerCon.disconnect();
                                atenderPeticionHTTP(url); // Atender Petición en el nuevo URL
                            } else {
                                System.out.println(FONT_RED + "# (" + this.getName() + ") El Recurso se ha movido a un URL no HTTP" + FONT_RESET);
                            }
                            break;
                        ///* Recurso del URL no encontrado *////
                        case HttpURLConnection.HTTP_NOT_FOUND:
                            System.err.println(FONT_RED + "# (" + this.getName() + ") No se encontró el Recurso solicitado: " + responseCode + FONT_RESET);
                            break;
                    }
                } catch (IOException ex) {
                    System.out.println(FONT_RED + "# (" + this.getName() + ") Conexión rechazada desde el servidor" + FONT_RED + ex + FONT_RESET);
                }

            }

        } else {
            System.out.println(FONT_YELLOW + "# SITIO BLOQUEADO:" + FONT_CYAN + " (" + this.getName() + ") " + url.getHost() + FONT_YELLOW + " #" + FONT_RESET);
        }

    }

    /* ALMACENAR EN CACHÉ **************************************************/
    private boolean almacenarEnCache(URL url, String contentMimeType) {

        /* TRATAMIENTO DEL URL
         * (Entrada): URL tipo = .\proxy\cache\authority.au\/path/file.xxx
         * (Salida): PATH resultante = .\proxy\cache\authority.au\__path__file.xxx
         */
        String rutaCache = ServidorProxyHTTP.CACHE.getRuta() + url.getHost() + "\\" + url.getPath();
        rutaCache = rutaCache.replace("/", "__");

        //* OBTENCIÓN DESDE SERVIDOR A LA CACHÉ (Descarga de Archivo Solicitado) *//
        InputStream serverToProxyInStream;
        FileOutputStream proxyToCacheFileOStream;

        try {

            ///* Crear directorio en base al Nombre de Host del URL *///
            File rutaDirectorio = new File(ServidorProxyHTTP.CACHE.getRuta() + url.getHost());
            if (!rutaDirectorio.exists()) {
                rutaDirectorio.mkdirs();
            }

            ///* Almacenar Archivo en CACHÉ *///
            serverToProxyInStream = proxyToServerCon.getInputStream(); // Conexión con el Servidor
            proxyToCacheFileOStream = new FileOutputStream(rutaCache);

            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = serverToProxyInStream.read(buffer)) != -1) {
                proxyToCacheFileOStream.write(buffer, 0, bytesRead); // Almacenado
            }

            proxyToCacheFileOStream.flush();

            proxyToCacheFileOStream.close();
            serverToProxyInStream.close();

            ///* Actualizar la Lista de Archivos Almacenados en Caché *///
            // Luego de almacenar el Archivo se debe agregar la Referencia en la Lista
            ServidorProxyHTTP.CACHE.addEntradaEnCache(url.toString() + " " + contentMimeType);

            return true;

        } catch (IOException ex) {
            System.out.println(FONT_RED + "# (" + this.getName() + ") No se pudo leer desde el servidor:\n" + FONT_RED + ex + FONT_RESET);
        }

        return false;

    }

    /* RESPONDER PETICIÓN **************************************************/
    private boolean responderPeticion(URL url, String contentMimeType) {

        /* TRATAMIENTO DEL URL
         * (Entrada): URL tipo = .\proxy\cache\authority.au\/path/file.xxx
         * (Salida): PATH resultante = .\proxy\cache\authority.au\__path__file.xxx
         */
        String rutaEnCache = ServidorProxyHTTP.CACHE.getRuta() + url.getHost() + "\\" + url.getPath();
        rutaEnCache = rutaEnCache.replace("/", "__");

        //* ENVÍO DESDE CACHÉ AL CLIENTE (Archivo Solicitado) *//
        PrintWriter proxyToClientePrintWriter;
        BufferedOutputStream proxyToClientBuffOutStream;

        try {

            ///* (Caché) Comprobar si el Archivo Solicitado existe *///
            File archivoEnCache = new File(rutaEnCache);
            if (archivoEnCache.exists()) {

                ///* (Caché) Leer el contenido del Archivo Solicitado *///
                int fileLength = (int) archivoEnCache.length();
                byte[] fileData = readFileData(archivoEnCache, fileLength); // Lectura de Datos

                ///* (Caché) Enviar Cabecera HTTP al Cliente *///
                proxyToClientePrintWriter = new PrintWriter(clienteSocket.getOutputStream()); // Conexión con el Cliente
                proxyToClientBuffOutStream = new BufferedOutputStream(clienteSocket.getOutputStream()); // Conexión con el Cliente

                proxyToClientePrintWriter.println("HTTP/1.1 200 OK");
                proxyToClientePrintWriter.println("Server: ServidorProxyHTTP");
                proxyToClientePrintWriter.println("Date: " + new Date());
                proxyToClientePrintWriter.println("Content-Type: " + contentMimeType);
                proxyToClientePrintWriter.println("Content-Length: " + fileLength);
                proxyToClientePrintWriter.println(); // Línea entre la CABECERA y el CONTENIDO (NO BORRAR)
                proxyToClientePrintWriter.flush();

                ///* (Caché) Enviar contenido del Archivo Solicitado al Cliente *///
                proxyToClientBuffOutStream.write(fileData, 0, fileLength);
                proxyToClientBuffOutStream.flush();

                proxyToClientePrintWriter.close();
                proxyToClientBuffOutStream.close();

                return true;

            } else {
                System.out.println(FONT_RED + "# (" + this.getName() + ") No se pudo leer desde CACHÉ: Intentando descargar recurso ***" + FONT_RESET);
                ServidorProxyHTTP.CACHE.delEntradaEnCache(url.toString() + " " + contentMimeType);
            }

        } catch (IOException ex) {
            System.out.println(FONT_RED + "# (" + this.getName() + ") Conexión fallida con el cliente:\n" + FONT_RED + ex + FONT_RESET);
        }

        return false;

    }

    ///* (Caché) Función para devolver el contenido del Archivo Solicitado en Bytes *///
    private byte[] readFileData(File file, int fileLength) {
        FileInputStream fileIn;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData); // Cargado de Datos
            fileIn.close();

        } catch (IOException ex) {
            System.out.println(FONT_RED + "# (" + this.getName() + ") No se pudo leer desde CACHÉ:\n" + FONT_RED + ex + FONT_RESET);
        }

        return fileData;
    }

}
