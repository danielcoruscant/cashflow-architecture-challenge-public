import { getApiErrorMessage } from '../../../shared/apiError';
import type { ConsolidadoDiarioResponse } from '../../../generated/consolidado';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

interface ConsolidadoOverviewProps {
  accountId: string;
  data?: ConsolidadoDiarioResponse;
  isLoading: boolean;
  error: unknown;
  selectedDate: string;
  onDateChange: (date: string) => void;
}

export function ConsolidadoOverview({ accountId, data, isLoading, error, selectedDate, onDateChange }: ConsolidadoOverviewProps) {
  return (
    <section className="overview-card" aria-labelledby="overview-title">
      <div className="overview-header">
        <div>
          <p className="eyebrow">RESUMO FINANCEIRO</p>
          <h2 id="overview-title">Consolidado diário</h2>
        </div>
        <label className="date-filter">
          <span>Data do saldo</span>
          <input type="date" aria-label="Data do consolidado" value={selectedDate} onChange={(event) => onDateChange(event.target.value)} />
        </label>
      </div>

      {!accountId.trim() ? (
        <p className="empty-state">Informe uma conta para consultar o consolidado.</p>
      ) : isLoading ? (
        <p className="list-message">Carregando consolidado…</p>
      ) : error ? (
        <div className="data-unavailable" role="alert">
          <span className="status-dot" aria-hidden="true" />
          <div>
            <strong>Consolidado indisponível</strong>
            <p>{getApiErrorMessage(error)}</p>
          </div>
        </div>
      ) : data ? (
        <div className="balance-grid">
          <article className="balance-tile balance-main">
            <span>Saldo do dia</span>
            <strong>{currency.format(data.saldo)}</strong>
            <small>Referente a {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(`${data.data}T00:00:00Z`))}</small>
          </article>
          <article className="balance-tile">
            <span>Entradas</span>
            <strong className="credit-text">{currency.format(data.creditos)}</strong>
          </article>
          <article className="balance-tile">
            <span>Saídas</span>
            <strong className="debit-text">{currency.format(data.debitos)}</strong>
          </article>
          <p className="eventual-consistency" role="status">
            O saldo é atualizado de forma assíncrona e pode levar alguns instantes para refletir novos lançamentos.
          </p>
        </div>
      ) : (
        <p className="empty-state">Nenhum consolidado disponível para esta data.</p>
      )}
    </section>
  );
}
