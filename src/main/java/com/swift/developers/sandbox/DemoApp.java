package com.swift.developers.sandbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.swift.developers.sandbox.exception.ApiSessionException;
import com.swift.developers.sandbox.util.SandboxUtil;
import com.swift.sdk.common.entity.ConnectionInfo;
import com.swift.sdk.common.entity.SecurityFootprintType;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.model.BusinessService12Code;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.model.PaymentScenario6Code;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.model.PaymentStatusRequest2;
import com.swift.sdk.gpi.tracker.v5.status.update.cov.model.PaymentScenario7Code;
import com.swift.sdk.gpi.tracker.v5.status.update.cov.model.PaymentStatusRequest3;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.BusinessService18Code;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.PaymentScenario8Code;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.PaymentStatusRequest7;
import com.swift.sdk.gpi.tracker.v5.status.update.inst.model.BusinessService16Code;
import com.swift.sdk.gpi.tracker.v5.status.update.inst.model.PaymentStatusRequest5;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.PaymentStatusRequest4;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.TransactionIndividualStatus5Code;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.CancelTransactionApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetChangedPaymentTransactionsApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetPaymentTransactionDetailsApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.TransactionCancellationStatusApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.model.*;
import com.swift.sdk.management.util.Util;
import com.swift.sdk.model.OauthProfile;
import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Scanner;


