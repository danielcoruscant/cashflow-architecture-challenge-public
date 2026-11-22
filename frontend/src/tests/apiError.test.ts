import { describe, expect, it } from 'vitest';
import { getApiErrorMessage } from '../shared/apiError';

describe('getApiErrorMessage', () => {
  it('prefere a mensagem retornada pela API', () => {
    expect(getApiErrorMessage({ body: { codigo: 'ERRO', mensagem: 'Serviço indisponível' } }))
      .toBe('Serviço indisponível');
  });

  it('usa a mensagem de erros JavaScript', () => {
    expect(getApiErrorMessage(new Error('Falha de rede'))).toBe('Falha de rede');
  });

  it('fornece mensagem para erros sem detalhe', () => {
    expect(getApiErrorMessage(null)).toBe('Ocorreu um erro inesperado. Tente novamente.');
  });

  it('ignora corpo da API quando a mensagem não é texto', () => {
    expect(getApiErrorMessage({ body: { mensagem: 42 } }))
      .toBe('Ocorreu um erro inesperado. Tente novamente.');
  });
});
