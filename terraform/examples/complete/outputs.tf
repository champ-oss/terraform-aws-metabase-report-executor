output "aws_region" {
  description = "AWS Region"
  value       = module.this.aws_region
}

output "bucket" {
  description = "S3 bucket name"
  value       = module.this.bucket
}

output "executor_function_name" {
  description = "Lambda function name for executor"
  value       = module.this.executor_function_name
}

output "lambda_executor_cloudwatch_log_group" {
  description = "CloudWatch log group"
  value       = module.this.lambda_executor_cloudwatch_log_group
}

output "lambda_notifier_cloudwatch_log_group" {
  description = "CloudWatch log group"
  value       = module.this.lambda_notifier_cloudwatch_log_group
}

output "metabase_device_uuid" {
  description = "Used as a cookie for metabase requests"
  value       = module.this.metabase_device_uuid
}

output "metabase_url" {
  description = "Metabase url"
  value       = local.metabase_url
}

output "metabase_username" {
  description = "Metabase username"
  value       = local.metabase_email
}

output "metabase_password_kms" {
  description = "Metabase password"
  sensitive   = true
  value       = aws_kms_ciphertext.metabase_password.ciphertext_blob
}