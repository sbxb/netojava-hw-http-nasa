import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final String API_KEY = Main.getApiKey();
    private static final String API_URL = "https://api.nasa.gov/planetary/apod";

    public static void main(String[] args) {
        //System.out.println(API_KEY);

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        //HttpGet request = new HttpGet(API_URL);
        ClassicHttpRequest request = ClassicRequestBuilder.get(API_URL)
                .addParameter("api_key", API_KEY)
                .build();
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            //String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            //System.out.println(body);
            //JsonNode node = new ObjectMapper().readTree(body);
            JsonNode node = new ObjectMapper().readTree(response.getEntity().getContent());
            if (node.has("hdurl")) {
                System.out.println("hdurl: " + node.get("hdurl"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getApiKey() {
        String prefix = "hwXUMAwCvIdIwGpOodLk";
        String suffix = "NXAcmp7XV5hHO3uAbAuk";
        return new StringBuilder(prefix+suffix).reverse().toString();
    }
}
