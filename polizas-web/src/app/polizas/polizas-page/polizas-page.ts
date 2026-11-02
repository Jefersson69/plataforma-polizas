import { CurrencyPipe } from '@angular/common';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { catchError, merge, of, startWith, Subject, switchMap, tap } from 'rxjs';

import { ESTILOS } from '../estilos';
import { mensajeDeError } from '../mensaje-de-error';
import { EstadoPoliza, Poliza, TipoPoliza } from '../poliza.model';
import { PolizasApi } from '../polizas-api';
import { RiesgosPanel } from '../riesgos-panel/riesgos-panel';

@Component({
  selector: 'app-polizas-page',
  imports: [CurrencyPipe, ReactiveFormsModule, RiesgosPanel],
  templateUrl: './polizas-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolizasPage {
  private readonly api = inject(PolizasApi);
  private readonly injector = inject(Injector);
  private readonly panelRiesgos = viewChild('panelRiesgos', { read: ElementRef });
  private readonly recargar$ = new Subject<void>();
  private readonly seleccionadaId = signal<number | null>(null);

  protected readonly estilos = ESTILOS;
  protected readonly polizas = signal<Poliza[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');
  protected readonly exito = signal('');

  protected readonly seleccionada = computed(
    () => this.polizas().find(({ id }) => id === this.seleccionadaId()) ?? null,
  );

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoPoliza | ''],
    estado: ['' as EstadoPoliza | ''],
  });

  constructor() {
    merge(this.filtros.valueChanges, this.recargar$)
      .pipe(
        startWith(undefined),
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.api.listar(this.filtros.getRawValue()).pipe(
            catchError((error: unknown) => {
              this.error.set(mensajeDeError(error));
              return of<Poliza[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((polizas) => {
        this.polizas.set(polizas);
        this.cargando.set(false);
      });
  }

  protected recargar(): void {
    this.recargar$.next();
  }

  protected verRiesgos({ id }: Poliza): void {
    this.seleccionadaId.set(id);
    afterNextRender(() => this.panelRiesgos()?.nativeElement.scrollIntoView({ block: 'start' }), {
      injector: this.injector,
    });
  }

  protected renovar({ id }: Poliza): void {
    this.limpiarMensajes();
    this.api.renovar(id).subscribe({
      next: ({ ipcPorcentajeAplicado }) => {
        this.exito.set(`Póliza ${id} renovada con un IPC de ${ipcPorcentajeAplicado} %.`);
        this.recargar();
      },
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  protected cancelar({ id }: Poliza): void {
    if (!confirm(`¿Cancelar la póliza ${id} y todos sus riesgos?`)) {
      return;
    }
    this.limpiarMensajes();
    this.api.cancelar(id).subscribe({
      next: () => {
        this.exito.set(`Póliza ${id} cancelada.`);
        this.recargar();
      },
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.exito.set('');
  }
}
