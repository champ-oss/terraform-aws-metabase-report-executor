output "account_id" {
  description = "AWS Account ID"
  value       = data.aws_caller_identity.this.account_id
}

output "aws_region" {
  description = "AWS Region"
  value       = data.aws_region.this.name
}

output "private_subnet_ids" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group#subnet_ids"
  value       = var.private_subnet_ids
}

output "vpc_id" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group#vpc_id"
  value       = var.vpc_id
}