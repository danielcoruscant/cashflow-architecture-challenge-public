import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';

const { renderRoot } = vi.hoisted(() => ({ renderRoot: vi.fn() }));

vi.mock('react-dom/client', () => ({
  default: { createRoot: () => ({ render: renderRoot }) },
}));

describe('bootstrap da aplicação', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    document.body.innerHTML = '<div id="root"></div>';
  });

  afterEach(() => {
    vi.doUnmock('../services/auth');
  });

  it('informa as variáveis ausentes sem iniciar o MSAL', async () => {
    vi.doMock('../services/auth', () => ({
      configureGeneratedClients: vi.fn(),
      hasAuthConfiguration: false,
      msalInstance: null,
    }));

    await import('../main');
    await waitFor(() => expect(renderRoot).toHaveBeenCalledOnce());
    render(renderRoot.mock.calls[0][0]);

    expect(screen.getByText('Conecte sua identidade')).toBeInTheDocument();
    expect(screen.getByText('VITE_ENTRA_API_SCOPE')).toBeInTheDocument();
  });

  it('inicializa a autenticação e monta o painel quando há configuração', async () => {
    const initialize = vi.fn().mockResolvedValue(undefined);
    const handleRedirectPromise = vi.fn().mockResolvedValue(undefined);
    const configureGeneratedClients = vi.fn();
    vi.doMock('../services/auth', () => ({
      configureGeneratedClients,
      hasAuthConfiguration: true,
      msalInstance: { initialize, handleRedirectPromise },
    }));

    await import('../main');
    await waitFor(() => expect(renderRoot).toHaveBeenCalledOnce());

    expect(configureGeneratedClients).toHaveBeenCalledOnce();
    expect(initialize).toHaveBeenCalledOnce();
    expect(handleRedirectPromise).toHaveBeenCalledOnce();
  });

  it('mostra erro amigável se a inicialização falhar com exceção desconhecida', async () => {
    const initialize = vi.fn().mockRejectedValue('falha não mapeada');
    vi.doMock('../services/auth', () => ({
      configureGeneratedClients: vi.fn(),
      hasAuthConfiguration: true,
      msalInstance: { initialize, handleRedirectPromise: vi.fn() },
    }));

    await import('../main');
    await waitFor(() => expect(renderRoot).toHaveBeenCalledOnce());
    render(renderRoot.mock.calls[0][0]);

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Falha ao iniciar a aplicação: Erro inesperado ao iniciar a aplicação.',
    );
  });

  it('apresenta a mensagem de uma exceção de inicialização', async () => {
    const initialize = vi.fn().mockRejectedValue(new Error('MSAL indisponível'));
    vi.doMock('../services/auth', () => ({
      configureGeneratedClients: vi.fn(),
      hasAuthConfiguration: true,
      msalInstance: { initialize, handleRedirectPromise: vi.fn() },
    }));

    await import('../main');
    await waitFor(() => expect(renderRoot).toHaveBeenCalledOnce());
    render(renderRoot.mock.calls[0][0]);

    expect(screen.getByRole('alert')).toHaveTextContent('Falha ao iniciar a aplicação: MSAL indisponível');
  });
});
