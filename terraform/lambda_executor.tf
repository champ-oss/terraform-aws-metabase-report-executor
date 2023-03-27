module "lambda_executor" {
  source              = "github.com/champ-oss/terraform-aws-lambda.git?ref=v1.0.114-72d2e3f"
  git                 = var.git
  name                = "executor"
  sync_image          = true
  sync_source_repo    = "champtitles/metabase-report-executor"
  ecr_name            = "${var.git}-executor"
  ecr_tag             = module.hash.hash
  tags                = merge(local.tags, var.tags)
  memory_size         = 256
  enable_vpc          = true
  vpc_id              = var.vpc_id
  private_subnet_ids  = var.private_subnet_ids
  enable_cw_event     = true
  schedule_expression = var.schedule_expression
  environment = {
    BUCKET            = module.s3.bucket
    METABASE_URL      = var.metabase_url
    METABASE_USERNAME = var.metabase_username
    METABASE_PASSWORD = var.metabase_password
    METABASE_CARD_ID  = var.metabase_card_id
    JAVA_TOOL_OPTIONS = "-Djdk.httpclient.keepalive.timeout=5"
  }
}

resource "aws_iam_role_policy_attachment" "lambda_executor" {
  policy_arn = aws_iam_policy.this.arn
  role       = module.lambda_executor.role_name
}