import { useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { useConsolidado } from '../features/consolidado/hooks/useConsolidado';
import { ConsolidadoOverview } from '../features/consolidado/components/ConsolidadoOverview';
import { LancamentoForm } from '../features/lancamentos/components/LancamentoForm';
import { LancamentosList } from '../features/lancamentos/components/LancamentosList';

function hojeIso() {
  const hoje = new Date();
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;
}

export function DashboardPage() {
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [logoutError, setLogoutError] = useState('');
  const { instance, accounts } = useMsal();
  const summary = useConsolidado(selectedAccountId, selectedDate || undefined);
  const accountName = accounts[0]?.name ?? 'Conta corporativa';
  const initials = accountName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();

  const signOut = async () => {
    setLogoutError('');
    try {
      await instance.logoutRedirect({ account: instance.getActiveAccount() ?? accounts[0] });
    } catch (error) {
      setLogoutError(error instanceof Error ? error.message : 'Não foi possível encerrar a sessão.');
    }
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Cashflow, início">
          <span className="brand-mark" aria-hidden="true">C</span>
          <span>cashflow<span className="brand-period">.</span></span>
        </a>
        <p className="sidebar-label">MENU PRINCIPAL</p>
        <nav className="side-nav" aria-label="Navegação principal">
          <a className="nav-item active" href="#inicio"><span aria-hidden="true">◫</span> Visão geral</a>
          <a className="nav-item" href="#lancamentos"><span aria-hidden="true">⇄</span> Lançamentos</a>
        </nav>
        <div className="sidebar-bottom">
          <span className="connection-indicator"><i /> API financeira</span>
          <small>Cashflow Management<br />Plataforma financeira</small>
        </div>
      </aside>

      <main className="main-content" id="inicio">
        <header className="topbar">
          <div className="breadcrumbs">Painel <span>/</span> <strong>Visão geral</strong></div>
          <button className="account-button" onClick={signOut} title="Sair da conta">
            <span className="avatar">{initials}</span>
            <span className="account-name">{accountName}</span>
            <span aria-hidden="true">⌄</span>
          </button>
        </header>

        {logoutError && <p className="form-error session-error" role="alert">{logoutError}</p>}
        <div className="page-content">
          <section className="page-heading">
            <div>
              <p className="eyebrow">{new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' }).format(new Date())}</p>
              <h1>Visão geral</h1>
              <p className="muted">Acompanhe o movimento financeiro do seu negócio.</p>
            </div>
            <a className="button button-secondary" href="#novo-lancamento">＋ Novo lançamento</a>
          </section>

          <label className="field-label" htmlFor="accountId">
            Conta consultada
            <input
              id="accountId"
              type="text"
              value={selectedAccountId}
              onChange={(event) => setSelectedAccountId(event.target.value)}
              placeholder="Ex.: C001"
            />
          </label>

          <ConsolidadoOverview
            accountId={selectedAccountId}
            data={summary.data}
            isLoading={summary.isPending}
            error={summary.error}
            selectedDate={selectedDate || hojeIso()}
            onDateChange={setSelectedDate}
          />

          <div className="dashboard-grid">
            <section className="panel transactions-panel" id="lancamentos" aria-labelledby="transactions-title">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">MOVIMENTAÇÕES</p>
                  <h2 id="transactions-title">Últimos lançamentos</h2>
                </div>
                <span className="panel-icon" aria-hidden="true">↗</span>
              </div>
              <LancamentosList accountId={selectedAccountId} />
            </section>

            <section className="panel new-entry-panel" id="novo-lancamento" aria-labelledby="new-entry-title">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">REGISTRO RÁPIDO</p>
                  <h2 id="new-entry-title">Novo lançamento</h2>
                </div>
                <span className="panel-icon" aria-hidden="true">＋</span>
              </div>
              <LancamentoForm accountId={selectedAccountId} />
            </section>
          </div>
          <footer className="page-footer">Seus dados financeiros são sincronizados de forma segura.</footer>
        </div>
      </main>
    </div>
  );
}
