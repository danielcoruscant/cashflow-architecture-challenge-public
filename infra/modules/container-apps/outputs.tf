output "environment_id" {
  value       = azurerm_container_app_environment.this.id
  description = "ID do ambiente Container Apps."
}

output "lancamentos_fqdn" {
  value       = azurerm_container_app.lancamentos.ingress[0].fqdn
  description = "FQDN privado do serviço Lançamentos."
}

output "consolidado_fqdn" {
  value       = azurerm_container_app.consolidado.ingress[0].fqdn
  description = "FQDN privado do serviço Consolidado."
}
