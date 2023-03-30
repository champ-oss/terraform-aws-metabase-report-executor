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

variable "private_subnet_ids" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group#subnet_ids"
  type        = list(string)
}

variable "protect" {
  description = "Enables deletion protection on eligible resources"
  type        = bool
  default     = true
}

variable "schedule_expression" {
  description = "schedule expression using cron"
  type        = string
  default     = "cron(0 7 * * ? *)"
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

variable "vpc_id" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group#vpc_id"
  type        = string
}
