import { HttpErrorResponse } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeEsCo from '@angular/common/locales/es-CO';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Poliza, Riesgo } from '../poliza.model';
import { PolizasApi } from '../polizas-api';
import { PolizasPage } from './polizas-page';

const colectiva: Poliza = {
  id: 2,
  tipo: 'COLECTIVA',
  estado: 'ACTIVA',
  tomador: 'Inmobiliaria Andes',
  beneficiario: 'Propietarios Andes',
  fechaInicio: '2026-06-01',
  fechaFin: '2026-12-01',
  mesesVigencia: 6,
  canon: 2000000,
  prima: 12000000,
  ipcAnioAplicado: null,
  ipcPorcentajeAplicado: null,
};
const individual: Poliza = { ...colectiva, id: 1, tipo: 'INDIVIDUAL', tomador: 'Ana Gomez' };
const cancelada: Poliza = { ...colectiva, id: 3, estado: 'CANCELADA' };
const riesgo: Riesgo = {
  id: 7,
  polizaId: 2,
  arrendatario: 'Luis Perez',
  direccionInmueble: 'Apto 301',
  estado: 'ACTIVO',
};

describe('PolizasPage', () => {
  let fixture: ComponentFixture<PolizasPage>;
  let pagina: HTMLElement;
  let api: Record<keyof PolizasApi, ReturnType<typeof vi.fn>>;

  const fila = (id: number) =>
    Array.from(pagina.querySelectorAll('tbody tr')).find(
      (tr) => tr.children[0]?.textContent?.trim() === String(id),
    )!;
  const boton = (contenedor: Element, texto: string) =>
    Array.from(contenedor.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto);
  const estabilizar = async () => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    registerLocaleData(localeEsCo);
    Element.prototype.scrollIntoView = vi.fn();
    api = {
      listar: vi.fn(() => of([individual, colectiva, cancelada])),
      renovar: vi.fn(() => of({ ...individual, estado: 'RENOVADA', ipcPorcentajeAplicado: 5.1 })),
      cancelar: vi.fn(() => of(cancelada)),
      listarRiesgos: vi.fn(() => of([riesgo])),
      agregarRiesgo: vi.fn(() => of(riesgo)),
      cancelarRiesgo: vi.fn(() => of({ ...riesgo, estado: 'CANCELADO' })),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: PolizasApi, useValue: api },
        { provide: LOCALE_ID, useValue: 'es-CO' },
      ],
    });
    fixture = TestBed.createComponent(PolizasPage);
    pagina = fixture.nativeElement;
    await estabilizar();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('lista las polizas y oculta las acciones de una cancelada', () => {
    expect(fila(1).textContent).toContain('Ana Gomez');
    expect(boton(fila(1), 'Renovar')).toBeTruthy();
    expect(boton(fila(3), 'Renovar')).toBeUndefined();
    expect(boton(fila(3), 'Cancelar')).toBeUndefined();
    expect(boton(fila(3), 'Ver riesgos')).toBeTruthy();
  });

  it('vuelve a consultar al cambiar un filtro', async () => {
    const select = pagina.querySelector<HTMLSelectElement>('select[formControlName="tipo"]')!;

    select.value = 'COLECTIVA';
    select.dispatchEvent(new Event('change'));
    await estabilizar();

    expect(api['listar']).toHaveBeenLastCalledWith({ tipo: 'COLECTIVA', estado: '' });
  });

  it('renueva, avisa con el IPC aplicado y recarga', async () => {
    boton(fila(1), 'Renovar')!.click();
    await estabilizar();

    expect(api['renovar']).toHaveBeenCalledWith(1);
    expect(pagina.querySelector('[role="status"]')?.textContent).toContain('5.1 %');
    expect(api['listar']).toHaveBeenCalledTimes(2);
  });

  it('muestra el mensaje del API cuando rechaza la renovacion', async () => {
    api['renovar'].mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { detail: 'Solo se puede renovar desde 2027-10-02' },
          }),
      ),
    );

    boton(fila(1), 'Renovar')!.click();
    await estabilizar();

    expect(pagina.querySelector('[role="alert"]')?.textContent).toContain('2027-10-02');
  });

  it('no cancela si el usuario no confirma', async () => {
    vi.stubGlobal(
      'confirm',
      vi.fn(() => false),
    );

    boton(fila(2), 'Cancelar')!.click();
    await estabilizar();

    expect(api['cancelar']).not.toHaveBeenCalled();
  });

  it('cancela cuando el usuario confirma', async () => {
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    );

    boton(fila(2), 'Cancelar')!.click();
    await estabilizar();

    expect(api['cancelar']).toHaveBeenCalledWith(2);
  });

  it('muestra los riesgos y el formulario solo en colectivas', async () => {
    boton(fila(2), 'Ver riesgos')!.click();
    await estabilizar();

    expect(api['listarRiesgos']).toHaveBeenCalledWith(2);
    expect(pagina.querySelector('app-riesgos-panel')?.textContent).toContain('Luis Perez');
    expect(pagina.querySelector('app-riesgos-panel form')).toBeTruthy();

    boton(fila(1), 'Ver riesgos')!.click();
    await estabilizar();

    expect(pagina.querySelector('app-riesgos-panel form')).toBeNull();
  });

  it('lleva la vista al panel de riesgos', async () => {
    boton(fila(2), 'Ver riesgos')!.click();
    await estabilizar();

    expect(Element.prototype.scrollIntoView).toHaveBeenCalledOnce();
  });

  it('no envia un riesgo con campos vacios', async () => {
    boton(fila(2), 'Ver riesgos')!.click();
    await estabilizar();

    boton(pagina.querySelector('app-riesgos-panel')!, 'Agregar')!.click();
    await estabilizar();

    expect(api['agregarRiesgo']).not.toHaveBeenCalled();
    expect(pagina.querySelector('app-riesgos-panel small')?.textContent).toContain('obligatorio');
  });

  it('agrega un riesgo valido y recarga', async () => {
    boton(fila(2), 'Ver riesgos')!.click();
    await estabilizar();
    const panel = pagina.querySelector('app-riesgos-panel')!;
    const escribir = (nombre: string, valor: string) => {
      const campo = panel.querySelector<HTMLInputElement>(`input[formControlName="${nombre}"]`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    };

    escribir('arrendatario', 'Pedro Rojas');
    escribir('direccionInmueble', 'Apto 303');
    boton(panel, 'Agregar')!.click();
    await estabilizar();

    expect(api['agregarRiesgo']).toHaveBeenCalledWith(2, {
      arrendatario: 'Pedro Rojas',
      direccionInmueble: 'Apto 303',
    });
    expect(api['listar']).toHaveBeenCalledTimes(2);
  });

  it('avisa cuando el API no responde', async () => {
    api['listar'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    fixture.destroy();
    fixture = TestBed.createComponent(PolizasPage);
    pagina = fixture.nativeElement;
    await estabilizar();

    expect(pagina.querySelector('[role="alert"]')?.textContent).toContain('No se pudo conectar');
    expect(pagina.textContent).toContain('No hay pólizas');
  });
});
