package br.com.cashflow.lancamentos.domain.service;

import br.com.cashflow.lancamentos.domain.model.Lancamento;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LancamentoDomainService {

    public Lancamento criarCompensacao(Lancamento original) {
        return new Lancamento(
                UUID.randomUUID().toString(),
                original.accountId(),
                original.tipo().oposto(),
                original.valor(),
                original.data(),
                original.correlationId(),
                original.id(),
                original.id());
    }
}
