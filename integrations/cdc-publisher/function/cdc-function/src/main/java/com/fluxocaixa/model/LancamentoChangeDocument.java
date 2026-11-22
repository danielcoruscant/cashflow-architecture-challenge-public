package com.fluxocaixa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LancamentoChangeDocument {

    private String id;
    private String accountId;
    private String tipo;
    private BigDecimal valor;
    private String data;
    private String compensaLancamentoId;
    private String correlationId;
    private String causationId;
    private Long timestampEpochSeconds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getCompensaLancamentoId() {
        return compensaLancamentoId;
    }

    public void setCompensaLancamentoId(String compensaLancamentoId) {
        this.compensaLancamentoId = compensaLancamentoId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }

    @JsonProperty("_ts")
    public Long getTimestampEpochSeconds() {
        return timestampEpochSeconds;
    }

    @JsonProperty("_ts")
    public void setTimestampEpochSeconds(Long timestampEpochSeconds) {
        this.timestampEpochSeconds = timestampEpochSeconds;
    }
}
