resource "azurerm_container_app_environment" "this" {
  name                           = var.environment_name
  location                       = var.location
  resource_group_name            = var.resource_group_name
  logs_destination               = "log-analytics"
  log_analytics_workspace_id     = var.log_analytics_workspace_id
  infrastructure_subnet_id       = var.infrastructure_subnet_id
  internal_load_balancer_enabled = true
  zone_redundancy_enabled        = true
  tags                           = var.tags
}

resource "azurerm_container_app" "lancamentos" {
  name                         = var.lancamentos_name
  container_app_environment_id = azurerm_container_app_environment.this.id
  resource_group_name          = var.resource_group_name
  revision_mode                = "Single"
  identity {
    type         = "UserAssigned"
    identity_ids = [var.lancamentos_identity_id]
  }
  registry {
    server   = var.registry_login_server
    identity = var.lancamentos_identity_id
  }
  ingress {
    external_enabled = true
    target_port      = var.container_port
    transport        = "auto"
    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
  template {
    min_replicas = var.minimum_replicas
    max_replicas = var.maximum_replicas
    container {
      name   = "lancamentos"
      image  = var.lancamentos_image
      cpu    = var.cpu
      memory = var.memory
      env {
        name  = "COSMOS_ENDPOINT"
        value = var.cosmos_endpoint
      }
      env {
        name  = "COSMOS_DATABASE"
        value = "fluxocaixa"
      }
      env {
        name  = "COSMOS_CONTAINER"
        value = "lancamentos"
      }
      env {
        name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        value = var.application_insights_connection_string
      }
      env {
        name  = "APPLICATIONINSIGHTS_ROLE_NAME"
        value = "lancamentos-service"
      }
    }
    http_scale_rule {
      name                = "http-concurrency"
      concurrent_requests = tostring(var.concurrent_requests_per_replica)
    }
  }
  tags = var.tags
}

resource "azurerm_container_app" "consolidado" {
  name                         = var.consolidado_name
  container_app_environment_id = azurerm_container_app_environment.this.id
  resource_group_name          = var.resource_group_name
  revision_mode                = "Single"
  identity {
    type         = "UserAssigned"
    identity_ids = [var.consolidado_identity_id]
  }
  registry {
    server   = var.registry_login_server
    identity = var.consolidado_identity_id
  }
  ingress {
    external_enabled = true
    target_port      = var.container_port
    transport        = "auto"
    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
  template {
    min_replicas = var.minimum_replicas
    max_replicas = var.maximum_replicas
    container {
      name   = "consolidado"
      image  = var.consolidado_image
      cpu    = var.cpu
      memory = var.memory
      env {
        name  = "COSMOS_ENDPOINT"
        value = var.cosmos_endpoint
      }
      env {
        name  = "COSMOS_DATABASE"
        value = "consolidado"
      }
      env {
        name  = "COSMOS_CONTAINER"
        value = "consolidado-diario"
      }
      env {
        name  = "SERVICE_BUS_NAMESPACE"
        value = var.servicebus_fqdn
      }
      env {
        name  = "SERVICE_BUS_TOPIC"
        value = "lancamentos-topic"
      }
      env {
        name  = "SERVICE_BUS_SUBSCRIPTION"
        value = "consolidado-sub"
      }
      env {
        name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        value = var.application_insights_connection_string
      }
      env {
        name  = "APPLICATIONINSIGHTS_ROLE_NAME"
        value = "consolidado-service"
      }
    }
    http_scale_rule {
      name                = "http-concurrency"
      concurrent_requests = tostring(var.concurrent_requests_per_replica)
    }
  }
  tags = var.tags
}
