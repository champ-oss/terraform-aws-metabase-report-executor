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

variable "tags" {
  description = "Map of tags to assign to resources"
  type        = map(string)
  default     = {}
}

variable "vpc_id" {
  description = "https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group#vpc_id"
  type        = string
}
