package com.agricola.servicio;

import com.ba.escuchador.utils.Servicios;
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
import java.util.List;
import com.ba.escuchador.resources.MDBResources;

public class ConexionAPI {
    static Logger LOG = LogManager.getLogger(ConexionAPI.class.getName());

    public static String StatusConfirmation(String uetr,
                                            String bic,
                                            String instructionIdentification,
                                            String originator,
                                            String fundsAvailableDate,
                                            String currencySymbol,
                                            String confirmedAmount,
                                            String chargeAmount,
                                            String transactionStatus,
                                            String transactionStatusReason,
                                            String businessService) throws Exception {
        String response = "";
        File connectionFile = ResourceUtils.getFile("classpath:config\\config-swift-connect.yaml");
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

                StatusConfirmationsApi status =
                        new StatusConfirmationsApi(connectionInfo);

                PaymentStatusRequest2 reqBody = new PaymentStatusRequest2();
                ActiveCurrencyAndAmount activeCurrencyAndAmount0 = new ActiveCurrencyAndAmount();
                String confirmedAmountStr = confirmedAmount.replace("USD-","");
                double confirmedAmountDbl = Double.parseDouble(confirmedAmountStr)/100;

                List<Charges8> cargos = generaCargos(bic, chargeAmount);

                reqBody.setFrom(bic);
                if(businessService.equals("001")){
                    reqBody.setServiceLevel(BusinessService12Code.G001);
                }
                reqBody.setPaymentScenario(PaymentScenario6Code.CCTR);
                reqBody.setInstructionIdentification(instructionIdentification);
                reqBody.setTrackerInformingParty(originator);
                if(!fundsAvailableDate.isEmpty()){
                    reqBody.setConfirmedDate(convertirEnFechaUTC(fundsAvailableDate));
                }
                switch(transactionStatus){
                    case "ACCC":
                        reqBody.setTransactionStatus(TransactionIndividualStatus5Code.ACCC);
                        break;
                    case "ACSP":
                        reqBody.setTransactionStatus(TransactionIndividualStatus5Code.ACSP);
                        break;
                    case "RJCT":
                        reqBody.setTransactionStatus(TransactionIndividualStatus5Code.RJCT);
                        reqBody.setRejectReturnReason(ExternalPaymentStatusReason3Code.valueOf(transactionStatusReason));
                        break;
                }
                switch(transactionStatusReason){
                    case "G000":
                        reqBody.setTransactionStatusReason(PaymentStatusReason10Code.G000);
                        break;
                    case "G001":
                        reqBody.setTransactionStatusReason(PaymentStatusReason10Code.G001);
                        break;
                    case "G002":
                        reqBody.setTransactionStatusReason(PaymentStatusReason10Code.G002);
                        break;
                    case "G003":
                        reqBody.setTransactionStatusReason(PaymentStatusReason10Code.G003);
                        break;
                }
                reqBody.setConfirmedAmount(activeCurrencyAndAmount0.currency(currencySymbol));
                reqBody.setConfirmedAmount(activeCurrencyAndAmount0.amount(String.valueOf(confirmedAmountDbl)));
                reqBody.setChargesInformation(cargos);
                LOG.info("\nBODY: {}",reqBody.toString());
                //String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-pilot/v5"; Cambiar para ambiente de pruebas
                String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-gcct/v5";
                status.setBasePath(basePath);
                LOG.info("\nREQUEST: {}/payments/{}/status",basePath,uetr);

                ApiResponse<Void> resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody, oauthProfile);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonOutput = gson.toJson(reqBody);

                response = String.valueOf(resp.getStatusCode());

