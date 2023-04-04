resource "aws_sns_topic" "this" {
  name_prefix = "${var.git}-s3-card-${var.metabase_card_id}-"
  policy      = data.aws_iam_policy_document.sns.json
}

data "aws_iam_policy_document" "sns" {
  statement {
    principals {
      identifiers = ["s3.amazonaws.com"]
      type        = "Service"
    }
    actions   = ["SNS:Publish"]
    resources = ["*"]
  }
}

resource "aws_sns_topic_subscription" "this" {
  count     = var.enable_email ? 1 : 0
  topic_arn = aws_sns_topic.this.arn
  protocol  = "lambda"
  endpoint  = module.lambda_notifier.arn
}

resource "aws_sns_topic" "alarms" {
  name_prefix = "${var.git}-card-${var.metabase_card_id}-alarms-"
}

resource "aws_sns_topic_subscription" "alarms" {
  count      = var.enable_alarms ? 1 : 0
  depends_on = [aws_sns_topic.alarms]
  topic_arn  = aws_sns_topic.alarms.arn
  protocol   = "email"
  endpoint   = var.alarms_email
}