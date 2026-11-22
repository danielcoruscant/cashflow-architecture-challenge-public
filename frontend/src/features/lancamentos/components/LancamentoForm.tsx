import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { TipoLancamento } from '../../../generated/lancamentos';
import { getApiErrorMessage } from '../../../shared/apiError';
import { useCreateLancamento } from '../hooks/useLancamentos';

const lancamentoSchema = z.object({
  tipo: z.nativeEnum(TipoLancamento),
  valor: z.number({ error: 'Informe um valor.' }).finite('Informe um valor válido.'),
  data: z.iso.date('Informe uma data válida.'),
});

type LancamentoFormValues = z.infer<typeof lancamentoSchema>;

function hojeIso() {
  const hoje = new Date();
  const ano = hoje.getFullYear();
  const mes = String(hoje.getMonth() + 1).padStart(2, '0');
  const dia = String(hoje.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function LancamentoForm({ accountId }: { accountId: string }) {
  const createLancamento = useCreateLancamento();
  const [submitted, setSubmitted] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LancamentoFormValues>({
    resolver: zodResolver(lancamentoSchema),
    defaultValues: { tipo: TipoLancamento.CREDITO, data: hojeIso() },
  });

  const onSubmit = (values: LancamentoFormValues) => {
    setSubmitted(false);
    createLancamento.mutate({ ...values, accountId: accountId.trim() }, {
      onSuccess: () => {
        reset({ tipo: TipoLancamento.CREDITO, data: hojeIso() });
        setSubmitted(true);
      },
    });
  };

  return (
    <form className="entry-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <fieldset className="form-fieldset">
        <legend className="field-label">Tipo de lançamento</legend>
        <div className="segmented-control">
          <label className="segment">
            <input type="radio" value={TipoLancamento.CREDITO} {...register('tipo')} />
            <span>＋ Crédito</span>
          </label>
          <label className="segment">
            <input type="radio" value={TipoLancamento.DEBITO} {...register('tipo')} />
            <span>− Débito</span>
          </label>
        </div>
        {errors.tipo && <span className="field-error">{errors.tipo.message}</span>}
      </fieldset>

      <label className="field-label" htmlFor="valor">Valor</label>
      <div className="input-with-prefix">
        <span aria-hidden="true">R$</span>
        <input id="valor" type="number" inputMode="decimal" step="0.01" {...register('valor', { valueAsNumber: true })} aria-invalid={Boolean(errors.valor)} />
      </div>
      {errors.valor && <span className="field-error">{errors.valor.message}</span>}

      <label className="field-label" htmlFor="data">Data</label>
      <input id="data" type="date" {...register('data')} aria-invalid={Boolean(errors.data)} />
      {errors.data && <span className="field-error">{errors.data.message}</span>}

      {createLancamento.isError && <p className="inline-error" role="alert">{getApiErrorMessage(createLancamento.error)}</p>}
      {submitted && <p className="inline-success" role="status">Lançamento registrado.</p>}

      {!accountId.trim() && <p className="field-error" role="status">Informe uma conta antes de registrar lançamentos.</p>}
      <button className="button button-primary button-wide" type="submit" disabled={!accountId.trim() || createLancamento.isPending}>
        {createLancamento.isPending ? 'Salvando…' : 'Adicionar lançamento'}
      </button>
    </form>
  );
}
