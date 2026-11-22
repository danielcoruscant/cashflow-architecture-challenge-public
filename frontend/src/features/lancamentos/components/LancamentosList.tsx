import { TipoLancamento } from '../../../generated/lancamentos';
import type { LancamentoResponse } from '../../../generated/lancamentos';
import { useCompensarLancamento, useLancamentos } from '../hooks/useLancamentos';
import { getApiErrorMessage } from '../../../shared/apiError';
import type { ReactNode } from 'react';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeZone: 'UTC' });

function TransactionRow({ item, children }: { item: LancamentoResponse; children: ReactNode }) {
  const isCredit = item.tipo === TipoLancamento.CREDITO;
  return (
    <li className="transaction-row">
      <span className={`transaction-icon ${isCredit ? 'credit' : 'debit'}`} aria-hidden="true">{isCredit ? '↗' : '↙'}</span>
      <span className="transaction-description">
        <strong>{isCredit ? 'Crédito' : 'Débito'}</strong>
        <small>{dateFormatter.format(new Date(`${item.data}T00:00:00Z`))}</small>
      </span>
      <strong className={`transaction-value ${isCredit ? 'credit-text' : 'debit-text'}`}>
        {isCredit ? '+' : '−'} {currency.format(item.valor)}
      </strong>
      {children}
    </li>
  );
}

export function LancamentosList({ accountId }: { accountId: string }) {
  const query = useLancamentos(accountId);
  const compensacao = useCompensarLancamento(accountId);

  if (!accountId.trim()) return <p className="empty-state">Informe uma conta para consultar os lançamentos.</p>;
  if (query.isPending) return <p className="list-message">Carregando lançamentos…</p>;
  if (query.isError) return <p className="inline-error" role="alert">Não foi possível carregar os lançamentos. {getApiErrorMessage(query.error)}</p>;
  if (query.data.items.length === 0) return <p className="empty-state">Nenhum lançamento encontrado para exibir.</p>;

  const lancamentosCompensados = new Set(
    query.data.items.flatMap((item) => item.compensaLancamentoId ? [item.compensaLancamentoId] : []),
  );

  return (
    <>
      {compensacao.isError && <p className="inline-error" role="alert">{getApiErrorMessage(compensacao.error)}</p>}
      {compensacao.isSuccess && <p className="inline-success" role="status">Lançamento compensatório registrado.</p>}
      <ul className="transaction-list">
        {query.data.items.map((item) => (
          <TransactionRow key={item.id} item={item}>
            {item.compensaLancamentoId || lancamentosCompensados.has(item.id) ? (
              <span className="transaction-compensated">Compensado</span>
            ) : (
              <button
                className="button button-secondary"
                type="button"
                disabled={compensacao.isPending}
                onClick={() => compensacao.mutate(item.id)}
                aria-label={`Compensar lançamento de ${currency.format(item.valor)} em ${dateFormatter.format(new Date(`${item.data}T00:00:00Z`))}`}
              >
                {compensacao.isPending && compensacao.variables === item.id ? 'Compensando…' : 'Compensar'}
              </button>
            )}
          </TransactionRow>
        ))}
      </ul>
    </>
  );
}
