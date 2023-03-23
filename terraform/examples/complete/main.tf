terraform {
  backend "s3" {}
}

provider "aws" {
  region = "us-east-2"
}

locals {
  git  = "terraform-aws-metabase-report-executor"
  name = "metabase-report-executor"
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
  domain_name       = "${local.name}.${data.aws_route53_zone.this.name}"
  create_wildcard   = false
  zone_id           = data.aws_route53_zone.this.zone_id
  enable_validation = true
}

module "metabase" {
  source              = "github.com/champ-oss/terraform-aws-metabase.git?ref=33c32cb500233bd64580aef5fb72b31236dcac5b"
  id                  = local.name
  public_subnet_ids   = data.aws_subnets.public.ids
  private_subnet_ids  = data.aws_subnets.private.ids
  vpc_id              = data.aws_vpcs.this.ids[0]
  domain              = "${local.name}.${data.aws_route53_zone.this.name}"
  certificate_arn     = module.acm.arn
  zone_id             = data.aws_route53_zone.this.zone_id
  protect             = false
  https_egress_only   = false
  ingress_cidr_blocks = ["10.0.0.0/8"]

  tags = {
    git     = local.git
    cost    = "metabase"
    creator = "terraform"
  }
}

module "this" {
  source             = "../../"
  private_subnet_ids = data.aws_subnets.private.ids
  vpc_id             = data.aws_vpcs.this.ids[0]
}