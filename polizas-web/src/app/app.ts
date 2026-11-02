import { Component } from '@angular/core';

import { PolizasPage } from './polizas/polizas-page/polizas-page';

@Component({
  selector: 'app-root',
  imports: [PolizasPage],
  template: '<app-polizas-page />',
})
export class App {}
