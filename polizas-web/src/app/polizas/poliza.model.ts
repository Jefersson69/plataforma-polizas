export type TipoPoliza = 'INDIVIDUAL' | 'COLECTIVA';
export type EstadoPoliza = 'ACTIVA' | 'RENOVADA' | 'CANCELADA';
export type EstadoRiesgo = 'ACTIVO' | 'CANCELADO';

export interface Poliza {
  id: number;
  tipo: TipoPoliza;
  estado: EstadoPoliza;
  tomador: string;
  beneficiario: string;
  fechaInicio: string;
  fechaFin: string;
  mesesVigencia: number;
  canon: number;
  prima: number;
  ipcAnioAplicado: number | null;
  ipcPorcentajeAplicado: number | null;
}

export interface Riesgo {
  id: number;
  polizaId: number;
  arrendatario: string;
  direccionInmueble: string;
  estado: EstadoRiesgo;
}

export interface NuevoRiesgo {
  arrendatario: string;
  direccionInmueble: string;
}

export interface FiltrosPoliza {
  tipo: TipoPoliza | '';
  estado: EstadoPoliza | '';
}
