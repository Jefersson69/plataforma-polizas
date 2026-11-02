import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FiltrosPoliza, NuevoRiesgo, Poliza, Riesgo } from './poliza.model';

/**
 * La clave no va en el navegador: en desarrollo la pone el proxy
 * y en prod le tocaria al API Gateway.
 */
const BASE = '/api';

@Injectable({ providedIn: 'root' })
export class PolizasApi {
  private readonly http = inject(HttpClient);

  listar({ tipo, estado }: FiltrosPoliza): Observable<Poliza[]> {
    let params = new HttpParams();
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<Poliza[]>(`${BASE}/polizas`, { params });
  }

  renovar(id: number): Observable<Poliza> {
    return this.http.post<Poliza>(`${BASE}/polizas/${id}/renovar`, null);
  }

  cancelar(id: number): Observable<Poliza> {
    return this.http.post<Poliza>(`${BASE}/polizas/${id}/cancelar`, null);
  }

  listarRiesgos(polizaId: number): Observable<Riesgo[]> {
    return this.http.get<Riesgo[]>(`${BASE}/polizas/${polizaId}/riesgos`);
  }

  agregarRiesgo(polizaId: number, riesgo: NuevoRiesgo): Observable<Riesgo> {
    return this.http.post<Riesgo>(`${BASE}/polizas/${polizaId}/riesgos`, riesgo);
  }

  cancelarRiesgo(id: number): Observable<Riesgo> {
    return this.http.post<Riesgo>(`${BASE}/riesgos/${id}/cancelar`, null);
  }
}
