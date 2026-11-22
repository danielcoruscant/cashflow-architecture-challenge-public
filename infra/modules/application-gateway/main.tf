resource "azurerm_public_ip" "this" {
  name                = var.public_ip_name
  location            = var.location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"
  zones               = ["1", "2", "3"]
  tags                = var.tags
}

resource "azurerm_web_application_firewall_policy" "this" {
  name                = var.waf_policy_name
  location            = var.location
  resource_group_name = var.resource_group_name
  tags                = var.tags

  policy_settings {
    enabled                     = true
    mode                        = "Prevention"
    request_body_check          = true
    max_request_body_size_in_kb = 128
    file_upload_limit_in_mb     = 100
  }

  managed_rules {
    managed_rule_set {
      type    = "OWASP"
      version = "3.2"
    }
  }
}

resource "azurerm_application_gateway" "this" {
  name                = var.name
  location            = var.location
  resource_group_name = var.resource_group_name
  zones               = ["1", "2", "3"]
  firewall_policy_id  = azurerm_web_application_firewall_policy.this.id
  tags                = var.tags

  sku {
    name = "WAF_v2"
    tier = "WAF_v2"
  }

  identity {
    type         = "UserAssigned"
    identity_ids = [var.key_vault_identity_id]
  }

  autoscale_configuration {
    min_capacity = 2
    max_capacity = 5
  }

  gateway_ip_configuration {
    name      = "gateway-ip-configuration"
    subnet_id = var.subnet_id
  }

  frontend_port {
    name = "https-port"
    port = 443
  }

  frontend_ip_configuration {
    name                 = "public-frontend"
    public_ip_address_id = azurerm_public_ip.this.id
  }

  ssl_certificate {
    name                = "api-listener-certificate"
    key_vault_secret_id = var.key_vault_certificate_secret_id
  }

  backend_address_pool {
    name         = "apim-backend"
    ip_addresses = [var.apim_private_ip]
  }

  probe {
    name                = "apim-health"
    protocol            = "Https"
    host                = var.apim_backend_host_name
    path                = "/status-0123456789abcdef"
    interval            = 30
    timeout             = 30
    unhealthy_threshold = 3
    match {
      status_code = ["200-399"]
    }
  }

  backend_http_settings {
    name                  = "apim-https"
    cookie_based_affinity = "Disabled"
    port                  = 443
    protocol              = "Https"
    request_timeout       = 60
    host_name             = var.apim_backend_host_name
    probe_name            = "apim-health"
  }

  http_listener {
    name                           = "api-https-listener"
    frontend_ip_configuration_name = "public-frontend"
    frontend_port_name             = "https-port"
    protocol                       = "Https"
    host_name                      = var.api_domain_name
    ssl_certificate_name           = "api-listener-certificate"
  }

  request_routing_rule {
    name                       = "https-api-route"
    priority                   = 100
    rule_type                  = "Basic"
    http_listener_name         = "api-https-listener"
    backend_address_pool_name  = "apim-backend"
    backend_http_settings_name = "apim-https"
  }
}
