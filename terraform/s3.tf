module "s3" {
  source  = "github.com/champ-oss/terraform-aws-s3.git?ref=v1.0.36-71b7f67"
  git     = substr(var.git, 0, 33)
  protect = var.protect
  tags    = merge(local.tags, var.tags)
}