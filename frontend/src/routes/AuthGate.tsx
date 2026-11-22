import { useState } from 'react';
import { useIsAuthenticated, useMsal } from '@azure/msal-react';
import { InteractionStatus } from '@azure/msal-browser';
import { loginRequest } from '../services/auth';
import type { PropsWithChildren } from 'react';

export function AuthGate({ children }: PropsWithChildren) {
  const authenticated = useIsAuthenticated();
  const { instance, inProgress } = useMsal();
  const [loginError, setLoginError] = useState('');

  if (authenticated) return children;

  const signIn = async () => {
    setLoginError('');
    try {
      await instance.loginRedirect(loginRequest);
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Não foi possível iniciar o login.');
    }
  };

  return (
    <main className="auth-screen">
      <div className="auth-card">
        <div className="brand-mark" aria-hidden="true">C</div>
        <p className="eyebrow">CASHFLOW MANAGEMENT</p>
        <h1>Seu fluxo financeiro,<br />em equilíbrio.</h1>
        <p className="muted">Entre com sua conta corporativa para acessar o painel de fluxo de caixa.</p>
        <button className="button button-primary button-wide" onClick={signIn} disabled={inProgress !== InteractionStatus.None}>
          {inProgress !== InteractionStatus.None ? 'Conectando…' : 'Entrar com Microsoft'}
        </button>
        {loginError && <p className="form-error" role="alert">{loginError}</p>}
        <p className="auth-footnote">Autenticação protegida pelo Microsoft Entra ID</p>
      </div>
    </main>
  );
}
