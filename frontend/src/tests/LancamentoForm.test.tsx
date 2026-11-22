import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LancamentoForm } from '../features/lancamentos/components/LancamentoForm';

const mutate = vi.fn();
const useCreateLancamento = vi.fn();
vi.mock('../features/lancamentos/hooks/useLancamentos', () => ({ useCreateLancamento: () => useCreateLancamento() }));

describe('LancamentoForm', () => {
  beforeEach(() => {
    mutate.mockReset();
    useCreateLancamento.mockReturnValue({ mutate, isPending: false, isError: false, error: null });
  });

  it('envia valores válidos do contrato para o cliente OpenAPI', async () => {
    const user = userEvent.setup();
    render(<LancamentoForm accountId="conta-1" />);
    await user.type(screen.getByLabelText('Valor'), '125.50');
    await user.click(screen.getByRole('button', { name: 'Adicionar lançamento' }));

    await waitFor(() => expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ accountId: 'conta-1', tipo: 'CREDITO', valor: 125.5, data: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/) }),
      expect.any(Object),
    ));
  });

  it('não envia um valor vazio e apresenta erro de validação', async () => {
    render(<LancamentoForm accountId="conta-1" />);
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar lançamento' }));

    expect(await screen.findByText('Informe um valor.')).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
  });

  it('exibe confirmação e limpa o formulário após sucesso', async () => {
    const user = userEvent.setup();
    mutate.mockImplementation((_values, options) => options.onSuccess());
    render(<LancamentoForm accountId="conta-1" />);
    await user.type(screen.getByLabelText('Valor'), '25');
    await user.click(screen.getByRole('button', { name: 'Adicionar lançamento' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Lançamento registrado.');
  });

  it('mostra erro retornado pela API', () => {
    useCreateLancamento.mockReturnValue({
      mutate,
      isPending: false,
      isError: true,
      error: { body: { mensagem: 'Persistência ainda não implementada.' } },
    });
    render(<LancamentoForm accountId="conta-1" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Persistência ainda não implementada.');
  });
});
