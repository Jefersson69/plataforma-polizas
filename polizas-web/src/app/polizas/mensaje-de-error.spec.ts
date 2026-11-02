import { HttpErrorResponse } from '@angular/common/http';

import { mensajeDeError } from './mensaje-de-error';

describe('mensajeDeError', () => {
  it('usa el detalle del ProblemDetail', () => {
    const error = new HttpErrorResponse({ status: 409, error: { detail: 'No se puede renovar' } });

    expect(mensajeDeError(error)).toBe('No se puede renovar');
  });

  it('lista los campos invalidos', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        detail: 'Hay campos invalidos',
        errores: [
          { campo: 'arrendatario', mensaje: 'es obligatorio' },
          { campo: 'direccionInmueble', mensaje: 'es obligatorio' },
        ],
      },
    });

    expect(mensajeDeError(error)).toBe(
      'arrendatario: es obligatorio. direccionInmueble: es obligatorio',
    );
  });

  it('explica cuando el API no responde', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain('No se pudo conectar');
  });

  it('tiene un mensaje para respuestas sin detalle y errores desconocidos', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 500 }))).toContain('no pudo procesar');
    expect(mensajeDeError(new Error('x'))).toBe('Ocurrió un error inesperado.');
  });
});
