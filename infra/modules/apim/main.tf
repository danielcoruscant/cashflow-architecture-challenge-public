locals {
  lancamentos_spec = yamldecode(var.lancamentos_openapi)
  consolidado_spec = yamldecode(var.consolidado_openapi)
  openapi = {
    openapi = "3.0.3"
    info = {
      title       = "Cash Flow API"
      version     = "v1"
      description = "Contratos de Lançamentos e Consolidado publicados sob o prefixo /api/v1."
    }
    servers = [{ url = "/api/v1" }]
    paths = merge(
      local.lancamentos_spec.paths,
      local.consolidado_spec.paths
    )
    components = {
      schemas = merge(
        try(local.lancamentos_spec.components.schemas, {}),
        try(local.consolidado_spec.components.schemas, {})
      )
      parameters = merge(
        try(local.lancamentos_spec.components.parameters, {}),
        try(local.consolidado_spec.components.parameters, {})
      )
      securitySchemes = {
        bearerAuth = {
          type = "oauth2"
          flows = {
            authorizationCode = {
              authorizationUrl = local.lancamentos_spec.components.securitySchemes.bearerAuth.flows.authorizationCode.authorizationUrl
              tokenUrl         = local.lancamentos_spec.components.securitySchemes.bearerAuth.flows.authorizationCode.tokenUrl
              scopes = merge(
                local.lancamentos_spec.components.securitySchemes.bearerAuth.flows.authorizationCode.scopes,
                local.consolidado_spec.components.securitySchemes.bearerAuth.flows.authorizationCode.scopes
              )
            }
          }
        }
      }
    }
    tags = concat(
      try(local.lancamentos_spec.tags, []),
      try(local.consolidado_spec.tags, [])
    )
  }
}

resource "azurerm_api_management" "this" {
  name                 = var.name
  location             = var.location
  resource_group_name  = var.resource_group_name
  publisher_name       = var.publisher_name
  publisher_email      = var.publisher_email
  sku_name             = "Developer_1"
  virtual_network_type = "Internal"
  tags                 = var.tags

  virtual_network_configuration {
    subnet_id = var.subnet_id
  }
}

resource "azurerm_api_management_named_value" "issuer" {
  name                = "entra-issuer"
  display_name        = "entra-issuer"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = var.entra_issuer
  secret              = false
}

resource "azurerm_api_management_named_value" "openid_configuration" {
  name                = "entra-openid-configuration-url"
  display_name        = "entra-openid-configuration-url"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = var.entra_openid_configuration_url
  secret              = false
}

resource "azurerm_api_management_named_value" "lancamentos_audience" {
  name                = "lancamentos-api-audience"
  display_name        = "lancamentos-api-audience"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = var.lancamentos_audience
  secret              = false
}

resource "azurerm_api_management_named_value" "consolidado_audience" {
  name                = "consolidado-api-audience"
  display_name        = "consolidado-api-audience"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = var.consolidado_audience
  secret              = false
}

resource "azurerm_api_management_named_value" "lancamentos_backend" {
  name                = "lancamentos-backend-url"
  display_name        = "lancamentos-backend-url"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = "https://${var.lancamentos_backend_fqdn}/api/v1"
  secret              = false
}

resource "azurerm_api_management_named_value" "consolidado_backend" {
  name                = "consolidado-backend-url"
  display_name        = "consolidado-backend-url"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = "https://${var.consolidado_backend_fqdn}/api/v1"
  secret              = false
}

resource "azurerm_api_management_named_value" "frontend_origin" {
  name                = "frontend-origin"
  display_name        = "frontend-origin"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.this.name
  value               = var.frontend_origin
  secret              = false
}

resource "azurerm_api_management_api" "cashflow" {
  name                  = "cashflow"
  resource_group_name   = var.resource_group_name
  api_management_name   = azurerm_api_management.this.name
  revision              = "1"
  display_name          = "Cash Flow APIs"
  path                  = "api/v1"
  protocols             = ["https"]
  service_url           = "https://${var.lancamentos_backend_fqdn}/api/v1"
  subscription_required = false

  import {
    content_format = "openapi+json"
    content_value  = jsonencode(local.openapi)
  }
}

resource "azurerm_api_management_api_policy" "cashflow" {
  api_name            = azurerm_api_management_api.cashflow.name
  api_management_name = azurerm_api_management.this.name
  resource_group_name = var.resource_group_name
  xml_content         = file("${path.module}/policies/cashflow.xml")

  depends_on = [
    azurerm_api_management_named_value.issuer,
    azurerm_api_management_named_value.openid_configuration,
    azurerm_api_management_named_value.lancamentos_audience,
    azurerm_api_management_named_value.consolidado_audience,
    azurerm_api_management_named_value.lancamentos_backend,
    azurerm_api_management_named_value.consolidado_backend,
    azurerm_api_management_named_value.frontend_origin
  ]
}
