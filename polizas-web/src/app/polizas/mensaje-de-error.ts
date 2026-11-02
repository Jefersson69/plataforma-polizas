import { HttpErrorResponse } from '@angular/common/http';

interface ErrorDeCampo {
  campo: string;
  mensaje: string;
}

export function mensajeDeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado.';
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el API. Verifica que esté en ejecución.';
  }
  const errores: ErrorDeCampo[] | undefined = error.error?.errores;
  if (errores?.length) {
    return errores.map(({ campo, mensaje }) => `${campo}: ${mensaje}`).join('. ');
  }
  return error.error?.detail ?? 'El API no pudo procesar la solicitud.';
}
