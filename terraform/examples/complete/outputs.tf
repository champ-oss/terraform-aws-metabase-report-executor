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

output "metabase_url" {
  description = "Metabase url"
  value       = local.metabase_url
}

output "metabase_username" {
  description = "Metabase username"
  value       = local.metabase_email
}

output "metabase_password" {
  description = "Metabase password"
  sensitive   = true
  value       = random_password.this.result
}