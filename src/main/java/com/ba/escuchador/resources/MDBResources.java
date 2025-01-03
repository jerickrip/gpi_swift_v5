package com.ba.escuchador.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDBResources {

    private static final Logger logger = LoggerFactory.getLogger(MDBResources.class.getName());
    public static final String MDB_PROPERTIES_CONFIG ="/miscUtil.properties";
    public static final String MDB_DYNAMICPROPERTIES_CONFIG ="/application.properties";
    public static String F_SHOW = "";
    public static String[] MDB_DEFAULT_OUTPUT_QUEUE = {};

    private MDBResources() {
        //Constructor privado para ocultar el publico implicito
    }

    public static void inicializarPropiedades(){

    }
}
