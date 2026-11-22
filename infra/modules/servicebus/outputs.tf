output "namespace_id" {
  value       = azurerm_servicebus_namespace.this.id
  description = "ID do namespace."
}

output "namespace_name" {
  value       = azurerm_servicebus_namespace.this.name
  description = "Nome do namespace."
}

output "fully_qualified_namespace" {
  value       = "${azurerm_servicebus_namespace.this.name}.servicebus.windows.net"
  description = "FQDN do namespace usado por conexões autenticadas por Managed Identity."
}

output "topic_id" {
  value       = azurerm_servicebus_topic.lancamentos.id
  description = "Escopo RBAC de publicação."
}

output "subscription_id" {
  value       = azurerm_servicebus_subscription.consolidado.id
  description = "Escopo RBAC do consumidor Consolidado."
}
