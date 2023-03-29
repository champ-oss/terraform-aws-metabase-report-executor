module "hash" {
  source = "github.com/champ-oss/terraform-git-hash.git?ref=v1.0.11-d044d32"
  path   = "${path.module}/.."
}

output "hash" {
  value = module.hash.hash
}