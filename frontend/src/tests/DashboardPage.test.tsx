import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { DashboardPage } from '../pages/DashboardPage';

const useConsolidado = vi.fn();
const useMsal = vi.fn();
const logoutRedirect = vi.fn();
vi.mock('../features/consolidado/hooks/useConsolidado', () => ({ useConsolidado: (accountId: string, date?: string) => useConsolidado(accountId, date) }));
vi.mock('../features/lancamentos/components/LancamentoForm', () => ({ LancamentoForm: () => <div>Formulário de lançamento</div> }));
vi.mock('../features/lancamentos/components/LancamentosList', () => ({ LancamentosList: () => <div>Lista de lançamentos</div> }));
vi.mock('@azure/msal-react', () => ({ useMsal: () => useMsal() }));

describe('DashboardPage', () => {
  beforeEach(() => {
    useConsolidado.mockReset().mockReturnValue({ isPending: true, error: null });
    useMsal.mockReset().mockReturnValue({
      instance: { logoutRedirect, getActiveAccount: () => null },
      accounts: [{ name: 'Ana Silva' }],
    });
    logoutRedirect.mockReset();
  });

  it('apresenta painel com conta e áreas de transações', () => {
    render(<DashboardPage />);
    expect(screen.getByRole('heading', { name: 'Visão geral' })).toBeInTheDocument();
    expect(screen.getByText('Ana Silva')).toBeInTheDocument();
    expect(screen.getByText('Conta consultada')).toBeInTheDocument();
    expect(screen.getByText('Formulário de lançamento')).toBeInTheDocument();
    expect(screen.getByText('Lista de lançamentos')).toBeInTheDocument();
  });

  it('consulta o consolidado filtrado pela data selecionada', async () => {
    render(<DashboardPage />);
    fireEvent.change(screen.getByLabelText('Conta consultada'), { target: { value: 'conta-1' } });
    fireEvent.change(screen.getByLabelText('Data do consolidado'), { target: { value: '2026-11-18' } });
    await waitFor(() => expect(useConsolidado).toHaveBeenLastCalledWith('conta-1', '2026-11-18'));
  });

  it('mostra erro quando o encerramento da sessão falha', async () => {
    logoutRedirect.mockRejectedValue(new Error('Falha ao sair'));
    render(<DashboardPage />);
    fireEvent.click(screen.getByRole('button', { name: /Ana Silva/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao sair');
  });

  it('encerra sessão da conta ativa', async () => {
    render(<DashboardPage />);
    fireEvent.click(screen.getByRole('button', { name: /Ana Silva/ }));
    await waitFor(() => expect(logoutRedirect).toHaveBeenCalled());
  });
});
