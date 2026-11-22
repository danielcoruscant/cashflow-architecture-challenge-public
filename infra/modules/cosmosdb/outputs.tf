output "account_id" {
  value       = azurerm_cosmosdb_account.this.id
  description = "ID ARM da conta Cosmos DB."
}

output "endpoint" {
  value       = azurerm_cosmosdb_account.this.endpoint
  description = "Endpoint HTTPS do Cosmos DB."
}

output "account_name" {
  value       = azurerm_cosmosdb_account.this.name
  description = "Nome da conta Cosmos DB."
}

output "lancamentos_container_id" {
  value       = azurerm_cosmosdb_sql_container.lancamentos.id
  description = "ID do container do ledger."
}

output "consolidado_container_id" {
  value       = azurerm_cosmosdb_sql_container.consolidado_diario.id
  description = "ID do container da projeção e Inbox."
}
