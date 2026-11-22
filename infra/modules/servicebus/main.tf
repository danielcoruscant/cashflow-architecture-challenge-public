resource "azurerm_servicebus_namespace" "this" {
  name                          = var.name
  location                      = var.location
  resource_group_name           = var.resource_group_name
  sku                           = "Premium"
  capacity                      = 1
  premium_messaging_partitions  = 1
  local_auth_enabled            = false
  minimum_tls_version           = "1.2"
  public_network_access_enabled = true
  tags                          = var.tags
}

resource "azurerm_servicebus_topic" "lancamentos" {
  name                         = "lancamentos-topic"
  namespace_id                 = azurerm_servicebus_namespace.this.id
  requires_duplicate_detection = false
}

resource "azurerm_servicebus_subscription" "consolidado" {
  name               = "consolidado-sub"
  topic_id           = azurerm_servicebus_topic.lancamentos.id
  max_delivery_count = var.max_delivery_count
  lock_duration      = "PT5M"
  requires_session   = false
}
