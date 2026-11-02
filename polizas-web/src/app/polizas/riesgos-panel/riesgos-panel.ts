import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, of, switchMap, tap } from 'rxjs';

import { ESTILOS } from '../estilos';
import { mensajeDeError } from '../mensaje-de-error';
import { Poliza, Riesgo } from '../poliza.model';
import { PolizasApi } from '../polizas-api';

@Component({
  selector: 'app-riesgos-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './riesgos-panel.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RiesgosPanel {
  private readonly api = inject(PolizasApi);

  readonly poliza = input.required<Poliza>();
  readonly cambio = output<void>();

  protected readonly estilos = ESTILOS;
  protected readonly riesgos = signal<Riesgo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');
  protected readonly guardando = signal(false);

  /** Las reglas las valida el API, aqui solo se ocultan acciones. */
  protected readonly editable = computed(
    () => this.poliza().tipo === 'COLECTIVA' && this.poliza().estado !== 'CANCELADA',
  );

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    arrendatario: ['', [Validators.required, Validators.maxLength(255)]],
    direccionInmueble: ['', [Validators.required, Validators.maxLength(255)]],
  });

  constructor() {
    toObservable(this.poliza)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ id }) =>
          this.api.listarRiesgos(id).pipe(
            catchError((error: unknown) => {
              this.error.set(mensajeDeError(error));
              return of<Riesgo[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((riesgos) => {
        this.riesgos.set(riesgos);
        this.cargando.set(false);
      });
  }

  protected agregar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.api.agregarRiesgo(this.poliza().id, this.formulario.getRawValue()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.formulario.reset();
        this.cambio.emit();
      },
      error: (error: unknown) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(error));
      },
    });
  }

  protected cancelar(riesgo: Riesgo): void {
    if (!confirm(`¿Cancelar el riesgo de ${riesgo.arrendatario}?`)) {
      return;
    }
    this.error.set('');
    this.api.cancelarRiesgo(riesgo.id).subscribe({
      next: () => this.cambio.emit(),
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }
}
