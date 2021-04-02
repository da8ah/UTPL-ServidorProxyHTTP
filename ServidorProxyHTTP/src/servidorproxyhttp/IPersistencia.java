/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorproxyhttp;

import java.util.List;

/**
 *
 * @author Danilo Alejandro Ochoa Hidalgo
 */
public interface IPersistencia {

    // ListadoBloqueados: Nombre de Hosts
    public List<String> cargarBloqueados();

    // ListadoAlmacenadosEnCache: URLs (String = URL + " " + Content-Type)
    public List<String> cargarAlmacenadosEnCache();

    // Guardar los registros del Servidor (Hosts Bloqueados, URLs en CACHÉ)
    public boolean guardarRegistros(List<String> listaBloqueados, List<String> listaAlmacenadosEnCache);

}
