import { useQuery } from '@tanstack/react-query';
import { ConsolidadoService } from '../../../generated/consolidado';

export function useConsolidado(accountId: string, data?: string) {
  const normalizedAccountId = accountId.trim();
  return useQuery({
    queryKey: ['consolidado', normalizedAccountId, data ?? 'atual'],
    enabled: normalizedAccountId.length > 0,
    queryFn: () => data
      ? ConsolidadoService.consultarConsolidadoPorData(data, normalizedAccountId)
      : ConsolidadoService.consultarConsolidadoAtual(normalizedAccountId),
  });
}
