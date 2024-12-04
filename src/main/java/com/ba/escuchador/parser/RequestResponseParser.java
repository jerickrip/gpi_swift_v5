/*
 * Created on Aug 13, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ba.escuchador.parser;

import com.ba.desarrollo.contenedores.DefinicionValorRespuesta;
import com.ba.desarrollo.contenedores.Peticion;
import com.ba.desarrollo.contenedores.Respuesta;
import com.bancoagricola.frmwrk.common.Helper;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * @author Roberto Gil
 * <p>
 * Proporciona los metodos necesarios para parsear de un xml request a un objeto Peticion, y de un objeto
 * Respuesta a un xml response. Los xml es la mensajeria estandard en MQ, mientras que los objetos Peticion
 * y Respuesta es la forma de comunicarse con el conector.
 */
public class RequestResponseParser {

    /**
     * Parsea un objeto Respuesta a un mensaje XML response, en el formato prestablecido para la mensajeria MQ. En
     * este metodo
     * se construye el header del mensaje y luego se llama al metodo contenedorToXML(), para construir el detalle del
     * mensaje.
     *
     * @param rp
     * @return
     */
    public static String getRespuestaXML(Peticion pt, Respuesta rp) {
        rp.primerContenedor();
        rp.siguiente();
        StringBuffer xml = new StringBuffer("<Response>\n");
        //xml.append("<id>" + Helper.completeRightWith(rp.obtenerString("idConector"), ' ', 30) + "</id>\n");
        xml.append("<id>" + Helper.completeRightWith(pt.obtenerValor("idConector"), ' ', 30) + "</id>\n");
        xml.append("<idTransaccion>" + Helper.completeRightWith(pt.obtenerValor("servicio"), ' ', 30) +
                "</idTransaccion>\n");
        //xml.append("<idTransaccion>" + Helper.completeRightWith(rp.obtenerString("servicio"), ' ', 30) +
        // "</idTransaccion>\n");
		/*SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
		Date fecha = null;
		try
		{
			fecha = sdf.parse(rp.obtenerString("fechaHoraRespuesta"));
		}catch(Exception e)
		{
			fecha = new Date();
		}
		sdf.applyPattern("yyyyMMddHHmmss");
		String sFecha = sdf.format(fecha);*/
        String sFecha = obtenerFechaFormateada(rp.obtenerString("fechaHoraRespuesta"), "yyyy-MM-dd HH:mm:ss",
                "yyyyMMddHHmmss");
        xml.append("<fechaHora>" + sFecha + "</fechaHora>\n");
        //Agrega el sistema que realizo la peticion
        xml.append("<sistema>" + (pt != null && pt.obtenerValor("sistema") != null ?
                pt.obtenerValor("sistema").trim() : "") + "</sistema>");
        xml.append("<codigo>" + rp.obtenerString("codigo") + "</codigo>\n");
        xml.append("<descripcion><![CDATA[" + Helper.completeLeftWith(rp.obtenerString("descripcion"), ' ', 50) +
                "]]></descripcion>\n");
        List lsr = rp.getListaDefincionesSubRespuestas();
        xml.append(contenedorToXML(rp, null, lsr, false, true));//xml.append(respuestaToXMl());
        xml.append("</Response>\n");

        return xml.toString();
    }


    /**
     * Parsea de forma dinamica un objeto Respuesta a un XML response, si se desea generar un Element padre
     * en este nivel de recusrsividad, se debera indicar un \'nombre\' para dicho elemento, de lo contrario se de
     * pasar null.
     * Si se desea crear un Element padre para cada subrespuesta, se debe indicar true para el argumento
     * \'tagSubRespuesta\'.
     * Adicionalmente se puede indicar si este nivel de recursividad es la primera iteracion, para formar los tags
     * necesarios
     * para este nivel, es decir el Elemento raiz.
     *
     * @param rp              objeto Respuesta a parsear.
     * @param nombre          nombre del Elmento (tag) padre a construir para esta iteracion.
     * @param tagSubRespuesta indica si se desea crear Elmentos (tags) padres, para cada subrespuesta contenido en la
     *                        Respuesta.
     * @param contenedorRaiz  indica si es la primera iteracion del proceso de parseo.
     * @return
     */
    public static String respuestaToXML(Respuesta rp, String nombre, boolean tagSubRespuesta, boolean contenedorRaiz) {
        StringBuffer res = new StringBuffer("");
        //String nombreTagRespuesta = nombre==null ? "Response" : nombre;
        if (rp == null)
            return "";
        List l = rp.getListaDefincionesValor();
        List lsr = rp.getListaDefincionesSubRespuestas();
        res.append((nombre == null ? "" : "<" + nombre + ">\n"));
        rp.primerContenedor();
        while (rp.siguiente()) {
            res.append(contenedorToXML(rp, l, lsr, tagSubRespuesta, contenedorRaiz));
        }
        res.append((nombre == null ? "" : "</" + nombre + ">\n"));//res += "</" + nombreTagRespuesta + ">";
        return res.toString();
    }


