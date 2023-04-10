resource "aws_cloudwatch_metric_alarm" "executor" {
  count               = var.enable_alarms ? 1 : 0
  alarm_name          = "${module.lambda_executor.function_name}-Errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.metric_evaluation_periods
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = var.metric_period
  statistic           = "Sum"
  threshold           = 1
  alarm_actions       = [aws_sns_topic.this.arn]
  ok_actions          = [aws_sns_topic.this.arn]
  treat_missing_data  = var.treat_missing_data
  tags                = merge(local.tags, var.tags)

  dimensions = {
    FunctionName = module.lambda_executor.function_name
  }
}

resource "aws_cloudwatch_metric_alarm" "notifier" {
  count               = var.enable_alarms ? 1 : 0
  alarm_name          = "${module.lambda_notifier.function_name}-Errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.metric_evaluation_periods
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = var.metric_period
  statistic           = "Sum"
  threshold           = 1
  alarm_actions       = [aws_sns_topic.this.arn]
  ok_actions          = [aws_sns_topic.this.arn]
  treat_missing_data  = var.treat_missing_data
  tags                = merge(local.tags, var.tags)

  dimensions = {
    FunctionName = module.lambda_notifier.function_name
  }
}
