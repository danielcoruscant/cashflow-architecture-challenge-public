import { afterEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { ConsolidadoService } from '../generated/consolidado';
import { LancamentosService } from '../generated/lancamentos';
import { TipoLancamento } from '../generated/lancamentos';
import { useConsolidado } from '../features/consolidado/hooks/useConsolidado';
import { useCompensarLancamento, useCreateLancamento, useLancamentos } from '../features/lancamentos/hooks/useLancamentos';

vi.mock('../generated/consolidado', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../generated/consolidado')>();
  return { ...actual, ConsolidadoService: { consultarConsolidadoAtual: vi.fn(), consultarConsolidadoPorData: vi.fn() } };
});
vi.mock('../generated/lancamentos', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../generated/lancamentos')>();
  return {
    ...actual,
    LancamentosService: {
      listarLancamentos: vi.fn(),
      criarLancamento: vi.fn(),
      compensarLancamento: vi.fn(),
    },
  };
});

function wrapper({ children }: { children: React.ReactNode }) {
  return <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>;
}

afterEach(() => vi.clearAllMocks());

describe('hooks de integração OpenAPI', () => {
  it('consulta o consolidado atual ou por data através do cliente gerado', async () => {
    vi.mocked(ConsolidadoService.consultarConsolidadoAtual).mockResolvedValue({ accountId: 'conta-1', data: '2026-11-20', creditos: 0, debitos: 0, saldo: 0 });
    const current = renderHook(() => useConsolidado('conta-1'), { wrapper });
    await waitFor(() => expect(current.result.current.isSuccess).toBe(true));
    expect(ConsolidadoService.consultarConsolidadoAtual).toHaveBeenCalledWith('conta-1');
    current.unmount();

    vi.mocked(ConsolidadoService.consultarConsolidadoPorData).mockResolvedValue({ accountId: 'conta-1', data: '2026-11-19', creditos: 10, debitos: 2, saldo: 8 });
    const byDate = renderHook(() => useConsolidado('conta-1', '2026-11-19'), { wrapper });
    await waitFor(() => expect(byDate.result.current.isSuccess).toBe(true));
    expect(ConsolidadoService.consultarConsolidadoPorData).toHaveBeenCalledWith('2026-11-19', 'conta-1');
  });

  it('lista e cria lançamentos usando o SDK gerado', async () => {
    vi.mocked(LancamentosService.listarLancamentos).mockResolvedValue({ items: [], page: 0, pageSize: 20, totalItems: 0, totalPages: 0 });
    const list = renderHook(() => useLancamentos('conta-1'), { wrapper });
    await waitFor(() => expect(list.result.current.isSuccess).toBe(true));
    expect(LancamentosService.listarLancamentos).toHaveBeenCalledWith('conta-1', 0, 20);
    list.unmount();

    vi.mocked(LancamentosService.criarLancamento).mockResolvedValue({ id: '1', accountId: 'conta-1', tipo: TipoLancamento.CREDITO, valor: 5, data: '2026-11-20', compensaLancamentoId: null });
    const create = renderHook(() => useCreateLancamento(), { wrapper });
    await act(async () => {
      await create.result.current.mutateAsync({ accountId: 'conta-1', tipo: TipoLancamento.CREDITO, valor: 5, data: '2026-11-20' });
    });
    expect(LancamentosService.criarLancamento).toHaveBeenCalledWith({ accountId: 'conta-1', tipo: TipoLancamento.CREDITO, valor: 5, data: '2026-11-20' });
  });

  it('compensa um lançamento usando o cliente gerado', async () => {
    vi.mocked(LancamentosService.compensarLancamento).mockResolvedValue({
      id: 'reversao',
      accountId: 'conta-1',
      tipo: TipoLancamento.DEBITO,
      valor: 5,
      data: '2026-11-20',
      compensaLancamentoId: 'original',
    });
    const compensation = renderHook(() => useCompensarLancamento(' conta-1 '), { wrapper });

    await act(async () => {
      await compensation.result.current.mutateAsync('original');
    });

    expect(LancamentosService.compensarLancamento).toHaveBeenCalledWith('original', 'conta-1');
  });
});
