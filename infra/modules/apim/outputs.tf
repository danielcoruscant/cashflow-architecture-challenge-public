output "id" {
  value       = azurerm_api_management.this.id
  description = "ID do APIM."
}

output "private_ip_addresses" {
  value       = azurerm_api_management.this.private_ip_addresses
  description = "VIP privado do APIM para o backend do Application Gateway."
}

output "gateway_url" {
  value       = azurerm_api_management.this.gateway_url
  description = "URL lógica do gateway do APIM."
}
