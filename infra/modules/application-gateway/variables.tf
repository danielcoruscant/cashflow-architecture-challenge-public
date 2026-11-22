variable "name" {
  type        = string
  description = "Nome do Application Gateway."
}

variable "public_ip_name" {
  type        = string
  description = "Nome do Public IP Standard."
}

variable "waf_policy_name" {
  type        = string
  description = "Nome da política WAF."
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
  description = "Subnet exclusiva para o Application Gateway."
}

variable "apim_private_ip" {
  type        = string
  description = "VIP privado do APIM."
}

variable "apim_backend_host_name" {
  type        = string
  description = "Hostname default do gateway APIM para SNI/Host."
}

variable "api_domain_name" {
  type        = string
  description = "Hostname público da API, apontado para o Public IP."
}

variable "key_vault_identity_id" {
  type        = string
  description = "Resource ID da identidade que lê o certificado no Key Vault."
}

variable "key_vault_certificate_secret_id" {
  type        = string
  description = "URI do secret do certificado PFX no Key Vault. Preferir URI sem versão para rotação."
}

variable "tags" {
  type        = map(string)
  description = "Tags dos recursos."
  default     = {}
}
