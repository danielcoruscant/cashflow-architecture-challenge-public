variable "workspace_name" {
  type        = string
  description = "Nome do Log Analytics Workspace."
}

variable "application_insights_name" {
  type        = string
  description = "Nome do Application Insights."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "retention_in_days" {
  type        = number
  description = "Retenção da telemetria."
  default     = 30
}

variable "daily_quota_gb" {
  type        = number
  description = "Limite diário de ingestão para controle de custo."
  default     = 1
}

variable "tags" {
  type        = map(string)
  description = "Tags dos recursos."
  default     = {}
}
