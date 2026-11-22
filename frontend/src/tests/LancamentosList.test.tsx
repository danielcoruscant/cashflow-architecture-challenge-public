import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LancamentosList } from '../features/lancamentos/components/LancamentosList';

const useLancamentos = vi.fn();
const useCompensarLancamento = vi.fn();
const compensar = vi.fn();
vi.mock('../features/lancamentos/hooks/useLancamentos', () => ({
  useLancamentos: (accountId: string) => useLancamentos(accountId),
  useCompensarLancamento: (accountId: string) => useCompensarLancamento(accountId),
}));

describe('LancamentosList', () => {
  beforeEach(() => {
    useLancamentos.mockReset();
    useCompensarLancamento.mockReset().mockReturnValue({
      mutate: compensar,
      isPending: false,
      isError: false,
      isSuccess: false,
      variables: undefined,
    });
    compensar.mockReset();
  });

  it('mostra o estado de carregamento', () => {
    useLancamentos.mockReturnValue({ isPending: true });
    render(<LancamentosList accountId="conta-1" />);
    expect(screen.getByText('Carregando lançamentos…')).toBeInTheDocument();
  });

  it('mostra erro da API', () => {
    useLancamentos.mockReturnValue({ isPending: false, isError: true, error: { body: { mensagem: 'API offline' } } });
    render(<LancamentosList accountId="conta-1" />);
    expect(screen.getByRole('alert')).toHaveTextContent('API offline');
  });

  it('informa quando não há dados', () => {
    useLancamentos.mockReturnValue({ isPending: false, isError: false, data: { items: [], page: 0, pageSize: 20, totalItems: 0, totalPages: 0 } });
    render(<LancamentosList accountId="conta-1" />);
    expect(screen.getByText('Nenhum lançamento encontrado para exibir.')).toBeInTheDocument();
  });

  it('renderiza valores e tipos provenientes do contrato', () => {
    useLancamentos.mockReturnValue({
      isPending: false,
      isError: false,
      data: {
        items: [
          { id: '1', accountId: 'conta-1', tipo: 'CREDITO', valor: 50, data: '2026-11-20', compensaLancamentoId: null },
          { id: '2', accountId: 'conta-1', tipo: 'DEBITO', valor: 12.5, data: '2026-11-19', compensaLancamentoId: null },
        ],
        page: 0,
        pageSize: 20,
        totalItems: 2,
        totalPages: 1,
      },
    });
    render(<LancamentosList accountId="conta-1" />);
    expect(screen.getByText('Crédito')).toBeInTheDocument();
    expect(screen.getByText('Débito')).toBeInTheDocument();
    expect(screen.getByText('+ R$ 50,00')).toBeInTheDocument();
    expect(screen.getByText('− R$ 12,50')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^Compensar lançamento/ })).toHaveLength(2);
  });

  it('marca compensações existentes e não oferece uma nova compensação', () => {
    useLancamentos.mockReturnValue({
      isPending: false,
      isError: false,
      data: {
        items: [
          { id: 'original', accountId: 'conta-1', tipo: 'CREDITO', valor: 50, data: '2026-11-20', compensaLancamentoId: null },
          { id: 'reversao', accountId: 'conta-1', tipo: 'DEBITO', valor: 50, data: '2026-11-21', compensaLancamentoId: 'original' },
        ],
        page: 0,
        pageSize: 20,
        totalItems: 2,
        totalPages: 1,
      },
    });
    render(<LancamentosList accountId="conta-1" />);

    expect(screen.getAllByText('Compensado')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: /^Compensar lançamento/ })).not.toBeInTheDocument();
    expect(useCompensarLancamento).toHaveBeenCalledWith('conta-1');
  });

  it('solicita a compensação do lançamento selecionado', async () => {
    const user = userEvent.setup();
    useLancamentos.mockReturnValue({
      isPending: false,
      isError: false,
      data: {
        items: [
          { id: 'original', accountId: 'conta-1', tipo: 'CREDITO', valor: 50, data: '2026-11-20', compensaLancamentoId: null },
        ],
        page: 0,
        pageSize: 20,
        totalItems: 1,
        totalPages: 1,
      },
    });
    render(<LancamentosList accountId="conta-1" />);

    await user.click(screen.getByRole('button', { name: /^Compensar lançamento/ }));

    expect(compensar).toHaveBeenCalledWith('original');
  });
});
