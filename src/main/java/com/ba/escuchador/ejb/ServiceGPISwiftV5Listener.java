package com.ba.escuchador.ejb;

import com.ba.escuchador.utils.Constantes;
import com.bancoagricola.frmwrk.common.Helper;
import com.ba.escuchador.parser.RequestResponseParser;
import com.ba.escuchador.resources.MDBResources;
import com.ba.desarrollo.contenedores.Peticion;
import com.ba.escuchador.utils.Servicios;

import javax.ws.rs.core.Response.Status;
import com.ibm.jakarta.jms.JMSTextMessage;
import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueueManager;
import jakarta.jms.Queue;
import org.apache.logging.log4j.LogManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ServiceGPISwiftV5Listener {
	public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ServiceGPISwiftV5Listener.class);

	public static String qChannel	= null;
	public static String qManager	= null;
	public static String qName	= null;
	public static String qHost = null;
	public static int qPort = 0;
	public static String qUsr= null;
	public static  String qPass=null;

	@Async("asyncMq")
	public void onMessage(Object msg) {
		/*try {
			MDBResources.inicializarPropiedades();
		} catch (Exception e) {
			logger.error("Error al cargar logger {}", e.getMessage());
		}*/
		long inicio = System.currentTimeMillis();
		long fin;
		//TextMessage mensaje;
		Queue destino = null;
		byte[] correlation = null;
		// Agregamos Queue para recuperar qName
		String xmlResponse = null;
		String sdestino = null;
		String contenido = null;

		try {
			if (msg instanceof JMSTextMessage message) {
				logger.info("MessageId del mensaje : {}", message.getJMSCorrelationID());
				String converted = new String(message.getJMSCorrelationIDAsBytes(), StandardCharsets.UTF_8);
				logger.info("MessageId del mensaje convertido : {}", converted);

				/*****************************Conexión con Swift - Inicio****************************************/
				if (message != null ) {
					Peticion pt = null;
					contenido = message.getBody(String.class);
					pt = RequestResponseParser.getPeticion(contenido);
					logger.info("\nArchivo XML: \n{}",contenido);

					String servicio = pt.obtenerValor("servicio");
					logger.info("Servicio a ejecutar: " + servicio, Servicios.class);

					try {
						HashMap<String, String> parametros = new HashMap<>();
						switch (servicio) {
							case Constantes.GPISWIFTUPDATESTATUS: {
								try {
									parametros.put("uetr", pt.obtenerValor("uetr").trim());
									parametros.put("from", pt.obtenerValor("from").trim());
									parametros.put("update_payment_scenario",
											pt.obtenerValor("update_payment_scenario").trim());
									parametros.put("instruction_identification", pt.obtenerValor(
											"instruction_identification").trim());
									parametros.put("originator", pt.obtenerValor("originator").trim());
									parametros.put("transaction_status", pt.obtenerValor("transaction_status").trim());
									parametros.put("transaction_status_reason", pt.obtenerValor(
											"transaction_status_reason").trim());
									parametros.put("return", pt.obtenerValor("return").trim());
									parametros.put("confirmed_amount", pt.obtenerValor("confirmed_amount").trim());
									parametros.put("charge_amount", pt.obtenerValor("charge_amount").trim());
									parametros.put("business_service", pt.obtenerValor("business_service").trim());
									parametros.put("funds_available", pt.obtenerValor("funds_available").trim());

									HashMap<String, String> respuesta = Servicios.actualizarEstadoTransaccion(parametros);

								} catch (Exception e) {
									logger.info("Ha ocurrido un error en el proceso: " + Constantes.GPISWIFTUPDATESTATUS,
											Servicios.class);
									xmlResponse = respuestaGenerica(pt.obtenerValor("servicio"), pt.obtenerValor(
											"servicio"), "0404", Constantes.ERRORGENERICO);
									logger.error("error -> ", e);
								}
								break;
							}
							case Constantes.GPISWIFTDETAILTRANSACTION: {
								try {
									parametros.put("uetr", pt.obtenerValor("uetr").trim());
									HashMap<String, String> respuesta = Servicios.obtenerDetalleTransaccion(parametros);

									if (respuesta != null && respuesta.get("status").contentEquals(Status.OK.getStatusCode() + "")) {
										logger.info("Proceso realizado correstamente", Servicios.class);
										xmlResponse = Servicios.generarRespuestaDetalleXmlFromJson(respuesta.get(
												"jsonResponse"), pt, Constantes.CODIGOEXITO, Constantes.MENSAJEEXITO);
									} else if (respuesta != null && respuesta.get("status").contentEquals(Status.CREATED.getStatusCode() + "")) {
										logger.info("Proceso realizado correstamente", Servicios.class);
										xmlResponse = Servicios.generarRespuestaDetalleXmlFromJson(respuesta.get(
												"jsonResponse"), pt, Constantes.CODIGOEXITO, Constantes.MENSAJEEXITO);
									} else if (respuesta != null && respuesta.get("status").contentEquals(Status.ACCEPTED.getStatusCode() + "")) {
										logger.info("Proceso realizado correstamente", Servicios.class);
										xmlResponse = Servicios.generarRespuestaDetalleXmlFromJson(respuesta.get(
												"jsonResponse"), pt, Constantes.CODIGOEXITO, Constantes.MENSAJEEXITO);
									} else {
										logger.info("Ha ocurrido un error en el proceso", Servicios.class);
										logger.info("Mensaje de respuesta de API Gpi", Servicios.class);
										if (respuesta != null)
											logger.info(respuesta.get("jsonResponse"), Servicios.class);
										xmlResponse = this.respuestaGenerica(servicio, servicio, Constantes.CODIGOERROR,
												Constantes.MENSAJEERROR);
									}
								} catch (Exception e) {
									logger.info("Ha ocurrido un error en el proceso: " + Constantes.GPISWIFTDETAILTRANSACTION, Servicios.class);
									xmlResponse = respuestaGenerica(pt.obtenerValor("servicio"), pt.obtenerValor(
											"servicio"), "0404", Constantes.ERRORGENERICO);
									logger.error("error -> ", e);
								}
								break;
							}
							case Constantes.GPISWIFTVALIDATIONACCOUNT: {
								try {

								} catch (Exception e) {
									logger.error("error -> ", e);
								}
								break;
							}
						}
					} finally {
						logger.info("Mensaje recibido correctamente del CORE", Servicios.class);
					}
				}
				/*****************************Conexión con Swift - Fin****************************************/
				if (xmlResponse != null) {
					logger.error("RESPONSE: [{}]", xmlResponse);
				}
			}
			}catch(Exception e){
				logger.error("Error ", e);
			}
			try {
				fin = System.currentTimeMillis();
				logger.info("tiempo del servicio + envio: {} s", ((float) (fin - inicio) / 1000));
			} catch (Exception ignored) {
				logger.info("tiempo del servicio + envio: 0s");
			}
	}

	/**
	 */

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static void enviarRespuestaMQ(String xml, byte[] correlation, String destino) {
		// Establecer la propiedad del sistema para deshabilitar la autenticación basada en MQCSP
		logger.info(" >> EscuchadorBean, enviarRespuestaMQ(): ingresa a enviar respuesta");
		int error = getConnectionData(MDBResources.MDB_DEFAULT_OUTPUT_QUEUE, destino);
		//llamar metodo conexionMQ
		if(error == 0){
			logger.info(" >> EscuchadorBean, enviarRespuestaMQ(): se intenta enviar respuesta por MQ ");

			MQQueueManager qMgr = null;
			MQQueue queue = null;
			try{
				MQEnvironment.hostname = qHost;
				MQEnvironment.port = qPort;
				MQEnvironment.channel = qChannel ;
				MQEnvironment.userID=qUsr;
				MQEnvironment.password=qPass;

				MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY,MQC.TRANSPORT_MQSERIES);
				MQEnvironment.properties.put(MQConstants.USE_MQCSP_AUTHENTICATION_PROPERTY, false);
				qMgr = new MQQueueManager(qManager);
				int openOptions = MQC.MQOO_OUTPUT | MQC.MQOO_FAIL_IF_QUIESCING;
				queue =qMgr.accessQueue(qName,openOptions,null,null,null);
				MQPutMessageOptions pmo = new MQPutMessageOptions();
				pmo.options = pmo.options + MQC.MQPMO_NEW_MSG_ID ;
				pmo.options = pmo.options + MQC.MQPMO_SYNCPOINT ;
				MQMessage outMsg = new MQMessage();
				outMsg.format = MQC.MQFMT_STRING ;
				outMsg.messageFlags = MQC.MQMT_REQUEST ;
				outMsg.writeString(xml);
				outMsg.correlationId = correlation;
				outMsg.expiry = 300000;
				queue.put(outMsg,pmo);
				qMgr.commit();
				error = -1; //Operacion satisfactoria
				logger.info(" >> EscuchadorBean, enviarRespuestaMQ(): se ha enviado respuesta por MQ ");
			}
			catch (MQException ex){
				logger.error(ex.getMessage());
				if(ex.completionCode!=2){
					logger.error("[ERROR-MQ]An MQ Error Occurred:Completion Code is : {}  The Reason Code is : {}",ex.completionCode, ex.reasonCode );
					error++;
				}
			}
			catch(Exception e){
				logger.error("[ERROR-MQ]: {}",e.getMessage());
				error++;
			}
			finally{
				try{
					if (queue != null) {
						queue.close();
					}
					if (qMgr != null) {
						qMgr.disconnect();
						qMgr.close();
					}
				}catch(Exception e){logger.error(e.getMessage());}
			}
		}
		logger.info(" >> EscuchadorBean, enviarRespuestaMQ(): se ha completado envio de respuesta por MQ con mensaje de retorno = {}",error);
	}

	/*----IMPRIMIR ESTADISTICAS DE LOS SERVICIOS + TIEMPO DE ENVIO------*/
	public static int getConnectionData(String[] propiedadesMQ, String destino){
		int error =0;
		if(propiedadesMQ != null && propiedadesMQ.length > 0){
			logger.info(" >> EscuchadorBean, enviarRespuestaMQ(): se obtienen las propiedades MQ {}",propiedadesMQ.length);
			try{
				qHost		= propiedadesMQ[0];
				qManager	= propiedadesMQ[1];

				qPort=getqPort(propiedadesMQ);
				qChannel	= propiedadesMQ[3];
				qUsr= propiedadesMQ[5];
				qPass=propiedadesMQ[6];
				if(destino!=null && !destino.isEmpty()){
					logger.info(">> EscuchadorBean, enviarRespuestaMQ():Se asigna nombre de la cola: {}",destino);
					qName		= destino;
				}else{
					logger.info(">> EscuchadorBean, enviarRespuestaMQ():Nombre de cola null se asigna el por defecto:");
					qName		= propiedadesMQ[4];
				}
			}catch(NullPointerException e){
				error = 99;
			}
		}else{
			error = 99;
		}
		return error;
	}

	public static int getqPort(String[] propiedades) {
		int puerto;
		try{
			puerto	= Integer.parseInt(propiedades[2]);
		}catch(NumberFormatException e){
			puerto	= 1414;
		}
		return puerto;
	}

	//METODO DE LA RESPUESTA GENERICA LO QUE RECIBE EL IBS.
	private String respuestaGenerica(String id, String idTransaccion, String codigo, String descripcion) {
		StringBuffer xml = new StringBuffer("<Response>\n");
		xml.append("<id>" + id + "</id>");
		xml.append("<idTransaccion>" + Helper.completeLeftWith(idTransaccion, ' ', 15) + "</idTransaccion>");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		sdf.applyPattern("yyyyMMddHHmmss");
		Date fecha = new Date();
		String sFecha = sdf.format(fecha);
		xml.append("<fechaHora>" + sFecha + "</fechaHora>\n");
		xml.append("<codigo>" + codigo + "</codigo>\n");
		xml.append("<descripcion>" + Helper.completeLeftWith(descripcion, ' ', 100) + "</descripcion>\n");
		xml.append("</Response>\n");
		return xml.toString();
	}

}

