variable "name" {
  type        = string
  description = "Nome do Static Web App."
}

variable "location" {
  type        = string
  description = "Região de controle do Static Web App."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group."
}

variable "tags" {
  type        = map(string)
  description = "Tags do recurso."
  default     = {}
}
