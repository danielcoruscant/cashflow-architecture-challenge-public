locals {
  host_storage_private_dns_zones = {
    blob  = "privatelink.blob.core.windows.net"
    file  = "privatelink.file.core.windows.net"
    queue = "privatelink.queue.core.windows.net"
    table = "privatelink.table.core.windows.net"
  }
}

resource "azurerm_storage_account" "host" {
  name                            = var.storage_account_name
  resource_group_name             = var.resource_group_name
  location                        = var.location
  account_tier                    = "Standard"
  account_replication_type        = "ZRS"
  account_kind                    = "StorageV2"
  min_tls_version                 = "TLS1_2"
  https_traffic_only_enabled      = true
  shared_access_key_enabled       = false
  public_network_access_enabled   = false
  allow_nested_items_to_be_public = false
  tags                            = var.tags

  network_rules {
    default_action = "Deny"
    bypass         = ["AzureServices"]
    ip_rules       = []
  }
}

resource "azurerm_storage_container" "deployment" {
  name                  = var.storage_container_name
  storage_account_id    = azurerm_storage_account.host.id
  container_access_type = "private"
}

resource "azurerm_private_dns_zone" "host_storage" {
  for_each            = local.host_storage_private_dns_zones
  name                = each.value
  resource_group_name = var.resource_group_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "host_storage" {
  for_each              = local.host_storage_private_dns_zones
  name                  = "${var.name}-${each.key}-link"
  resource_group_name   = var.resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.host_storage[each.key].name
  virtual_network_id    = var.virtual_network_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_endpoint" "host_storage" {
  for_each            = local.host_storage_private_dns_zones
  name                = "${var.name}-storage-${each.key}"
  location            = var.location
  resource_group_name = var.resource_group_name
  subnet_id           = var.private_endpoint_subnet_id
  tags                = var.tags

  private_service_connection {
    name                           = "${var.name}-storage-${each.key}"
    private_connection_resource_id = azurerm_storage_account.host.id
    subresource_names              = [each.key]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "${var.name}-${each.key}-dns"
    private_dns_zone_ids = [azurerm_private_dns_zone.host_storage[each.key].id]
  }
}

resource "azurerm_service_plan" "this" {
  name                = var.service_plan_name
  location            = var.location
  resource_group_name = var.resource_group_name
  os_type             = "Linux"
  sku_name            = "FC1"
  tags                = var.tags
}

resource "azurerm_function_app_flex_consumption" "this" {
  name                = var.name
  location            = var.location
  resource_group_name = var.resource_group_name
  service_plan_id     = azurerm_service_plan.this.id

  storage_container_type                         = "blobContainer"
  storage_container_endpoint                     = "${azurerm_storage_account.host.primary_blob_endpoint}${azurerm_storage_container.deployment.name}"
  storage_authentication_type                    = "UserAssignedIdentity"
  storage_user_assigned_identity_id              = var.identity_id
  virtual_network_subnet_id                      = var.function_integration_subnet_id
  runtime_name                                   = "java"
  runtime_version                                = var.java_version
  maximum_instance_count                         = var.maximum_instance_count
  instance_memory_in_mb                          = 2048
  https_only                                     = true
  public_network_access_enabled                  = true
  webdeploy_publish_basic_authentication_enabled = false
  tags                                           = var.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [var.identity_id]
  }

  site_config {
    minimum_tls_version = "1.2"
  }

  app_settings = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = var.application_insights_connection_string
    "COSMOS_CONNECTION__accountEndpoint"    = var.cosmos_endpoint
    "COSMOS_CONNECTION__credential"         = "managedidentity"
    "COSMOS_CONNECTION__clientId"           = var.identity_client_id
    "COSMOS_DATABASE"                       = "fluxocaixa"
    "COSMOS_CONTAINER"                      = "lancamentos"
    "COSMOS_LEASE_CONTAINER"                = "leases"
    "SERVICE_BUS_NAMESPACE"                 = var.servicebus_fqdn
  }

  depends_on = [
    azurerm_private_endpoint.host_storage,
    azurerm_private_dns_zone_virtual_network_link.host_storage
  ]
}

resource "azurerm_role_assignment" "storage_blob_data_owner" {
  scope                = azurerm_storage_account.host.id
  role_definition_name = "Storage Blob Data Owner"
  principal_id         = var.identity_principal_id
}

resource "azurerm_role_assignment" "storage_queue_data_contributor" {
  scope                = azurerm_storage_account.host.id
  role_definition_name = "Storage Queue Data Contributor"
  principal_id         = var.identity_principal_id
}

resource "azurerm_role_assignment" "storage_table_data_contributor" {
  scope                = azurerm_storage_account.host.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = var.identity_principal_id
}
