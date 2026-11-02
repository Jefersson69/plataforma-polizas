package com.pruebatecnica.polizas.core;

import com.pruebatecnica.polizas.dto.CoreEventoRequest;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class HttpCoreClient implements CoreClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final RestClient restClient;

    HttpCoreClient(CoreProperties propiedades) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(propiedades.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", propiedades.apiKey())
                .build();
    }

    @Override
    public void notificarActualizacion(Long polizaId) {
        restClient.post()
                .uri("/core-mock/evento")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoreEventoRequest("ACTUALIZACION", polizaId))
                .retrieve()
                .toBodilessEntity();
    }
}
