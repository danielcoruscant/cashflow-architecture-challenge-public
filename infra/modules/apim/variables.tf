variable "name" {
  type        = string
  description = "Nome global do APIM."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "subnet_id" {
  type        = string
  description = "Subnet privada exclusiva para VNet injection."
}

variable "publisher_name" {
  type        = string
  description = "Contato editorial do APIM."
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
  description = "URL do documento OpenID Connect do tenant Entra."
}

variable "lancamentos_audience" {
  type        = string
  description = "Audience exigida pela API de Lançamentos."
}

variable "consolidado_audience" {
  type        = string
  description = "Audience exigida pela API de Consolidado."
}

variable "lancamentos_backend_fqdn" {
  type        = string
  description = "FQDN privado do Container App de Lançamentos."
}

variable "consolidado_backend_fqdn" {
  type        = string
  description = "FQDN privado do Container App de Consolidado."
}

variable "frontend_origin" {
  type        = string
  description = "Origem HTTPS permitida pela política CORS da SPA."
}

variable "lancamentos_openapi" {
  type        = string
  description = "Contrato OpenAPI versionado do serviço de Lançamentos."
}

variable "consolidado_openapi" {
  type        = string
  description = "Contrato OpenAPI versionado do serviço de Consolidado."
}

variable "tags" {
  type        = map(string)
  description = "Tags do recurso."
  default     = {}
}
