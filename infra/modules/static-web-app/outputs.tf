output "default_host_name" {
  value       = azurerm_static_web_app.this.default_host_name
  description = "Hostname público da SPA."
}

output "id" {
  value       = azurerm_static_web_app.this.id
  description = "ID do recurso."
}
