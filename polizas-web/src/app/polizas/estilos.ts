const BOTON =
  'cursor-pointer rounded-md border px-3 py-1.5 font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700 disabled:cursor-not-allowed disabled:opacity-60';
const ETIQUETA = 'inline-block rounded-full px-2 py-0.5 text-xs font-bold';
const ETIQUETA_ACTIVA = `${ETIQUETA} bg-green-100 text-green-800`;
const ETIQUETA_CANCELADA = `${ETIQUETA} bg-red-100 text-red-700`;
const TITULO_DE_CAMPO =
  'before:flex-none before:text-[0.8125rem] before:font-semibold before:text-slate-500 before:content-[attr(data-titulo)]';

export const ESTILOS = {
  boton: `${BOTON} border-blue-700 bg-white text-blue-700 hover:bg-blue-50`,
  botonPrimario: `${BOTON} border-blue-700 bg-blue-700 text-white hover:bg-blue-900`,
  botonPeligro: `${BOTON} border-red-700 bg-white text-red-700 hover:bg-red-50`,
  campo:
    'mt-1 block w-full rounded-md border border-slate-400 bg-white px-2.5 py-2 font-normal focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700',
  alertaError: 'mb-4 rounded-md bg-red-100 px-4 py-3 text-red-700',
  alertaExito: 'mb-4 rounded-md bg-green-100 px-4 py-3 text-green-800',
  encabezadoColumna:
    'whitespace-nowrap p-3 text-left text-[0.8125rem] font-semibold text-slate-500',
  etiqueta: {
    ACTIVA: ETIQUETA_ACTIVA,
    ACTIVO: ETIQUETA_ACTIVA,
    RENOVADA: `${ETIQUETA} bg-blue-100 text-blue-900`,
    CANCELADA: ETIQUETA_CANCELADA,
    CANCELADO: ETIQUETA_CANCELADA,
  },
  celdaPoliza: `flex items-baseline justify-between gap-4 py-1.5 text-right wrap-anywhere md:block md:p-0 md:text-left md:before:block xl:table-cell xl:p-3 xl:before:hidden ${TITULO_DE_CAMPO}`,
  celdaRiesgo: `flex items-baseline justify-between gap-4 py-1.5 text-right wrap-anywhere md:table-cell md:p-3 md:text-left md:before:hidden ${TITULO_DE_CAMPO}`,
} as const;
