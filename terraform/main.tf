data "aws_region" "this" {}
data "aws_caller_identity" "this" {}

locals {
  tags = {
    cost    = "shared"
    creator = "terraform"
    git     = var.git
  }
}

module "hash" {
  source   = "github.com/champ-oss/terraform-git-hash.git?ref=v1.0.11-d044d32"
  path     = "${path.module}/.."
  fallback = var.git_hash
}

# Used to uniquely name resources
resource "random_string" "this" {
  length  = 5
  special = false
  upper   = false
  lower   = true
  number  = true
}

# Used as a cookie for metabase requests
resource "random_uuid" "this" {}