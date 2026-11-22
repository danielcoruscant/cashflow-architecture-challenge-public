import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { ConsolidadoOverview } from '../features/consolidado/components/ConsolidadoOverview';

const defaults = {
  accountId: 'conta-1',
  isLoading: false,
  error: null,
  selectedDate: '2026-11-20',
  onDateChange: vi.fn(),
};

describe('ConsolidadoOverview', () => {
  it('mostra carregamento', () => {
    render(<ConsolidadoOverview {...defaults} isLoading />);
    expect(screen.getByText('Carregando consolidado…')).toBeInTheDocument();
  });

  it('expõe erros da API', () => {
    render(<ConsolidadoOverview {...defaults} error={{ body: { mensagem: 'Read model indisponível' } }} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Read model indisponível');
  });

  it('mostra valores e permite escolher a data', () => {
    const onDateChange = vi.fn();
    render(<ConsolidadoOverview
      {...defaults}
      onDateChange={onDateChange}
      data={{ accountId: 'conta-1', data: '2026-11-20', creditos: 200, debitos: 80, saldo: 120 }}
    />);

    expect(screen.getByText('R$ 120,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 200,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 80,00')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('atualizado de forma assíncrona');
    fireEvent.change(screen.getByLabelText('Data do consolidado'), { target: { value: '2026-11-19' } });
    expect(onDateChange).toHaveBeenCalledWith('2026-11-19');
  });

  it('indica ausência de consolidado para a data selecionada', () => {
    render(<ConsolidadoOverview {...defaults} />);
    expect(screen.getByText('Nenhum consolidado disponível para esta data.')).toBeInTheDocument();
  });
});
