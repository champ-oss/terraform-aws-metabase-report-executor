terraform {
  backend "s3" {}
}

provider "aws" {
  region = "us-east-2"
}

locals {
  git            = "terraform-aws-metabase-report-executor"
  name           = "metabase-report-executor"
  metabase_email = "test@example.com"
  metabase_host  = "${local.name}.${data.aws_route53_zone.this.name}"
  metabase_url   = "https://${local.metabase_host}"
  tags = {
    git     = local.git
    cost    = "shared"
    creator = "terraform"
  }
}

data "aws_vpcs" "this" {
  tags = {
    purpose = "vega"
  }
}

data "aws_subnets" "private" {
  tags = {
    purpose = "vega"
    Type    = "Private"
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpcs.this.ids[0]]
  }
}

data "aws_subnets" "public" {
  tags = {
    purpose = "vega"
    Type    = "Public"
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpcs.this.ids[0]]
  }
}

data "aws_route53_zone" "this" {
  name = "oss.champtest.net."
}

module "acm" {
  source            = "github.com/champ-oss/terraform-aws-acm.git?ref=v1.0.110-61ad6b7"
  git               = local.git
  domain_name       = local.metabase_host
  create_wildcard   = false
  zone_id           = data.aws_route53_zone.this.zone_id
  enable_validation = true
  tags              = local.tags
}

module "kms" {
  source                  = "github.com/champ-oss/terraform-aws-kms.git?ref=v1.0.30-44f94bf"
  git                     = local.git
  name                    = "alias/${local.git}-test"
  deletion_window_in_days = 7
  account_actions         = []
  tags                    = local.tags
}

module "metabase" {
  source              = "github.com/champ-oss/terraform-aws-metabase.git?ref=v1.0.69-27ec655"
  id                  = local.name
  public_subnet_ids   = data.aws_subnets.public.ids
  private_subnet_ids  = data.aws_subnets.private.ids
  vpc_id              = data.aws_vpcs.this.ids[0]
  domain              = local.metabase_host
  certificate_arn     = module.acm.arn
  zone_id             = data.aws_route53_zone.this.zone_id
  protect             = false
  https_egress_only   = false
  ingress_cidr_blocks = ["0.0.0.0/0"]
  tags                = local.tags
}

resource "random_password" "this" {
  length  = 20
  special = false
}

resource "aws_kms_ciphertext" "this" {
  key_id    = module.kms.key_id
  plaintext = random_password.this.result
}

module "this" {
  source                = "../../"
  private_subnet_ids    = data.aws_subnets.private.ids
  vpc_id                = data.aws_vpcs.this.ids[0]
  metabase_card_id      = "1"
  metabase_url          = local.metabase_url
  metabase_password_kms = aws_kms_ciphertext.this.ciphertext_blob
  metabase_username     = local.metabase_email
  protect               = false
  schedule_expression   = "cron(0 7 * * ? *)"
  tags                  = local.tags
  kms_key_arn           = module.kms.arn
}