import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PolizasApi } from './polizas-api';

describe('PolizasApi', () => {
  let api: PolizasApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PolizasApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia solo los filtros que tienen valor', () => {
    api.listar({ tipo: 'COLECTIVA', estado: '' }).subscribe();

    const peticion = http.expectOne((r) => r.url === '/api/polizas');

    expect(peticion.request.params.get('tipo')).toBe('COLECTIVA');
    expect(peticion.request.params.has('estado')).toBe(false);
    peticion.flush([]);
  });

  it('no envia la clave del API desde el navegador', () => {
    api.listar({ tipo: '', estado: '' }).subscribe();

    const peticion = http.expectOne('/api/polizas');

    expect(peticion.request.headers.has('x-api-key')).toBe(false);
    peticion.flush([]);
  });

  it('usa las rutas del API para las acciones', () => {
    api.renovar(1).subscribe();
    api.cancelar(2).subscribe();
    api.listarRiesgos(3).subscribe();
    api.agregarRiesgo(4, { arrendatario: 'A', direccionInmueble: 'D' }).subscribe();
    api.cancelarRiesgo(5).subscribe();

    expect(http.expectOne('/api/polizas/1/renovar').request.method).toBe('POST');
    expect(http.expectOne('/api/polizas/2/cancelar').request.method).toBe('POST');
    expect(http.expectOne('/api/polizas/3/riesgos').request.method).toBe('GET');
    expect(http.expectOne({ url: '/api/polizas/4/riesgos', method: 'POST' }).request.body).toEqual({
      arrendatario: 'A',
      direccionInmueble: 'D',
    });
    expect(http.expectOne('/api/riesgos/5/cancelar').request.method).toBe('POST');
  });
});
