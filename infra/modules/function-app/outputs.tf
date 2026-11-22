output "function_app_id" {
  value       = azurerm_function_app_flex_consumption.this.id
  description = "ID da Function App."
}

output "function_app_name" {
  value       = azurerm_function_app_flex_consumption.this.name
  description = "Nome da Function App."
}

output "storage_account_id" {
  value       = azurerm_storage_account.host.id
  description = "ID do Storage de host."
}