    /**
     * Construye dinamicamente el contenido del mensaje xml response partir del objeto Respuesta y las lista de
     * DefinicionesValor,
     * y la lista de DefinicionesSubRespuestas, para este nivel de recursividad. Adicionalmente podemos indicar si se
     * desea
     * crear un Element padre para cada subrespuesta (tagSubRespuesta = true), y si esta iteracion proviene del Elemento
     * (tag) raiz.
     *
     * @param rp              objeto Respuesta a parsear.
     * @param l               lista de DefinicionesValor.
     * @param lsr             lista de DefinicionesSubRespuestas
     * @param tagSubRespuesta indica si se desea crear Elmentos (tags) padres, para cada subrespuesta contenido en la
     *                        Respuesta.
     * @param contenedorRaiz  indica si proviene del elemento raiz.
     * @return
     */
    private static String contenedorToXML(Respuesta rp, List l, List lsr, boolean tagSubRespuesta,
                                          boolean contenedorRaiz) {
        String res = contenedorRaiz ? "" : "<Contenedor>\n";
        Iterator il = l != null ? l.iterator() : null;
        Iterator ilsr = lsr != null ? lsr.iterator() : null;
        while (il != null && il.hasNext()) {
            DefinicionValorRespuesta dvr = (DefinicionValorRespuesta) il.next();
            String nombreTag = dvr.getIdentificador();
            String valorTag = rp.obtenerString(nombreTag);
            res += "<" + nombreTag + "><![CDATA[" + valorTag + "]]></" + nombreTag + ">\n";
        }
        res += (contenedorRaiz && lsr.size() > 0) ? "<Body>\n<Respuesta>\n" : "";
        while (ilsr != null && ilsr.hasNext()) {
            DefinicionValorRespuesta dvr = (DefinicionValorRespuesta) ilsr.next();
            String nombreTag = dvr.getIdentificador();
            Respuesta rtag = rp.obtenerSubRespuesta(nombreTag);
            res += respuestaToXML(rtag, (tagSubRespuesta ? nombreTag : null), tagSubRespuesta, false);
        }
        res += (contenedorRaiz && lsr.size() > 0) ? "</Respuesta>\n</Body>\n" : "";
        res += contenedorRaiz ? "" : "</Contenedor>\n";
        return res;
    }


    /**
     * Construye un un objeto Peticion a partir de un mensaje XML request con el formato prestablecido, para la
     * mensajeria MQ.
     * En este metodo se obtienen los valores del encabezado del mensaje, y luego el detalle dinamicamente, utilizando
     * el metodo procesarElementoPeticion().
     *
     * @param xml mensaje request a parsear.
     * @return
     */
    public static Peticion getPeticion(String xml) {
        Peticion pt = null;
        System.out.println("MENSAJE_REQUEST_XML : " + xml);
        try {
            Document document = DocumentHelper.parseText(xml);
            Node id = document.selectSingleNode("/Request/id");
            Node idTransaccion = document.selectSingleNode("/Request/idTransaccion");
            Node fechahora = document.selectSingleNode("/Request/fechaHora");
            Node usuario = document.selectSingleNode("/Request/usuario");
            Node ubicacion = document.selectSingleNode("/Request/ubicacionUsuario");
            Node agencia = document.selectSingleNode("/Request/agencia");
            Node cajero = document.selectSingleNode("/Request/cajero");
            Node sistema = document.selectSingleNode("/Request/sistema");
            pt = new Peticion();
            pt.agregarParametro("idConector", (id != null && id.getText() != null ? id.getText().trim() : ""));
            pt.agregarParametro("servicio", (idTransaccion != null && idTransaccion.getText() != null ?
                    idTransaccion.getText().trim() : ""));
            pt.agregarParametro("agencia", (agencia != null && agencia.getText() != null ? agencia.getText().trim() :
                    ""));
            pt.agregarParametro("cajero", (cajero != null && cajero.getText() != null ? cajero.getText().trim() : ""));
            pt.agregarParametro("ubicacion", (ubicacion != null && ubicacion.getText() != null ?
                    ubicacion.getText().trim() : ""));
            pt.agregarParametro("sistema", (sistema != null && sistema.getText() != null ? sistema.getText().trim() :
                    ""));
            pt.agregarParametro("usuario", (usuario != null && usuario.getText() != null ? usuario.getText().trim() :
                    ""));
            String fecha = obtenerFechaFormateada((fechahora != null && fechahora.getText() != null ?
                    fechahora.getText().trim() : ""), "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss");
            pt.agregarParametro("fechaHoraSolicitud", fecha);

            Element elementoRespuestaRaiz = document.getRootElement().element("Body");
            if (elementoRespuestaRaiz != null) {
                procesarElementoPeticion(elementoRespuestaRaiz, pt);
                //responseDTO.primerContenedor();
            } else {
                throw new Exception("No existe el elemento Body");
            }

        } catch (Exception e) {
            pt = null;
            System.out.println("Error parseando XML : " + e.getMessage());
        }
        if (pt != null)
            pt.toString();
        return pt;
    }


