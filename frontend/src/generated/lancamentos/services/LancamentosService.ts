/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CriarLancamentoRequest } from '../models/CriarLancamentoRequest';
import type { LancamentoResponse } from '../models/LancamentoResponse';
import type { PaginaLancamentosResponse } from '../models/PaginaLancamentosResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class LancamentosService {
    /**
     * Criar lançamento financeiro
     * @param requestBody
     * @returns LancamentoResponse Lançamento criado com sucesso.
     * @throws ApiError
     */
    public static criarLancamento(
        requestBody: CriarLancamentoRequest,
    ): CancelablePromise<LancamentoResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/lancamentos',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Requisição inválida.`,
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para registrar lançamentos.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
    /**
     * Listar lançamentos de uma conta
     * @param accountId Identificador da conta à qual a operação se aplica.
     * @param page Índice da página, começando em zero.
     * @param pageSize Quantidade máxima de itens por página.
     * @returns PaginaLancamentosResponse Página de lançamentos.
     * @throws ApiError
     */
    public static listarLancamentos(
        accountId: string,
        page?: number,
        pageSize: number = 20,
    ): CancelablePromise<PaginaLancamentosResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/lancamentos',
            query: {
                'accountId': accountId,
                'page': page,
                'pageSize': pageSize,
            },
            errors: {
                400: `Requisição inválida.`,
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para consultar lançamentos.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
    /**
     * Consultar lançamento por id
     * @param id Identificador do lançamento.
     * @param accountId Identificador da conta à qual a operação se aplica.
     * @returns LancamentoResponse Lançamento consultado com sucesso.
     * @throws ApiError
     */
    public static consultarLancamento(
        id: string,
        accountId: string,
    ): CancelablePromise<LancamentoResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/lancamentos/{id}',
            path: {
                'id': id,
            },
            query: {
                'accountId': accountId,
            },
            errors: {
                400: `Requisição inválida.`,
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para consultar lançamentos.`,
                404: `Lançamento não encontrado na conta informada.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
    /**
     * Criar lançamento compensatório
     * Cria um novo lançamento de tipo oposto e mesmo valor, sem alterar o lançamento original.
     * @param id Identificador do lançamento original.
     * @param accountId Identificador da conta à qual a operação se aplica.
     * @returns LancamentoResponse Lançamento compensatório criado.
     * @throws ApiError
     */
    public static compensarLancamento(
        id: string,
        accountId: string,
    ): CancelablePromise<LancamentoResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/lancamentos/{id}/compensacao',
            path: {
                'id': id,
            },
            query: {
                'accountId': accountId,
            },
            errors: {
                401: `Token ausente ou inválido.`,
                403: `Usuário sem permissão para registrar lançamentos.`,
                404: `Lançamento original não encontrado na conta informada.`,
                500: `Erro interno ao processar a operação.`,
            },
        });
    }
}
