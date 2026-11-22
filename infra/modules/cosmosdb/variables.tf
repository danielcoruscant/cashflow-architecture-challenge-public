variable "name" {
  type        = string
  description = "Nome global da conta Cosmos DB."
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
  description = "Tags dos recursos."
  default     = {}
}
