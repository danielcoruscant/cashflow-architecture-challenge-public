variable "name" {
  type        = string
  description = "Nome do Resource Group."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "tags" {
  type        = map(string)
  description = "Tags do Resource Group."
  default     = {}
}
