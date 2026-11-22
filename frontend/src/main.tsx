import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { MsalProvider } from '@azure/msal-react';
import { QueryProvider } from './providers/QueryProvider';
import { App } from './app/App';
import { configureGeneratedClients, hasAuthConfiguration, msalInstance } from './services/auth';
import './shared/styles.css';

const root = ReactDOM.createRoot(document.getElementById('root')!);

function ConfigurationRequired() {
  return (
    <main className="auth-screen">
      <section className="auth-card" aria-labelledby="setup-title">
        <div className="brand-mark" aria-hidden="true">C</div>
        <p className="eyebrow">CONFIGURAÇÃO NECESSÁRIA</p>
        <h1 id="setup-title">Conecte sua identidade</h1>
        <p className="muted">Defina as variáveis de ambiente do Microsoft Entra ID para iniciar o painel:</p>
        <ul className="setup-list">
          <li><code>VITE_ENTRA_CLIENT_ID</code></li>
          <li><code>VITE_ENTRA_API_SCOPE</code></li>
        </ul>
        <p className="auth-footnote">Use <code>.env.example</code> como referência. Nenhum segredo deve ser salvo no repositório.</p>
      </section>
    </main>
  );
}

async function start() {
  if (!hasAuthConfiguration || !msalInstance) {
    root.render(<ConfigurationRequired />);
    return;
  }

  configureGeneratedClients();
  await msalInstance.initialize();
  await msalInstance.handleRedirectPromise();

  root.render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <QueryProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </QueryProvider>
      </MsalProvider>
    </React.StrictMode>,
  );
}

start().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : 'Erro inesperado ao iniciar a aplicação.';
  root.render(<main className="auth-screen"><p className="form-error" role="alert">Falha ao iniciar a aplicação: {message}</p></main>);
});
