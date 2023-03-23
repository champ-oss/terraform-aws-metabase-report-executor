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