output "virtual_network_id" {
  value       = azurerm_virtual_network.this.id
  description = "ID da VNet."
}

output "application_gateway_subnet_id" {
  value       = azurerm_subnet.application_gateway.id
  description = "ID da subnet dedicada ao Application Gateway."
}

output "api_management_subnet_id" {
  value       = azurerm_subnet.api_management.id
  description = "ID da subnet dedicada ao APIM."
}

output "container_apps_subnet_id" {
  value       = azurerm_subnet.container_apps.id
  description = "ID da subnet delegada a Container Apps."
}

output "function_integration_subnet_id" {
  value       = azurerm_subnet.function_integration.id
  description = "ID da subnet dedicada à integração VNet da Function Flex Consumption."
}

output "private_endpoint_subnet_id" {
  value       = azurerm_subnet.private_endpoints.id
  description = "ID da subnet dedicada a Private Endpoints."
}
