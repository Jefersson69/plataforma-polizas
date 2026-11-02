# polizas-web

Front en Angular 21 con Tailwind. Es un extra, la prueba no lo pide, y necesita la API corriendo. Lista y filtra pólizas, las renueva o cancela, y maneja los riesgos de las colectivas. Las reglas las valida la API.

```bash
npm install --legacy-peer-deps
npm start
npm test
```

Si la instalación falla con `edgesOut` es por una versión vieja de npm, se arregla con `npx -y npm@11 install --legacy-peer-deps`.

La clave no llega al navegador: en desarrollo la pone `proxy.conf.json`, y en prod le tocaría al API Gateway.
