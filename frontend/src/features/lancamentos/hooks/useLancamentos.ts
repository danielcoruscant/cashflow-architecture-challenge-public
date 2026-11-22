import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { LancamentosService } from '../../../generated/lancamentos';
import type { CriarLancamentoRequest } from '../../../generated/lancamentos';

export const lancamentosQueryKey = ['lancamentos'] as const;

export function useLancamentos(accountId: string, page = 0, pageSize = 20) {
  const normalizedAccountId = accountId.trim();
  return useQuery({
    queryKey: [...lancamentosQueryKey, normalizedAccountId, page, pageSize],
    enabled: normalizedAccountId.length > 0,
    queryFn: () => LancamentosService.listarLancamentos(normalizedAccountId, page, pageSize),
  });
}

export function useCreateLancamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CriarLancamentoRequest) => LancamentosService.criarLancamento(request),
    onSuccess: async (_response, request) => queryClient.invalidateQueries({
      queryKey: [...lancamentosQueryKey, request.accountId],
    }),
  });
}

export function useCompensarLancamento(accountId: string) {
  const queryClient = useQueryClient();
  const normalizedAccountId = accountId.trim();
  return useMutation({
    mutationFn: (lancamentoId: string) =>
      LancamentosService.compensarLancamento(lancamentoId, normalizedAccountId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: [...lancamentosQueryKey, normalizedAccountId] }),
        queryClient.invalidateQueries({ queryKey: ['consolidado', normalizedAccountId] }),
      ]);
    },
  });
}
