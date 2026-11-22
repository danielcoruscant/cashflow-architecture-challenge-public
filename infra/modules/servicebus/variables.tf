variable "name" {
  type        = string
  description = "Nome global do namespace Service Bus."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "max_delivery_count" {
  type        = number
  description = "Limite explícito de tentativas antes da DLQ gerenciada pelo Service Bus."
  default     = 10
}

variable "tags" {
  type        = map(string)
  description = "Tags dos recursos."
  default     = {}
}
