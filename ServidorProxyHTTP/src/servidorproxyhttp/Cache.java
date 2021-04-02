/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorproxyhttp;

import java.io.File;
import java.util.List;

/**
 *
 * @author Danilo Alejandro Ochoa Hidalgo
 */
public class Cache {

    private final String RUTA_CACHE = ".\\proxy\\cache\\";
    private List<String> almacenadosEnCache;
    private static Cache instancia;

    // PATRÓN DE DISEÑO APLICADO = SINGLETON
    protected static Cache getInstance() {
        if (instancia == null) {
            instancia = new Cache();
        }
        return instancia;
    }

    private Cache() {

        /* Crea la RUTA_CACHE si no existe */
        File rutaDirectorio = new File(RUTA_CACHE);
        if (!rutaDirectorio.exists()) {
            rutaDirectorio.mkdirs();
        }

    }

    /* MÉTODOS */
    protected String getRuta() {
        return RUTA_CACHE;
    }

    protected List<String> getAlmacenadosEnCache() {
        return almacenadosEnCache;
    }

    protected void setAlmacenadosEnCache(List<String> almacenadosEnCache) {
        this.almacenadosEnCache = almacenadosEnCache;
    }

    protected void addEntradaEnCache(String url) {
        if (this.almacenadosEnCache.indexOf(url) == -1) {
            this.almacenadosEnCache.add(url);
        }
    }

    protected void delEntradaEnCache(String url) {
        if (this.almacenadosEnCache.indexOf(url) != -1) {
            this.almacenadosEnCache.remove(url);
        }
    }

    protected String isEnCache(String url) {
        for (String url_contentType : this.almacenadosEnCache) {
            String[] urlSplit = url_contentType.split(" ");
            if ((urlSplit[0]).equals(url)) {
                return urlSplit[1];
            }
        }
        return null;
    }

}
