terraform {
  required_version = ">= 1.8.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Configurar externamente com -backend-config ou ARM_* / TF_VAR_*.
  # O backend remoto não foi configurado nem validado nesta entrega.
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

provider "random" {}
