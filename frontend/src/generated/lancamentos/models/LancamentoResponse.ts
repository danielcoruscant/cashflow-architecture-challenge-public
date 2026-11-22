/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TipoLancamento } from './TipoLancamento';
export type LancamentoResponse = {
    id: string;
    accountId: string;
    tipo: TipoLancamento;
    valor: number;
    data: string;
    compensaLancamentoId: string | null;
};

