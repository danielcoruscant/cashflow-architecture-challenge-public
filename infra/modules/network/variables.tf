variable "name" {
  type        = string
  description = "Nome da VNet."
}

variable "location" {
  type        = string
  description = "Região Azure."
}

variable "resource_group_name" {
  type        = string
  description = "Resource Group da rede."
}

variable "address_space" {
  type        = string
  description = "CIDR da VNet."
  default     = "10.40.0.0/16"
}

variable "application_gateway_subnet_prefix" {
  type        = string
  description = "Subnet dedicada ao Application Gateway."
  default     = "10.40.0.0/24"
}

variable "api_management_subnet_prefix" {
  type        = string
  description = "Subnet dedicada ao APIM com VNet injection."
  default     = "10.40.1.0/24"
}

variable "container_apps_subnet_prefix" {
  type        = string
  description = "Subnet dedicada ao ambiente interno de Container Apps (mínimo recomendado /23)."
  default     = "10.40.2.0/23"
}

variable "function_integration_subnet_prefix" {
  type        = string
  description = "Subnet /27 dedicada à integração VNet da Function Flex Consumption."
  default     = "10.40.4.0/27"
}

variable "private_endpoint_subnet_prefix" {
  type        = string
  description = "Subnet dedicada aos Private Endpoints de serviços da VNet."
  default     = "10.40.5.0/27"
}

variable "tags" {
  type        = map(string)
  description = "Tags aplicadas à VNet."
  default     = {}
}
