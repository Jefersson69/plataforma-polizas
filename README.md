# Plataforma de pólizas

Prueba técnica: una API en Spring Boot y, como extra opcional, un front en Angular.

## Cómo correrlo

Para la API solo se necesita Java 21:

```bash
cd polizas-api && ./mvnw spring-boot:run
```

Y ya, queda en http://localhost:8080 con 10 pólizas de ejemplo. Si quieres ver el front, con Node 22 y en otra terminal:

```bash
cd polizas-web && npm install --legacy-peer-deps && npm start
```

Queda en http://localhost:4200. El detalle de cada uno está en su README.
