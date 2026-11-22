/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ConsolidadoDiarioResponse } from '../models/ConsolidadoDiarioResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ConsolidadoService {
    /**
     * Consultar consolidado atual de uma conta
     * @param accountId Identificador da conta consultada.
     * @returns ConsolidadoDiarioResponse Consolidado atual consultado com sucesso.
     * @throws ApiError
     */
    public static consultarConsolidadoAtual(
        accountId: string,
    ): CancelablePromise<ConsolidadoDiarioResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/consolidado',
            query: {
                'accountId': accountId,
            },
            errors: {
                400: `Requisição inválida.`,
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para consultar o consolidado.`,
                404: `Consolidado atual não encontrado para a conta.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
    /**
     * Consultar consolidado por conta e data
     * @param data Data do consolidado no formato ISO 8601.
     * @param accountId Identificador da conta consultada.
     * @returns ConsolidadoDiarioResponse Consolidado da data consultado com sucesso.
     * @throws ApiError
     */
    public static consultarConsolidadoPorData(
        data: string,
        accountId: string,
    ): CancelablePromise<ConsolidadoDiarioResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/consolidado/{data}',
            path: {
                'data': data,
            },
            query: {
                'accountId': accountId,
            },
            errors: {
                400: `Data ou conta inválida.`,
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para consultar o consolidado.`,
                404: `Consolidado da data não encontrado para a conta.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
}
