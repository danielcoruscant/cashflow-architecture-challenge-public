resource "random_string" "suffix" {
  length  = 5
  upper   = false
  special = false
}

locals {
  suffix = random_string.suffix.result
  names = {
    resource_group     = "rg-cashflow-prod-${local.suffix}"
    network            = "vnet-cashflow-prod-${local.suffix}"
    workspace          = "law-cashflow-prod-${local.suffix}"
    insights           = "appi-cashflow-prod-${local.suffix}"
    cosmos             = "cosmos-cashflow-prod-${local.suffix}"
    servicebus         = "sb-cashflow-prod-${local.suffix}"
    registry           = "cashflow${local.suffix}"
    aca_environment    = "cae-cashflow-prod-${local.suffix}"
    lancamentos        = "ca-lancamentos-prod-${local.suffix}"
    consolidado        = "ca-consolidado-prod-${local.suffix}"
    function           = "func-cdc-prod-${local.suffix}"
    function_plan      = "asp-cdc-prod-${local.suffix}"
    storage            = "stcashflow${local.suffix}"
    function_container = "function-host"
    apim               = "apim-cashflow-prod-${local.suffix}"
    app_gateway        = "agw-cashflow-prod-${local.suffix}"
    public_ip          = "pip-cashflow-prod-${local.suffix}"
    waf_policy         = "waf-cashflow-prod-${local.suffix}"
    static_web_app     = "swa-cashflow-prod-${local.suffix}"
  }
  cosmos_data_contributor_role = "${module.cosmosdb.account_id}/sqlRoleDefinitions/00000000-0000-0000-0000-000000000002"
  acr_pull_role_id             = "/subscriptions/${data.azurerm_client_config.current.subscription_id}/providers/Microsoft.Authorization/roleDefinitions/7f951dda-4ed3-4680-a7ca-43fe172d538d"
  apim_backend_host_name       = "${local.names.apim}.azure-api.net"
}

data "azurerm_client_config" "current" {}

module "resource_group" {
  source   = "../../modules/resource-group"
  name     = local.names.resource_group
  location = var.location
  tags     = var.tags
}