public class DemoApp {
    private static JsonObject configJson = null;
    private static ConnectionInfo connectionInfo = null;
    private static OauthProfile oauthProfile = null;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage : DemoApp <Configuration File Name>");
            System.exit(-1);
        } else if(args.length >= 1 && !(args[0].endsWith(".yaml") || args[0].endsWith(".json"))) {
            System.out.println("Please use file type yaml or json for configurations");
            System.exit(-1);
        } else{
            System.out.println("Using the configuration file - " + args[0] + " to configure SDK Demo App.");
        }
        try {
            if(args[0].endsWith(".yaml")){
                configJson = SandboxUtil.readConfigurationPropertiesYaml(args[0]);
            }
            else{
                configJson = SandboxUtil.readConfigurationPropertiesJson(args[0]);
            }
            connectionInfo = Util.createConnectionInfo(configJson);
            connectionInfo.setSecurityFootprintType(SecurityFootprintType.SOFT);
            System.out.println("\nConnectionInfo read from " + connectionInfo);

            oauthProfile = OauthProfile.builder()
                    .scope("swift.apitracker/FullViewer")
                    .userDn("CN=sandbox, O=Swift, L=sdk, ST=Sandbox, C=BE")
                    .audience("sandbox.swift.com/oauth2/v1/token")
                    .build();

            System.out.println("\nOauthProfile read from " + oauthProfile.toString());

            String number = "";
            int num = 0;
            Scanner scan = new Scanner(System.in);

            do {
                scan = new Scanner(System.in);
                System.out.print("\n--------------Select the API you would like to call-------------------\n");
                System.out.print("1 - CCT StatusConfirmation\n" + "2 - COV StatusConfirmation\n" + "3 - FIT StatusConfirmation\n"
                        + "4 - INST StatusConfirmation\n" + "5 - Universal StatusConfirmation\n" + "6 - getPaymentTransactionDetails\n"
                        + "7 - getChangedPaymentTransaction\n" + "8 - CancelTransaction\n" + "9 - TransactionCancellationStatus\n"
                        + "\nSelect an API you would like to call or 'bye' to exit: ");
                number = scan.nextLine();

                if (!number.equalsIgnoreCase("")) {
                    if (StringUtils.isNumeric(number)) {
                        num = Integer.parseInt(number);
                        if (num == 1) {
                            StatusConfirmationCCT();
                        } else if (num == 2) {
                            StatusConfirmationCOV();
                        }else if (num == 3) {
                            StatusConfirmationFIT();
                        }else if (num == 4) {
                            StatusConfirmationINST();
                        }else if (num == 5) {
                            StatusConfirmationUni();
                        }else if (num == 6) {
                            getPaymentTransactionDetails();
                        } else if (num == 7) {
                            getChangedPaymentTransaction();
                        } else if (num == 8) {
                            //CancelTransaction();
                        } else if (num == 9) {
                            //TransactionCancellationStatus();
                        }
                    }
                }
            } while (number.equalsIgnoreCase("") || !number.equalsIgnoreCase("bye"));

            scan.close();

        } catch (ApiSessionException ex) {
            // TODO Auto-generated catch block
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void StatusConfirmationCCT() {
        try {
            com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi status = new com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi(connectionInfo);
            PaymentStatusRequest2 reqBody = new PaymentStatusRequest2();
            reqBody.setFrom("BANCUS33XXX");
            reqBody.setInstructionIdentification("jkl000");
            reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.TransactionIndividualStatus5Code.ACCC);
            reqBody.setTrackerInformingParty("BANABEBBXXX");
            reqBody.setServiceLevel(BusinessService12Code.G001);
            reqBody.setPaymentScenario(PaymentScenario6Code.CCTR);

            String uetr = "46ed4827-7b6f-4491-a06f-b548d5a7512d";
            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-gcct/v5";
            status.setBasePath(basePath);
            System.out.println("PASO 1 ---------------");
            com.swift.sdk.gpi.tracker.v5.status.update.cct.ApiResponse resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody,oauthProfile);

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/status\n";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(reqBody);
            String response = "\n" + resp.getStatusCode();

            System.out.println("\nREQUEST" + url + "\nBody:\n" + jsonOutput + "\n" + "\nRESPONSE" + response);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.cct.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void StatusConfirmationCOV() {
        try {
            com.swift.sdk.gpi.tracker.v5.status.update.cov.api.StatusConfirmationsApi status = new com.swift.sdk.gpi.tracker.v5.status.update.cov.api.StatusConfirmationsApi(connectionInfo);
            PaymentStatusRequest3 reqBody = new PaymentStatusRequest3();
            reqBody.setFrom("BANCUS33XXX");
            reqBody.setInstructionIdentification("jkl000");
            reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cov.model.TransactionIndividualStatus5Code.ACCC);
            reqBody.setTrackerInformingParty("BANABEBBXXX");
            reqBody.setServiceLevel(com.swift.sdk.gpi.tracker.v5.status.update.cov.model.BusinessService12Code.G001);
            reqBody.setPaymentScenario(PaymentScenario7Code.COVE);
            reqBody.setEndToEndIdentification("123Ref");

            String uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";

            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-gcov/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.status.update.cov.ApiResponse resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody,oauthProfile);

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/status\n";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(reqBody);
            String response = "\n" + resp.getStatusCode();

            System.out.println("\nREQUEST" + url + "\nBody:\n" + jsonOutput + "\n" + "\nRESPONSE" + response);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.cov.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void StatusConfirmationFIT() {
        try {
            com.swift.sdk.gpi.tracker.v5.status.update.fit.api.StatusConfirmationsApi status = new com.swift.sdk.gpi.tracker.v5.status.update.fit.api.StatusConfirmationsApi(connectionInfo);
            PaymentStatusRequest7 reqBody = new PaymentStatusRequest7();
            reqBody.setFrom("BANCUS33XXX");
            reqBody.setInstructionIdentification("jkl000");
            reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.fit.model.TransactionIndividualStatus5Code.ACCC);
            reqBody.setTrackerInformingParty("BANABEBBXXX");
            reqBody.setServiceLevel(BusinessService18Code.G004);
            reqBody.setPaymentScenario(PaymentScenario8Code.FCTR);
            reqBody.setEndToEndIdentification("123Ref");

            String uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-gfit/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.status.update.fit.ApiResponse resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody,oauthProfile);

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/status\n";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(reqBody);
            String response = "\n" + resp.getStatusCode();

            System.out.println("\nREQUEST" + url + "\nBody:\n" + jsonOutput + "\n" + "\nRESPONSE" + response);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.fit.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void StatusConfirmationINST() {
        try {
            com.swift.sdk.gpi.tracker.v5.status.update.inst.api.StatusConfirmationsApi status = new com.swift.sdk.gpi.tracker.v5.status.update.inst.api.StatusConfirmationsApi(connectionInfo);
            PaymentStatusRequest5 reqBody = new PaymentStatusRequest5();
            reqBody.setFrom("BANCUS33XXX");
            reqBody.setInstructionIdentification("jkl000");
            reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.inst.model.TransactionIndividualStatus5Code.ACCC);
            reqBody.setTrackerInformingParty("BANABEBBXXX");
            reqBody.setServiceLevel(BusinessService16Code.G005);
            reqBody.setPaymentScenario(com.swift.sdk.gpi.tracker.v5.status.update.inst.model.PaymentScenario6Code.CCTR);

            String uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";

            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-gcct-inst/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.status.update.inst.ApiResponse resp = status.statusConfirmationsWithHttpInfo(uetr, reqBody,oauthProfile);

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/status\n";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(reqBody);
            String response = "\n" + resp.getStatusCode();

            System.out.println("\nREQUEST" + url + "\nBody:\n" + jsonOutput + "\n" + "\nRESPONSE" + response);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.inst.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void StatusConfirmationUni() {
        try{
            com.swift.sdk.gpi.tracker.v5.status.update.universal.api.StatusConfirmationsApi status = new com.swift.sdk.gpi.tracker.v5.status.update.universal.api.StatusConfirmationsApi(connectionInfo);

            PaymentStatusRequest4 reqBody = new PaymentStatusRequest4();
            reqBody.setFrom("BANCUS33XXX");
            reqBody.setInstructionIdentification("jkl000");
            reqBody.setTransactionStatus(TransactionIndividualStatus5Code.ACCC);
            reqBody.setTrackerInformingParty("BANABEBBXXX");

            String uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";
            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker-uc-cct/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.status.update.universal.ApiResponse resp = status.statusConfirmationsWithHttpInfo(uetr,reqBody,oauthProfile);

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/status\n";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(reqBody);
            String response = "\n" + resp.getStatusCode();

            System.out.println("\nREQUEST" + url + "\nBody:\n" + jsonOutput + "\n" + "\nRESPONSE" + response);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.universal.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getPaymentTransactionDetails() {
        try{
            com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetPaymentTransactionDetailsApi status = new GetPaymentTransactionDetailsApi(connectionInfo);
            String uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.ApiResponse resp = status.getPaymentTransactionDetailsWithHttpInfo(uetr,oauthProfile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(resp.getData());

            String url = "\nURL: " + basePath + "/payments/" + uetr + "/transactions" + "\n";

            System.out.println("\nREQUEST" + url + "\nRESPONSE\n " + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getChangedPaymentTransaction() {
        try{
            com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetChangedPaymentTransactionsApi status = new GetChangedPaymentTransactionsApi(connectionInfo);
            OffsetDateTime fromDateTime = OffsetDateTime.parse("2020-04-11T00:00:00.0Z");
            OffsetDateTime toDateTime = OffsetDateTime.parse("2020-04-16T00:00:00.0Z");
            int maxNumber = Integer.parseInt("10");
            String paymentScenario = "CCTR";
            String next = null;

            String basePath = connectionInfo.getGatewayHost() + "/swift-apitracker/v5";
            status.setBasePath(basePath);
            com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.ApiResponse resp = status.getChangedPaymentTransactionsWithHttpInfo(fromDateTime, toDateTime, maxNumber, paymentScenario, next,oauthProfile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(resp.getData());

            String url = "\nURL: " + basePath + "/payments/changed/transactions?from_date_time=" + fromDateTime + "&to_date_time=" + toDateTime + "&maximum_number=" + maxNumber + "\n";

            System.out.println("\nREQUEST" + url + "\nRESPONSE\n " + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.ApiException e) {
            throw new RuntimeException(e);
        }
    }
}