module "lambda_executor" {
  source              = "github.com/champ-oss/terraform-aws-lambda.git?ref=v1.0.119-2052713"
  git                 = var.git
  name                = "card-${var.metabase_card_id}-executor-${random_string.this.result}"
  sync_image          = true
  sync_source_repo    = "champtitles/metabase-report-executor"
  ecr_name            = "${var.git}-executor-${random_string.this.result}"
  ecr_tag             = module.hash.hash
  tags                = merge(local.tags, var.tags)
  memory_size         = 256
  enable_vpc          = true
  vpc_id              = var.vpc_id
  private_subnet_ids  = var.private_subnet_ids
  enable_cw_event     = var.enable_schedule
  schedule_expression = var.schedule_expression
  timeout             = var.timeout
  environment = {
    BUCKET                = module.s3.bucket
    METABASE_URL          = var.metabase_url
    METABASE_USERNAME     = var.metabase_username
    METABASE_PASSWORD_KMS = var.metabase_password_kms
    METABASE_CARD_ID      = var.metabase_card_id
    METABASE_DEVICE_UUID  = random_uuid.this.result
    JAVA_TOOL_OPTIONS     = "-Djdk.httpclient.keepalive.timeout=5"
  }
}

resource "aws_iam_role_policy_attachment" "lambda_executor" {
  policy_arn = aws_iam_policy.this.arn
  role       = module.lambda_executor.role_name
}