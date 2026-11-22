variable "name" {
  type        = string
  description = "Nome global da Function App."
}

variable "service_plan_name" {
  type        = string
  description = "Nome do plano de hospedagem."
}

variable "storage_account_name" {
  type        = string
  description = "Nome global de Storage para host/leases da Function."
}

variable "storage_container_name" {
  type        = string
  description = "Container privado de implantação/host Flex Consumption."
  default     = "function-host"
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "virtual_network_id" {
  type        = string
  description = "Resource ID da VNet associada às zonas DNS privadas."
}

variable "function_integration_subnet_id" {
  type        = string
  description = "Resource ID da subnet delegada à integração VNet da Function Flex Consumption."
}

variable "private_endpoint_subnet_id" {
  type        = string
  description = "Resource ID da subnet dedicada aos Private Endpoints do Storage."
}

variable "identity_id" {
  type        = string
  description = "Resource ID da identidade gerenciada da Function."
}

variable "identity_client_id" {
  type        = string
  description = "Client ID da identidade gerenciada da Function."
}

variable "identity_principal_id" {
  type        = string
  description = "Principal ID da identidade gerenciada da Function."
}

variable "cosmos_endpoint" {
  type        = string
  description = "Endpoint do Cosmos DB."
}

variable "servicebus_fqdn" {
  type        = string
  description = "FQDN do namespace Service Bus."
}

variable "application_insights_connection_string" {
  type        = string
  description = "Connection string do Application Insights."
  sensitive   = true
}

variable "java_version" {
  type        = string
  description = "Versão do runtime Java no Flex Consumption."
  default     = "25"
}

variable "maximum_instance_count" {
  type        = number
  description = "Limite de escala da Function Flex Consumption."
  default     = 50
}

variable "tags" {
  type        = map(string)
  description = "Tags dos recursos."
  default     = {}
}
