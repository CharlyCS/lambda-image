package com.gope.image.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gope.image.util.RequestUtil;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class UploadImageHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final S3Client s3Client;
    private static final String BUCKET_NAME = "tour-stack-bucket1";
    public UploadImageHandler() {
        s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Cambia la región según tu configuración
                .build();
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(RequestUtil.buildHeader());

        String imageData = input.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        List<ObjectNode> imageInfoList = new ArrayList<>();

        try {
            JsonNode jsonArray = objectMapper.readTree(imageData);

            for (JsonNode jsonObject : jsonArray) {
                String imageDataWithPrefix = jsonObject.get("image").asText();
                String imageDataBase64 = imageDataWithPrefix.split(",")[1];
                String filename = jsonObject.get("name").asText();
                String filetype = jsonObject.get("filetype").asText();
                Integer size = jsonObject.get("size").asInt();

                // Añadir la fecha actual al nombre del archivo
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
                String filenameWithTimestamp = timestamp + "_" + filename;

                byte[] imageBytes = Base64.getDecoder().decode(imageDataBase64.getBytes(StandardCharsets.UTF_8));
                InputStream inputStream = new ByteArrayInputStream(imageBytes);


                // Cargar la imagen en S3
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(filenameWithTimestamp)
                        .contentType(filetype)
                        .build(), RequestBody.fromInputStream(inputStream, imageBytes.length));

                String objectUrl = s3Client.utilities().getUrl(GetUrlRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(filenameWithTimestamp)
                        .build()).toExternalForm();

                // Agregar información de la imagen a la lista
                ObjectNode imageInfo = objectMapper.createObjectNode();
                imageInfo.put("url", objectUrl);
                imageInfo.put("name", filenameWithTimestamp);
                imageInfo.put("size", size);
                imageInfoList.add(imageInfo);
            }

            // Convertir la lista a JSON y establecer como cuerpo de la respuesta
            String jsonResponse = objectMapper.writeValueAsString(imageInfoList);
            response.setStatusCode(200);
            response.setBody(jsonResponse);
        } catch (S3Exception e) {
            response.setStatusCode(500);
            response.setBody("Error al cargar la imagen en S3: " + e.getMessage());
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error al cargar la imagen: " + e.getMessage());
        }
        return response;
    }
}
