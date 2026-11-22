output "public_ip_address" {
  value       = azurerm_public_ip.this.ip_address
  description = "Endereço público do WAF."
}

output "id" {
  value       = azurerm_application_gateway.this.id
  description = "ID do Application Gateway."
}
