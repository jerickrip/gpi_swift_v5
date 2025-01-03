package com.ba.escuchador.utils;

import com.ba.desarrollo.Entorno;
import com.ba.desarrollo.contenedores.Peticion;
import com.ba.desarrollo.contenedores.Respuesta;
import com.ba.desarrollo.interconexion.Conector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import static com.agricola.servicio.ConexionAPI.StatusConfirmation;

public class Servicios {

	public static final Logger logger = LogManager.getLogger(Servicios.class);

	public static HashMap<String,String> actualizarEstadoTransaccion(HashMap<String, String> parametros){
		HashMap<String,String> respuesta = null;
		try {
			logger.info("\nIniciando la llamada al API", Servicios.class);
			String bic = parametros.get("from");
			String fundsAvailableDate = parametros.get("funds_available");
			String currencySymbol = "USD";

			logger.info("Parametros de la transacción", Servicios.class);
			logger.info("BIC del banco " + bic, Servicios.class);
			logger.info("Instruction identification " + parametros.get("instruction_identification"), Servicios.class);
			logger.info("Originador " + parametros.get("originator") , Servicios.class);
			logger.info("Fecha de disponibilidad " + fundsAvailableDate, Servicios.class);
			logger.info("Moneda Actual " + currencySymbol, Servicios.class);
			logger.info("Cantidad Confirmada " + parametros.get("confirmed_amount") ,Servicios.class);
			logger.info("Cantidad Deducida " + parametros.get("charge_amount") ,Servicios.class);

			String respAPI = StatusConfirmation(parametros);
			/*String respAPI = StatusConfirmation(
					parametros.get("uetr"),
					bic,
					parametros.get("instruction_identification"),
					parametros.get("originator"),
					fundsAvailableDate,
					currencySymbol,
					parametros.get("confirmed_amount"),
					parametros.get("charge_amount"),
					parametros.get("transaction_status"),
					parametros.get("transaction_status_reason"),
					parametros.get("business_service")
			);*/
			logger.info("Codigo de Respuesta API: {}", respAPI, Servicios.class);

			respuesta = new HashMap<String, String>();
			respuesta.put("status", respAPI);

		}catch(Exception e) {
			logger.info("Error - actualizarEstadoTransaccion()", Servicios.class);
			logger.error("error -> ", e);
		}
		return respuesta;
	}

	public static HashMap<String,String> obtenerDetalleTransaccion(HashMap<String, String> parametros){
		HashMap<String,String> respuesta = null;
		try {
			Conector conector = Entorno.e().obtenerConector("ConectorSwiftGPI");

			Peticion peticion = new Peticion();
			peticion.agregarParametro("servicio", "PaymentTransactionDetails");
			peticion.agregarParametro("fecha", System.currentTimeMillis());

			parametros.forEach((key, value)->{
				peticion.agregarParametro(key, value);
			});

			Respuesta resp = conector.obtenerDatos(peticion, null, "PaymentTransactionDetails", null);

			resp.primerContenedor();
			resp.siguiente();

			respuesta = new HashMap<String, String>();
			respuesta.put("status", resp.obtenerString("status"));
			respuesta.put("jsonResponse", resp.obtenerString("jsonResponse"));
		}catch(Exception e) {
			logger.info("Error - obtenerDetalleTransaccion()", Servicios.class);
			logger.error("error -> ", e);		}

		return respuesta;
	}

	public static String generarRespuestaDetalleXmlFromJson(String jsonString, Peticion pt, String codigo , String descripcion) {
		StringBuilder resultado = new StringBuilder();
		JSONObject json = new JSONObject(jsonString);
		resultado.append("<Response>");
		resultado.append("<id>" + pt.obtenerValor("servicio") + "</id>");
		resultado.append("<idTransaccion>"+pt.obtenerValor("servicio")+ "</idTransaccion>");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		sdf.applyPattern("yyyyMMddHHmmss");
		Date fecha = new Date();
		String sFecha = sdf.format(fecha);
		resultado.append("<fechaHora>" + sFecha + "</fechaHora>\n");
		resultado.append("<codigo>" + codigo + "</codigo>\n");
		resultado.append("<descripcion>" + descripcion +"</descripcion>\n");
		resultado.append("<Body>");
		resultado.append(XML.toString(json));
		resultado.append("</Body>");
		resultado.append("</Response>");

		return resultado.toString();
	}

	public static String[] obtenerPropiedadesServicios(String llavePropiedad) throws Exception {
		InputStream inputStream = null;
		String[] propiedades = null;
		try {
			File connectionFile = ResourceUtils.getFile("classpath:miscUtil.properties");
			inputStream = new FileInputStream(connectionFile.toString());
			Properties properties = new Properties();
			properties.load(inputStream);
			String s_webServiceDbConfig = properties.getProperty(llavePropiedad);
			StringTokenizer tokens = new StringTokenizer(s_webServiceDbConfig, "|");
			int i = 0;
			propiedades = new String[tokens.countTokens()];
			while (tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				propiedades[i] = token;
				//logger.info(token, Servicios.class);
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {
				inputStream.close();
			} catch (Exception e1) {

			}
		}
		return propiedades;
	}
}
