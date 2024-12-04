package com.swift.developers.sandbox.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.swift.developers.sandbox.exception.ApiSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SandboxUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SandboxUtil.class);

    public static JsonObject readConfigurationPropertiesYaml(String configPath) throws ApiSessionException {
        byte[] fileContent = null;
        JsonObject jsonObject = null;

        try {
            //fileContent = Files.readAllBytes((new File(configPath)).toPath());
            //String tmpStr = new String(fileContent, StandardCharsets.UTF_8);
            File fileJson = new File(configPath);
            LOG.info("file path -> {}", fileJson.toPath().toUri());
            InputStream inputStream = new FileInputStream(fileJson);

            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }

            String tmpStr = stringBuilder.toString();
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(tmpStr, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();

            JsonParser jsonParser = new JsonParser();
            // System.out.println(jsonWriter.writeValueAsString(obj));
            jsonObject = (JsonObject) jsonParser.parse(new StringReader(jsonWriter.writeValueAsString(obj)));
        }
        catch (IOException | JsonIOException | JsonSyntaxException ex) {
            throw new ApiSessionException(ex.getMessage(), ex);
        }

        return jsonObject;
    }

    public static JsonObject readConfigurationPropertiesJson(String configPath) throws ApiSessionException {
        JsonObject jsonObject = null;
        JsonParser jsonParser = new JsonParser();

        try {
            Object obj = jsonParser.parse(new FileReader(configPath));
            jsonObject = (JsonObject) obj;
        }
        catch (FileNotFoundException | JsonIOException | JsonSyntaxException ex) {
            throw new ApiSessionException(ex.getMessage(), ex);
        }

        // jsonObject = new JsonParser().parse(configPath).getAsJsonObject();
        return jsonObject;
    }
}
