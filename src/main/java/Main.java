import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String API_KEY = Main.getApiKey();
    private static final String API_URL = "https://api.nasa.gov/planetary/apod";
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB should be enough

    public static void main(String[] args) {
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

            String imageURL = "";
            ClassicHttpRequest request = ClassicRequestBuilder.get(API_URL)
                    .addParameter("api_key", API_KEY)
                    .build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                JsonNode node = new ObjectMapper().readTree(response.getEntity().getContent());
                if (!node.has("hdurl")) {
                    throw new IOException("cannot find hdurl field in received json object");
                }
                imageURL = node.get("hdurl").asText();
            }

            System.out.println("hdurl: " + imageURL);
            String fileName = getFileNameFromURL(imageURL);
            if (fileName == null) {
                throw new IOException("cannot get filename for storing the image");
            }
            System.out.println("filename: " + fileName);

            request = ClassicRequestBuilder.get(imageURL).build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity ent = response.getEntity();
                if (ent == null) {
                    throw new IOException("cannot download image " + imageURL);
                }

                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName), BUFFER_SIZE)) {
                    ent.writeTo(outputStream);
                    outputStream.flush();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
