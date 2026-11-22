output "id" {
  value       = azurerm_container_registry.this.id
  description = "ID do ACR."
}

output "login_server" {
  value       = azurerm_container_registry.this.login_server
  description = "FQDN do registry."
}
