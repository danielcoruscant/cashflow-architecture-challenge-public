package br.com.cashflow.consolidado.domain.repository;

import br.com.cashflow.consolidado.domain.inbox.MensagemProcessada;

public interface MensagemProcessadaRepository {

    boolean existePorId(String accountId, String id);

    void salvar(MensagemProcessada mensagemProcessada);
}
