output "account_id" {
  description = "AWS Account ID"
  value       = data.aws_caller_identity.this.account_id
}

output "aws_region" {
  description = "AWS Region"
  value       = data.aws_region.this.name
}

output "bucket" {
  description = "S3 bucket name"
  value       = module.s3.bucket
}

output "lambda_executor_cloudwatch_log_group" {
  description = "CloudWatch log group"
  value       = module.lambda_executor.cloudwatch_log_group
}

output "lambda_notifier_cloudwatch_log_group" {
  description = "CloudWatch log group"
  value       = module.lambda_notifier.cloudwatch_log_group
}

output "executor_function_name" {
  description = "Lambda function name for executor"
  value       = module.lambda_executor.function_name
}

output "metabase_device_uuid" {
  description = "Used as a cookie for metabase requests"
  value       = random_uuid.this.result
}

output "private_subnet_ids" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group#subnet_ids"
  value       = var.private_subnet_ids
}

output "vpc_id" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group#vpc_id"
  value       = var.vpc_id
}