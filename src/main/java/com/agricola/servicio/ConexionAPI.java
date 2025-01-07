package com.agricola.servicio;

import com.ba.escuchador.utils.Servicios.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.swift.developers.sandbox.util.SandboxUtil;
import com.swift.sdk.common.entity.ConnectionInfo;
import com.swift.sdk.common.entity.SecurityFootprintType;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.ApiException;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.ApiResponse;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.model.*;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.ExternalPaymentStatusReason5Code;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.PaymentStatusRequest4;
import com.swift.sdk.model.OauthProfile;
import com.swift.sdk.management.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.ba.escuchador.utils.Servicios.obtenerPropiedadesServicios;

public class ConexionAPI {
    static Logger LOG = LogManager.getLogger(ConexionAPI.class.getName());
    static String response = "";
    static String jsonOutput = "";
    static String[] extraAmounts = null;
    static String[] propiedadesUtil = null;
    static String comision1 = "";
    static String comision2 = "";
    static String comision3 = "";
    static double comision1Dbl = 0.0;
    static double comision2Dbl = 0.0;
    static double comision3Dbl = 0.0;
    static String uetr = "";
    static String bic = "";;
    static String instructionIdentification = "";
    static String originator = "";
    static String fundsAvailableDate = "";
    static String currencySymbol = "";
    static String confirmedAmount = "";
    static String chargeAmount = "";
    static String transactionStatus = "";
    static String transactionStatusReason = "";
    static String businessService = "";
    static File connectionFile = null;
    static String confirmedAmountStr = "";
    static double confirmedAmountDbl = 0.0;

    public static String StatusConfirmation(HashMap<String, String> parametros) throws Exception {
        uetr = parametros.get("uetr");
        bic = parametros.get("from");;
        instructionIdentification = parametros.get("instruction_identification");
        originator = parametros.get("originator");
        fundsAvailableDate = parametros.get("funds_available");
        currencySymbol = "USD";
        confirmedAmount = parametros.get("confirmed_amount");
        chargeAmount = parametros.get("charge_amount");
        transactionStatus = parametros.get("transaction_status");
        transactionStatusReason = parametros.get("transaction_status_reason");
        businessService = parametros.get("business_service");
        connectionFile = ResourceUtils.getFile("classpath:config\\config-swift-connect.yaml");
        if(connectionFile.exists() && !connectionFile.isDirectory()) {
            try {
                JsonObject configJson = SandboxUtil.readConfigurationPropertiesYaml(connectionFile.toString());
                ConnectionInfo connectionInfo = Util.createConnectionInfo(configJson);
                connectionInfo.setSecurityFootprintType(SecurityFootprintType.SOFT);

                OauthProfile oauthProfile = OauthProfile.builder()
                        .scope(connectionInfo.getScope())
                        .userDn("CN=sandbox, O=Swift, L=sdk, ST=Sandbox, C=BE")
                        .audience(connectionInfo.getAudience())
                        .build();
                //LOG.info("\n\n*** oauthProfile: passed, \n{}",oauthProfile.toString());

                propiedadesUtil = obtenerPropiedadesServicios("configuracionRequestGpiSwift");
                response = CCT(oauthProfile, connectionInfo);

                LOG.info("\nRESPONSE: {}", response);
            } catch (Exception ex) {
                LOG.error("ERROR -> ", ex);
            }
        }else{
            return "ERROR 500: archivo YAML no existe o no fue encontrado.";
        }
        return (response.equals("200"))?response:"ERROR 500: Solicitud de datos denegada.";
    }

