variable "alarms_email" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription#endpoint"
  type        = string
  default     = null
}

variable "body" {
  description = "HTML email body"
  type        = string
  default     = "<html></html>"
}

variable "enable_alarms" {
  description = "Enable the CloudWatch metric alarms for failures"
  type        = bool
  default     = false
}

variable "enable_email" {
  description = "Enable sending the email"
  type        = bool
  default     = true
}

variable "enable_schedule" {
  description = "Enable the schedule"
  type        = bool
  default     = true
}

variable "filename_prefix" {
  description = "Prefix for report attachment filename"
  type        = string
  default     = ""
}

variable "from_address" {
  description = "Email address to use as sender"
  type        = string
  default     = ""
}

variable "git" {
  description = "Name of git repository"
  type        = string
  default     = "terraform-aws-metabase-report-executor"
}

variable "git_hash" {
  description = "Which hash of the git code to deploy"
  type        = string
  default     = "latest"
}

variable "include_card_in_subject" {
  description = "Enables Card information to be added to email subject"
  type        = bool
  default     = false
}

variable "kms_key_arn" {
  description = "ARN of KMS key that was used to encrypt sensitive values (to set IAM permissions)"
  type        = string
  default     = "*"
}

variable "metabase_url" {
  description = "Metabase server URL"
  type        = string
}

variable "metabase_username" {
  description = "Metabase username to use to connect"
  type        = string
}

variable "metabase_password_kms" {
  description = "Metabase password to use to connect (KMS encrypted)"
  sensitive   = true
  type        = string
}

variable "metabase_card_id" {
  description = "Metabase card to query"
  type        = string
}

variable "metric_evaluation_periods" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm#evaluation_periods"
  type        = number
  default     = 1
}

variable "metric_period" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm#period"
  type        = number
  default     = 300 # in seconds
}

variable "name" {
  description = "name of the report"
  type        = string
}

variable "private_subnet_ids" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group#subnet_ids"
  type        = list(string)
}

variable "protect" {
  description = "Enables deletion protection on eligible resources"
  type        = bool
  default     = true
}

variable "recipients" {
  description = "List of email address recipients"
  type        = list(string)
  default     = []
}

variable "schedule_expression" {
  description = "schedule expression using cron"
  type        = string
  default     = "cron(0 7 * * ? *)"
}

variable "size_limit_bytes" {
  description = "The maximum allowed size of the file attachment"
  type        = number
  default     = 26214400 # 25 MB
}

variable "smtp_host" {
  description = "SMTP server hostname"
  type        = string
  default     = ""
}

variable "smtp_port" {
  description = "SMTP server port"
  type        = string
  default     = ""
}

variable "smtp_user" {
  description = "SMTP username"
  type        = string
  sensitive   = true
  default     = ""
}

variable "smtp_password_kms" {
  description = "SMTP password (KMS encrypted)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "tags" {
  description = "Map of tags to assign to resources"
  type        = map(string)
  default     = {}
}

variable "timeout" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function#timeout"
  type        = number
  default     = 900
}

variable "treat_missing_data" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm#treat_missing_data"
  type        = string
  default     = "ignore"
}

variable "vpc_id" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group#vpc_id"
  type        = string
}
