import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  imports: [FormsModule, JsonPipe],
  template: `
    <header class="page-header"><div><p class="eyebrow">Flujo end-to-end</p><h1>Demo de correo</h1><p class="muted">Publica un correo en mail-ingestion-service y observa el procesamiento por RabbitMQ.</p></div></header>
    <div class="two-column">
      <section class="panel">
        <form class="form-grid" (ngSubmit)="submit()">
          <label>Perfil<input name="profile" [(ngModel)]="form.profileCode" required></label>
          <label>Remitente<input name="from" [(ngModel)]="form.from" type="email" required></label>
          <label class="span-2">Asunto<input name="subject" [(ngModel)]="form.subject" required></label>
          <label class="span-2">Cuerpo<textarea name="body" [(ngModel)]="form.body" rows="14" required></textarea></label>
          <div class="actions span-2"><button class="btn btn-primary">Enviar al flujo</button></div>
        </form>
      </section>
      <section class="panel">
        <h2>Resultado de ingreso</h2>
        @if(result()){<pre>{{result() | json}}</pre>} @else {<p class="empty">Todavía no se ha enviado un correo.</p>}
        <p class="muted">El endpoint devuelve el requestId. El resto del flujo ocurre de manera asíncrona.</p>
      </section>
    </div>
  `
})
export class IntakeComponent {
  result = signal<any>(null);
  form:any={
    profileCode:'TELCO_DEMO',
    from:'cliente@example.com',
    to:['atencion@example.com'],
    cc:[],
    subject:'TRASPASO DE RUC BFS ECUADOR a ARDANARETAIL',
    body:'Por favor gestionar el traspaso de las líneas detalladas y del servicio de internet al nuevo RUC. Se solicita facturación independiente.',
    metadata:{source:'ANGULAR_DEMO'}
  };
  constructor(private readonly api:ApiService){}
  submit():void{this.api.submitEmail(this.form).subscribe({next:x=>this.result.set(x),error:e=>this.result.set({error:e.message})});}
}
