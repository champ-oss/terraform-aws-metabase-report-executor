output "metabase_host" {
  description = "Metabase Host"
  value       = "https://${local.name}.${data.aws_route53_zone.this.name}"
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