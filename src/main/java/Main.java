import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.HttpEntity;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String API_KEY = Main.getApiKey();
    private static final String API_URL = "https://api.nasa.gov/planetary/apod";

    public static void main(String[] args) {
        //System.out.println(API_KEY);
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(5, TimeUnit.SECONDS)
                .setSocketTimeout(20, TimeUnit.SECONDS)
                .build();

        RequestConfig reqConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(5, TimeUnit.SECONDS)
                .build();

        BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        cm.setConnectionConfig(connConfig);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(reqConfig)
                .setConnectionManager(cm)
                .build()) {

            //HttpGet request = new HttpGet(API_URL);
            ClassicHttpRequest request = ClassicRequestBuilder.get(API_URL)
                    .addParameter("api_key", API_KEY)
                    .build();

            String imageURL = "";
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                //String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                //System.out.println(body);
                JsonNode node = new ObjectMapper().readTree(response.getEntity().getContent());
                if (!node.has("hdurl")) {
                    System.out.println("ERROR cannot find hdurl field in received json object");
                    System.exit(1);
                }
                imageURL = node.get("hdurl").asText();
            }
            System.out.println("hdurl: " + imageURL);
            String fileName = getFileNameFromURL(imageURL);
            System.out.println(fileName);

            request = ClassicRequestBuilder.get(imageURL).build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Arrays.stream(response.getHeaders()).forEach(System.out::println);
                HttpEntity ent = response.getEntity();
                if (ent == null) {
                    System.out.println("ERROR cannot download image");
                    System.exit(1);
                }
                int bufferSize = 1024 * 1024; // 1MB should be enough
                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName), bufferSize)) {
                    ent.writeTo(outputStream);
                    outputStream.flush();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // Make it a little bit more secure than storing a key as plaintext
    private static String getApiKey() {
        String prefix = "hwXUMAwCvIdIwGpOodLk";
        String suffix = "NXAcmp7XV5hHO3uAbAuk";
        return new StringBuilder(prefix+suffix).reverse().toString();
    }

    private static String getFileNameFromURL(String url) {
        try {
            String path = new URI(url).getPath();
            return (path == null) ? null : path.substring(path.lastIndexOf('/') + 1);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
