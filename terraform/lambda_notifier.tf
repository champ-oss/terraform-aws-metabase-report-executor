module "lambda_notifier" {
  source             = "github.com/champ-oss/terraform-aws-lambda.git?ref=v1.0.119-2052713"
  git                = var.git
  name               = "card-${var.metabase_card_id}-notifier-${random_string.this.result}"
  sync_image         = true
  sync_source_repo   = "champtitles/metabase-report-notifier"
  ecr_name           = "${var.git}-notifier-${random_string.this.result}"
  ecr_tag            = module.hash.hash
  tags               = merge(local.tags, var.tags)
  memory_size        = 256
  enable_vpc         = true
  vpc_id             = var.vpc_id
  private_subnet_ids = var.private_subnet_ids
  timeout            = var.timeout
  environment = {
    BUCKET                  = module.s3.bucket
    SMTP_HOST               = var.smtp_host
    SMTP_PORT               = var.smtp_port
    SMTP_USER               = var.smtp_user
    SMTP_PASSWORD_KMS       = var.smtp_password_kms
    FROM_ADDRESS            = var.from_address
    RECIPIENTS              = join(",", var.recipients)
    METABASE_CARD_ID        = var.metabase_card_id
    NAME                    = var.name
    SIZE_LIMIT_BYTES        = var.size_limit_bytes
    BODY                    = var.body
    INCLUDE_CARD_IN_SUBJECT = var.include_card_in_subject
  }
}

resource "aws_lambda_permission" "lambda_notifier" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda_notifier.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.this.arn
}

resource "aws_iam_role_policy_attachment" "lambda_notifier" {
  policy_arn = aws_iam_policy.this.arn
  role       = module.lambda_notifier.role_name
}