                LOG.info("\nRESPONSE: {}", response);
            } catch (ApiException ex) {
                LOG.info("ERROR > MESSAGE: [{}]", ex.getMessage());
                LOG.info("ERROR > RESPONSE CODE: [{}]", ex.getCode());
                LOG.info("ERROR > RESPONSE BODY: [{}]", ex.getResponseBody());
            } catch (Exception ex) {
                LOG.error("ERROR -> ", ex);
            }
        }else{
            return "ERROR 500: archivo YAML no existe o no fue encontrado.";
        }
        return (response.equals("200"))?response:"ERROR 500: Solicitud de datos denegada.";
    }

    public static List<Charges8> generaCargos(String bic, String chargeAmount){
            ActiveCurrencyAndAmount activeCurrencyAndAmount1 = new ActiveCurrencyAndAmount();
            ActiveCurrencyAndAmount activeCurrencyAndAmount2 = new ActiveCurrencyAndAmount();
            ActiveCurrencyAndAmount activeCurrencyAndAmount3 = new ActiveCurrencyAndAmount();

            BranchAndFinancialInstitutionIdentification1Choice bfiic1 = new
                    BranchAndFinancialInstitutionIdentification1Choice();
            bfiic1.setBicfi(bic);
            BranchAndFinancialInstitutionIdentification1Choice bfiic2 = new
                    BranchAndFinancialInstitutionIdentification1Choice();
            bfiic2.setBicfi(bic);
            BranchAndFinancialInstitutionIdentification1Choice bfiic3 = new
                    BranchAndFinancialInstitutionIdentification1Choice();
            bfiic3.setBicfi(bic);

            Charges8 comision1Chr = new Charges8();
            Charges8 comision2Chr = new Charges8();
            Charges8 comision3Chr = new Charges8();

            String[] extraAmounts = null;
            String comision1 = "";
            String comision2 = "";
            String comision3 = "";

            double comision1Dbl = 0.0;
            double comision2Dbl = 0.0;
            double comision3Dbl = 0.0;
            double sumaCargos = comision2Dbl + comision3Dbl;

            if(chargeAmount.contains(",")){
                extraAmounts = chargeAmount.split(",");
                if(extraAmounts.length == 2){
                    comision1 = (!extraAmounts[0].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[0].replaceAll("\\D+", ""):"0.0";
                    comision2 = (!extraAmounts[1].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[1].replaceAll("\\D+", ""):"0.0";

                    comision1Dbl = (comision1.isEmpty()|comision1.equals("0.0"))?0.0:Double.parseDouble(comision1)/100;
                    comision2Dbl = (comision2.isEmpty()|comision2.equals("0.0"))?0.0:Double.parseDouble(comision2)/100;
                    comision3Dbl = 0.0;

                    comision1Chr.setAmount(activeCurrencyAndAmount1.amount(String.valueOf(comision1Dbl)));
                }

                if(extraAmounts.length == 3){
                    comision1 = (!extraAmounts[0].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[0].replaceAll("\\D+", ""):"0.0";
                    comision2 = (!extraAmounts[1].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[1].replaceAll("\\D+", ""):"0.0";
                    comision3 = (!extraAmounts[2].isBlank()|!extraAmounts[0].isEmpty())?extraAmounts[2].replaceAll("\\D+", ""):"0.0";

                    comision1Dbl = (comision1.isEmpty()|comision1.equals("0.0"))?0.0:Double.parseDouble(comision1)/100;
                    comision2Dbl = (comision2.isEmpty()|comision2.equals("0.0"))?0.0:Double.parseDouble(comision2)/100;
                    comision3Dbl = (comision3.isEmpty()|comision3.equals("0.0"))?0.0:Double.parseDouble(comision3)/100;

                    comision1Chr.setAmount(activeCurrencyAndAmount1.amount(String.valueOf(comision1Dbl)));
                }
            }else{
                    comision1 = (!chargeAmount.isBlank()|!chargeAmount.isEmpty())?chargeAmount.replaceAll("\\D+", ""):"0.0";
                    comision1Dbl = (comision1.isEmpty()|comision1.equals("0.0"))?0.0:Double.parseDouble(comision1)/100;
                    comision1Chr.setAmount(activeCurrencyAndAmount1.amount(String.valueOf((comision1Dbl))));
            }

        comision1Chr.setAmount(activeCurrencyAndAmount1.currency("USD"));
        comision1Chr.setAgent(bfiic1);
        comision2Chr.setAmount(activeCurrencyAndAmount2.currency("USD"));
        comision2Chr.setAgent(bfiic2);
        comision3Chr.setAmount(activeCurrencyAndAmount3.currency("USD"));
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
            if(fecha.isBlank()){
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
