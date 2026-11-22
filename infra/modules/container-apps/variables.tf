variable "environment_name" {
  type        = string
  description = "Nome do Container Apps Environment."
}

variable "lancamentos_name" {
  type        = string
  description = "Nome do Container App de Lançamentos."
}

variable "consolidado_name" {
  type        = string
  description = "Nome do Container App de Consolidado."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "infrastructure_subnet_id" {
  type        = string
  description = "Subnet delegada ao Container Apps Environment."
}

variable "log_analytics_workspace_id" {
  type        = string
  description = "Workspace para logs do ambiente."
}

variable "registry_login_server" {
  type        = string
  description = "FQDN do ACR."
}

variable "lancamentos_identity_id" {
  type        = string
  description = "Resource ID da identidade do serviço Lançamentos."
}

variable "consolidado_identity_id" {
  type        = string
  description = "Resource ID da identidade do serviço Consolidado."
}

variable "lancamentos_image" {
  type        = string
  description = "Imagem Linux pré-publicada para Lançamentos."
}

variable "consolidado_image" {
  type        = string
  description = "Imagem Linux pré-publicada para Consolidado."
}

variable "cosmos_endpoint" {
  type        = string
  description = "Endpoint da conta Cosmos DB."
}

variable "servicebus_fqdn" {
  type        = string
  description = "Namespace Service Bus, sem protocolo."
}

variable "application_insights_connection_string" {
  type        = string
  description = "Connection string do Application Insights."
  sensitive   = true
}

variable "container_port" {
  type        = number
  description = "Porta HTTP dos serviços."
  default     = 8080
}

variable "minimum_replicas" {
  type        = number
  description = "Réplicas mínimas para disponibilidade básica; ajustar após teste de carga."
  default     = 2
}

variable "maximum_replicas" {
  type        = number
  description = "Limite superior de escala horizontal."
  default     = 10
}

variable "concurrent_requests_per_replica" {
  type        = number
  description = "Limiar de concorrência HTTP para escala."
  default     = 50
}

variable "cpu" {
  type        = number
  description = "CPU por container em vCPU."
  default     = 0.5
}

variable "memory" {
  type        = string
  description = "Memória por container."
  default     = "1Gi"
}

variable "tags" {
  type        = map(string)
  description = "Tags dos recursos."
  default     = {}
}