    public static String CCT(OauthProfile oauthProfile, ConnectionInfo connectionInfo){
        try {
            com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi status =
                    new com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi(connectionInfo);
            com.swift.sdk.gpi.tracker.v5.status.update.cct.model.ActiveCurrencyAndAmount activeCurrencyAndAmount0 =
                    new com.swift.sdk.gpi.tracker.v5.status.update.cct.model.ActiveCurrencyAndAmount();
            PaymentStatusRequest2 reqBody = new PaymentStatusRequest2();
            confirmedAmountStr = (!confirmedAmount.isBlank()|!confirmedAmount.isEmpty())?confirmedAmount.replaceAll("\\D+", ""):"0.0";
            double numero = (confirmedAmountStr.equals("0.0"))?0.0:Double.parseDouble(confirmedAmountStr)/100;
            confirmedAmountDbl = Math.round(numero * Math.pow(10, 3)) / Math.pow(10, 3);

            List<com.swift.sdk.gpi.tracker.v5.status.update.cct.model.Charges8> cargos = generaCargosCCT();

            reqBody.setFrom(bic);
            reqBody.setPaymentScenario(PaymentScenario6Code.CCTR);
            reqBody.setInstructionIdentification(instructionIdentification);
            reqBody.setTrackerInformingParty(originator);

            if(transactionStatus.equals("ACCC")){
                reqBody.setConfirmedAmount(activeCurrencyAndAmount0.currency(currencySymbol));
                reqBody.setConfirmedAmount(activeCurrencyAndAmount0.amount(String.valueOf(confirmedAmountDbl)));
                reqBody.setTransactionStatus(TransactionIndividualStatus5Code.valueOf(transactionStatus));
                if(businessService.equals("001")){
                    reqBody.setServiceLevel(BusinessService12Code.G001);
                }
                reqBody.setConfirmedDate(convertirEnFechaUTC(fundsAvailableDate));
            }

            if(transactionStatus.equals("ACSP")){
                reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.TransactionIndividualStatus5Code.ACSP);
                reqBody.setTransactionStatusReason(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.PaymentStatusReason10Code.valueOf(transactionStatusReason));
                if(transactionStatusReason.equals("G001") || transactionStatusReason.equals("G002")){
                    reqBody.setSettlementMethod(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.SettlementMethod1Code.INGA);
                    reqBody.setSourceCurrency(currencySymbol);
                    reqBody.setTargetCurrency(currencySymbol);
                }

                if(transactionStatusReason.equals("G000") || transactionStatusReason.equals("G001")){
                    reqBody.setTransactionStatusDate(convertirEnFechaUTC(fundsAvailableDate));
                    reqBody.setChargeBearer(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.ChargeBearerType3Code.SHAR);
                    reqBody.setInterbankSettlementAmount(activeCurrencyAndAmount0.currency(currencySymbol));
                    reqBody.setInterbankSettlementAmount(activeCurrencyAndAmount0.amount(String.valueOf(confirmedAmountDbl)));
                    reqBody.setSettlementMethod(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.SettlementMethod1Code.INGA);
                    reqBody.setSourceCurrency(currencySymbol);
                    reqBody.setTargetCurrency(currencySymbol);
                }
                if(businessService.equals("001")){
                    reqBody.setServiceLevel(BusinessService12Code.G001);
                }
            }

            if(transactionStatus.equals("RJCT")){
                reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.TransactionIndividualStatus5Code.RJCT);
                reqBody.setRejectReturnReason(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.ExternalPaymentStatusReason3Code.valueOf(transactionStatusReason));
                reqBody.setTransactionStatusDate(convertirEnFechaUTC(fundsAvailableDate));
                if(transactionStatusReason.equals("G002") || transactionStatusReason.equals("G003") || transactionStatusReason.equals("G004")){
                    reqBody.setInterbankSettlementAmount(activeCurrencyAndAmount0.currency(currencySymbol));
                    reqBody.setInterbankSettlementAmount(activeCurrencyAndAmount0.amount(String.valueOf(confirmedAmountDbl)));
                }
                if(businessService.equals("001")){
                    reqBody.setServiceLevel(BusinessService12Code.G001);
                }
            }

