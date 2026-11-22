variable "name" {
  type        = string
  description = "Nome global do Azure Container Registry (5–50 caracteres alfanuméricos)."
}

variable "location" {
  type        = string
  description = "Região Azure."
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
