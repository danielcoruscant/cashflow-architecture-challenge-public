import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { InteractionStatus } from '@azure/msal-browser';
import { AuthGate } from '../routes/AuthGate';

const isAuthenticated = vi.fn();
const loginRedirect = vi.fn();
const useMsal = vi.fn();
vi.mock('@azure/msal-react', () => ({ useIsAuthenticated: () => isAuthenticated(), useMsal: () => useMsal() }));

describe('AuthGate', () => {
  beforeEach(() => {
    isAuthenticated.mockReset();
    loginRedirect.mockReset();
    useMsal.mockReturnValue({ instance: { loginRedirect }, inProgress: InteractionStatus.None });
  });

  it('renderiza conteúdo apenas com sessão autenticada', () => {
    isAuthenticated.mockReturnValue(true);
    render(<AuthGate><p>Painel protegido</p></AuthGate>);
    expect(screen.getByText('Painel protegido')).toBeInTheDocument();
  });

  it('oferece login Microsoft para sessão anônima', () => {
    isAuthenticated.mockReturnValue(false);
    render(<AuthGate><p>Painel protegido</p></AuthGate>);
    expect(screen.getByRole('button', { name: 'Entrar com Microsoft' })).toBeEnabled();
    expect(screen.queryByText('Painel protegido')).not.toBeInTheDocument();
  });

  it('desabilita o login enquanto uma interação MSAL está em andamento', () => {
    isAuthenticated.mockReturnValue(false);
    useMsal.mockReturnValue({ instance: { loginRedirect }, inProgress: InteractionStatus.Startup });
    render(<AuthGate><p>Painel</p></AuthGate>);
    expect(screen.getByRole('button', { name: 'Conectando…' })).toBeDisabled();
  });

  it('inicia login com redirect', async () => {
    isAuthenticated.mockReturnValue(false);
    render(<AuthGate><p>Painel</p></AuthGate>);
    fireEvent.click(screen.getByRole('button', { name: 'Entrar com Microsoft' }));
    await waitFor(() => expect(loginRedirect).toHaveBeenCalledWith(expect.objectContaining({ scopes: expect.any(Array) })));
  });

  it('expõe falha ao iniciar o login', async () => {
    isAuthenticated.mockReturnValue(false);
    loginRedirect.mockRejectedValue(new Error('Login indisponível'));
    render(<AuthGate><p>Painel</p></AuthGate>);
    fireEvent.click(screen.getByRole('button', { name: 'Entrar com Microsoft' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Login indisponível');
  });

  it('usa mensagem genérica quando o erro do login não é uma exceção', async () => {
    isAuthenticated.mockReturnValue(false);
    loginRedirect.mockRejectedValue('erro desconhecido');
    render(<AuthGate><p>Painel</p></AuthGate>);
    fireEvent.click(screen.getByRole('button', { name: 'Entrar com Microsoft' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível iniciar o login.');
  });
});
