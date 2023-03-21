module "lambda_notifier" {
  source             = "github.com/champ-oss/terraform-aws-lambda.git?ref=v1.0.114-72d2e3f"
  git                = var.git
  name               = "notifier"
  sync_image         = true
  sync_source_repo   = "champtitles/metabase-report-notifier"
  ecr_name           = "${var.git}-notifier"
  ecr_tag            = module.hash.hash
  tags               = merge(local.tags, var.tags)
  memory_size        = 256
  enable_vpc         = true
  vpc_id             = var.vpc_id
  private_subnet_ids = var.private_subnet_ids
  environment        = {}
}