module "network" {
  source              = "../../modules/network"
  name                = local.names.network
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

module "monitoring" {
  source                    = "../../modules/monitoring"
  workspace_name            = local.names.workspace
  application_insights_name = local.names.insights
  location                  = var.location
  resource_group_name       = module.resource_group.name
  retention_in_days         = 30
  daily_quota_gb            = 1
  tags                      = var.tags
}

module "cosmosdb" {
  source              = "../../modules/cosmosdb"
  name                = local.names.cosmos
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

module "servicebus" {
  source              = "../../modules/servicebus"
  name                = local.names.servicebus
  location            = var.location
  resource_group_name = module.resource_group.name
  max_delivery_count  = 10
  tags                = var.tags
}

module "acr" {
  source              = "../../modules/acr"
  name                = local.names.registry
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "lancamentos" {
  name                = "id-lancamentos-prod-${local.suffix}"
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "consolidado" {
  name                = "id-consolidado-prod-${local.suffix}"
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "function" {
  name                = "id-cdc-publisher-prod-${local.suffix}"
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "application_gateway" {
  name                = "id-appgw-keyvault-prod-${local.suffix}"
  location            = var.location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}

resource "azurerm_role_assignment" "application_gateway_certificate_reader" {
  name                 = uuidv5("url", "${azurerm_user_assigned_identity.application_gateway.principal_id}-keyvault-cert")
  scope                = var.application_gateway_key_vault_id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.application_gateway.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_cosmosdb_sql_role_assignment" "lancamentos" {
  name                = uuidv5("url", "${azurerm_user_assigned_identity.lancamentos.principal_id}-fluxocaixa")
  resource_group_name = module.resource_group.name
  account_name        = module.cosmosdb.account_name
  scope               = "${module.cosmosdb.account_id}/dbs/fluxocaixa"
  role_definition_id  = local.cosmos_data_contributor_role
  principal_id        = azurerm_user_assigned_identity.lancamentos.principal_id

  depends_on = [module.cosmosdb]
}

resource "azurerm_cosmosdb_sql_role_assignment" "consolidado" {
  name                = uuidv5("url", "${azurerm_user_assigned_identity.consolidado.principal_id}-consolidado")
  resource_group_name = module.resource_group.name
  account_name        = module.cosmosdb.account_name
  scope               = "${module.cosmosdb.account_id}/dbs/consolidado"
  role_definition_id  = local.cosmos_data_contributor_role
  principal_id        = azurerm_user_assigned_identity.consolidado.principal_id

  depends_on = [module.cosmosdb]
}

resource "azurerm_cosmosdb_sql_role_assignment" "function" {
  name                = uuidv5("url", "${azurerm_user_assigned_identity.function.principal_id}-fluxocaixa-function")
  resource_group_name = module.resource_group.name
  account_name        = module.cosmosdb.account_name
  scope               = "${module.cosmosdb.account_id}/dbs/fluxocaixa"
  role_definition_id  = local.cosmos_data_contributor_role
  principal_id        = azurerm_user_assigned_identity.function.principal_id

  depends_on = [module.cosmosdb]
}

resource "azurerm_role_assignment" "function_servicebus_sender" {
  name                 = uuidv5("url", "${azurerm_user_assigned_identity.function.principal_id}-lancamentos-topic-sender")
  scope                = module.servicebus.topic_id
  role_definition_name = "Azure Service Bus Data Sender"
  principal_id         = azurerm_user_assigned_identity.function.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "consolidado_servicebus_receiver" {
  name                 = uuidv5("url", "${azurerm_user_assigned_identity.consolidado.principal_id}-consolidado-sub-receiver")
  scope                = module.servicebus.topic_id
  role_definition_name = "Azure Service Bus Data Receiver"
  principal_id         = azurerm_user_assigned_identity.consolidado.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "lancamentos_acr_pull" {
  name               = uuidv5("url", "${azurerm_user_assigned_identity.lancamentos.principal_id}-acr-pull")
  scope              = module.acr.id
  role_definition_id = local.acr_pull_role_id
  principal_id       = azurerm_user_assigned_identity.lancamentos.principal_id
  principal_type     = "ServicePrincipal"
}

resource "azurerm_role_assignment" "consolidado_acr_pull" {
  name               = uuidv5("url", "${azurerm_user_assigned_identity.consolidado.principal_id}-acr-pull")
  scope              = module.acr.id
  role_definition_id = local.acr_pull_role_id
  principal_id       = azurerm_user_assigned_identity.consolidado.principal_id
  principal_type     = "ServicePrincipal"
}

module "container_apps" {
  source                                 = "../../modules/container-apps"
  environment_name                       = local.names.aca_environment
  lancamentos_name                       = local.names.lancamentos
  consolidado_name                       = local.names.consolidado
  resource_group_name                    = module.resource_group.name
  location                               = var.location
  infrastructure_subnet_id               = module.network.container_apps_subnet_id
  log_analytics_workspace_id             = module.monitoring.workspace_id
  registry_login_server                  = module.acr.login_server
  lancamentos_identity_id                = azurerm_user_assigned_identity.lancamentos.id
  consolidado_identity_id                = azurerm_user_assigned_identity.consolidado.id
  lancamentos_image                      = var.lancamentos_image
  consolidado_image                      = var.consolidado_image
  cosmos_endpoint                        = module.cosmosdb.endpoint
  servicebus_fqdn                        = module.servicebus.fully_qualified_namespace
  application_insights_connection_string = module.monitoring.application_insights_connection_string
  minimum_replicas                       = 2
  maximum_replicas                       = 10
  tags                                   = var.tags
  depends_on = [
    azurerm_role_assignment.lancamentos_acr_pull,
    azurerm_role_assignment.consolidado_acr_pull
  ]
}

module "function_app" {
  source                                 = "../../modules/function-app"
  name                                   = local.names.function
  service_plan_name                      = local.names.function_plan
  storage_account_name                   = local.names.storage
  storage_container_name                 = local.names.function_container
  location                               = var.location
  resource_group_name                    = module.resource_group.name
  virtual_network_id                     = module.network.virtual_network_id
  function_integration_subnet_id         = module.network.function_integration_subnet_id
  private_endpoint_subnet_id             = module.network.private_endpoint_subnet_id
  identity_id                            = azurerm_user_assigned_identity.function.id
  identity_client_id                     = azurerm_user_assigned_identity.function.client_id
  identity_principal_id                  = azurerm_user_assigned_identity.function.principal_id
  cosmos_endpoint                        = module.cosmosdb.endpoint
  servicebus_fqdn                        = module.servicebus.fully_qualified_namespace
  application_insights_connection_string = module.monitoring.application_insights_connection_string
  java_version                           = "25"
  tags                                   = var.tags
}

module "apim" {
  source                         = "../../modules/apim"
  name                           = local.names.apim
  location                       = var.location
  resource_group_name            = module.resource_group.name
  subnet_id                      = module.network.api_management_subnet_id
  publisher_name                 = var.publisher_name
  publisher_email                = var.publisher_email
  entra_issuer                   = var.entra_issuer
  entra_openid_configuration_url = var.entra_openid_configuration_url
  lancamentos_audience           = var.lancamentos_audience
  consolidado_audience           = var.consolidado_audience
  lancamentos_backend_fqdn       = module.container_apps.lancamentos_fqdn
  consolidado_backend_fqdn       = module.container_apps.consolidado_fqdn
  frontend_origin                = "https://${module.static_web_app.default_host_name}"
  lancamentos_openapi            = file("${path.root}/../../../contracts/lancamentos-api/openapi.yaml")
  consolidado_openapi            = file("${path.root}/../../../contracts/consolidado-api/openapi.yaml")
  tags                           = var.tags
}

module "application_gateway" {
  source                          = "../../modules/application-gateway"
  name                            = local.names.app_gateway
  public_ip_name                  = local.names.public_ip
  waf_policy_name                 = local.names.waf_policy
  location                        = var.location
  resource_group_name             = module.resource_group.name
  subnet_id                       = module.network.application_gateway_subnet_id
  apim_private_ip                 = module.apim.private_ip_addresses[0]
  apim_backend_host_name          = local.apim_backend_host_name
  api_domain_name                 = var.api_domain_name
  key_vault_identity_id           = azurerm_user_assigned_identity.application_gateway.id
  key_vault_certificate_secret_id = var.application_gateway_certificate_secret_id
  tags                            = var.tags
  depends_on                      = [azurerm_role_assignment.application_gateway_certificate_reader]
}

module "static_web_app" {
  source              = "../../modules/static-web-app"
  name                = local.names.static_web_app
  location            = var.static_web_app_location
  resource_group_name = module.resource_group.name
  tags                = var.tags
}
