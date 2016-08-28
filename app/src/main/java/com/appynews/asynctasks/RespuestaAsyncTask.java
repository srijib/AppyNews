package com.appynews.asynctasks;

import com.appynews.model.dto.Noticia;

import java.io.Serializable;
import java.util.List;

/**
 * Clase que contiene la respuesta devuelta por un AsyncTask
 *
 * Created by oscar on 26/08/16.
 */
public class RespuestaAsyncTask implements Serializable {

    private Integer status    = null;
    private String descStatus = null;
    private List<Noticia> noticias = null;


    /**
     * Constructor
     * @param status: Integer
     * @param descStatus: String
     */
    public RespuestaAsyncTask(Integer status, String descStatus) {
        this.status     = status;
        this.descStatus = descStatus;
    }

    /**
     * Devuelve la respuesta devuelta por el AsyncTask
     * @return Integer
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Establece la respuesta devuelta por el AsyncTask
     * @param status: Integer
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * Devuelve la descripción del código de error
     * @return String
     */
    public String getDescStatus() {
        return descStatus;
    }

    /**
     * Permite establecer la descripción del código de error
     * @param descStatus: String
     */
    public void setDescStatus(String descStatus) {
        this.descStatus = descStatus;
    }


    /**
     * Devuelve la colección de noticias recuperadas de la BBD
     * @return List<Noticia>
     */
    public List<Noticia> getNoticias() {
        return noticias;
    }

    /**
     * Establece la colección de noticias recuperadas de la BBD
     * @param noticias: List<Noticia>
     */
    public void setNoticias(List<Noticia> noticias) {
        this.noticias = noticias;
    }
}
