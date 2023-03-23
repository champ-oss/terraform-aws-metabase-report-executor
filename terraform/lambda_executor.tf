module "lambda_executor" {
  source             = "github.com/champ-oss/terraform-aws-lambda.git?ref=v1.0.114-72d2e3f"
  git                = var.git
  name               = "executor"
  sync_image         = true
  sync_source_repo   = "champtitles/metabase-report-executor"
  ecr_name           = "${var.git}-executor"
  ecr_tag            = module.hash.hash
  tags               = merge(local.tags, var.tags)
  memory_size        = 256
  enable_vpc         = true
  vpc_id             = var.vpc_id
  private_subnet_ids = var.private_subnet_ids
  environment = {
    METABASE_URL      = var.metabase_url
    METABASE_USERNAME = var.metabase_username
    METABASE_PASSWORD = var.metabase_password
    METABASE_CARD_ID  = var.metabase_card_id
  }
}