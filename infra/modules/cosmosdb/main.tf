resource "azurerm_cosmosdb_account" "this" {
  name                              = var.name
  location                          = var.location
  resource_group_name               = var.resource_group_name
  offer_type                        = "Standard"
  kind                              = "GlobalDocumentDB"
  automatic_failover_enabled        = false
  public_network_access_enabled     = true
  local_authentication_enabled      = false
  minimal_tls_version               = "Tls12"
  is_virtual_network_filter_enabled = false
  tags                              = var.tags

  capabilities {
    name = "EnableServerless"
  }

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = var.location
    failover_priority = 0
    zone_redundant    = true
  }

  backup {
    type                = "Periodic"
    interval_in_minutes = 240
    retention_in_hours  = 8
  }
}

resource "azurerm_cosmosdb_sql_database" "fluxocaixa" {
  name                = "fluxocaixa"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.this.name
}

resource "azurerm_cosmosdb_sql_database" "consolidado" {
  name                = "consolidado"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.this.name
}

resource "azurerm_cosmosdb_sql_container" "lancamentos" {
  name                = "lancamentos"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.this.name
  database_name       = azurerm_cosmosdb_sql_database.fluxocaixa.name
  partition_key_paths = ["/accountId"]
}

resource "azurerm_cosmosdb_sql_container" "leases" {
  name                = "leases"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.this.name
  database_name       = azurerm_cosmosdb_sql_database.fluxocaixa.name
  partition_key_paths = ["/id"]
}

resource "azurerm_cosmosdb_sql_container" "consolidado_diario" {
  name                = "consolidado-diario"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.this.name
  database_name       = azurerm_cosmosdb_sql_database.consolidado.name
  partition_key_paths = ["/accountId"]
}
