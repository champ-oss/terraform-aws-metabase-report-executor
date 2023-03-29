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
  topic_arn = aws_sns_topic.this.arn
  protocol  = "lambda"
  endpoint  = module.lambda_notifier.arn
}