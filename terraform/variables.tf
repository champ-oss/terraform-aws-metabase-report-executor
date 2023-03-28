variable "git" {
  description = "Name of git repository"
  type        = string
  default     = "terraform-aws-metabase-report-executor"
}

variable "metabase_url" {
  description = "Metabase server URL"
  type        = string
}

variable "metabase_username" {
  description = "Metabase username to use to connect"
  type        = string
}

variable "metabase_password" {
  description = "Metabase password to use to connect"
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