    /**
     * Construye dinamicamente el detalle del mensaje xml request.
     *
     * @param elementoPeticion
     * @param pt
     */
    private static void procesarElementoPeticion(Element elementoPeticion, Peticion pt) {
        List elementosValoresDinamicos = elementoPeticion.elements();
        Iterator iValores = elementosValoresDinamicos.iterator();
        int x = 0;
        while (iValores.hasNext()) {
            Element elementoValor = (Element) iValores.next();
            String nombre = elementoValor.getName();
            String valor = elementoValor.getText();
            if (nombre.startsWith("monto") && valor.indexOf(".") == -1) {
                BigDecimal montoB = null;
                try {
                    montoB = new BigDecimal(valor).setScale(2, BigDecimal.ROUND_HALF_UP);
                } catch (Exception e) {
                    montoB = new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
                valor = montoB.movePointLeft(2).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            }
            pt.agregarParametro(nombre, valor);
        }
    }


    /**
     * Convierte un string de fecha en un formato determinado a otro string con otro formato. Tanto el formato
     * origen como el formato destino, deben ser pasados como argumento.
     *
     * @param fechaToFormat string de la fecha a formatear.
     * @param fromPatron    patron actual de la fecha
     * @param toPatron      patron que se dsea aplicar a la fecha.
     * @return
     */
    public static String obtenerFechaFormateada(String fechaToFormat, String fromPatron, String toPatron) {
        String fecha = null;
        SimpleDateFormat sdf = new SimpleDateFormat(fromPatron);
        sdf.applyPattern(fromPatron);
        Date d_fecha = null;
        try {
            d_fecha = sdf.parse(fechaToFormat);
        } catch (Exception e) {
            d_fecha = new Date();
        }
        sdf.applyPattern(toPatron);
        fecha = sdf.format(d_fecha);

        return fecha;
    }


    public static void main(String[] args) {
        Respuesta rp = new Respuesta();
        rp.agregarValor("codigo", "000000");
        rp.agregarValor("descripcion", "                                                  ");
        rp.agregarValor("idConector", "conectorWebServices           ");
        rp.agregarValor("servicio", "getSaldosAES");
        rp.agregarValor("fechaHoraRespuesta", "2008-05-20 17:22:16");

        Respuesta rp1 = new Respuesta();
        rp1.agregarValor("tipoEntrada", "1");
        rp1.agregarValor("fechaVencimiento", "20080522");
        rp1.agregarValor("reconexion", "000000000000");
        rp1.agregarValor("alcaldia", "000000000000");
        rp1.agregarValor("codigoEmpresa", "2260");
        rp1.agregarValor("numeroNicNpe", "513575600000000000000000000000");
        rp1.agregarValor("energia", "000000045604");
        rp1.agregarValor("nombreCliente", "W & S, S. A. DE C. V. .            ");
        rp1.agregarValor("nombreEmpresa", "CLESA      ");
        rp1.guardarContenedor();
		/*for(int i = 0 ; i < 4 ; i++)
		{
			rp1.agregarValor("numeroCliente", ""+i);
			rp1.agregarValor("nombre", "Cliente"+i);
			rp1.agregarValor("flag", "Flag"+i);
			rp1.guardarContenedor();
		}*/
        rp.agregarRespuestaDependiente("detalle", rp1);

        rp.guardarContenedor();
        String xml = RequestResponseParser.getRespuestaXML(new Peticion(), rp);
        System.out.println(xml);
        String mensaje = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><Request><id>conectorWebServices</id>" +
                "<idTransaccion>notificacionPagoCNR</idTransaccion><fechaHora>20071029123000</fechaHora>" + "<usuario"
                + ">" + " </usuario><ubicacionUsuario> </ubicacionUsuario><agencia>042</agencia>" + "<cajero>4201" +
                "</cajero" + "><sistema>VENTANILLA</sistema><Body><codigoBanco>04</codigoBanco>" + "<cuenta> " +
                "</cuenta><agencia>042" + "</agencia><numeroComprobante>8888888888888</numeroComprobante>" + "<monto" + ">000000000100</monto><fecha" + ">29/10/2007</fecha><folio>000777777777777</folio>" + "<codigoCanal" + ">VENT</codigoCanal><tipoServicio> " + "</tipoServicio><operacion>0</operacion>" + "</Body></Request>";
        Peticion pt = RequestResponseParser.getPeticion(mensaje);
        System.out.println(pt.toString());
    }
}
