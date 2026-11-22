variable "location" {
  type        = string
  description = "Região de produção."
  default     = "brazilsouth"
}

variable "static_web_app_location" {
  type        = string
  description = "Região de gerenciamento do Static Web App, distinta da região servida globalmente."
  default     = "eastus2"
}

variable "publisher_name" {
  type        = string
  description = "Responsável pelo APIM."
}

variable "publisher_email" {
  type        = string
  description = "E-mail operacional do APIM."
}

variable "entra_issuer" {
  type        = string
  description = "Issuer OIDC do tenant Entra ID."
}

variable "entra_openid_configuration_url" {
  type        = string
  description = "URL do documento OIDC do tenant Entra ID."
}

variable "lancamentos_audience" {
  type        = string
  description = "Audience da API de Lançamentos."
}

variable "consolidado_audience" {
  type        = string
  description = "Audience da API de Consolidado."
}

variable "api_domain_name" {
  type        = string
  description = "FQDN público reservado para o listener HTTPS do Application Gateway."
}

variable "application_gateway_key_vault_id" {
  type        = string
  description = "ID ARM do Key Vault existente que contém o certificado TLS."
}

variable "application_gateway_certificate_secret_id" {
  type        = string
  description = "URI do secret do PFX no Key Vault, preferencialmente sem versão."
}

variable "lancamentos_image" {
  type        = string
  description = "Imagem Linux previamente publicada no ACR para o serviço Lançamentos."
}

variable "consolidado_image" {
  type        = string
  description = "Imagem Linux previamente publicada no ACR para o serviço Consolidado."
}

variable "tags" {
  type        = map(string)
  description = "Tags obrigatórias de governança."
  default = {
    application = "cashflow"
    environment = "prod"
    managed-by  = "terraform"
  }
}
