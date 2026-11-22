output "resource_group_name" {
  value       = module.resource_group.name
  description = "Resource Group de produção."
}

output "api_public_ip" {
  value       = module.application_gateway.public_ip_address
  description = "IP público do Application Gateway WAF."
}

output "static_web_app_hostname" {
  value       = module.static_web_app.default_host_name
  description = "Hostname público da SPA."
}

output "cosmos_account_name" {
  value       = module.cosmosdb.account_name
  description = "Conta Cosmos DB."
}