            if(transactionStatus.equals("ACCC") || transactionStatus.equals("ACSP")){
                if(!transactionStatusReason.equals("G003")){
                    reqBody.setChargesInformation(cargos);
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            jsonOutput = gson.toJson(reqBody);
            LOG.info("\nBODY: {}", jsonOutput);

            String basePath = connectionInfo.getGatewayHost() + propiedadesUtil[0];
            status.setBasePath(basePath);
            LOG.info("\nREQUEST: {}/payments/{}/status",basePath,uetr);

            ApiResponse<Void> resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody, oauthProfile);
            response = String.valueOf(resp.getStatusCode());
        }catch(ApiException ex){
            LOG.info("ERROR > MESSAGE: [{}]", ex.getMessage());
            LOG.info("ERROR > RESPONSE CODE: [{}]", ex.getCode());
            LOG.info("ERROR > RESPONSE BODY: [{}]", ex.getResponseBody());
        }catch (Exception ex) {
            LOG.error("ERROR -> ", ex);
        }
        return response;
    }

    public static List<com.swift.sdk.gpi.tracker.v5.status.update.cct.model.Charges8> generaCargosCCT() throws ApiException {
        ActiveCurrencyAndAmount activeCurrencyAndAmount1 = new ActiveCurrencyAndAmount();
        ActiveCurrencyAndAmount activeCurrencyAndAmount2 = new ActiveCurrencyAndAmount();
        ActiveCurrencyAndAmount activeCurrencyAndAmount3 = new ActiveCurrencyAndAmount();

        BranchAndFinancialInstitutionIdentification1Choice bfiic1 = new BranchAndFinancialInstitutionIdentification1Choice();
        bfiic1.setBicfi(bic);
        BranchAndFinancialInstitutionIdentification1Choice bfiic2 = new BranchAndFinancialInstitutionIdentification1Choice();
        bfiic2.setBicfi(bic);
        BranchAndFinancialInstitutionIdentification1Choice bfiic3 = new BranchAndFinancialInstitutionIdentification1Choice();
        bfiic3.setBicfi(bic);

        Charges8 comision1Chr = new Charges8();
        Charges8 comision2Chr = new Charges8();
        Charges8 comision3Chr = new Charges8();

        double numero1 = 0.0;
        double numero2 = 0.0;
        double numero3 = 0.0;

        if(chargeAmount.contains(",")){
            extraAmounts = chargeAmount.split(",");
            if(extraAmounts.length == 2){
                comision2 = (!extraAmounts[1].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[1].replaceAll("\\D+", ""):"0.0";
                numero2 = (comision2.isEmpty()|comision2.equals("0.0"))?0.0:Double.parseDouble(comision2)/100;
                comision2Dbl = Math.round(numero2 * Math.pow(10, 3)) / Math.pow(10, 3);
                comision3Dbl = 0.0;
            }

            if(extraAmounts.length == 3){
                comision2 = (!extraAmounts[1].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[1].replaceAll("\\D+", ""):"0.0";
                comision3 = (!extraAmounts[2].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[2].replaceAll("\\D+", ""):"0.0";

                numero2 = (comision2.isEmpty()|comision2.equals("0.0"))?0.0:Double.parseDouble(comision2)/100;
                numero3 = (comision3.isEmpty()|comision3.equals("0.0"))?0.0:Double.parseDouble(comision3)/100;

                comision2Dbl = Math.round(numero2 * Math.pow(10, 3)) / Math.pow(10, 3);
                comision3Dbl = Math.round(numero3 * Math.pow(10, 3)) / Math.pow(10, 3);
            }

            comision1 = (!extraAmounts[0].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[0].replaceAll("\\D+", ""):"0.0";
            numero1 = (comision1.isEmpty()|comision1.equals("0.0"))?0.0:Double.parseDouble(comision1)/100;
            comision1Dbl = Math.round(numero1 * Math.pow(10, 3)) / Math.pow(10, 3);

            comision1Chr.setAmount(activeCurrencyAndAmount1.amount(String.valueOf(comision1Dbl)));
        }else{
            comision1 = (!chargeAmount.isBlank()|!chargeAmount.isEmpty())?chargeAmount.replaceAll("\\D+", ""):"0.0";
            numero1 = (comision1.isEmpty()|comision1.equals("0.0"))?0.0:Double.parseDouble(comision1)/100;
            comision1Dbl = Math.round(numero1 * Math.pow(10, 3)) / Math.pow(10, 3);
            comision2Dbl = 0.0;
            comision3Dbl = 0.0;

            comision1Chr.setAmount(activeCurrencyAndAmount1.amount(String.valueOf((comision1Dbl))));
        }

        comision1Chr.setAmount(activeCurrencyAndAmount1.currency(currencySymbol));
        comision1Chr.setAgent(bfiic1);
        comision2Chr.setAmount(activeCurrencyAndAmount2.currency(currencySymbol));
        comision2Chr.setAgent(bfiic2);
        comision3Chr.setAmount(activeCurrencyAndAmount3.currency(currencySymbol));
        comision3Chr.setAgent(bfiic3);

        comision2Chr.setAmount(activeCurrencyAndAmount2.amount("0.0"));
        comision3Chr.setAmount(activeCurrencyAndAmount3.amount(String.valueOf(comision2Dbl + comision3Dbl)));
        LOG.info("\n*** extra amounts: \nComision 1: {}, \nComision 2: {}, \nComision 3: {}",
                String.valueOf(comision1Dbl),String.valueOf(comision2Dbl),String.valueOf(comision3Dbl));

        return Arrays.asList(comision1Chr, comision2Chr, comision3Chr);
    }

    public static String convertirEnFechaUTC(String fecha){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            if(fecha.isBlank() | fecha.isEmpty()){
                LocalDateTime now = LocalDateTime.now();
                fecha = now.format(dtf);
            }
            fecha = fecha.replaceAll("-$", "");
            LocalDateTime ldt = LocalDateTime.parse(fecha, dtf);

            return ldt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        }catch(DateTimeParseException e) {
            throw new IllegalArgumentException("Valor de fecha "+fecha+" no puede ser interpretado: "+e);
        }
    }
}
